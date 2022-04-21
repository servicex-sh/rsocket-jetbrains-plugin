package org.jetbrains.plugins.rsocket.restClient.execution

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.httpClient.execution.common.CommonClientRequest
import com.intellij.util.queryParameters
import io.rsocket.metadata.WellKnownMimeType
import java.net.URI
import java.util.*


@Suppress("UnstableApiUsage")
class RSocketRequest(override val URL: String?, override val httpMethod: String?, override val textToSend: String?, val headers: Map<String, String>) : CommonClientRequest {
    val rsocketURI: URI
    val dataMimeType: String
    val metadataMimeType: String
    val setupData: String?
    val setupMetadata: String?
    val metadata: String?
    val acceptMimeType: String?
    val authorization: String?
    val userAgent: String?
    val bodyText: String?
    var graphqlOperationName = "request"

    init {
        rsocketURI = URI.create(URL!!)
        this.metadataMimeType = headers["Metadata-Type"] ?: WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.string
        authorization = headers["Authorization"]
        userAgent = headers["User-Agent"]
        acceptMimeType = headers["Accept"]
        setupMetadata = headers["Setup-Metadata"]
        setupData = headers["Setup-Data"]
        metadata = headers["Metadata"]
        // graphql convert https://github.com/spring-projects/spring-graphql/issues/339
        var tempDataMimeType = headers["Content-Type"] ?: WellKnownMimeType.APPLICATION_JSON.string
        var tempBody = textToSend!!
        if (httpMethod == "GRAPHQLRS" || tempDataMimeType == "application/graphql") {
            val objectMapper = ObjectMapper()
            if (tempDataMimeType == "application/graphql") {
                if (tempBody.startsWith("subscription")) {
                    graphqlOperationName = "subscription"
                }
                val jsonRequest = mutableMapOf<String, Any>()
                jsonRequest["query"] = tempBody
                val graphqlVariables = getHeadValue("x-graphql-variables")
                if (graphqlVariables != null) {
                    jsonRequest["variables"] = objectMapper.readValue(graphqlVariables, Map::class.java)
                }
                tempBody = objectMapper.writeValueAsString(jsonRequest)
                tempDataMimeType = "application/json"
            } else if (tempDataMimeType.contains("json")) {  // application/graphql+json is not well-known type now
                tempDataMimeType = "application/json"
                val document = objectMapper.readValue<Map<String, Any>>(tempBody)
                if (document.contains("query")) {
                    if (document["query"].toString().contains("subscription")) {
                        graphqlOperationName = "subscription"
                    }
                }
            }
        }
        this.dataMimeType = tempDataMimeType
        this.bodyText = tempBody
    }

    fun routingMetadata(): List<String> {
        var path = rsocketURI.path ?: ""
        if (rsocketURI.scheme.contains("+ws") && path.startsWith("/rsocket")) {
            path = path.substring(8)
        }
        if (path.startsWith("/")) {
            path = path.substring(1)
        }
        val routing = mutableListOf(path);
        val params = rsocketURI.queryParameters
        if (params.isNotEmpty()) {
            params.forEach { (key, value) -> routing.add("${key}=${value}") }
        }
        return routing
    }

    fun acceptMimeTypeHint(): String {
        return acceptMimeType ?: dataMimeType
    }

    fun isAliBroker(): Boolean {
        return this.headers.contains("X-AliBroker")
    }

    fun isSpringBroker(): Boolean {
        return this.headers.contains("X-ServiceName")
    }

    fun getWebsocketRequestURI(): URI {
        var connectionURL = rsocketURI.toString()
        if (connectionURL.contains("/rsocket/")) { //rsocket as ws path
            connectionURL = connectionURL.substring(0, connectionURL.indexOf("/rsocket") + 8)
        } else { // without /rsocket
            connectionURL = connectionURL.substring(0, connectionURL.indexOf('/', 10) + 1)
        }
        connectionURL = connectionURL.replace("http://", "ws://").replace("https://", "wss://")
        return URI.create(connectionURL)
    }

    fun body(): ByteArray {
        return if (bodyText == null) {
            ByteArray(0)
        } else if (bodyText.startsWith("data:")) {
            val base64Text = bodyText.substring(bodyText.indexOf(",") + 1).trim()
            Base64.getDecoder().decode(base64Text);
        } else {
            if (dataMimeType.startsWith("application/json") && bodyText.startsWith("\"")) {
                bodyText.trim('"').toByteArray(Charsets.UTF_8)
            } else {
                bodyText.toByteArray(Charsets.UTF_8)
            }
        }
    }

    fun textToBytes(text: String?): ByteArray {
        return if (text == null) {
            ByteArray(0)
        } else if (text.startsWith("data:")) {
            val base64Text = text.substring(text.indexOf(",") + 1).trim()
            Base64.getDecoder().decode(base64Text);
        } else {
            text.toByteArray(Charsets.UTF_8)
        }
    }

    private fun getHeadValue(name: String): String? {
        for (header in headers) {
            if (header.key.lowercase() == name) {
                return header.value
            }
        }
        return null
    }

}