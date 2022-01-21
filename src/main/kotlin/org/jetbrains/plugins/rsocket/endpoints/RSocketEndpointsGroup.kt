package org.jetbrains.plugins.rsocket.endpoints

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import org.jetbrains.plugins.rsocket.psi.convertToRSocketRequestType
import org.jetbrains.plugins.rsocket.psi.extractAliRSocketService
import org.jetbrains.plugins.rsocket.psi.extractValueFromMessageMapping

class RSocketEndpointsGroup(private val project: Project, private val psiFile: PsiFile, val vendor: String) {

    fun endpoints(): List<RSocketEndpoint> {
        val endpoints = mutableListOf<RSocketEndpoint>()
        if (psiFile is PsiJavaFile) {
            val psiClass = psiFile.classes[0]
            val rsocketService = psiClass.hasAnnotation("com.alibaba.rsocket.RSocketService")
            if (rsocketService) {
                val aliRSocketService = extractAliRSocketService(psiClass)
                val serviceInterfaceMethods = aliRSocketService.serviceInterface.methods.map { it.name }.toList()
                psiClass.methods
                    .filter {
                        serviceInterfaceMethods.contains(it.name)
                    }
                    .filter {
                        val returnType = it.returnType?.canonicalText
                        returnType != null && (returnType.contains("Mono<") || returnType.contains("Flux"))
                    }
                    .map {
                        val requestType = convertToRSocketRequestType(it.returnType?.canonicalText)
                        RSocketEndpoint("[$requestType]", "${aliRSocketService.serviceName}.${it.name}", it)
                    }.forEach {
                        endpoints.add(it)
                    }
            } else {
                var baseNameSpace = extractValueFromMessageMapping(psiClass)
                baseNameSpace = if (baseNameSpace == null) {
                    ""
                } else {
                    "${baseNameSpace}."
                }
                psiClass.methods
                    .filter {
                        it.hasAnnotation("org.springframework.messaging.handler.annotation.MessageMapping")
                    }.map {
                        val routingKey = extractValueFromMessageMapping(it) ?: it.name
                        val requestType = convertToRSocketRequestType(it.returnType?.canonicalText)
                        RSocketEndpoint("[$requestType]", "${baseNameSpace}${routingKey}", it)
                    }.forEach {
                        endpoints.add(it)
                    }
            }
            psiClass.methods
                .filter {
                    rsocketService || it.hasAnnotation("org.springframework.messaging.handler.annotation.MessageMapping")
                }.map {
                    val requestType = convertToRSocketRequestType(it.returnType?.canonicalText)
                    RSocketEndpoint("[$requestType]", it.name, it)
                }.forEach {
                    endpoints.add(it)
                }
        }
        return endpoints
    }


}