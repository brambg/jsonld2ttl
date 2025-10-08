package nl.knaw.huc.di.cli

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlinx.coroutines.runBlocking
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.RDFLanguages
import org.apache.jena.riot.RDFParser
import org.apache.jena.riot.system.ErrorHandlerFactory
import nl.knaw.huc.di.cli.JsonLdTools.addVocabContext
import nl.knaw.huc.di.cli.JsonLdTools.extractNamespaces
import nl.knaw.huc.di.cli.JsonLdTools.mergeMaps

class JsonLd2TtlTest {
    @Test
    fun `extract namespaces from given jsonld url`() {
        runBlocking {
            val namespaceMap = extractNamespaces("http://www.w3.org/ns/anno.jsonld").getOrThrow()
            namespaceMap.forEach { println(it) }
            assert(namespaceMap.isNotEmpty())
        }
    }

    val jsonld = """
            {
              "@context": ["http://www.w3.org/ns/anno.jsonld",{"@vocab":"urn:uncontextualized:"}],
              "id": "urn:something",
              "type": "Annotation",
              "body": {
                "type": "MyType",
                "extra": "additional value",
                "value": "I like this page!",
                "custom_url":"http://this-is-no-identifier.com"
              },
              "target": "http://www.example.com/index.html",
              "mycustomfield": "my custom value"
            }
        """.trimMargin()

    @Test
    fun `correctly convert valid jsonld`() {
        val expected = """@prefix as:      <http://www.w3.org/ns/activitystreams#> .
@prefix dc:      <http://purl.org/dc/elements/1.1/> .
@prefix dcterms: <http://purl.org/dc/terms/> .
@prefix dctypes: <http://purl.org/dc/dcmitype/> .
@prefix ex:      <http://example.org/> .
@prefix foaf:    <http://xmlns.com/foaf/0.1/> .
@prefix iana:    <http://www.iana.org/assignments/relation/> .
@prefix oa:      <http://www.w3.org/ns/oa#> .
@prefix owl:     <http://www.w3.org/2002/07/owl#> .
@prefix rdfs:    <http://www.w3.org/2000/01/rdf-schema#> .
@prefix schema:  <http://schema.org/> .
@prefix skos:    <http://www.w3.org/2004/02/skos/core#> .
@prefix xsd:     <http://www.w3.org/2001/XMLSchema#> .

<urn:something>  a        oa:Annotation;
        ex:mycustomfield  "my custom value";
        oa:hasBody        [ a         oa:TextualBody;
                            <http://www.w3.org/1999/02/22-rdf-syntax-ns#value>
                                    "I like this page!";
                            extra  "additional value"
                          ];
        oa:hasTarget      <http://www.example.com/index.html> .
"""
        val errorHandler = ErrorHandlerFactory.errorHandlerWarn
        val dataset = RDFParser.create()
            .source(jsonld.byteInputStream())
            .lang(RDFLanguages.JSONLD)
            .errorHandler(errorHandler)
            .base("http://example/base")
            .toDataset()
        val m = dataset.defaultModel
        RDFDataMgr.write(System.out, m, Lang.TTL)

        println("\n============================\n")
        val ttl = toTTL(jsonld.byteInputStream())
        println(ttl)

        println("\n============================\n")
        val ttl2 = toTTL2(jsonld.byteInputStream())
        println(ttl2)
//        assertEquals(expected, ttl)

        assert(ttl.contains("mycustomfield"))
        assert(ttl.contains("extra"))

        assert(ttl2.contains("mycustomfield"))
        assert(ttl2.contains("extra"))
    }

    @Nested
    inner class AddVocabTest {

        val jsonld = """
            {
              "@context": ["http://www.w3.org/ns/anno.jsonld",{"@vocab":"urn:uncontextualized:"}],
              "id": "urn:something",
              "type": "Annotation",
              "body": {
                "type": "MyType",
                "extra": "additional value",
                "value": "I like this page!",
                "custom_url":"http://this-is-no-identifier.com"
              },
              "target": "http://www.example.com/index.html",
              "mycustomfield": "my custom value"
            }
        """.trimMargin()
        val vocabDef = """"@vocab": "urn:uncontextualized:"""

        @Test
        fun `add vocab to context if it doesn't exist`() {
            val (context, withVocabContext) = addVocabContext(jsonld)
            println(withVocabContext)
            println(context)
            assert(withVocabContext.contains(vocabDef))
        }

        @Test
        fun `add vocab to simple context`() {
            val json = """{"@context":"http://example.com"}"""
            val (context, withVocabContext) = addVocabContext(json)
            println(withVocabContext)
            println(context)
            assert(withVocabContext.contains(vocabDef))
        }

        @Test
        fun `add vocab to context object`() {
            val json = """{"@context":{"e1": "http://example.com","e2":"http://example2.com"}}"""
            val (context, withVocabContext) =  addVocabContext(json)
            println(withVocabContext)
            println(context)
            assert(withVocabContext.contains(vocabDef))
        }

        @Test
        fun `add vocab to context array`() {
            val json = """{"@context":["http://example.com","http://example2.com","http://example3.com"]}"""
            val (context, withVocabContext) =  addVocabContext(json)
            println(withVocabContext)
            println(context)
            assert(withVocabContext.contains(vocabDef))
        }

    }

    @Test
    fun `test mergeMaps`() {
        val nsPrefix1 = mapOf(
            "foaf" to "http://xmlns.com/foaf/0.1/",
            "dc" to "http://purl.org/dc/elements/1.1/"
        )
        val nsPrefix2 = mapOf(
            "foaf" to "http://xmlns.com/foaf/0.2/",
            "dc" to "http://purl.org/dc/elements/1.1/",
            "dcterms" to "http://purl.org/dc/terms/",
            "iana" to "http://www.iana.org/assignments/relation/"
        )
        val expectedMergedNsPrefix = mapOf(
            "foaf" to "http://xmlns.com/foaf/0.1/",
            "dc" to "http://purl.org/dc/elements/1.1/",
            "foaf1" to "http://xmlns.com/foaf/0.2/",
            "dcterms" to "http://purl.org/dc/terms/",
            "iana" to "http://www.iana.org/assignments/relation/"
        )
        assertEquals(expectedMergedNsPrefix, mergeMaps(nsPrefix1, nsPrefix2))
    }

}
