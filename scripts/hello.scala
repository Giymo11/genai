#!/usr/bin/env -S scala-cli shebang

//> using scala "3.7.3"
//> using dep "com.lihaoyi::requests:0.9.0"
//> using dep "com.lihaoyi::upickle:4.4.1"

import requests.*
import ujson.*

@main
def helloOllama(): Unit = {
  // use OpenAI compatible API to talk to Ollama local server
  val ollamaUrl = "http://host.docker.internal:11434/v1/chat/completions"
  val model     = "gemma3:12b"
  val prompt    = "Tell me all you know about cocktails."

  println(s"🤖 Asking $model: $prompt")
  println("-" * 50)

  def buildData(content: String): ujson.Value = Obj(
    "model"    -> Str(model),
    "messages" -> Arr(Obj("role" -> Str("user"), "content" -> Str(content))),
    "stream"   -> Bool(false)
  )
  try {
    val response = requests
      .post(ollamaUrl, data = buildData(prompt).render(), readTimeout = 60000, connectTimeout = 5000)
    if response.statusCode == 200 then
      val json   = ujson.read(response.text())
      val answer = json("choices")(0)("message")("content").str.trim
      println(s"✅ Response: $answer")
    else
      println(s"❌ Error: HTTP ${response.statusCode}")
      println(response.text())
  } catch
    case e: Exception =>
      println(s"❌ Connection error: ${e.getMessage}")
      println("💡 Make sure Ollama is running: ollama serve")
}
end helloOllama
