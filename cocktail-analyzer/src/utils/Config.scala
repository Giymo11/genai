package utils

import com.typesafe.config.{Config as TConfig, ConfigFactory}

final case class Config(
  sheetId: String,
  llmProvider: String,
  ollamaUrl: String,
  ollamaModel: String,
  openRouterUrl: String,
  openRouterModel: String,
  googleCredsPath: String,
  openRouterApiKey: Option[String]
)

object Config:
  private val DefaultConfigPath = "config/cocktails.conf"

  def load(path: String = DefaultConfigPath): Config = {
    val configFile    = os.pwd / os.RelPath(path)
    val base: TConfig = ConfigFactory.parseFile(configFile.toIO).withFallback(ConfigFactory.load())

    def get(key: String, default: String): String = if base.hasPath(key) then base.getString(key) else default

    val sheetId     = get("cocktails.sheet-id", "")
    val llmProvider = get("llm.provider", "ollama").toLowerCase

    val ollamaUrl   = get("llm.ollama.url", "http://host.docker.internal:11434/v1/chat/completions")
    val ollamaModel = get("llm.ollama.model", "gemma3:12b")

    val openRouterUrl   = get("llm.openrouter.url", "https://openrouter.ai/api/v1/chat/completions")
    val openRouterModel = get("llm.openrouter.model", "meta-llama/llama-3.1-70b-instruct")

    val googleCredsPath = sys.env.getOrElse("GOOGLE_APPLICATION_CREDENTIALS", "secrets/service-account.json")

    val openRouterApiKeyFromFile = readFirstLineIfExists(os.pwd / "secrets" / "openrouter.key")
    val openRouterApiKey         = openRouterApiKeyFromFile.orElse(sys.env.get("OPENROUTER_API_KEY"))

    Config(
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

  private def readFirstLineIfExists(path: os.Path): Option[String] =
    if !os.exists(path) then None else os.read.lines(path).find(_.trim.nonEmpty).map(_.trim)
