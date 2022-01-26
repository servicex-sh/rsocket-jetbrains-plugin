package org.jetbrains.plugins.rsocket.restClient.execution

import com.intellij.httpClient.execution.common.CommonClientRequest
import com.intellij.util.queryParameters
import io.rsocket.metadata.WellKnownMimeType
import java.net.URI
import java.util.*


@Suppress("UnstableApiUsage")
class RSocketRequest(override val URL: String?, override val httpMethod: String?, override val textToSend: String?, val headers: Map<String, String>?) : CommonClientRequest {
    val rsocketURI: URI
    val dataMimeTyp: String
    val metadataMimeType: String
    val setupData: String?
    val setupMetadata: String?
    val metadata: String?
    val acceptMimeType: String?
    val authorization: String?
    val userAgent: String?

    init {
        var tempUri = "tcp://localhost:42252"
        if (URL != null && URL.trim().isNotEmpty() && (URL.trim() != "/")) {
            if (URL.contains("://")) { //with schema
                tempUri = URL.replace("http://", "ws://").replace("https://", "wss://")
            } else {
                if (URL.indexOf("/") > 0) { //contains host
                    tempUri = if (URL.contains("/rsocket")) {
                        "ws://$URL"
                    } else {
                        "tcp://$URL"
                    }
                } else { // get host information from header
                    val host = headers?.get("Host") ?: "localhost"
                    tempUri = if (host.contains("://")) {
                        "$host/$URL"
                    } else {
                        "tcp://$host/$URL"
                    }
                }
            }
        }
        rsocketURI = URI.create(tempUri)
        dataMimeTyp = headers?.get("Content-Type") ?: WellKnownMimeType.APPLICATION_JSON.string
        metadataMimeType = headers?.get("Metadata-Type") ?: WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.string
        authorization = headers?.get("Authorization")
        userAgent = headers?.get("User-Agent")
        acceptMimeType = headers?.get("Accept")
        setupMetadata = headers?.get("Setup-Metadata")
        setupData = headers?.get("Setup-Data")
        metadata = headers?.get("Metadata")
    }

    fun routingMetadata(): List<String> {
        var path = rsocketURI.path ?: ""
        if (rsocketURI.scheme.startsWith("ws") && path.startsWith("/rsocket")) {
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
        return acceptMimeType ?: dataMimeTyp
    }

    fun isAliBroker(): Boolean {
        return this.headers != null && this.headers.contains("X-AliBroker")
    }

    fun isSpringBroker(): Boolean {
        return this.headers != null && this.headers.contains("X-ServiceName")
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
        return if (textToSend == null) {
            ByteArray(0)
        } else if (textToSend.startsWith("data:")) {
            val base64Text = textToSend.substring(textToSend.indexOf(",") + 1).trim()
            Base64.getDecoder().decode(base64Text);
        } else {
            if (dataMimeTyp.startsWith("application/json") && textToSend.startsWith("\"")) {
                textToSend.trim('"').toByteArray(Charsets.UTF_8)
            } else {
                textToSend.toByteArray(Charsets.UTF_8)
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

}