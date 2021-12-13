package org.jetbrains.plugins.rsocket.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.httpClient.http.request.psi.HttpHeaderField
import com.intellij.httpClient.http.request.psi.HttpHeaderFieldName
import com.intellij.httpClient.http.request.psi.HttpRequest
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.jetbrains.plugins.rsocket.restClient.execution.RSocketRequestExecutionSupport.Companion.RSOCKET_REQUEST_TYPES

class RSocketRequestCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement().withParent(HttpHeaderFieldName::class.java), RSocketHeaderFieldNamesProvider())
    }

    private class RSocketHeaderFieldNamesProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
            val currentHeader = PsiTreeUtil.getParentOfType(CompletionUtil.getOriginalOrSelf(parameters.position), HttpHeaderField::class.java);
            val httpRequest = PsiTreeUtil.getParentOfType(currentHeader, HttpRequest::class.java)
            if (httpRequest != null && RSOCKET_REQUEST_TYPES.contains(httpRequest.httpMethod)) {
                var isSpringBroker = false
                var isAliBroker = false
                httpRequest.headerFieldList.forEach {
                    val headerName = it.name
                    if (headerName == "X-AliBroker") {
                        isAliBroker = true
                    }
                    if (headerName == "X-ServiceName") {
                        isSpringBroker = true
                    }
                }
                if (isSpringBroker) {
                    result.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create("X-InstanceName"), 100.0))
                    result.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create("X-MajorVersion"), 100.0))
                    result.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create("X-ClusterName"), 100.0))
                } else if (isAliBroker) {
                    result.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create("X-Endpoint-UUID"), 100.0))
                    result.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create("X-Endpoint-Ip"), 100.0))
                } else {
                    result.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create("X-AliBroker"), 100.0))
                    result.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create("X-ServiceName"), 100.0))
                }
            }
        }
    }
}