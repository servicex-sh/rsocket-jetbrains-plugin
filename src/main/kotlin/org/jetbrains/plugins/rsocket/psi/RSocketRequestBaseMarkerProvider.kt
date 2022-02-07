package org.jetbrains.plugins.rsocket.psi

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.httpClient.http.request.HttpRequestFileType
import com.intellij.httpClient.http.request.psi.HttpRequestBlock
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.plugins.rsocket.file.RSocketRoutingHttpIndex


open class RSocketRequestBaseMarkerProvider : RelatedItemLineMarkerProvider() {

    fun rsocketRequestsNavigationBuilder(project: Project, routing: String): NavigationGutterIconBuilder<PsiElement>? {
        val httpFiles = RSocketRoutingHttpIndex.findHttpFiles(project, routing)
        if (httpFiles.isNotEmpty()) {
            val targets = mutableListOf<PsiElement>()
            httpFiles.forEach { virtualFile ->
                val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                psiFile?.getChildrenOfType<HttpRequestBlock>()?.forEach {
                    val requestTarget = it.request.requestTarget!!.text
                    if (requestTarget.contains(routing)) {
                        targets.add(it.request.requestTarget!!)
                    }
                }
            }
            return NavigationGutterIconBuilder.create(HttpRequestFileType.INSTANCE.icon)
                .setTargets(targets)
                .setTooltipText("Navigate to RSocket request")
        }
        return null
    }
}