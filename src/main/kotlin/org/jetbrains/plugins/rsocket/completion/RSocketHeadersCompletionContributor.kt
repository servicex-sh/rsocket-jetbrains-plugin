package org.jetbrains.plugins.rsocket.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.httpClient.http.request.psi.HttpHeaderField
import com.intellij.httpClient.http.request.psi.HttpHeaderFieldName
import com.intellij.httpClient.http.request.psi.HttpRequest
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.jetbrains.plugins.rsocket.RSOCKET_REQUEST_TYPES
import org.jetbrains.plugins.rsocket.rsocketIcon

class RSocketHeadersCompletionContributor : CompletionContributor() {
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
                    result.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create("X-InstanceName").withIcon(rsocketIcon), Double.MAX_VALUE))
                    result.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create("X-MajorVersion").withIcon(rsocketIcon), Double.MAX_VALUE))
                    result.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create("X-ClusterName").withIcon(rsocketIcon), Double.MAX_VALUE))
                } else if (isAliBroker) {
                    result.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create("X-Endpoint-UUID").withIcon(rsocketIcon), Double.MAX_VALUE))
                    result.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create("X-Endpoint-Ip").withIcon(rsocketIcon), Double.MAX_VALUE))
                } else {
                    result.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create("Setup-Metadata").withIcon(rsocketIcon), Double.MAX_VALUE))
                    result.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create("Setup-Data").withIcon(rsocketIcon), Double.MAX_VALUE))
                    result.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create("Metadata-Type").withIcon(rsocketIcon), Double.MAX_VALUE))
                    result.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create("Metadata").withIcon(rsocketIcon), Double.MAX_VALUE))
                    result.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create("X-AliBroker").withIcon(rsocketIcon), Double.MAX_VALUE))
                    result.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create("X-ServiceName").withIcon(rsocketIcon), Double.MAX_VALUE))

                }
            }
        }
    }
}