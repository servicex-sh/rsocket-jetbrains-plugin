package org.jetbrains.plugins.rsocket.msa

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import org.jetbrains.uast.UCallExpression
import org.strangeway.msa.db.InteractionType
import org.strangeway.msa.frameworks.CallDetector
import org.strangeway.msa.frameworks.FrameworkInteraction
import org.strangeway.msa.frameworks.Interaction
import org.strangeway.msa.frameworks.hasLibraryClass

class RSocketCallDetector : CallDetector {
    private val interaction: Interaction = FrameworkInteraction(InteractionType.REQUEST, "RSocket")
    private val rsocketServiceAnnotations = listOf("com.alibaba.rsocket.RSocketServiceInterface")
    private val rsocketStubInterfaces = listOf("io.rsocket.RSocket", "org.springframework.messaging.rsocket.RSocketRequester")
    override fun getCallInteraction(project: Project, uCall: UCallExpression): Interaction? {
        val psiMethod = uCall.resolve()
        if (psiMethod != null) {
            val psiClass = psiMethod.containingClass
            if (psiClass != null) {
                if (isRSocketStub(psiClass)) {
                    return interaction
                } else if (psiClass.isInterface && AnnotationUtil.isAnnotated(psiClass, rsocketServiceAnnotations, 0)) {
                    return interaction
                }
            }
        }
        return null
    }

    override fun isAvailable(project: Project): Boolean {
        return hasLibraryClass(project, "io.rsocket.RSocket")
    }

    private fun isRSocketStub(clazz: PsiClass): Boolean {
        return rsocketStubInterfaces.contains(clazz.qualifiedName)
    }
}