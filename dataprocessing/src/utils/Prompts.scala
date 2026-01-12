package utils

object Prompts {
  def parsingPrompt(promptType: String): String =
    os.read(os.pwd / "prompts" / s"$promptType.md")
}
