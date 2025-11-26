package utils

object Csv:
  /**
   * Formats a grid of strings into a CSV string.
   * Handles escaping of quotes, commas, and newlines according to RFC 4180.
   */
  def format(rows: Vector[Vector[String]]): String =
    rows.map(formatRow).mkString("\n")

  private def formatRow(row: Vector[String]): String =
    row.map(escape).mkString(",")

  private def escape(cell: String): String =
    val needsQuotes = cell.contains(",") || cell.contains("\n") || cell.contains("\"")
    if needsQuotes then
      val escaped = cell.replace("\"", "\"\"")
      s""""$escaped""""
    else
      cell