package cocktails

import utils.*
import mainargs.{main, arg, ParserForMethods, Flag}

object AppMain:
  def main(args: Array[String]): Unit =
    // println(s"DEBUG: Received args: ${args.mkString(", ")}")
    ParserForMethods(this).runOrExit(args)

  @main(name = "download", doc = "Download Google Sheets and save as local CSVs")
  def download() =
    println("Downloading Google Sheets and saving as local CSVs...")
    val cfg = Config.load()
    require(cfg.sheetId.nonEmpty, "cocktails.sheet-id must be set in cocktail-analyzer.conf")

    val sheet = Sheet.connect(cfg.googleCredsPath, cfg.sheetId)
    val allSheets = sheet.readAllSheets()

    allSheets.foreach { case (title, rows) =>
      val csvContent = Csv.format(rows)
      val safeTitle  = title.replaceAll("[\\\\/:*?\"<>|]", "_")
      // name__sheetTitle.csv
      val name = sheet.readName() 
      val filename   = s"${name}__${safeTitle}.csv"
      val path       = os.pwd / "data" / filename
      os.write.over(path, csvContent)
      println(s"Saved $filename")
    }

  @main(name = "suggestFormat", doc = "Ask LLM to suggest a coktail json format based on the data for each sheet")
  def suggestFormat() =
    println("Asking LLM to suggest a cocktail json format based on the data for each sheet...")

    val cfg = Config.load()
    require(cfg.sheetId.nonEmpty, "cocktails.sheet-id must be set in cocktail-analyzer.conf")

    val llm      = LlmClient.fromConfig(cfg)

    def suggestFormat(data: String): String = {
      val prompt = "Based on the following data from a Google Sheet used to store cocktail-related data, " +
        "propose a robust JSON format to represent cocktail recipes parsed from this sheet. " +
        "Consider that one recipe may span multiple rows or columns. " +
        "Respond with only the JSON format definition."
      
      llm.complete(
        system = "You are a precise data analyst for cocktail datasets.",
        user   = prompt + "\n\n" + data
      )
    }

    def combineFormats(formats: Seq[String]): String = {
      val prompt = "Given the following proposed JSON format definitions for cocktail recipes, " +
        "combine them into a single robust JSON format that can accommodate all the variations. " +
        "Respond with only the combined JSON format definition."

      llm.complete(
        system = "You are a precise data analyst for cocktail datasets.",
        user   = prompt + "\n\n" + formats.mkString("\n\n")
      )
    }
    
    val csvFiles = os.list(os.pwd / "data")
    val suggestedFormats = csvFiles.map { file =>
      val data = os.read(file)
      val suggestion = suggestFormat(data)
      println(s"Suggested format for ${file.last}:\n$suggestion")
      suggestion
    }

    val combinedFormat = combineFormats(suggestedFormats)

    // Save combined format to file
    val outputPath = os.pwd / "data" / "combined_cocktail_format.json"
    os.write.over(outputPath, combinedFormat)

  @main(name = "transform", doc = "Transform the data into the suggested format")
  def transform() =
    println("Transforming the data into the suggested format...")
    val cfg = Config.load()
    require(cfg.sheetId.nonEmpty, "cocktails.sheet-id must be set in cocktail-analyzer.conf") 

    val llm      = LlmClient.fromConfig(cfg)
    val combinedFormat = os.read(os.pwd / "data" / "combined_cocktail_format.json")
    val csvFiles = os.list(os.pwd / "data")
    val transformedData = csvFiles.map { file =>
      val data = os.read(file)
      val prompt = "Based on the following data from a Google Sheet used to store cocktail-related data. The data can be spon multiple rows or columns, and the format may vary considerably within the sheet." +
        "transform the data into the following JSON format. Include only cocktail specs in the first part (and a cocktail has at least two ingredients). clearly mark when you have to make guesses to fill in missing or ambiguous information. " +
        "Include whatever information you can find that is not cocktail specs in the second part. " +
        "Respond with only the transformed JSON data."
      val transformed = llm.complete(
        system = "You are a precise data analyst for cocktail datasets.",
        user   = s"$prompt\n\nFormat Definition:\n$combinedFormat\n\nData:\n$data"
      )
      println(s"Transformed data for ${file.last}:\n$transformed")
      file -> transformed
    }
    // Save transformed data to file
    transformedData.foreach { case (file, jsonData) =>
      val outputPath = os.pwd / "data" / s"${file.baseName}_transformed.json"
      os.write.over(outputPath, jsonData)
    }



  

  @main(name = "demo", doc = "Run the demo analysis")
  def demo() = {
    val cfg = Config.load()
    require(cfg.sheetId.nonEmpty, "cocktails.sheet-id must be set in cocktail-analyzer.conf")

    println(s"Fetching full grid for first sheet of ${cfg.sheetId} ...")
    val sheet = Sheet.connect(cfg.googleCredsPath, cfg.sheetId)
    val data  = sheet.readEntireFirstSheet()

    demoAnalyze(cfg, data)

    // read all data, save to local CSVs
    val allSheets = sheet.readAllSheets()

    allSheets.foreach { case (title, rows) =>
      val csvContent = Csv.format(rows)
      val safeTitle  = title.replaceAll("[\\\\/:*?\"<>|]", "_")
      val filename   = s"sheet_$safeTitle.csv"
      val path       = os.pwd / "data" / filename

      os.write.over(path, csvContent)
      println(s"Wrote sheet '$title' to data/$filename")
    }
  }

  private def demoAnalyze(cfg: Config, data: Vector[Vector[String]]): Unit =
    if data.isEmpty then println("No data found in sheet.")
    else {
      println(s"Read ${data.size} rows from sheet.")
      println("--- Sheet preview (first 20 rows) ---")
      data.take(20).zipWithIndex.foreach {
        case (row, idx) =>
          val line = row.mkString(" | ")
          println(f"${idx + 1}%3d: $line")
      }
      println("--- End preview ---")

      val maxRows = 80
      val maxCols = 12

      // Build a truncated sheet snapshot similar to scripts/scrapeCocktails.scala
      val header = "You are analyzing a Google Sheet used to store cocktail-related data. " +
        "Your tasks: (1) Say if this sheet likely contains cocktail recipes, defined as a collection of amounts of each ingredient to use, " +
        "as well as clear separation between cocktails (true/false). " +
        "(2) Give a short 2-3 sentence description of the data. " +
        "(3) Propose a robust strategy to parse this sheet into structured cocktail recipes, " +
        "considering that one recipe may span multiple rows or columns. " +
        "Respond in clear markdown with three sections: HasCocktails, Description, ParsingStrategy.\n\n" +
        s"Below is a truncated snapshot (up to $maxRows rows x $maxCols columns):\n\n"

      val bodyLines = data.take(maxRows).zipWithIndex.map {
        case (row, idx) =>
          val cols = row.take(maxCols).map(_.replace("\n", " ").trim)
          f"${idx + 1}%3d | " + cols.mkString(" | ")
      }

      val snapshot = header + bodyLines.mkString("\n")

      println("\nAsking LLM to analyze sheet structure...")

      val llm      = LlmClient.fromConfig(cfg)
      val analysis = llm
        .complete(system = "You are a precise data analyst for cocktail datasets.", user = snapshot)

      println("\n=== LLM Analysis ===")
      println(analysis)
    }
