package cocktails

import utils.*

object AppMain extends App:
  val cfg = Config.load()
  require(cfg.sheetId.nonEmpty, "cocktails.sheet-id must be set in cocktail-analyzer.conf")

  println(s"Fetching full grid for first sheet of ${cfg.sheetId} ...")
  val sheet = Sheet.connect(cfg.googleCredsPath, cfg.sheetId)
  val data  = sheet.readEntireFirstSheet()

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
