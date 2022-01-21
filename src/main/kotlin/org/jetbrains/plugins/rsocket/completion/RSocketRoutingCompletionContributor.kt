package org.jetbrains.plugins.rsocket.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.httpClient.http.request.psi.HttpHost
import com.intellij.httpClient.http.request.psi.HttpPathAbsolute
import com.intellij.httpClient.http.request.psi.HttpRequest
import com.intellij.httpClient.http.request.psi.HttpRequestTarget
import com.intellij.openapi.util.text.StringUtil
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext
import org.jetbrains.plugins.rsocket.RSOCKET_REQUEST_TYPES
import org.jetbrains.plugins.rsocket.file.RSocketServiceFileIndex
import org.jetbrains.plugins.rsocket.psi.convertToRSocketRequestType
import org.jetbrains.plugins.rsocket.psi.extractAliRSocketService
import org.jetbrains.plugins.rsocket.psi.extractValueFromMessageMapping
import org.jetbrains.plugins.rsocket.rsocketIcon

class RSocketRoutingCompletionContributor : CompletionContributor() {
    companion object {
        val rsocketRoutingCapture: PsiElementPattern.Capture<LeafPsiElement> = PlatformPatterns.psiElement(LeafPsiElement::class.java)
            .withSuperParent(2,HttpRequestTarget::class.java)
    }

    init {
        extend(CompletionType.BASIC, rsocketRoutingCapture, RSocketRoutingProvider())
    }

    private class RSocketRoutingProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
            val element = parameters.position
            val prefix = trimDummy(element.text)
            val httpRequest = element.parentOfType<HttpRequest>()
            if (httpRequest != null && RSOCKET_REQUEST_TYPES.contains(httpRequest.httpMethod)) {
                val rsocketRequestMethod = httpRequest.httpMethod
                RSocketServiceFileIndex.findRSocketServiceFiles(httpRequest.project).forEach { psiFile ->
                    if (psiFile is PsiJavaFile) {
                        val psiJavaClass = psiFile.classes[0]
                        val rsocketService = psiJavaClass.hasAnnotation("com.alibaba.rsocket.RSocketService")
                        if (rsocketService) { // AliRSocket
                            val aliRSocketService = extractAliRSocketService(psiJavaClass)
                            val serviceFullName = aliRSocketService.serviceName
                            if (prefix.isEmpty() || serviceFullName.contains(prefix)) { // class name completion
                                result.addElement(LookupElementBuilder.create(serviceFullName).withIcon(rsocketIcon))
                            } else if (prefix.contains(serviceFullName)) {  // method completion
                                aliRSocketService.serviceInterface
                                    .methods
                                    .forEach {
                                        val requestType = convertToRSocketRequestType(it.returnType?.canonicalText)
                                        if (isRequestTypeMatch(requestType, rsocketRequestMethod)) {
                                            val routingKey = "${serviceFullName}.${it.name}"
                                            result.addElement(LookupElementBuilder.create(routingKey).withIcon(rsocketIcon))
                                        }
                                    }
                            }
                        } else { //MessageMapping
                            var baseNameSpace = extractValueFromMessageMapping(psiJavaClass)
                            baseNameSpace = if (baseNameSpace == null) {
                                ""
                            } else {
                                "${baseNameSpace}."
                            }
                            if (prefix.isEmpty() || baseNameSpace.isNotEmpty()
                                || baseNameSpace.contains(prefix) || prefix.contains(baseNameSpace)
                            ) {
                                psiJavaClass.methods
                                    .filter {
                                        it.hasAnnotation("org.springframework.messaging.handler.annotation.MessageMapping")
                                    }.forEach {
                                        val mappingValue = extractValueFromMessageMapping(it) ?: it.name
                                        val routingKey = "${baseNameSpace}${mappingValue}"
                                        val requestType = convertToRSocketRequestType(it.returnType?.canonicalText)
                                        if (isRequestTypeMatch(requestType, rsocketRequestMethod)) {
                                            if (prefix.isEmpty() || routingKey.contains(prefix)) {
                                                result.addElement(LookupElementBuilder.create(routingKey).withIcon(rsocketIcon))
                                            }
                                        }
                                    }
                            }
                        }
                    }
                }
            }
        }

        private fun trimDummy(value: String): String {
            return StringUtil.trim(value.replace(CompletionUtil.DUMMY_IDENTIFIER, "").replace(CompletionUtil.DUMMY_IDENTIFIER_TRIMMED, ""))
        }

        private fun isRequestTypeMatch(requestType: String, rsocketRequestMethod: String): Boolean {
            return if (rsocketRequestMethod == requestType) {
                true
            } else
                (rsocketRequestMethod == "RSOCKET" || rsocketRequestMethod == "RPC") && requestType == "REQUEST"
        }
    }


}