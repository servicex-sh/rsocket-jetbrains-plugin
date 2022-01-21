package org.jetbrains.plugins.rsocket.psi

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifierListOwner

data class AliRSocketService(val serviceName: String, val serviceInterface: PsiClass)

fun convertToRSocketRequestType(returnType: String?): String {
    if (returnType == null) return "REQUEST"
    return if (returnType.contains("Mono<") && returnType.contains("Void")) {
        "FNF"
    } else if (returnType.contains("Flux<")) {
        "STREAM"
    } else {
        "REQUEST"
    }
}

fun extractAliRSocketService(serviceImpPsiClass: PsiClass): AliRSocketService {
    var serviceFullName = serviceImpPsiClass.name!!
    var serviceInterfacePsiClass: PsiClass = serviceImpPsiClass
    val rsocketServiceAnnotation = serviceImpPsiClass.getAnnotation("com.alibaba.rsocket.RSocketService")!!
    val serviceInterface = rsocketServiceAnnotation.findAttributeValue("serviceInterface")
    if (serviceInterface != null && serviceInterface.text.trim('"').isNotEmpty()) {
        val serviceInterfaceClassName = serviceInterface.text.trim('"').replace(".class", "")
        val serviceInterfaceClassType = serviceImpPsiClass.superTypes.firstOrNull {
            it.className == serviceInterfaceClassName
        }
        if (serviceInterfaceClassType != null) {
            serviceFullName = serviceInterfaceClassType.canonicalText
            serviceInterfacePsiClass = serviceInterfaceClassType.resolve()!!
        } else {
            serviceFullName = serviceInterfaceClassName
        }
    }
    val serviceName = rsocketServiceAnnotation.findAttributeValue("name")
    if (serviceName != null && serviceName.text.trim('"').isNotEmpty()) {
        serviceFullName = serviceName.text.trim('"')
    }
    return AliRSocketService(serviceFullName, serviceInterfacePsiClass)
}

fun extractValueFromMessageMapping(owner: PsiModifierListOwner): String? {
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