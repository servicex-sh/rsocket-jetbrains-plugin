package org.jetbrains.plugins.rsocket.file

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.*
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import org.jetbrains.plugins.rsocket.RSOCKET_REQUEST_TYPES

class RSocketRoutingHttpIndex : ScalarIndexExtension<String>() {
    override fun getName() = NAME

    override fun getIndexer(): DataIndexer<String, Void, FileContent> {
        return DataIndexer { fileContent ->
            fileContent.contentAsText.lines()
                .filter { it.contains(' ') }
                .filter { RSOCKET_REQUEST_TYPES.contains(it.substring(0, it.indexOf(' '))) }
                .map {
                    val routing = it.substring(it.indexOf(' ')).trim()
                    if (routing.contains('/')) {
                        routing.substring(routing.lastIndexOf('/') + 1)
                    }
                    routing
                }.associateWith { null }

        }
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> {
        return EnumeratorStringDescriptor.INSTANCE
    }

    override fun getVersion() = 0

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return FileBasedIndex.InputFilter {
            it.name.endsWith(".http") || it.name.endsWith(".rest")
        }
    }

    override fun dependsOnFileContent() = true

    companion object {
        val NAME = ID.create<String, Void?>("rsocket.routingHttpIndex")

        fun findAllRSocketRouting(project: Project): Collection<String> {
            val fileBasedIndex = FileBasedIndex.getInstance()
            return fileBasedIndex.getAllKeys(NAME, project)
        }

        fun findHttpFiles(project: Project, routing: String): Collection<VirtualFile> {
            val fileBasedIndex = FileBasedIndex.getInstance()
            return fileBasedIndex.getContainingFiles(NAME, routing, GlobalSearchScope.allScope(project))
        }
    }

}