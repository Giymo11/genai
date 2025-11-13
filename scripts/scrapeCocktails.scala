#!/usr/bin/env -S scala-cli shebang

//> using scala "3.7.3"
//> using dep "com.lihaoyi::requests:0.9.0"
//> using dep "com.lihaoyi::upickle:4.4.1"
//> using dep "com.google.api-client:google-api-client:2.8.1"
//> using dep "com.google.oauth-client:google-oauth-client-jetty:1.39.0"
//> using dep "com.google.apis:google-api-services-sheets:v4-rev20220927-2.0.0"
//> using dep "com.typesafe:config:1.4.5"

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.{Sheets, SheetsScopes}
import com.google.api.services.sheets.v4.model.ValueRange
import com.typesafe.config.{Config, ConfigFactory}
import java.io.FileInputStream
import java.util.Collections
import requests.*
import scala.io.Source
import scala.jdk.CollectionConverters.*
import upickle.default.*

// Domain models (kept here for now)
case class Ingredient(name: String, amount: Double, unit: String, notes: String)
case class Cocktail(
  name: String,
  glassware: String,
  ingredients: Seq[Ingredient],
  method: String,
  garnish: String,
  steps: Seq[String],
  tags: Seq[String],
  source: String
)

object Cocktail:
  given ReadWriter[Ingredient] = macroRW
  given ReadWriter[Cocktail]   = macroRW

// ---------------- ConfigUtils ----------------
final case class AppConfig(
  sheetId: String,
  llmProvider: String,
  ollamaUrl: String,
  ollamaModel: String,
  openRouterUrl: String,
  openRouterModel: String,
  googleCredsPath: String,
  openRouterApiKey: Option[String]
)

object ConfigUtils:
  private val DefaultConfigPath = "config/cocktails.conf"

  def load(path: String = DefaultConfigPath): AppConfig = {
    val base: Config = ConfigFactory.parseFile(new java.io.File(path))
      .withFallback(ConfigFactory.load())

    def get(path: String, default: String): String =
      if base.hasPath(path) then base.getString(path) else default

    val sheetId     = get("cocktails.sheet-id", "")
    val llmProvider = get("llm.provider", "ollama").toLowerCase

    val ollamaUrl   = get("llm.ollama.url", "http://host.docker.internal:11434/v1/chat/completions")
    val ollamaModel = get("llm.ollama.model", "gemma3:12b")

    val openRouterUrl   = get("llm.openrouter.url", "https://openrouter.ai/api/v1/chat/completions")
    val openRouterModel = get("llm.openrouter.model", "meta-llama/llama-3.1-70b-instruct")

    val googleCredsPath = sys.env
      .getOrElse("GOOGLE_APPLICATION_CREDENTIALS", "secrets/service-account.json")

    val openRouterApiKeyFromFile = readFirstLineIfExists("secrets/openrouter.key")
    val openRouterApiKey = openRouterApiKeyFromFile.orElse(sys.env.get("OPENROUTER_API_KEY"))

    AppConfig(
      sheetId = sheetId,
      llmProvider = llmProvider,
      ollamaUrl = ollamaUrl,
      ollamaModel = ollamaModel,
      openRouterUrl = openRouterUrl,
      openRouterModel = openRouterModel,
      googleCredsPath = googleCredsPath,
      openRouterApiKey = openRouterApiKey
    )
  }
  end load

  private def readFirstLineIfExists(path: String): Option[String] =
    val f = new java.io.File(path)
    if !f.exists() then None
    else
      val src = Source.fromFile(f)
      try src.getLines().find(_.nonEmpty).map(_.trim)
      finally src.close()
  end readFirstLineIfExists
end ConfigUtils

// ---------------- SheetsUtil ----------------
object SheetsUtil:
  private val ApplicationName = "ScalaCocktailScraper"

  def buildService(credsPath: String): Sheets = {
    val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    val jsonFactory   = GsonFactory.getDefaultInstance

    val credential = GoogleCredential.fromStream(FileInputStream(credsPath))
      .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS_READONLY))

    new Sheets.Builder(httpTransport, jsonFactory, credential).setApplicationName(ApplicationName)
      .build()
  }
  end buildService

  def readRange(service: Sheets, sheetId: String, range: String): Vector[Vector[String]] =
    val resp   = service.spreadsheets().values().get(sheetId, range).execute()
    val values = Option(resp.getValues).getOrElse(Collections.emptyList[java.util.List[AnyRef]]())
    values.asScala.toVector
      .map(row => row.asScala.toVector.map(v => Option(v).map(_.toString).getOrElse("")))

  def readEntireFirstSheet(service: Sheets, sheetId: String): Vector[Vector[String]] = {
    val meta       = service.spreadsheets().get(sheetId).execute()
    val firstSheet = meta.getSheets.get(0)
    val title      = firstSheet.getProperties.getTitle
    val rowCount   = firstSheet.getProperties.getGridProperties.getRowCount
    val colCount   = firstSheet.getProperties.getGridProperties.getColumnCount
    val range      = s"$title!A1:${columnIndexToLetter(colCount - 1)}$rowCount"
    readRange(service, sheetId, range)
  }
  end readEntireFirstSheet

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
  end columnIndexToLetter
end SheetsUtil

// ---------------- LlmUtils ----------------
object LlmUtils:

  def buildSheetSnapshot(data: Vector[Vector[String]], maxRows: Int, maxCols: Int): String = {
    val header = "You are analyzing a Google Sheet used to store cocktail-related data. " +
      "Your tasks: (1) Say if this sheet likely contains cocktail recipes, defined as a collection of amounts of each ingredient to use, as well as clear separation between cocktails (true/false). " +
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

    header + bodyLines.mkString("\n")
  }
  end buildSheetSnapshot

  def analyzeWithLlm(cfg: AppConfig, snapshot: String): String = cfg.llmProvider match
    case "ollama"     => callOllama(cfg, snapshot)
    case "openrouter" => callOpenRouter(cfg, snapshot)
    case other        =>
      System.err.println(s"Unknown llm.provider '$other', defaulting to ollama")
      callOllama(cfg, snapshot)

  private def callOllama(cfg: AppConfig, prompt: String): String = {
    val body = ujson.Obj(
      "model"    -> cfg.ollamaModel,
      "messages" -> ujson.Arr(
        ujson.Obj(
          "role"    -> "system",
          "content" -> "You are a precise data analyst for cocktail datasets."
        ),
        ujson.Obj("role" -> "user", "content" -> prompt)
      ),
      "stream" -> false
    )

    try {
      val resp = requests.post(
        url = cfg.ollamaUrl,
        data = body.render(),
        readTimeout = 120000,
        connectTimeout = 5000
      )
      if resp.statusCode == 200 then
        val json = ujson.read(resp.text())
        json("choices")(0)("message")("content").str.trim
      else s"[Error] Ollama HTTP ${resp.statusCode}: ${resp.text()}"
    } catch case e: Exception => s"[Error] Ollama request failed: ${e.getMessage}"
  }
  end callOllama

  private def callOpenRouter(cfg: AppConfig, prompt: String): String = {
    val apiKey = cfg.openRouterApiKey.getOrElse(
      return "[Error] OpenRouter API key is not set (secrets/openrouter.key or OPENROUTER_API_KEY)"
    )

    val body = ujson.Obj(
      "model"    -> cfg.openRouterModel,
      "messages" -> ujson.Arr(
        ujson.Obj(
          "role"    -> "system",
          "content" -> "You are a precise data analyst for cocktail datasets."
        ),
        ujson.Obj("role" -> "user", "content" -> prompt)
      ),
      "stream" -> false
    )

    val headers = Map("Authorization" -> s"Bearer $apiKey", "Content-Type" -> "application/json")

    try {
      val resp = requests.post(
        url = cfg.openRouterUrl,
        data = body.render(),
        headers = headers,
        readTimeout = 120000,
        connectTimeout = 5000
      )
      if resp.statusCode == 200 then
        val json = ujson.read(resp.text())
        json("choices")(0)("message")("content").str.trim
      else s"[Error] OpenRouter HTTP ${resp.statusCode}: ${resp.text()}"
    } catch case e: Exception => s"[Error] OpenRouter request failed: ${e.getMessage}"
  }
  end callOpenRouter
end LlmUtils

// ---------------- Main orchestration ----------------
object ScrapeCocktails:

  def main(args: Array[String]): Unit = {
    val cfg = ConfigUtils.load()
    require(cfg.sheetId.nonEmpty, "cocktails.sheet-id must be set in config/cocktails.conf")

    val sheets = SheetsUtil.buildService(cfg.googleCredsPath)

    println(s"Fetching full grid for first sheet of ${cfg.sheetId} ...")
    val data = SheetsUtil.readEntireFirstSheet(sheets, cfg.sheetId)

    if data.isEmpty then
      println("No data found in sheet.")
      return

    println(s"Read ${data.size} rows from sheet.")
    println("--- Sheet preview (first 20 rows) ---")
    data.take(20).zipWithIndex.foreach {
      case (row, idx) =>
        val line = row.mkString(" | ")
        println(f"${idx + 1}%3d: $line")
    }
    println("--- End preview ---")

    val snapshot = LlmUtils.buildSheetSnapshot(data, maxRows = 80, maxCols = 12)

    println("\nAsking LLM to analyze sheet structure...")
    val analysis = LlmUtils.analyzeWithLlm(cfg, snapshot)
    println("\n=== LLM Analysis ===")
    println(analysis)
  }
  end main
end ScrapeCocktails
