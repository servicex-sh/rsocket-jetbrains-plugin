package org.jetbrains.plugins.rsocket.psi

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.httpClient.http.request.HttpRequestFileType
import com.intellij.httpClient.http.request.psi.HttpRequestBlock
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.plugins.rsocket.file.RSocketRoutingHttpIndex


class RSocketRequestMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<*>>) {
        if (element is PsiAnnotation) {
            val psiAnnotation: PsiAnnotation = element
            if ("org.springframework.messaging.handler.annotation.MessageMapping" == element.qualifiedName) {
                var routing = psiAnnotation.findAttributeValue("value")?.text?.trim('"')
                val psiClass = element.getParentOfType<PsiClass>(true)!!
                if (psiClass.hasAnnotation("org.springframework.messaging.handler.annotation.MessageMapping")) {
                    val namespace = extractValueFromMessageMapping(psiClass)
                    if (namespace != null) {
                        routing = "${namespace}.${routing}"
                    }
                }
                val project = element.project
                if (RSocketRoutingHttpIndex.findAllRSocketRouting(project).contains(routing)) {
                    val httpFiles = RSocketRoutingHttpIndex.findHttpFiles(project, routing!!)
                    if (httpFiles.isNotEmpty()) {
                        val targets = mutableListOf<PsiElement>()
                        httpFiles.forEach { virtualFile ->
                            val psiFile = PsiManager.getInstance(element.project).findFile(virtualFile)
                            psiFile?.getChildrenOfType<HttpRequestBlock>()?.forEach {
                                val requestTarget = it.request.requestTarget!!.text
                                if (requestTarget.contains(routing)) {
                                    targets.add(it.request)
                                }
                            }
                        }
                        val builder: NavigationGutterIconBuilder<PsiElement> = NavigationGutterIconBuilder.create(HttpRequestFileType.INSTANCE.icon)
                            .setTargets(targets)
                            .setTooltipText("Navigate to RSocket request")
                        result.add(builder.createLineMarkerInfo(element))
                    }
                }
            }
        }
    }

}

