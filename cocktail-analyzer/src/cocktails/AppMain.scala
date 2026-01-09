package cocktails

import mainargs.{arg, main, Flag, ParserForMethods}
import utils.*

object AppMain:
  def main(args: Array[String]): Unit =
    // println(s"DEBUG: Received args: ${args.mkString(", ")}")
    ParserForMethods(this).runOrExit(args)

  def downloadDir = os.pwd / "data" / "downloaded"
  def chunkDir    = os.pwd / "data" / "chunks"

  @main(name = "download", doc = "Download Google Sheets and save as local CSVs")
  def download() = {
    println("Downloading Google Sheets and saving as local CSVs...")
    val cfg = Config.load()
    require(cfg.sheetId.nonEmpty, "cocktails.sheet-id must be set in cocktail-analyzer.conf")

    val sheet     = Sheet.connect(cfg.googleCredsPath, cfg.sheetId)
    val allSheets = sheet.readAllSheets()

    allSheets.foreach {
      case (title, rows) =>
        val csvContent = Csv.format(rows)
        val safeTitle  = title.replaceAll("[\\\\/:*?\"<>|]", "_")
        // name__sheetTitle.csv
        val name     = sheet.readName()
        val filename = s"${name}__${safeTitle}.tsv"
        val path     = downloadDir / filename
        os.write.over(path, csvContent)
        println(s"Saved $filename")
    }

    // cut up large CSV into smaller chunks. Recursive split on empty rows/columns
  }
  @main(name = "preprocess", doc = "Preprocess the downloaded CSVs")
  def preprocess() = {
    println("Preprocessing the downloaded CSVs...")
    val csvFiles = os.list(downloadDir).filter(_.ext == "csv")
    println(s"Found ${csvFiles.length} CSV files to preprocess.")
    // read each csv as cells
    val data = csvFiles.map { file => Csv.parse(os.read(file)) }

    val chunks = data.map { grid => Csv.extractBlocks(grid) }

    // save each chunk as separate csv
    os.makeDir.all(chunkDir)
    csvFiles.zip(chunks).foreach {
      case (file, blocks) => blocks.zipWithIndex.foreach {
          case (block, idx) =>
            val csvContent = Csv.format(block)
            val outputPath = chunkDir / s"${file.baseName}_part${idx + 1}.tsv"
            os.write.over(outputPath, csvContent)
            println(s"Wrote chunk to ${outputPath.last}")
        }
    }
  }

  @main(name = "parse", doc = "Parse the preprocessed TSVs into JSON cocktail data")
  def parse() = {
    println("Parsing the preprocessed TSVs into JSON cocktail data...")

    val cfg = Config.load()
    val llm = LlmClient.fromConfig(cfg)

    // read from chunkDir
    val tsvFiles = os.list(chunkDir).filter(_.ext == "tsv")
    println(s"Found ${tsvFiles.length} TSV files to parse.")

    if tsvFiles.isEmpty then {
      println("No TSV files found. Please run 'preprocess' first.")
      sys.exit(1)
    }

    val formatDef = os.read(os.pwd / "data" / "combined_cocktail_format.json")

    // Time tracking for ETA
    val startTime = System.currentTimeMillis()

    val parsedResults = tsvFiles.zipWithIndex.map {
      case (file, idx) =>
        val fileStartTime = System.currentTimeMillis()
        println(s"Processing file ${idx + 1}/${tsvFiles.length}: ${file.last}")
        val content = os.read(file)

        val prompt = {
          s"""You are a precise data analyst. Parse the following TSV data into the JSON format defined below.
           |
           |Format Definition:
           |$formatDef
           |
           |
           |Guidelines:
           |1. If the data contains a cocktail recipe, extract the name, ingredients, and notes. Do not use the "other_data" field in this case.
           |2. If the data does not contain a cocktail recipe, describe what the data contains in the "other_data" field. Do not use the "cocktail_specs" field in this case.
           |3. If the data contains multiple cocktail recipes, include all of them in the "cocktail_specs" array. Use the "other_data" field only if there is additional non-cocktail information.
           |
           |Ingredients always follow the format "<amount> <unit (optional)> <ingredient>. If there is an amount, write the ingredient and amount into the ingredients list. If there is no amount specified, write the ingredient into the notes or garnish. Do not write ingredients with amounts into the notes or garnish.
           |If there are multiple ingredients possible as substitutions, write them into the ingredient name. 
           |If the amount is a range, repeat the cocktail recipe and include both, one with the minimum and one with the maximum amount.
           |Ensure that each cocktail has at least two ingredients to be considered a valid recipe. Again, if not, use "other_data" instead.
           |
           |
           |Data (TSV):
           |$content
           |
           |Respond ONLY with the valid JSON. Do not include markdown formatting or explanations.
           |""".stripMargin
        }

        val response = llm
          .complete(system = "You are a precise data analyst for cocktail datasets.", user = prompt)

        // clean up response (remove markdown code blocks if present)
        val cleanResponse = response.replaceAll("^```json", "").replaceAll("^```", "")
          .replaceAll("```$", "").trim

        val result = {
          try {
            val json = ujson.read(cleanResponse)
            // add file source info
            json.obj.update("source_file", ujson.Str(file.last))
            Some(json)
          } catch {
            case e: Exception =>
              println(s"Failed to parse JSON for ${file.last}: ${e.getMessage}")
              println(s"Response was: $cleanResponse")
              None
          }
        }

        // Calculate and display ETA
        val fileElapsed    = System.currentTimeMillis() - fileStartTime
        val totalElapsed   = System.currentTimeMillis() - startTime
        val remainingFiles = tsvFiles.length - (idx + 1)
        if remainingFiles > 0 then {
          val avgTimePerFile        = totalElapsed / (idx + 1)
          val estimatedRemainingMs  = avgTimePerFile * remainingFiles
          val estimatedRemainingSec = estimatedRemainingMs / 1000
          val minutes               = estimatedRemainingSec / 60
          val seconds               = estimatedRemainingSec % 60
          println(
            s"  Estimated time remaining: ${minutes}m ${seconds}s (${remainingFiles} files left)"
          )
        }

        result
    }

    // Merge results
    val allSpecs = parsedResults.flatten.flatMap {
      json =>
        try {
          json.obj.get("cocktail_specs") match {
            case Some(arr: ujson.Arr) => arr.value
            case _                    =>
              println("Warning: JSON does not contain 'cocktail_specs' array")
              // also print source file for debugging
              println(s"Source file: ${json.obj.get("source_file")}")
              Seq.empty
          }
        } catch {
          case e: Exception =>
            println(s"Error extracting specs: ${e.getMessage}")
            Seq.empty
        }
    }

    val nonCocktailData = parsedResults.flatten.flatMap {
      json =>
        // extract other_data if present, combine with source_file info,
        try {
          json.obj.get("other_data") match {
            case Some(data: ujson.Obj) =>
              // include source_file info
              val sourceFile = json.obj.get("source_file") match {
                case Some(ujson.Str(name)) => name
                case _                     => "unknown"
              }
              data.obj.update("source_file", ujson.Str(sourceFile))
              // print warning if also contains cocktail_specs
              if json.obj.get("cocktail_specs").isDefined then
                println(
                  s"Warning: JSON contains both 'cocktail_specs' and 'other_data' in $sourceFile"
                )

              Seq(data)
            case _ => Seq.empty
          }
        } catch {
          case e: Exception =>
            println(s"Error extracting other_data: ${e.getMessage}")
            Seq.empty
        }

    }

    val finalJson = ujson.Obj(
      "cocktail_specs" -> ujson.Arr(allSpecs*),
      "other_data"     -> ujson.Obj(
        "description"               -> "Aggregated data parsed from multiple TSV files.",
        "source_files_count"        -> tsvFiles.length,
        "generated_at"              -> java.time.Instant.now().toString,
        "non_cocktail_data_entries" -> ujson.Arr(nonCocktailData*)
      )
    )

    val outputPath = os.pwd / "data" / "parsed_cocktails.json"
    os.write.over(outputPath, ujson.write(finalJson, indent = 2))
    println(s"Saved parsed data to $outputPath")
  }

  @main(
    name = "suggestFormat",
    doc = "Ask LLM to suggest a coktail json format based on the data for each sheet"
  )
  def suggestFormat() = {
    println("Asking LLM to suggest a cocktail json format based on the data for each sheet...")

    val cfg = Config.load()
    require(cfg.sheetId.nonEmpty, "cocktails.sheet-id must be set in cocktail-analyzer.conf")

    val llm = LlmClient.fromConfig(cfg)

    def suggestFormat(data: String): String = {
      val prompt =
        "Based on the following data from a Google Sheet used to store cocktail-related data, " +
          "propose a robust JSON format to represent cocktail recipes parsed from this sheet. " +
          "Consider that one recipe may span multiple rows or columns. " +
          "Respond with only the JSON format definition."

      llm.complete(
        system = "You are a precise data analyst for cocktail datasets.",
        user = prompt + "\n\n" + data
      )
    }

    def combineFormats(formats: Seq[String]): String = {
      val prompt = "Given the following proposed JSON format definitions for cocktail recipes, " +
        "combine them into a single robust JSON format that can accommodate all the variations. " +
        "Respond with only the combined JSON format definition."

      llm.complete(
        system = "You are a precise data analyst for cocktail datasets.",
        user = prompt + "\n\n" + formats.mkString("\n\n")
      )
    }

    val csvFiles         = os.list(os.pwd / "data")
    val suggestedFormats = csvFiles.map {
      file =>
        val data       = os.read(file)
        val suggestion = suggestFormat(data)
        println(s"Suggested format for ${file.last}:\n$suggestion")
        suggestion
    }

    val combinedFormat = combineFormats(suggestedFormats)

    // Save combined format to file
    val outputPath = os.pwd / "data" / "combined_cocktail_format.json"
    os.write.over(outputPath, combinedFormat)
  }

  @main(name = "transform", doc = "Transform the data into the suggested format")
  def transform() = {
    println("Transforming the data into the suggested format...")
    val cfg = Config.load()
    require(cfg.sheetId.nonEmpty, "cocktails.sheet-id must be set in cocktail-analyzer.conf")

    val llm             = LlmClient.fromConfig(cfg)
    val combinedFormat  = os.read(os.pwd / "data" / "combined_cocktail_format.json")
    val csvFiles        = os.list(os.pwd / "data")
    val transformedData = csvFiles.map {
      file =>
        val data   = os.read(file)
        val prompt =
          "Based on the following data from a Google Sheet used to store cocktail-related data. The data can be spon multiple rows or columns, and the format may vary considerably within the sheet." +
            "transform the data into the following JSON format. Include only cocktail specs in the first part (and a cocktail has at least two ingredients). clearly mark when you have to make guesses to fill in missing or ambiguous information. " +
            "Include whatever information you can find that is not cocktail specs in the second part. " +
            "Respond with only the transformed JSON data."
        val transformed = llm.complete(
          system = "You are a precise data analyst for cocktail datasets.",
          user = s"$prompt\n\nFormat Definition:\n$combinedFormat\n\nData:\n$data"
        )
        println(s"Transformed data for ${file.last}:\n$transformed")
        file -> transformed
    }
    // Save transformed data to file
    transformedData.foreach {
      case (file, jsonData) =>
        val outputPath = os.pwd / "data" / s"${file.baseName}_transformed.json"
        os.write.over(outputPath, jsonData)
    }
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

    allSheets.foreach {
      case (title, rows) =>
        val csvContent = Csv.format(rows)
        val safeTitle  = title.replaceAll("[\\\\/:*?\"<>|]", "_")
        val filename   = s"sheet_$safeTitle.csv"
        val path       = os.pwd / "data" / filename

        os.write.over(path, csvContent)
        println(s"Wrote sheet '$title' to data/$filename")
    }
  }

  private def demoAnalyze(cfg: Config, data: Vector[Vector[String]]): Unit = {
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
  }
