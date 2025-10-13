package nl.knaw.huc.di.cli

import java.io.ByteArrayOutputStream
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.coroutines.runBlocking
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.apache.logging.log4j.kotlin.logger
import nl.knaw.huc.di.cli.JsonLdTools.addVocabContext
import nl.knaw.huc.di.cli.JsonLdTools.extractNamespaces
import nl.knaw.huc.di.cli.JsonLdTools.mergeMaps

@OptIn(ExperimentalCli::class)
fun main(args: Array<String>) {
    val parser = ArgParser("jsonld2ttl")
    val jsonldPath by parser.argument(
        type = ArgType.String,
        fullName = "jsonld_file",
        description = "The jsonld file to convert"
    )
    val arguments = if (args.isEmpty()) arrayOf("-h") else args
    parser.parse(arguments)
    val ttl = toTTL(Path(jsonldPath).readText())
    println(ttl)
}

fun toTTL(jsonld: String): String =
    runBlocking {
        val (enhancedContext, jsonLdWithVocabContext) = addVocabContext(jsonld)
        val jsonldInputStream = jsonLdWithVocabContext.byteInputStream()

        val mergedNsPrefixMap = enhancedContext.filter { it is String }
            .map { extractNamespaces(it as String) }
            .also { resultList -> resultList.filter { it.isFailure }.forEach { logger.warn { it.exceptionOrNull() } } }
            .filter { r -> r.isSuccess }
            .map { it.getOrNull() ?: emptyMap() }
//            .map { it - "rdf" } // ignore the rdf mapping
            .fold(emptyMap<String, String>()) { acc, map -> mergeMaps(acc, map) }

        val model = ModelFactory.createDefaultModel().apply {
            mergedNsPrefixMap.forEach { entry ->
                setNsPrefix(entry.key, entry.value)
            }
        }
        RDFDataMgr.read(model, jsonldInputStream, "", Lang.JSONLD)

        val statementElementSet = model.listStatements().asSequence()
            .flatMap { listOf(it.`object`, it.predicate, it.subject) }
            .map { it.toString() }
            .toSet()
        val uncontextualized =
            statementElementSet
                .filter { it.startsWith(UNCONTEXTUALIZED_PREFIX) }
                .map { it.substringAfter(UNCONTEXTUALIZED_PREFIX) }
        if (uncontextualized.isNotEmpty()) {
            logger.warn { "Uncontextualized elements found:\n  ${uncontextualized.sorted().joinToString("\n  ")}" }
        } else {
            model.removeNsPrefix("")
        }

        val prefix2namespaceMap = mergedNsPrefixMap.map { it.value to it.key }.toMap()
        val usedNamespaces = statementElementSet
            .mapNotNull { e -> prefix2namespaceMap.keys.firstOrNull { prefix -> e.startsWith(prefix) } }
            .map { p -> prefix2namespaceMap[p] }
            .toSet()
        val unusedNamespaces = mergedNsPrefixMap.keys - usedNamespaces
        unusedNamespaces.forEach { model.removeNsPrefix(it) }

        val baos = ByteArrayOutputStream()
        RDFDataMgr.write(baos, model, Lang.TURTLE)
        baos.toString()
            .replace(" rdf:type ", " a ")
    }

