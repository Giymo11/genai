package utils

import scala.annotation.tailrec

object Tsv:

  type Grid = Vector[Vector[String]]

  /** Formats a grid of strings into a CSV string. separates cells with tabs and escapes as
    * necessary.
    */
  def format(rows: Grid): String = rows.map(formatRow).mkString("\n")

  private def formatRow(row: Vector[String]): String = row.map(escape).mkString("\t")

  private def escape(cell: String): String =
    val needsQuotes = cell.contains("\t") || cell.contains("\n") || cell.contains("\"")
    if needsQuotes then
      val escaped = cell.replace("\"", "\"\"")
      s""""$escaped""""
    else cell

  /** Parses a Tab-Separated Value string into a grid of strings. Pure functional implementation
    * using tail recursion.
    */
  def parse(tsvContent: String): Grid = {

    @tailrec
    def parseRec(
      chars: List[Char],
      inQuotes: Boolean,
      cellAcc: List[Char],
      rowAcc: Vector[String],
      result: Grid
    ): Grid = chars match {
      // 1. End of Input
      case Nil =>
        // If we have leftover data in the accumulators, capture it
        if cellAcc.isEmpty && rowAcc.isEmpty then result
        else result :+ (rowAcc :+ cellAcc.reverse.mkString)

      // 2. Escaped Quote ("") inside a quoted cell
      case '"' :: '"' :: tail if inQuotes => parseRec(tail, true, '"' :: cellAcc, rowAcc, result)

      // 3. Quote boundary (Toggle inQuotes state)
      case '"' :: tail => parseRec(tail, !inQuotes, cellAcc, rowAcc, result)

      // 4. Cell Separator (Tab) - only if NOT in quotes
      case '\t' :: tail if !inQuotes =>
        val completedCell = cellAcc.reverse.mkString
        parseRec(tail, false, Nil, rowAcc :+ completedCell, result)

      // 5. Row Separator (Newline/CRLF) - only if NOT in quotes
      case '\r' :: '\n' :: tail if !inQuotes =>
        val completedCell = cellAcc.reverse.mkString
        val completedRow  = rowAcc :+ completedCell
        parseRec(tail, false, Nil, Vector.empty, result :+ completedRow)

      case '\n' :: tail if !inQuotes =>
        val completedCell = cellAcc.reverse.mkString
        val completedRow  = rowAcc :+ completedCell
        parseRec(tail, false, Nil, Vector.empty, result :+ completedRow)

      // 6. Regular characters
      case c :: tail => parseRec(tail, inQuotes, c :: cellAcc, rowAcc, result)
    }

    parseRec(tsvContent.toList, inQuotes = false, Nil, Vector.empty, Vector.empty)
  }

  /** Recursively splits a grid into sub-blocks based on empty rows and columns.
    *
    * Strategy:
    *   1. Split by empty rows.
    *   2. If splits occurred, recurse on the parts.
    *   3. If no splits occurred (the block is vertically contiguous), transpose and attempt to
    *      split by columns.
    */
  def extractBlocks(grid: Grid): Vector[Grid] = {
    if grid.isEmpty then Vector.empty
    else {
      def isEmptyRow(row: Vector[String]): Boolean = row.forall(_.trim.isEmpty)

      // 1. Split vertically (Rows)
      val rowBlocks = splitBy(grid)(isEmptyRow)

      rowBlocks match
        case Vector() => Vector.empty // The whole grid was empty

        case blocks if blocks.size > 1 =>
          // Gap found! We must recurse on the sub-blocks
          blocks.flatMap(extractBlocks)

        case Vector(singleRowBlock) =>
          // No gap found inside, BUT splitBy might have stripped top/bottom whitespace.
          // 2. Split horizontally (Cols) on the row-trimmed block
          val normalized = normalizeGrid(singleRowBlock)
          val transposed = normalized.transpose
          val colBlocks  = splitBy(transposed)(isEmptyRow)

          colBlocks match
            case Vector() => Vector.empty

            case cBlocks if cBlocks.size > 1 =>
              // Gap found horizontally! Recurse (and transpose back later)
              cBlocks.flatMap(b => extractBlocks(b.transpose))

            case Vector(singleColBlock) =>
              // No internal gaps found.
              // This block is now trimmed of both empty rows AND empty columns.
              Vector(singleColBlock.transpose)
    }
  }

  /** A pure functional generic split. similar to string.split, but for Vectors and keeps 'chunks'
    * distinct. Consecutive separators are treated as one break.
    */
  private def splitBy[T](data: Vector[T])(isSeparator: T => Boolean): Vector[Vector[T]] = {
    val (lastChunk, accumulated) = data.foldLeft((Vector.empty[T], Vector.empty[Vector[T]])) {
      case ((currentChunk, acc), item) =>
        if isSeparator(item) then
          if currentChunk.nonEmpty then (Vector.empty, acc :+ currentChunk)
          else (Vector.empty, acc) // Skip consecutive separators or leading separators
        else (currentChunk :+ item, acc)
    }
    // Append the final chunk if it wasn't empty
    if lastChunk.nonEmpty then accumulated :+ lastChunk else accumulated
  }

  /** Ensures the grid is rectangular so it can be transposed safely. Finds the max row length and
    * pads shorter rows with empty strings.
    */
  private def normalizeGrid(grid: Grid): Grid =
    val maxWidth = grid.map(_.size).maxOption.getOrElse(0)
    grid
      .map(row => if row.size < maxWidth then row ++ Vector.fill(maxWidth - row.size)("") else row)
