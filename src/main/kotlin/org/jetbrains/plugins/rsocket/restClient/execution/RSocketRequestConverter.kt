package org.jetbrains.plugins.rsocket.restClient.execution

import com.intellij.httpClient.execution.common.RequestConverter
import com.intellij.httpClient.http.request.HttpRequestVariableSubstitutor
import com.intellij.httpClient.http.request.psi.HttpRequest
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.SmartPsiElementPointer


@Suppress("UnstableApiUsage")
class RSocketRequestConverter : RequestConverter<RSocketRequest>() {

    override val requestType: Class<RSocketRequest> get() = RSocketRequest::class.java

    override fun psiToCommonRequest(requestPsiPointer: SmartPsiElementPointer<HttpRequest>, substitutor: HttpRequestVariableSubstitutor): RSocketRequest {
        var httpRequest: HttpRequest? = null
        ApplicationManager.getApplication().runReadAction {
            httpRequest = requestPsiPointer.element
        }
        val headers = httpRequest?.headerFieldList?.associate { it.name to it.getValue(substitutor) }
        var requestType = httpRequest?.httpMethod
        if (requestType == "RSOCKET") {
            requestType = "RPC"
        }
        return RSocketRequest(httpRequest?.getHttpUrl(substitutor), requestType, httpRequest?.requestBody?.text, headers)
    }

    override fun toExternalFormInner(request: RSocketRequest, fileName: String?): String {
        val builder = StringBuilder()
        builder.append("### rsocket request").append("\n")
        builder.append("RSOCKET ${request.rsocketURI}").append("\n")
        builder.append("\n");
        builder.append(request.textToSend ?: "")
        return builder.toString()
    }

}