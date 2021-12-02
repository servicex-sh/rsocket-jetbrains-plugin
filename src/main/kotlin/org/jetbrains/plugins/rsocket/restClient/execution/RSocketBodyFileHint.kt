package org.jetbrains.plugins.rsocket.restClient.execution

import com.intellij.httpClient.execution.common.CommonClientBodyFileHint
import com.intellij.json.JsonFileType
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.PlainTextFileType

@Suppress("UnstableApiUsage")
class RSocketBodyFileHint(override val fileExtensionHint: String?, override val fileNameHint: String?, override val fileTypeHint: FileType?) : CommonClientBodyFileHint {

    companion object {
        
        fun jsonBodyFileHint(fileName: String): RSocketBodyFileHint {
            return if (fileName.endsWith(".json")) {
                RSocketBodyFileHint("json", fileName, JsonFileType.INSTANCE)
            } else {
                RSocketBodyFileHint("json", "${fileName}.json", JsonFileType.INSTANCE)
            }
        }

        fun textBodyFileHint(fileName: String): RSocketBodyFileHint {
            return if (fileName.endsWith(".txt")) {
                RSocketBodyFileHint("txt", fileName, PlainTextFileType.INSTANCE)
            } else {
                RSocketBodyFileHint("txt", "${fileName}.txt", PlainTextFileType.INSTANCE)
            }
        }

    }
}