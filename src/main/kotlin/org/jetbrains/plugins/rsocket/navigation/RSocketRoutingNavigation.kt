package org.jetbrains.plugins.rsocket.navigation

import com.intellij.httpClient.http.request.psi.HttpRequestTarget
import com.intellij.navigation.DirectNavigationProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.jetbrains.plugins.rsocket.completion.RSocketRoutingCompletionContributor.Companion.rsocketRoutingCapture
import org.jetbrains.plugins.rsocket.file.RSocketServiceFileIndex
import org.jetbrains.plugins.rsocket.psi.extractAliRSocketService
import org.jetbrains.plugins.rsocket.psi.extractFirstClassFromJavaOrKt
import org.jetbrains.plugins.rsocket.psi.extractValueFromMessageMapping
import org.jetbrains.plugins.rsocket.rsocketServiceFullName


@Suppress("UnstableApiUsage")
class RSocketRoutingNavigation : DirectNavigationProvider {
    override fun getNavigationElement(element: PsiElement): PsiElement? {
        if (rsocketRoutingCapture.accepts(element)) {
            val httpRequestTarget = element.parentOfType<HttpRequestTarget>()!!
            val host = httpRequestTarget.host
            val pathAbsolute = httpRequestTarget.pathAbsolute
            if (host != null || pathAbsolute != null) {
                val rsocketRouting = if (pathAbsolute != null) {
                    val path = pathAbsolute.text
                    path.substring(path.lastIndexOf('/') + 1)
                } else {
                    host!!.text
                }
                if (rsocketRouting.isNotEmpty()) {
                    RSocketServiceFileIndex.findRSocketServiceFiles(element.project).forEach { psiFile ->
                        val psiJavaClass = extractFirstClassFromJavaOrKt(psiFile)
                        if (psiJavaClass != null) {
                            val rsocketService = psiJavaClass.hasAnnotation(rsocketServiceFullName)
                            if (rsocketService) {
                                val aliRSocketService = extractAliRSocketService(psiJavaClass)
                                val serviceFullName = aliRSocketService.serviceName
                                if (rsocketRouting.startsWith(serviceFullName)) {
                                    psiJavaClass
                                        .methods
                                        .forEach {
                                            val routingKey = "${serviceFullName}.${it.name}"
                                            if (routingKey == rsocketRouting) {
                                                return it
                                            }
                                        }
                                }
                            } else {
                                var baseNameSpace = extractValueFromMessageMapping(psiJavaClass)
                                baseNameSpace = if (baseNameSpace == null) {
                                    ""
                                } else {
                                    "${baseNameSpace}."
                                }
                                if (baseNameSpace.isEmpty() || rsocketRouting.startsWith(baseNameSpace)) {
                                    psiJavaClass.methods
                                        .forEach {
                                            val mappingValue = extractValueFromMessageMapping(it) ?: it.name
                                            val routingKey = "${baseNameSpace}${mappingValue}"
                                            if (routingKey == rsocketRouting) {
                                                return it
                                            }
                                        }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null
    }
}