package org.jetbrains.plugins.rsocket.navigation

import com.intellij.httpClient.http.request.psi.HttpRequestTarget
import com.intellij.navigation.DirectNavigationProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.jetbrains.plugins.rsocket.completion.RSocketRoutingCompletionContributor.Companion.rsocketRoutingCapture
import org.jetbrains.plugins.rsocket.file.RSocketServiceFileIndex


@Suppress("UnstableApiUsage")
class RSocketRoutingNavigation : DirectNavigationProvider {
    override fun getNavigationElement(element: PsiElement): PsiElement? {
        if (rsocketRoutingCapture.accepts(element)) {
            val httpRequestTarget = element.parentOfType<HttpRequestTarget>()!!
            val host = httpRequestTarget.host
            val pathAbsolute = httpRequestTarget.pathAbsolute
            if (host != null || pathAbsolute != null) {
                val rsocketRouting = if (pathAbsolute != null) {
                    val path = pathAbsolute.text
                    path.substring(path.lastIndexOf('/') + 1)
                } else {
                    host!!.text
                }
                if (rsocketRouting.isNotEmpty()) {
                    return RSocketServiceFileIndex.findRelatedElement(element.project, rsocketRouting)
                }
            }
        }
        return null
    }
}