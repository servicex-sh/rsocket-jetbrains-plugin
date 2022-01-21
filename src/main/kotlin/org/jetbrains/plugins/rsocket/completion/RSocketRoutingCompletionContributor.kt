package org.jetbrains.plugins.rsocket.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.httpClient.http.request.psi.HttpHost
import com.intellij.httpClient.http.request.psi.HttpRequest
import com.intellij.openapi.util.text.StringUtil
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext
import org.jetbrains.plugins.rsocket.RSOCKET_REQUEST_TYPES
import org.jetbrains.plugins.rsocket.endpoints.RSocketEndpoint
import org.jetbrains.plugins.rsocket.file.RSocketServiceFileIndex
import org.jetbrains.plugins.rsocket.psi.convertToRSocketRequestType
import org.jetbrains.plugins.rsocket.psi.extractAliRSocketService
import org.jetbrains.plugins.rsocket.psi.extractValueFromMessageMapping
import org.jetbrains.plugins.rsocket.rsocketIcon

class RSocketRoutingCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement().withParent(HttpHost::class.java), RSocketRoutingProvider())
    }

    private class RSocketRoutingProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
            val element = parameters.position
            val prefix = trimDummy(element.text)
            val httpRequest = element.parentOfType<HttpRequest>()
            if (httpRequest != null && RSOCKET_REQUEST_TYPES.contains(httpRequest.httpMethod)) {
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
                                aliRSocketService.serviceInterface.methods.forEach {
                                    result.addElement(LookupElementBuilder.create("${serviceFullName}.${it.name}").withIcon(rsocketIcon))
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
                                    }.map {
                                        val routingKey = extractValueFromMessageMapping(it) ?: it.name
                                        val requestType = convertToRSocketRequestType(it.returnType?.canonicalText)
                                        RSocketEndpoint("[$requestType]", "${baseNameSpace}${routingKey}", it)
                                    }.forEach {
                                        if (prefix.isEmpty() || it.routing.contains(prefix)) {
                                            result.addElement(LookupElementBuilder.create(it.routing).withIcon(rsocketIcon))
                                        }
                                    }
                            }
                        }
                    }
                }
            }
        }

        protected fun trimDummy(value: String): String {
            return StringUtil.trim(value.replace(CompletionUtil.DUMMY_IDENTIFIER, "").replace(CompletionUtil.DUMMY_IDENTIFIER_TRIMMED, ""))
        }
    }


}