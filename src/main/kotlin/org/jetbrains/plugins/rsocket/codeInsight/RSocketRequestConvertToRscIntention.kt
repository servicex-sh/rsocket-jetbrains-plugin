package org.jetbrains.plugins.rsocket.codeInsight

import com.intellij.codeInsight.intention.BaseElementAtCaretIntentionAction
import com.intellij.httpClient.http.request.HttpRequestVariableSubstitutor
import com.intellij.httpClient.http.request.psi.HttpRequest
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.plugins.rsocket.restClient.execution.RSocketRequest
import org.jetbrains.plugins.rsocket.restClient.execution.RSocketRequestExecutionSupport
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection


class RSocketRequestConvertToRscIntention : BaseElementAtCaretIntentionAction() {
    override fun getFamilyName(): String {
        return "Convert to rsc and copy to clipboard"
    }

    override fun getText(): String {
        return this.familyName
    }

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        val httpRequest = PsiTreeUtil.getParentOfType(element, HttpRequest::class.java)
        return httpRequest != null && RSocketRequestExecutionSupport.RSOCKET_REQUEST_TYPES.contains(httpRequest.httpMethod)
    }

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        val httpRequest = PsiTreeUtil.getParentOfType(element, HttpRequest::class.java)
        val substitutor = HttpRequestVariableSubstitutor.getDefault(project);
        val headers = httpRequest?.headerFieldList?.associate { it.name to it.getValue(substitutor) }
        var requestType = httpRequest?.httpMethod
        if (requestType == "RSOCKET") {
            requestType = "RPC"
        }
        val rsocketRequest = RSocketRequest(httpRequest?.getHttpUrl(substitutor), requestType, httpRequest?.requestBody?.text, headers)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val testData = StringSelection(convertToRscCli(rsocketRequest))
        clipboard.setContents(testData, testData)
    }

    override fun startInWriteAction(): Boolean {
        return false
    }

    private fun convertToRscCli(rsocketRequest: RSocketRequest): String {
        val requestType = when (rsocketRequest.httpMethod) {
            "RPC" -> "request"
            else -> rsocketRequest.httpMethod?.lowercase()
        }
        var data = rsocketRequest.textToSend ?: ""
        if (rsocketRequest.dataMimeTyp == "application/json") {
            data = data.replace("\\s+", "")
        }
        var extra = ""
        if (rsocketRequest.isAliBroker()) {
            extra = "--setupMetadata '{\"ip\":\"127.0.0.1\",\"name\":\"MockApp\",\"sdk\":\"SpringBoot/2.5.7\",\"device\":\"JavaApp\"}' " +
                    "--setupMetadataMimeType \"message/x.rsocket.application+json\""
        }
        return "rsc --${requestType} --route ${rsocketRequest.routingMetadata()[0]} --data='$data' ${extra} ${rsocketRequest.rsocketURI}"
    }

}