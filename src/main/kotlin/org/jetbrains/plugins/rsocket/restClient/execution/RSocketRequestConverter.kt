package org.jetbrains.plugins.rsocket.restClient.execution

import com.intellij.httpClient.execution.common.RequestConverter
import com.intellij.httpClient.http.request.HttpRequestVariableSubstitutor
import com.intellij.httpClient.http.request.psi.HttpRequest
import com.intellij.psi.SmartPsiElementPointer


@Suppress("UnstableApiUsage")
class RSocketRequestConverter : RequestConverter<RSocketRequest>() {

    override val requestType: Class<RSocketRequest> get() = RSocketRequest::class.java

    override fun psiToCommonRequest(requestPsiPointer: SmartPsiElementPointer<HttpRequest>, substitutor: HttpRequestVariableSubstitutor): RSocketRequest {
        val httpRequest = requestPsiPointer.element
        return RSocketRequest(httpRequest?.getHttpUrl(substitutor), httpRequest?.httpMethod, httpRequest?.requestBody?.text, httpRequest?.headerFieldList)
    }

    override fun toExternalFormInner(request: RSocketRequest, fileName: String?): String {
        return ""
    }

}