package org.jetbrains.plugins.rsocket.file

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.*
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import org.jetbrains.plugins.rsocket.psi.extractAliRSocketService
import org.jetbrains.plugins.rsocket.psi.extractFirstClassFromJavaOrKt
import org.jetbrains.plugins.rsocket.psi.extractValueFromMessageMapping
import org.jetbrains.plugins.rsocket.rsocketServiceFullName

class RSocketServiceFileIndex : ScalarIndexExtension<String>() {
    override fun getName() = NAME

    override fun getIndexer(): DataIndexer<String, Void, FileContent> {
        return DataIndexer {
            if (it.contentAsText.contains("@RSocketService")) {
                mapOf("rsocket" to null, "ali_rsocket" to null)
                //mapOf("aliRsocket" to null)
            } else if (it.contentAsText.contains("@MessageMapping")) {
                mapOf("rsocket" to null, "spring_rsocket" to null)
            } else {
                emptyMap()
            }
        }
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> {
        return EnumeratorStringDescriptor.INSTANCE
    }

    override fun getVersion() = 0

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return FileBasedIndex.InputFilter {
            it.name.endsWith(".java") || it.name.endsWith(".kt")
        }
    }

    override fun dependsOnFileContent() = true

    companion object {
        val NAME = ID.create<String, Void?>("rsocket.ServiceFileIndex")

        fun findRSocketServiceFiles(project: Project): List<PsiFile> {
            val fileBasedIndex = FileBasedIndex.getInstance()
            val psiManager = PsiManager.getInstance(project)
            return ReadAction.compute<List<PsiFile>, Throwable> {
                fileBasedIndex.getContainingFiles(NAME, "rsocket", GlobalSearchScope.projectScope(project))
                    .map { psiManager.findFile(it)!! }
                    .toList()
            }
        }

        fun findRelatedElement(project: Project, rsocketRouting: String): PsiElement? {
            findRSocketServiceFiles(project).forEach { psiFile ->
                val psiJavaClass = extractFirstClassFromJavaOrKt(psiFile)
                if (psiJavaClass != null) {
                    val rsocketService = psiJavaClass.hasAnnotation(rsocketServiceFullName)
                    if (rsocketService) {
                        val aliRSocketService = extractAliRSocketService(psiJavaClass)
                        val serviceFullName = aliRSocketService.serviceName
                        if (rsocketRouting.startsWith(serviceFullName)) {
                            psiJavaClass
                                .methods
                                .forEach {
                                    val routingKey = "${serviceFullName}.${it.name}"
                                    if (routingKey == rsocketRouting) {
                                        return it
                                    }
                                }
                        }
                    } else {
                        var baseNameSpace = extractValueFromMessageMapping(psiJavaClass)
                        baseNameSpace = if (baseNameSpace == null) {
                            ""
                        } else {
                            "${baseNameSpace}."
                        }
                        if (baseNameSpace.isEmpty() || rsocketRouting.startsWith(baseNameSpace)) {
                            psiJavaClass.methods
                                .forEach {
                                    val mappingValue = extractValueFromMessageMapping(it) ?: it.name
                                    val routingKey = "${baseNameSpace}${mappingValue}"
                                    if (routingKey == rsocketRouting) {
                                        return it
                                    }
                                }
                        }
                    }
                }
            }
            return null
        }
    }
}