package utils

import scala.util.control.NonFatal
import ujson.*

/** Thin, focused HTTP client for calling chat-style LLM APIs.
  *
  * Design goals:
  *   - Support multiple providers (ollama, openrouter) configured via [[Config]].
  *   - No external side effects beyond HTTP; all configuration flows through [[Config]].
  */
private object JsonCodec:

  def encodeRequest(model: String, system: String, user: String, stream: Boolean = false): Value = {
    Obj(
      "model"    -> Str(model),
      "stream"   -> Bool(stream),
      "messages" -> Arr(
        Obj("role" -> Str("system"), "content" -> Str(system)),
        Obj("role" -> Str("user"), "content"   -> Str(user))
      )
    )
  }

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
                case _ => "[Error] LLM response missing 'message.content'"
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
  /** Execute a chat completion call and return the assistant content as plain String. Returns a
    * best-effort error message prefixed with "[Error]" on failures, instead of throwing, so call
    * sites can simply `println`.
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
    case "ollama"                                     => new OllamaClient(cfg)
    case "openrouter" if cfg.openRouterApiKey.isEmpty =>
      System.err.println(s"OpenRouter API key is not set, defaulting to ollama")
      new OllamaClient(cfg)
    case "openrouter" => new OpenRouterClient(cfg)
    case other        =>
      System.err.println(s"Unknown llm.provider '$other', defaulting to ollama")
      new OllamaClient(cfg)

  // ---------------- Ollama implementation ----------------

  /** Generic OpenAI-compatible HTTP client.
    *
    * Parametrized by:
    *   - debugName : label used in error messages/logging
    *   - url : endpoint URL
    *   - model : model name
    *   - headers : additional HTTP headers
    */
  sealed private class OpenAiApiClient(
    debugName: String,
    url: String,
    model: String,
    headers: Map[String, String] = Map.empty
  ) extends LlmClient:

    def complete(system: String, user: String): String = {
      val bodyJson = JsonCodec.encodeRequest(model, system, user)
      try {
        val resp = requests.post(
          url = url,
          data = bodyJson.render(),
          headers = headers,
          readTimeout = 120000,
          connectTimeout = 5000
        )
        if resp.statusCode == 200 then JsonCodec.extractFirstMessageContent(resp.text())
        else s"[Error] $debugName HTTP ${resp.statusCode}: ${resp.text()}"
      } catch case NonFatal(e) => s"[Error] $debugName request failed: ${e.getMessage}"
    }

  // ---------------- Provider-specialized constructors ----------------

  final private class OllamaClient(cfg: Config)
      extends OpenAiApiClient("Ollama", cfg.ollamaUrl, cfg.ollamaModel)

  // ---------------- OpenRouter implementation ----------------

  final private class OpenRouterClient(cfg: Config)
      extends OpenAiApiClient(
        debugName = "OpenRouter",
        url = cfg.openRouterUrl,
        model = cfg.openRouterModel,
        headers = Map(
          "Authorization" -> s"Bearer ${cfg.openRouterApiKey.getOrElse("")}",
          "Content-Type"  -> "application/json"
        )
      )
