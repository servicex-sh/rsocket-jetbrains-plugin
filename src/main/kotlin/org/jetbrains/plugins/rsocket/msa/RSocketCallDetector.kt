package org.jetbrains.plugins.rsocket.msa

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.project.Project
import org.jetbrains.uast.UCallExpression
/*

import org.strangeway.msa.db.InteractionType
import org.strangeway.msa.frameworks.CallDetector
import org.strangeway.msa.frameworks.FrameworkInteraction
import org.strangeway.msa.frameworks.Interaction
import org.strangeway.msa.frameworks.hasLibraryClass

class RSocketCallDetector : CallDetector {
    private val interaction: Interaction = FrameworkInteraction(InteractionType.REQUEST, "RSocket")
    private val rsocketServiceAnnotations = listOf("com.alibaba.rsocket.RSocketServiceInterface", "org.springframework.messaging.rsocket.service.RSocketExchange")
    private val rsocketExchangeAnnotations = listOf("org.springframework.messaging.rsocket.service.RSocketExchange")
    private val rsocketStubInterfaces = listOf("io.rsocket.RSocket", "io.rsocket.kotlin.RSocket", "org.springframework.messaging.rsocket.RSocketRequester")
    private val graphqlRSocketStubMethods = listOf("retrieve", "retrieveSubscription", "execute", "executeSubscription")
    override fun getCallInteraction(project: Project, uCall: UCallExpression): Interaction? {
        val psiMethod = uCall.resolve()
        if (psiMethod != null) {
            if (AnnotationUtil.isAnnotated(psiMethod, rsocketExchangeAnnotations, 0)) {
                return interaction
            } else {
                val psiClass = psiMethod.containingClass
                if (psiClass != null) {
                    val psiClassFullName = psiClass.qualifiedName!!
                    if (isRSocketStub(psiClassFullName)) {
                        return interaction
                    } else if (psiClass.isInterface && AnnotationUtil.isAnnotated(psiClass, rsocketServiceAnnotations, 0)) {
                        return interaction
                    } else if (psiClassFullName == "org.springframework.graphql.client.GraphQlClient.RequestSpec") {
                        if (graphqlRSocketStubMethods.contains(psiMethod.name)) {
                            return interaction
                        }
                    }
                }
            }
        }
        return null
    }

    override fun isAvailable(project: Project): Boolean {
        return hasLibraryClass(project, "io.rsocket.RSocket")
    }

    private fun isRSocketStub(classFullName: String): Boolean {
        return rsocketStubInterfaces.contains(classFullName)
    }
}*/
