package nl.knaw.huc.di.cli

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.json.JSONArray
import org.json.JSONObject

object JsonLdTools {

    /**
     * Loads JSON-LD from a URL (follows redirects) and extracts namespace
     * mappings.
     */
    suspend fun extractNamespaces(jsonLdUrl: String): Result<Map<String, String>> = runCatching {
        withContext(Dispatchers.IO) {
            val finalUrl = followRedirects(jsonLdUrl)
            val model = ModelFactory.createDefaultModel()

            finalUrl.openStream().use { input ->
                RDFDataMgr.read(model, input, Lang.JSONLD)
            }

            model.nsPrefixMap
        }
    }

    fun mergeMaps(map1: Map<String, String>, map2: Map<String, String>): Map<String, String> {
        val result = map1.toMutableMap()
        val existingValues = map1.values.toMutableSet()

        for ((key, value) in map2) {
            if (value in existingValues) {
                // skip values already in map1
                continue
            }

            var newKey = key
            var counter = 1
            while (result.containsKey(newKey)) {
                newKey = "$key$counter"
                counter++
            }

            result[newKey] = value
            existingValues.add(value)
        }

        return result
    }

    fun addVocabContext(jsonld: String): String {
        val json = JSONObject(jsonld)
        val contexts = when (val contextObject = json.opt("@context")) {
            is JSONArray -> List(contextObject.length()) { i -> contextObject.get(i).toString() }
            else -> listOf(contextObject.toString())
        }
        if (contexts.none { it.contains("@vocab") }) {
            json.put("@context", contexts + mapOf("@vocab" to "urn:uncontextualized:"))
        }
        return json.toString(2)
    }

    /** Follows redirects (up to 5 times) before returning the resolved URL. */
    private fun followRedirects(urlString: String, maxRedirects: Int = 5): URL {
        var currentUrl = URL(urlString)
        var redirects = 0

        while (true) {
            val conn = currentUrl.openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"

            when (conn.responseCode) {
                HttpURLConnection.HTTP_MOVED_PERM,
                HttpURLConnection.HTTP_MOVED_TEMP,
                307, 308 -> {
                    if (redirects++ >= maxRedirects)
                        throw IllegalStateException("Too many redirects for $urlString")

                    val location = conn.getHeaderField("Location")
                        ?: throw IllegalStateException("Redirect without Location header for $urlString")

                    currentUrl = URL(currentUrl, location)
                    continue
                }

                else -> return currentUrl
            }
        }
    }

}
