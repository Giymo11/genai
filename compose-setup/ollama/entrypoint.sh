#!/bin/sh
set -eux

: "${OLLAMA_HOST:=0.0.0.0:11434}"
: "${OLLAMA_MODEL:=llama3.2:3b}"

# Trap signals and forward to child process
trap 'kill -TERM "$pid" 2>/dev/null' TERM INT

# Start the server in the background
ollama serve >/var/log/ollama.log 2>&1 &
pid="$!"

# Wait until API is up
until curl -sf "http://localhost:11434/api/tags" >/dev/null 2>&1; do
  sleep 0.2
done

# Pull model if missing (persists if you mount /root/.ollama)
if ! ollama list | awk '{print $1}' | grep -qx "${OLLAMA_MODEL}"; then
  echo "Pulling model: ${OLLAMA_MODEL}"
  ollama pull "${OLLAMA_MODEL}"
else
  echo "Model already present: ${OLLAMA_MODEL}"
fi

# Keep server in foreground
wait "$pid"

curl http://localhost:11434/api/generate -d '{
  "model": "llama3.2:3b",
  "stream": false,
  "prompt": "Why is the sky blue?"
}'