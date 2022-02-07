package org.jetbrains.plugins.rsocket.psi

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.plugins.rsocket.file.RSocketRoutingHttpIndex
import org.jetbrains.plugins.rsocket.rsocketServiceFullName


class AliRSocketRequestMarkerProvider : RSocketRequestBaseMarkerProvider() {

    override fun collectNavigationMarkers(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<*>>) {
        var psiMethod: PsiMethod? = null;
        if (element is PsiMethod) {
            psiMethod = element
        } else if (element is KtNamedFunction) {
            val ktNamedFunction: KtNamedFunction = element
            val lightMethods = ktNamedFunction.toLightMethods()
            if (lightMethods.isNotEmpty()) {
                psiMethod = lightMethods[0]
            }
        }
        if (psiMethod != null) {
            val psiClass = psiMethod.parentOfType<PsiClass>()
            if (psiClass != null && psiClass.hasAnnotation(rsocketServiceFullName)) {
                val serviceName = extractAliRSocketService(psiClass).serviceName
                val routing = "${serviceName}.${psiMethod.name}"
                val project = element.project
                if (RSocketRoutingHttpIndex.findAllRSocketRouting(project).contains(routing)) {
                    val navigationBuilder = rsocketRequestsNavigationBuilder(project, routing)
                    if (navigationBuilder != null) {
                        result.add(navigationBuilder.createLineMarkerInfo(psiMethod.nameIdentifier!!))
                    }
                }
            }
        }
    }

}

