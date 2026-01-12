package cocktails

import mainargs.{arg, main, Flag, ParserForMethods}
import scala.util.{Failure, Success, Try}
import utils.*

object AppMain:
  def main(args: Array[String]): Unit =
    // println(s"DEBUG: Received args: ${args.mkString(", ")}")
    ParserForMethods(this).runOrExit(args)

  def downloadDir = os.pwd / "data" / "downloaded"
  def chunkDir    = os.pwd / "data" / "chunks"

  @main(name = "download", doc = "Download Google Sheets and save as local CSVs")
  def download() = {
    println("Downloading Google Sheets and saving as local CSVs...")
    val cfg = Config.load()
    require(cfg.sheetId.nonEmpty, "cocktails.sheet-id must be set in cocktail-analyzer.conf")

    val sheet     = Sheet.connect(cfg.googleCredsPath, cfg.sheetId)
    val allSheets = sheet.readAllSheets()

    allSheets.foreach {
      case (title, rows) =>
        val tsvContent = Tsv.format(rows)
        val safeTitle  = title.replaceAll("[\\\\/:*?\"<>|]", "_")
        // name__sheetTitle.csv
        val name     = sheet.readName()
        val filename = s"${name}__${safeTitle}.tsv"
        val path     = downloadDir / filename
        os.write.over(path, tsvContent)
        println(s"Saved $filename")
    }

  }

  // cut up large CSV into smaller chunks. Recursive split on empty rows/columns
  @main(name = "preprocess", doc = "Preprocess the downloaded TSVs")
  def preprocess() = {
    println("Preprocessing the downloaded TSVs...")
    val tsvFiles = os.list(downloadDir).filter(_.ext == "tsv")
    println(s"Found ${tsvFiles.length} TSV files to preprocess.")
    // read each tsv as cells
    val data = tsvFiles.map { file => Tsv.parse(os.read(file)) }

    val chunks = data.map { grid => Tsv.extractBlocks(grid) }

    // save each chunk as separate csv
    os.makeDir.all(chunkDir)
    tsvFiles.zip(chunks).foreach {
      case (file, blocks) => blocks.zipWithIndex.foreach {
          case (block, idx) =>
            val tsvContent = Tsv.format(block)
            val outputPath = chunkDir / s"${file.baseName}_part${idx + 1}.tsv"
            os.write.over(outputPath, tsvContent)
            println(s"Wrote chunk to ${outputPath.last}")
        }
    }
  }

  @main(name = "parse", doc = "Parse the preprocessed TSVs into JSON cocktail data using an LLM")
  def parse() = {
    println("Parsing the preprocessed TSVs into JSON cocktail data...")

    val cfg = Config.load()
    val llm = LlmClient.fromConfig(cfg)

    // read from chunkDir
    val tsvFiles = os.list(chunkDir).filter(_.ext == "tsv")
    println(s"Found ${tsvFiles.length} TSV files to parse.")

    if tsvFiles.isEmpty then {
      println("No TSV files found. Please run 'preprocess' first.")
      sys.exit(1)
    }

    // Time tracking for ETA
    val startTime = System.currentTimeMillis()

    val promptName     = "fewShotToJson"
    val promptTemplate = Prompts.parsingPrompt(promptName)

    val parsedResults = tsvFiles.zipWithIndex.map {
      case (file, idx) =>
        val fileStartTime = System.currentTimeMillis()
        println(s"Processing file ${idx + 1}/${tsvFiles.length}: ${file.last}")
        val content = os.read(file)

        val prompt   = promptTemplate.replace("$content", content)
        val response = llm
          .complete(system = "You are a precise data analyst for cocktail datasets.", user = prompt)

        // clean up response (remove markdown code blocks if present)
        val cleanResponse = response.replaceAll("^```json", "").replaceAll("^```", "")
          .replaceAll("```$", "").trim

        val result = {
          try {
            val json = ujson.read(cleanResponse)
            // add file source info
            json.obj.update("source_file", ujson.Str(file.last))
            Some(json)
          } catch {
            case e: Exception =>
              println(s"Failed to parse JSON for ${file.last}: ${e.getMessage}")
              // save the raw response to a file for debugging
              os.write.over(os.pwd / "data" / "debug" / s"${file.last}.json", cleanResponse)
              None
          }
        }

        // Calculate and display ETA
        val fileElapsed    = System.currentTimeMillis() - fileStartTime
        val totalElapsed   = System.currentTimeMillis() - startTime
        val remainingFiles = tsvFiles.length - (idx + 1)
        if remainingFiles > 0 then {
          val avgTimePerFile        = totalElapsed / (idx + 1)
          val estimatedRemainingMs  = avgTimePerFile * remainingFiles
          val estimatedRemainingSec = estimatedRemainingMs / 1000
          val minutes               = estimatedRemainingSec / 60
          val seconds               = estimatedRemainingSec % 60
          println(
            s"  Estimated time remaining: ${minutes}m ${seconds}s (${remainingFiles} files left)"
          )
        }

        result
    }

    // Merge results, separating cocktail_specs and other_data
    val allSpecs = parsedResults.flatten.flatMap {
      json =>
        try {
          val specs = json.obj.get("cocktail_specs") match {
            case Some(arr: ujson.Arr) => arr.value
            case _                    =>
              println("Warning: JSON does not contain 'cocktail_specs' array")
              // also print source file for debugging
              println(s"Source file: ${json.obj.get("source_file")}")
              Seq.empty
          }
          specs
        } catch {
          case e: Exception =>
            println(s"Error extracting specs: ${e.getMessage}")
            Seq.empty
        }
    }

    val nonCocktailData = parsedResults.flatten.flatMap {
      json =>
        try {
          json.obj.get("other_data") match {
            case Some(data: ujson.Obj) =>
              // include source_file info
              val sourceFile = json.obj.get("source_file") match {
                case Some(ujson.Str(name)) => name
                case _                     => "unknown"
              }
              data.obj.update("source_file", ujson.Str(sourceFile))
              Some(data)
            case _ => None
          }
        } catch {
          case e: Exception =>
            println(s"Error extracting other_data: ${e.getMessage}")
            None
        }
    }

    val finalJson = ujson.Obj(
      "cocktail_specs" -> ujson.Arr(allSpecs*),
      "other_data"     -> ujson.Obj(
        "description"               -> "Aggregated data parsed from multiple TSV files.",
        "source_files_count"        -> tsvFiles.length,
        "generated_at"              -> java.time.Instant.now().toString,
        "non_cocktail_data_entries" -> ujson.Arr(nonCocktailData*)
      )
    )

    val outputPath = os.pwd / "data" /
      s"parsed_${promptName}_cocktails_${java.time.Instant.now().toString}.json"
    os.write.over(outputPath, ujson.write(finalJson, indent = 2))
    println(s"Saved parsed data to $outputPath")
  }

  @main(name = "postProcess", doc = "Post-process the parsed JSON cocktail data")
  def postProcess() = {
    println("Post-processing the parsed JSON cocktail data...")

    // load the latest parsed JSON file
    val dataDir   = os.pwd / "data"
    val jsonFiles = os.list(dataDir).filter(f => f.ext == "json" && f.last.startsWith("parsed_"))
    require(jsonFiles.nonEmpty, "No parsed JSON files found in data directory.")
    val latestFile = jsonFiles.maxBy(f => os.stat(f).mtime)
    println(s"Loading latest parsed JSON file: ${latestFile.last}")
    val parsedJson = ujson.read(os.read(latestFile))

    // filter out cocktails with less than 2 ingredients
    val specs                      = parsedJson("cocktail_specs").arr
    val (validSpecs, invalidSpecs) = specs.partition {
      spec =>
        spec.obj.get("ingredients") match {
          case Some(ingArr: ujson.Arr) => ingArr.value.size >= 2
          case _                       => false
        }
    }

    val relevantFields = Seq("ingredients", "garnish", "method", "serve", "name")

    val normalize = (spec: ujson.Value, field: String) =>
      {
        spec.obj.get(field) match {
          case Some(ujson.Str(s))   => Seq(s.toLowerCase.trim)
          case Some(ujson.Arr(arr)) => arr.value.collect {
              case ujson.Str(s)   => s.toLowerCase.trim
              case ujson.Obj(obj) => obj.get("name") match {
                  case Some(ujson.Str(s)) => s.toLowerCase.trim
                  case _                  => ""
                }
            }
          case _ => Seq.empty[String]
        }
      }.distinct.sorted

    // create list of unique ingredient names, sorted by name/similarity
    // the same for garnish, method, etc -> collect all unique values for relevant fields to normalize them
    val normalizedData = relevantFields
      .map { field => field -> validSpecs.flatMap(spec => normalize(spec, field)).distinct.sorted }
      .toMap

    // save normalized data
    val normalizedFile = dataDir / "uniqed_cocktail_data.json"
    os.write(normalizedFile, ujson.write(normalizedData, indent = 2))
    println(s"Saved normalized data to: ${normalizedFile.last}")

    // remove all other_data where there both description and data are empty or null
    val filteredOtherData = parsedJson.obj.get("other_data") match {
      case Some(otherData: ujson.Obj) =>
        val filteredEntries = otherData.obj.get("non_cocktail_data_entries") match {
          case Some(entries: ujson.Arr) =>
            val filtered = entries.value.filter {
              entry =>
                val desc = entry.obj.get("description")
                val data = entry.obj.get("data")
                // Keep entries where either description or data is not empty
                val descEmpty = desc == Some(ujson.Str("")) || desc == Some(ujson.Null) ||
                  desc.isEmpty
                val dataEmpty = data.isEmpty || data == Some(ujson.Arr(Seq.empty)) ||
                  data == Some(ujson.Null)
                !descEmpty || !dataEmpty
            }.toSeq
            ujson.Arr(filtered*)
          case _ => ujson.Arr() // If no entries or wrong type, return empty array
        }
        // Create new other_data object with filtered entries
        ujson.Obj(
          "description"        -> otherData.obj.getOrElse("description", ujson.Str("")),
          "source_files_count" -> otherData.obj.getOrElse("source_files_count", ujson.Num(0)),
          "generated_at"       -> otherData.obj.getOrElse("generated_at", ujson.Str("")),
          "non_cocktail_data_entries" -> filteredEntries
        )
      case _ => ujson.Obj() // If no other_data, return empty object
    }

    // Update the parsedJson with filtered other_data
    val updatedParsedJson = parsedJson.obj.updated("other_data", filteredOtherData)
    // combine all invalid specs and other_data into a single debug file
    val debugData = ujson.Obj(
      "invalid_specs" -> invalidSpecs,
      "other_data"    -> updatedParsedJson.obj.getOrElse("other_data", ujson.Num(0)),
      "metadata"      -> ujson.Obj(
        "total_specs"    -> specs.size,
        "valid_specs"    -> validSpecs.size,
        "invalid_specs"  -> invalidSpecs.size,
        "processed_file" -> latestFile.last,
        "timestamp"      -> java.time.Instant.now().toString
      )
    )

    val debugFile = dataDir /
      s"debug_cocktail_data_${java.time.Instant.now().toString.replace(":", "-")}.json"
    os.write(debugFile, ujson.write(debugData, indent = 2))
    println(s"Saved debug data to: ${debugFile.last}")

    // save all the valid specs into a file as well
    val validData = ujson.Obj(
      "cocktail_specs" -> validSpecs,
      "metadata"       -> ujson.Obj(
        "total_valid_specs" -> validSpecs.size,
        "source_file"       -> latestFile.last,
        "timestamp"         -> java.time.Instant.now().toString
      )
    )

    val validFile = dataDir /
      s"valid_cocktail_specs_${java.time.Instant.now().toString.replace(":", "-")}.json"
    os.write(validFile, ujson.write(validData, indent = 2))
    println(s"Saved valid specs to: ${validFile.last}")

    println(s"Post-processing complete!")
    println(s"Total specs: ${specs.size}")
    println(s"Valid specs: ${validSpecs.size}")
    println(s"Invalid specs: ${invalidSpecs.size}")

  }

  @main(name = "normalize", doc = "use an LLM to deduplicate and normalize the data")
  def normalize() = {
    println("Normalizing data using LLM...")
    val cfg = Config.load()
    val llm = LlmClient.fromConfig(cfg)

    // read unique data from file
    val dataDir        = os.pwd / "data"
    val normalizedFile = dataDir / "uniqued_cocktail_data.json"
    require(os.exists(normalizedFile), s"File not found: ${normalizedFile.last}")
    val uniqueData = ujson.read(os.read(normalizedFile)).obj

    val relevantFields = Seq("ingredients", "method", "serve", "garnish")

    val taxonomyPromptTemplate = os.read(os.pwd / "prompts" / "buildTaxonomy.md")
    val mappingPromptTemplate  = os.read(os.pwd / "prompts" / "createMappings.md")

    val allMappings = relevantFields.map {
      field =>
        println(s"Processing field: $field")
        val values = uniqueData(field).arr.map(_.str).filter(_.nonEmpty).mkString("\n")

        // 1. Build Taxonomy
        val taxonomyPrompt = taxonomyPromptTemplate.replace("$field", field)
          .replace("$values", values)

        println(s"  Building taxonomy for $field...")
        val taxonomy = llm.complete(
          system = "You are a precise data analyst for cocktail datasets.",
          user = taxonomyPrompt
        )

        // 2. Create Mappings
        val mappingPrompt = mappingPromptTemplate.replace("$field", field)
          .replace("$taxonomy", taxonomy).replace("$values", values)

        println(s"  Creating mappings for $field...")
        val mappingResponse = llm.complete(
          system = "You are a precise data analyst for cocktail datasets.",
          user = mappingPrompt
        )

        // clean up response (remove markdown code blocks if present)
        val cleanMappingResponse = mappingResponse.replaceAll("^```json", "").replaceAll("^```", "")
          .replaceAll("```$", "").trim

        val mapping = ujson.read(cleanMappingResponse).obj
        field -> mapping
    }.toMap

    // save mappings to file
    val mappingsFile = dataDir / "field_mappings.json"
    os.write.over(mappingsFile, ujson.write(allMappings, indent = 2))
    println(s"Saved mappings to: ${mappingsFile.last}")

    // apply mappings to relevant fields in the latest valid specs file
    val validSpecsFiles = os.list(dataDir)
      .filter(f => f.ext == "json" && f.last.startsWith("valid_cocktail_specs_"))
    require(validSpecsFiles.nonEmpty, "No valid specs files found.")
    val latestValidFile = validSpecsFiles.maxBy(f => os.stat(f).mtime)
    println(s"Applying mappings to: ${latestValidFile.last}")

    val validData = ujson.read(os.read(latestValidFile))
    val specs     = validData("cocktail_specs").arr

    specs.foreach {
      spec =>
        relevantFields.foreach {
          field =>
            spec.obj.get(field) match {
              case Some(ujson.Str(s)) =>
                val normalized = s.toLowerCase.trim
                if allMappings(field).obj.contains(normalized) then {
                  spec.obj.update(field, allMappings(field)(normalized))
                }
              case Some(ujson.Arr(arr)) if field == "ingredients" =>
                arr.value.foreach {
                  ing =>
                    ing.obj.get("name") match {
                      case Some(ujson.Str(s)) =>
                        val normalized = s.toLowerCase.trim
                        if allMappings(field).obj.contains(normalized) then {
                          ing.obj.update("taxonomy", allMappings(field)(normalized))
                        }
                      case _ =>
                    }
                }
              case _ =>
            }
        }
    }

    // save normalized data to file
    val finalNormalizedFile = dataDir / "final_normalized_cocktail_data.json"
    os.write.over(finalNormalizedFile, ujson.write(validData, indent = 2))
    println(s"Saved final normalized data to: ${finalNormalizedFile.last}")
  }

  @main(name = "enrichDataset", doc = "Use LLMs to add descriptions to cocktails")
  def enrichDataset() = {
    println("Enriching dataset with descriptions...")
    val cfg = Config.load()

    val llm = LlmClient.fromConfig(cfg)

    val dataDir        = os.pwd / "data"
    val normalizedFile = dataDir / "final_normalized_cocktail_data.json"
    require(os.exists(normalizedFile), s"Normalized file not found: ${normalizedFile.last}")

    val normalizedData = ujson.read(os.read(normalizedFile))
    val specs          = normalizedData("cocktail_specs").arr

    val promptTemplate = os.read(os.pwd / "prompts" / "enrichCocktail.md")

    val startTime = System.currentTimeMillis()

    val enrichedSpecs = specs.zipWithIndex.map {
      case (spec, idx) =>
        val specStartTime = System.currentTimeMillis()
        val cocktailName  = spec.obj.get("name").map(_.str).getOrElse("Unknown")
        println(s"Enriching cocktail ${idx + 1}/${specs.length}: $cocktailName")

        val specJson = ujson.write(spec, indent = 2)
        val response = llm.complete(
          system = "You are a world-class mixologist and cocktail historian.",
          user = s"$promptTemplate\n\nInput:\n$specJson"
        )

        val cleanResponse = response.replaceAll("^```json", "").replaceAll("^```", "")
          .replaceAll("```$", "").trim

        val result = try {
          ujson.read(cleanResponse)
        } catch {
          case e: Exception =>
            println(s"  Failed to parse enriched JSON for $cocktailName: ${e.getMessage}")
            // fallback to original spec if enrichment fails
            spec
        }

        // ETA tracking
        val totalElapsed = System.currentTimeMillis() - startTime
        val remaining    = specs.length - (idx + 1)
        if remaining > 0 then {
          val avgTime     = totalElapsed / (idx + 1)
          val estimatedMs = avgTime * remaining
          val minutes     = (estimatedMs / 1000) / 60
          val seconds     = (estimatedMs / 1000) % 60
          println(s"  Estimated time remaining: ${minutes}m ${seconds}s ($remaining left)")
        }

        result
    }

    val finalEnrichedData = normalizedData
    finalEnrichedData.obj.update("cocktail_specs", ujson.Arr(enrichedSpecs.toSeq*))

    // update metadata
    val metadata = finalEnrichedData.obj.getOrElse("metadata", ujson.Obj()).obj
    metadata.update("enriched_at", ujson.Str(java.time.Instant.now().toString))
    metadata.update("total_specs", ujson.Num(enrichedSpecs.size))
    finalEnrichedData.obj.update("metadata", metadata)

    val enrichedFile = dataDir / "enriched_cocktail_data.json"
    os.write.over(enrichedFile, ujson.write(finalEnrichedData, indent = 2))
    println(s"Saved enriched dataset to: ${enrichedFile.last}")
  }

  @main(
    name = "suggestFormat",
    doc = "Ask LLM to suggest a coktail json format based on the data for each sheet"
  )
  def suggestFormat() = {
    println("Asking LLM to suggest a cocktail json format based on the data for each sheet...")

    val cfg = Config.load()
    require(cfg.sheetId.nonEmpty, "cocktails.sheet-id must be set in cocktail-analyzer.conf")

    val llm = LlmClient.fromConfig(cfg)

    def suggestFormat(data: String): String = {
      val prompt =
        "Based on the following data from a Google Sheet used to store cocktail-related data, " +
          "propose a robust JSON format to represent cocktail recipes parsed from this sheet. " +
          "Consider that one recipe may span multiple rows or columns. " +
          "Respond with only the JSON format definition."

      llm.complete(
        system = "You are a precise data analyst for cocktail datasets.",
        user = prompt + "\n\n" + data
      )
    }

    def combineFormats(formats: Seq[String]): String = {
      val prompt = "Given the following proposed JSON format definitions for cocktail recipes, " +
        "combine them into a single robust JSON format that can accommodate all the variations. " +
        "Respond with only the combined JSON format definition."

      llm.complete(
        system = "You are a precise data analyst for cocktail datasets.",
        user = prompt + "\n\n" + formats.mkString("\n\n")
      )
    }

    val csvFiles         = os.list(os.pwd / "data")
    val suggestedFormats = csvFiles.map {
      file =>
        val data       = os.read(file)
        val suggestion = suggestFormat(data)
        println(s"Suggested format for ${file.last}:\n$suggestion")
        suggestion
    }

    val combinedFormat = combineFormats(suggestedFormats)

    // Save combined format to file
    val outputPath = os.pwd / "data" / "combined_cocktail_format.json"
    os.write.over(outputPath, combinedFormat)
  }
