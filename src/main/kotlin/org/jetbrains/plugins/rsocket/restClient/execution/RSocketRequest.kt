package org.jetbrains.plugins.rsocket.restClient.execution

import com.intellij.httpClient.execution.common.CommonClientRequest
import com.intellij.httpClient.http.request.psi.HttpHeaderField


@Suppress("UnstableApiUsage")
class RSocketRequest(override val URL: String?, override val httpMethod: String?, override val textToSend: String?, val headers: List<HttpHeaderField>?) : CommonClientRequest {
}