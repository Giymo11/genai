package utils

import scala.util.control.NonFatal
import ujson.*

/** Thin, focused HTTP client for calling chat-style LLM APIs.
  *
  * Design goals:
  *   - Simple dev experience from AppMain / other call sites.
  *   - Support multiple providers (ollama, openrouter) configured via [[Config]].
  *   - No external side effects beyond HTTP; all configuration flows through [[Config]].
  *
  * This implementation mirrors the logic used in the standalone
  * scripts/[scrapeCocktails.scala](scripts/scrapeCocktails.scala:1) utility.
  */
/** Lightweight message model for callers; serialized ad-hoc via ujson. */
final case class ChatMessage(role: String, content: String)

private object JsonCodec:

  def encodeRequest(model: String, system: String, user: String, stream: Boolean = false): Value = Obj(
    "model"    -> Str(model),
    "stream"   -> Bool(stream),
    "messages" ->
      Arr(Obj("role" -> Str("system"), "content" -> Str(system)), Obj("role" -> Str("user"), "content" -> Str(user)))
  )

  def extractFirstMessageContent(raw: String): String = {
    try {
      val json    = ujson.read(raw)
      val choices = json.obj.get("choices") match
        case Some(arr: Arr) => arr.value
        case _              => return "[Error] LLM response missing 'choices' array"

      choices.headOption match
        case None        => "[Error] LLM response has empty 'choices'"
        case Some(first) =>
          val msgOpt = first.obj.get("message")
          msgOpt match
            case Some(msg: Obj) => msg.obj.get("content") match
                case Some(Str(text)) if text.trim.nonEmpty => text.trim
                case _                                     => "[Error] LLM response missing 'message.content'"
            case _ => "[Error] LLM response missing 'message' object"
    } catch case NonFatal(e) => s"[Error] Failed to parse LLM response JSON: ${e.getMessage}"
  }

/** Provider-agnostic LLM client.
  *
  * Usage:
  * {{{
  * val client  = LlmClient.fromConfig(cfg)
  * val result  = client.complete(
  *   system = "You are a precise data analyst for cocktail datasets.",
  *   user   = snapshot
  * )
  * }}}
  */
trait LlmClient:
  /** Execute a chat completion call and return the assistant content as plain String. Returns a best-effort error
    * message prefixed with "[Error]" on failures, instead of throwing, so call sites can simply `println`.
    */
  def complete(system: String, user: String): String

object LlmClient:

  /** Build a concrete [[LlmClient]] implementation from [[Config]].
    *
    * Supports:
    *   - "ollama"
    *   - "openrouter"
    * Falls back to "ollama" on unknown provider.
    */
  def fromConfig(cfg: Config): LlmClient = cfg.llmProvider match
    case "ollama"     => new OllamaClient(cfg)
    case "openrouter" => new OpenRouterClient(cfg)
    case other        =>
      System.err.println(s"Unknown llm.provider '$other', defaulting to ollama")
      new OllamaClient(cfg)

  // ---------------- Ollama implementation ----------------

  final private class OllamaClient(cfg: Config) extends LlmClient:
    private val url   = cfg.ollamaUrl
    private val model = cfg.ollamaModel

    def complete(system: String, user: String): String = {
      val bodyJson = JsonCodec.encodeRequest(model = model, system = system, user = user, stream = false)

      try
        val resp = requests.post(url = url, data = bodyJson.render(), readTimeout = 120000, connectTimeout = 5000)

        if resp.statusCode == 200 then JsonCodec.extractFirstMessageContent(resp.text())
        else s"[Error] Ollama HTTP ${resp.statusCode}: ${resp.text()}"
      catch case NonFatal(e) => s"[Error] Ollama request failed: ${e.getMessage}"
    }

  // ---------------- OpenRouter implementation ----------------

  final private class OpenRouterClient(cfg: Config) extends LlmClient:
    private val url     = cfg.openRouterUrl
    private val model   = cfg.openRouterModel
    private val apiKeyO = cfg.openRouterApiKey

    def complete(system: String, user: String): String = {
      val apiKey = apiKeyO
        .getOrElse(return "[Error] OpenRouter API key is not set (secrets/openrouter.key or OPENROUTER_API_KEY)")

      val bodyJson = JsonCodec.encodeRequest(model = model, system = system, user = user, stream = false)

      val headers = Map("Authorization" -> s"Bearer $apiKey", "Content-Type" -> "application/json")

      try
        val resp = requests
          .post(url = url, data = bodyJson.render(), headers = headers, readTimeout = 120000, connectTimeout = 5000)

        if resp.statusCode == 200 then JsonCodec.extractFirstMessageContent(resp.text())
        else s"[Error] OpenRouter HTTP ${resp.statusCode}: ${resp.text()}"
      catch case NonFatal(e) => s"[Error] OpenRouter request failed: ${e.getMessage}"
    }
