package org.jetbrains.plugins.rsocket.restClient.execution

import com.intellij.httpClient.execution.common.CommonClientResponse
import com.intellij.httpClient.execution.common.CommonClientResponseBody
import com.intellij.json.JsonFileType
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.PlainTextFileType

@Suppress("UnstableApiUsage")
class RSocketClientResponse(override var executionTime: Long?, private val responseBody: CommonClientResponseBody = CommonClientResponseBody.Empty()) : CommonClientResponse {
    override val body: CommonClientResponseBody
        get() = responseBody

    override fun suggestFileTypeForPresentation(): FileType? {
        if (body is CommonClientResponseBody.Text) {
            if ((body as CommonClientResponseBody.Text).content[0] == '{') {
                return JsonFileType.INSTANCE
            }
        }
        return PlainTextFileType.INSTANCE
    }

    override val statusPresentation: String
        get() = "OK"

    override val presentationHeader: String
        get() {
            // return payload information or exception message
            return when (body) {
                is CommonClientResponseBody.Text -> {
                    (body as CommonClientResponseBody.Text).content + "\n"
                }
                is CommonClientResponseBody.Empty -> {
                    ""
                }
                else -> {
                    throw IllegalStateException("Cannot get string content, because body is not text")
                }
            }
        }

    override val presentationFooter: String
        get() {
            return "Execution time: $executionTime ms"
        }
}
