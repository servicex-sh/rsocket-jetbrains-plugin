package org.jetbrains.plugins.rsocket.restClient.execution

import com.intellij.httpClient.execution.common.CommonClientRequest
import com.intellij.util.queryParameters
import java.net.URI


@Suppress("UnstableApiUsage")
class RSocketRequest(override val URL: String?, override val httpMethod: String?, override val textToSend: String?, val headers: Map<String, String>?) : CommonClientRequest {
    val rsocketURI: URI
    val dataMimeTyp: String
    val metadataMimeTyp: String
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
                    tempUri = "tcp://$URL"
                } else { // get host information from header
                    val host = headers?.get("host") ?: "localhost"
                    tempUri = if (URL.startsWith("/")) {
                        "tcp://$host$URL"
                    } else {
                        "tcp://$host/$URL"
                    }
                }
            }
        }
        rsocketURI = URI.create(tempUri)
        dataMimeTyp = headers?.get("content-type") ?: "application/json"
        metadataMimeTyp = headers?.get("metadata-type") ?: "message/x.rsocket.composite-metadata.v0"
        authorization = headers?.get("authorization")
        userAgent = headers?.get("user-agent")
        acceptMimeType = headers?.get("accept")
    }

    fun routingMetadata(): List<String> {
        var path = rsocketURI.path ?: ""
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
}