package org.jetbrains.plugins.rsocket.endpoints

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiModifierListOwner

class RSocketEndpointsGroup(private val project: Project, private val psiFile: PsiFile, val vendor: String) {

    fun endpoints(): List<RSocketEndpoint> {
        val endpoints = mutableListOf<RSocketEndpoint>()
        if (psiFile is PsiJavaFile) {
            val psiClass = psiFile.classes[0]
            val rsocketService = psiClass.hasAnnotation("com.alibaba.rsocket.RSocketService")
            if (rsocketService) {
                var baseNameSpace = psiClass.name
                val rsocketServiceAnnotation = psiClass.getAnnotation("com.alibaba.rsocket.RSocketService")!!
                val serviceName = rsocketServiceAnnotation.findAttributeValue("name")
                if (serviceName != null && serviceName.text.trim('"').isNotEmpty()) {
                    baseNameSpace = serviceName.text.trim('"')
                } else {
                    val serviceInterface = rsocketServiceAnnotation.findAttributeValue("serviceInterface")
                    if (serviceInterface != null && serviceInterface.text.trim('"').isNotEmpty()) {
                        val targetClass = serviceInterface.text.trim('"').replace(".class", "")
                        val serviceInterfacePsiClass = psiClass.superTypes.firstOrNull {
                            it.className == targetClass
                        }
                        if (serviceInterfacePsiClass != null) {
                            baseNameSpace = serviceInterfacePsiClass.canonicalText
                        } else {
                            baseNameSpace = targetClass
                        }
                    }
                }
                psiClass.methods
                    .filter {
                        it.hasAnnotation("java.lang.Override")
                    }
                    .filter {
                        val returnType = it.returnType?.canonicalText
                        returnType != null && (returnType.contains("Mono<") || returnType.contains("Flux"))
                    }
                    .map {
                        val requestType = convertToRSocketRequestType(it.returnType?.canonicalText)
                        RSocketEndpoint("[$requestType]", "${baseNameSpace}.${it.name}", it)
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

    private fun convertToRSocketRequestType(returnType: String?): String {
        if (returnType == null) return "REQUEST"
        return if (returnType.contains("Mono<") && returnType.contains("Void")) {
            "FNF"
        } else if (returnType.contains("Flux<")) {
            "STREAM"
        } else {
            "REQUEST"
        }
    }


    private fun extractValueFromMessageMapping(owner: PsiModifierListOwner): String? {
        val annotationFullName = "org.springframework.messaging.handler.annotation.MessageMapping"
        if (owner.hasAnnotation(annotationFullName)) {
            val messageMappingAnnotation = owner.getAnnotation(annotationFullName)!!
            val mappingValue = messageMappingAnnotation.findAttributeValue("value")
            if (mappingValue != null) {
                return mappingValue.text.trim('"')
            }
        }
        return null
    }

}