@file:Suppress("UnstableApiUsage")

package org.jetbrains.plugins.rsocket.restClient.url

import com.intellij.microservices.url.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.plugins.rsocket.RSOCKET_REQUEST_TYPES
import org.jetbrains.plugins.rsocket.file.RSocketServiceFileIndex


class RSocketUrlResolverFactory : UrlResolverFactory {
    override fun forProject(project: Project): UrlResolver {
        return RSocketUrlResolver(project)
    }
}

class RSocketUrlResolver(private val project: Project) : UrlResolver {
    override val supportedSchemes: List<String>
        get() {
            return listOf("rsocket://", "rsocketws://", "rsocketwss://")
        }

    override fun getVariants(): Iterable<UrlTargetInfo> {
        return listOf()
    }

    override fun resolve(request: UrlResolveRequest): Iterable<UrlTargetInfo> {
        return listOf(RSocketUrlTargetInfo(project, request.path))
    }

    override fun getAuthorityHints(schema: String?): List<Authority.Exact> {
        return listOf()
    }
}

class RSocketUrlTargetInfo(private val project: Project, private val urlPath: UrlPath) : UrlTargetInfo {
    override val authorities: List<Authority> = listOf()
    override val path: UrlPath = urlPath
    override val schemes: List<String> = listOf("rsocket", "rsocket+ws", "rsocket+wss")

    override val methods: Set<String> = RSOCKET_REQUEST_TYPES

    override fun resolveToPsiElement(): PsiElement? {
        var rsocketRouting = urlPath.getPresentation()
        if (rsocketRouting.contains("/")) {
            rsocketRouting = rsocketRouting.substring(rsocketRouting.lastIndexOf('/') + 1)
        }
        return RSocketServiceFileIndex.findRelatedElement(project, rsocketRouting)
    }

}

