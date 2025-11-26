# genai

a simple scala script to interact with genAI

## installation & setup

install coursier, then

```bash
cs setup
```

create a secrets/ directory and add your google api key like so:

- Go to: https://console.cloud.google.com/
- Create/select a project.
- “APIs & Services” → “Enabled APIs & services”:
  - Enable:
  - “Google Sheets API”
- “IAM & Admin” → “Service Accounts”:
  - Create service account (e.g. “cocktail-scraper”).
  - After creation, go to “Keys” tab → “Add key” → “Create new key” → JSON.
  - Download the JSON to your machine, e.g.:
  - genai\service-account.json


## Usage

for a super simple hello world:
```bash
scala scrpt/hello.scala
```

for the cocktail analyzer:
```bash
./mill cocktail-analyzer.run
```


