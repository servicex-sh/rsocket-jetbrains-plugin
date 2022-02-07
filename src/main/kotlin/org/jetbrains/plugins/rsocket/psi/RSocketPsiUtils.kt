package org.jetbrains.plugins.rsocket.psi

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.plugins.rsocket.messageMappingFullName
import org.jetbrains.plugins.rsocket.rsocketServiceFullName

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

fun extractFirstClassFromJavaOrKt(psiFile: PsiFile): PsiClass? {
    return when (psiFile) {
        is KtFile -> {
            psiFile.classes.firstOrNull()
        }
        is PsiJavaFile -> {
            psiFile.classes.firstOrNull()
        }
        else -> {
            null
        }
    }
}

fun extractAliRSocketService(serviceImpPsiClass: PsiClass): AliRSocketService {
    var serviceFullName = serviceImpPsiClass.name!!
    var serviceInterfacePsiClass: PsiClass = serviceImpPsiClass
    val rsocketServiceAnnotation = serviceImpPsiClass.getAnnotation(rsocketServiceFullName)!!
    val serviceInterface = rsocketServiceAnnotation.findAttributeValue("serviceInterface")
    if (serviceInterface != null && serviceInterface.text.trim('"').isNotEmpty()) {
        val serviceInterfaceClassName = serviceInterface.text.trim('"')
            .replace(".class", "")
            .replace("::class", "")
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
    if (owner.hasAnnotation(messageMappingFullName)) {
        val messageMappingAnnotation = owner.getAnnotation(messageMappingFullName)!!
        val mappingValue = messageMappingAnnotation.findAttributeValue("value")
        if (mappingValue != null) {
            return mappingValue.text.trim('"')
        }
    }
    return null
}