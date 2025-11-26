package utils

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.{Sheets as GSheets, SheetsScopes}
import java.io.FileInputStream
import java.util.Collections
import scala.jdk.CollectionConverters.*

final class Sheet private (private val service: GSheets, val sheetId: String):

  def readEntireFirstSheet(): Vector[Vector[String]] = {
    val meta       = service.spreadsheets().get(sheetId).execute()
    val firstSheet = meta.getSheets.get(0)
    val title      = firstSheet.getProperties.getTitle
    val rowCount   = firstSheet.getProperties.getGridProperties.getRowCount
    val colCount   = firstSheet.getProperties.getGridProperties.getColumnCount
    val range      = s"$title!A1:${columnIndexToLetter(colCount - 1)}$rowCount"

    readRange(range)
  }

  def readName() : String = {
    val meta = service.spreadsheets().get(sheetId).execute()
    meta.getProperties.getTitle
  }

  def readAllSheets(): Map[String, Vector[Vector[String]]] = {
    val meta   = service.spreadsheets().get(sheetId).execute()
    val sheets = meta.getSheets.asScala

    sheets.map { sheet =>
      val title    = sheet.getProperties.getTitle
      val rowCount = sheet.getProperties.getGridProperties.getRowCount
      val colCount = sheet.getProperties.getGridProperties.getColumnCount
      val range    = s"$title!A1:${columnIndexToLetter(colCount - 1)}$rowCount"

      title -> readRange(range)
    }.toMap
  }

  def readRange(range: String): Vector[Vector[String]] =
    val resp   = service.spreadsheets().values().get(sheetId, range).execute()
    val values = Option(resp.getValues).getOrElse(Collections.emptyList[java.util.List[AnyRef]]())

    values.asScala.toVector
      .map(row => row.asScala.toVector.map(v => Option(v).map(_.toString).getOrElse("")))

  private def columnIndexToLetter(index: Int): String = {
    @annotation.tailrec
    def loop(i: Int, acc: String): String =
      if i < 0 then acc
      else
        val rem = i % 26
        val ch  = ('A' + rem).toChar
        loop(i / 26 - 1, ch + acc)
    loop(index, "")
  }

object Sheet:
  private val ApplicationName = "ScalaCocktailScraper"

  def connect(credsPath: String, sheetId: String): Sheet = {
    val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    val jsonFactory   = GsonFactory.getDefaultInstance

    val credential = GoogleCredential.fromStream(FileInputStream(credsPath))
      .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS_READONLY))

    val gsvc = new GSheets.Builder(httpTransport, jsonFactory, credential)
      .setApplicationName(ApplicationName).build()

    new Sheet(gsvc, sheetId)
  }
