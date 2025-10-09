package nl.knaw.huc.di.cli

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlinx.coroutines.runBlocking
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
              "@context": "http://www.w3.org/ns/anno.jsonld",
              "id": "urn:example:something",
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
        val expected = """PREFIX :    <urn:example:uncontextualized:>
PREFIX oa:  <http://www.w3.org/ns/oa#>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

<urn:example:something>
        a        oa:Annotation;
        oa:hasBody      [ a     :MyType;
                          rdf:value    "I like this page!";
                          :custom_url  "http://this-is-no-identifier.com";
                          :extra       "additional value"
                        ];
        oa:hasTarget    <http://www.example.com/index.html>;
        :mycustomfield  "my custom value" .
"""
//        val errorHandler = ErrorHandlerFactory.errorHandlerWarn
//        val dataset = RDFParser.create()
//            .source(jsonld.byteInputStream())
//            .lang(RDFLanguages.JSONLD)
//            .errorHandler(errorHandler)
//            .base("http://example/base")
//            .toDataset()
//        val m = dataset.defaultModel
//        RDFDataMgr.write(System.out, m, Lang.TTL)

        println("\n============================\n")
        val ttl = toTTL(jsonld)
        println(ttl)

        assert(ttl.contains("mycustomfield"))
        assert(ttl.contains("extra"))

        assertEquals(expected, ttl)

    }

    @Nested
    inner class AddVocabTest {

        val jsonld = """
            {
              "@context": "http://www.w3.org/ns/anno.jsonld"},
              "id": "urn:example:something",
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
        val vocabDef = """"@vocab": "$UNCONTEXTUALIZED_PREFIX"""

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
            val (context, withVocabContext) = addVocabContext(json)
            println(withVocabContext)
            println(context)
            assert(withVocabContext.contains(vocabDef))
        }

        @Test
        fun `add vocab to context array`() {
            val json = """{"@context":["http://example.com","http://example2.com","http://example3.com"]}"""
            val (context, withVocabContext) = addVocabContext(json)
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
