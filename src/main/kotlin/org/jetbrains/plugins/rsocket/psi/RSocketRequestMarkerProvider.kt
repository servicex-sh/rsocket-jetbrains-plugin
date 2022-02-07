package org.jetbrains.plugins.rsocket.psi

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.toLightAnnotation
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.plugins.rsocket.file.RSocketRoutingHttpIndex
import org.jetbrains.plugins.rsocket.messageMappingFullName


class RSocketRequestMarkerProvider : RSocketRequestBaseMarkerProvider() {

    override fun collectNavigationMarkers(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<*>>) {
        var psiAnnotation: PsiAnnotation? = null
        if (element is PsiAnnotation) {
            psiAnnotation = element
        } else if (element is KtAnnotationEntry) {
            psiAnnotation = element.toLightAnnotation()
        }
        if (psiAnnotation != null) {
            if (messageMappingFullName == psiAnnotation.qualifiedName) {
                var routing = psiAnnotation.findAttributeValue("value")?.text?.trim('"')
                val psiClass = psiAnnotation.getParentOfType<PsiClass>(true)
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

