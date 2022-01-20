package org.jetbrains.plugins.rsocket.endpoints

import com.intellij.microservices.url.Authority
import com.intellij.microservices.url.UrlPath
import com.intellij.microservices.url.UrlTargetInfo
import com.intellij.psi.PsiElement


@Suppress("UnstableApiUsage")
class RSocketUrlTargetInfo(private val element: PsiElement, private val requestType: String, private val routing: String) : UrlTargetInfo {
    override val authorities: List<Authority>
        get() = emptyList()
    override val path: UrlPath
        get() = UrlPath.fromExactString(routing)
    override val schemes: List<String>
        get() = listOf()

    override fun resolveToPsiElement() = element

    override val methods: Set<String>
        get() = when (requestType) {
            "FNF" -> setOf("FNF")
            "STREAM" -> setOf("STREAM")
            "METADATA" -> setOf("METADATA")
            else -> setOf("RSOCKET")
        }

    override val documentationPsiElement: PsiElement
        get() = element
}