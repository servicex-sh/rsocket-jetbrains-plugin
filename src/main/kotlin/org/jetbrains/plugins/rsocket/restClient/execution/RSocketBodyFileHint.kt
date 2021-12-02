package org.jetbrains.plugins.rsocket.restClient.execution

import com.intellij.httpClient.execution.common.CommonClientBodyFileHint
import com.intellij.json.JsonFileType
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.PlainTextFileType

@Suppress("UnstableApiUsage")
class RSocketBodyFileHint(override val fileExtensionHint: String?, override val fileNameHint: String?, override val fileTypeHint: FileType?) : CommonClientBodyFileHint {

    companion object {
        val TEXT = RSocketBodyFileHint("txt", "rsocket", PlainTextFileType.INSTANCE)
        val JSON = RSocketBodyFileHint("json", "rsocket", JsonFileType.INSTANCE)
    }
}