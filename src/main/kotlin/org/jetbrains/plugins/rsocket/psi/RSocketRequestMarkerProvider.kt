package org.jetbrains.plugins.rsocket.psi

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.jetbrains.plugins.rsocket.file.RSocketRoutingHttpIndex
import org.jetbrains.plugins.rsocket.messageMappingFullName


class RSocketRequestMarkerProvider : RSocketRequestBaseMarkerProvider() {

    override fun collectNavigationMarkers(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<*>>) {
        var psiAnnotation: PsiAnnotation? = null
        if (element is PsiAnnotation) {
            psiAnnotation = element
        }
        if (psiAnnotation != null) {
            if (messageMappingFullName == psiAnnotation.qualifiedName) {
                var routing = psiAnnotation.findAttributeValue("value")?.text?.trim('"')
                val psiClass = psiAnnotation.parentOfType<PsiClass>(true)
                if (psiClass != null && psiClass.hasAnnotation(messageMappingFullName)) {
                    val namespace = extractValueFromMessageMapping(psiClass)
                    if (namespace != null) {
                        routing = "${namespace}.${routing}"
                    }
                }
                if (routing != null) {
                    val project = element.project
                    if (RSocketRoutingHttpIndex.findAllRSocketRouting(project).contains(routing)) {
                        val navigationBuilder = rsocketRequestsNavigationBuilder(project, routing)
                        if (navigationBuilder != null) {
                            result.add(navigationBuilder.createLineMarkerInfo(element))
                        }
                    }
                }
            }
        }
    }

}

