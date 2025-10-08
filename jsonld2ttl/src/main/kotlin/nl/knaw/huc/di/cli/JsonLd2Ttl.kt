package nl.knaw.huc.di.cli

import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.io.path.Path
import kotlin.io.path.inputStream
import com.github.jsonldjava.core.JsonLdOptions
import com.github.jsonldjava.core.JsonLdProcessor
import com.github.jsonldjava.utils.JsonUtils
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr

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
    val ttl = toTTL(Path(jsonldPath).inputStream())
    println(ttl)
}

fun toTTL(jsonldInputStream: InputStream): String {
    val jsonObject = JsonUtils.fromInputStream(jsonldInputStream)

    val map = jsonObject as MutableMap<String, Any>
    val context = map["@context"]
    val vocabDef = mapOf("@vocab" to "http://customfields.org/#")
    val enhancedContext = when {
        context == null -> {
            vocabDef
        }

        context is String -> listOf(context, vocabDef)
        context is List<*> -> context.toMutableList().apply { add(vocabDef) }
        else -> ""
    }

    map["@context"] = enhancedContext
    // Customise context...
    // Create an instance of JsonLdOptions with the standard JSON-LD options
    val options = JsonLdOptions()
    options.explicit = true

    // Customise options...
    // Call whichever JSONLD function you want! (e.g. compact)
    val compact = JsonLdProcessor.compact(map, enhancedContext, options)
//    val rdf = JsonLdProcessor.toRDF(map, options)
//    println(JsonUtils.toPrettyString(rdf))

    return (JsonUtils.toPrettyString(compact))
}

fun toTTL2(jsonldInputStream: InputStream): String {
    val model = ModelFactory.createDefaultModel().apply {
//        setNsPrefix("", "http://namespaces_r_us/ns#")
//        setNsPrefix("oa", "http://www.w3.org/ns/oa#")
//        setNsPrefix("dc", "http://purl.org/dc/elements/1.1/")
//        setNsPrefix("dcterms", "http://purl.org/dc/terms/")
//        setNsPrefix("dctypes", "http://purl.org/dc/dcmitype/")
//        setNsPrefix("foaf", "http://xmlns.com/foaf/0.1/")
////            setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
//        setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#")
//        setNsPrefix("skos", "http://www.w3.org/2004/02/skos/core#")
//        setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#")
//        setNsPrefix("iana", "http://www.iana.org/assignments/relation/")
//        setNsPrefix("owl", "http://www.w3.org/2002/07/owl#")
//        setNsPrefix("as", "http://www.w3.org/ns/activitystreams#")
//        setNsPrefix("schema", "http://schema.org/")
    }
    RDFDataMgr.read(model, jsonldInputStream, "http://example.org/ns#", Lang.JSONLD)
    val baos = ByteArrayOutputStream()
    model.listStatements().forEach { println(it) }
    val vocabPrefix = "urn:uncontextualized:"
    val uncontextualized =
        model.listStatements().asSequence()
            .flatMap { listOf(it.`object`, it.predicate, it.subject) }
            .map { it.toString() }.toSet().filter { it.startsWith(vocabPrefix) }
            .map { it.substringAfter(vocabPrefix) }
    println(uncontextualized)
    RDFDataMgr.write(baos, model, Lang.TTL)
    return baos.toString()
}
