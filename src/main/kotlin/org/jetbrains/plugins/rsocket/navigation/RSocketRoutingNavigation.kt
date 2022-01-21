package org.jetbrains.plugins.rsocket.navigation

import com.intellij.navigation.DirectNavigationProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import org.jetbrains.plugins.rsocket.completion.RSocketRoutingCompletionContributor.Companion.rsocketRoutingCapture
import org.jetbrains.plugins.rsocket.file.RSocketServiceFileIndex
import org.jetbrains.plugins.rsocket.psi.extractAliRSocketService
import org.jetbrains.plugins.rsocket.psi.extractValueFromMessageMapping


@Suppress("UnstableApiUsage")
class RSocketRoutingNavigation : DirectNavigationProvider {
    override fun getNavigationElement(element: PsiElement): PsiElement? {
        if (rsocketRoutingCapture.accepts(element)) {
            val rsocketRouting = element.text.trim('/')
            if (rsocketRouting.isNotEmpty() && !rsocketRouting.contains("://")) {
                RSocketServiceFileIndex.findRSocketServiceFiles(element.project).forEach { psiFile ->
                    if (psiFile is PsiJavaFile) {
                        val psiJavaClass = psiFile.classes[0]
                        val rsocketService = psiJavaClass.hasAnnotation("com.alibaba.rsocket.RSocketService")
                        if (rsocketService) {
                            val aliRSocketService = extractAliRSocketService(psiJavaClass)
                            val serviceFullName = aliRSocketService.serviceName
                            if (rsocketRouting.startsWith(serviceFullName)) {
                                aliRSocketService.serviceInterface
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
        return null
    }
}