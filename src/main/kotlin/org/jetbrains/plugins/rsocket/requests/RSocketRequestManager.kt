package org.jetbrains.plugins.rsocket.requests

import com.intellij.httpClient.execution.common.CommonClientBodyFileHint
import com.intellij.httpClient.execution.common.CommonClientResponse
import com.intellij.httpClient.execution.common.CommonClientResponseBody
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.jetbrains.plugins.rsocket.restClient.execution.RSocketBodyFileHint
import org.jetbrains.plugins.rsocket.restClient.execution.RSocketClientResponse
import org.jetbrains.plugins.rsocket.restClient.execution.RSocketRequest
import reactor.core.publisher.Flux
import java.time.Duration

@Suppress("UnstableApiUsage")
class RSocketRequestManager(private val project: Project) : Disposable {

    override fun dispose() {
    }

    fun requestResponse(rsocketRequest: RSocketRequest): CommonClientResponse {
        // language=json
        val jsonText = """
            {"id": 1, "nick": "linux_china" }
        """.trimIndent()
        return RSocketClientResponse(10, CommonClientResponseBody.Text(jsonText, bodyFileHint(rsocketRequest)))
    }

    fun fireAndForget(rsocketRequest: RSocketRequest): CommonClientResponse {
        return RSocketClientResponse(111)
    }

    fun requestStream(rsocketRequest: RSocketRequest): CommonClientResponse {
        val shared = MutableSharedFlow<CommonClientResponseBody.TextStream.Message>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val textStream = CommonClientResponseBody.TextStream(shared, bodyFileHint(rsocketRequest))
            .withConnectionDisposable {
                //todo close rsocket connection
            }
        Flux.just(
            CommonClientResponseBody.TextStream.Message.Chunk("first\n"),
            CommonClientResponseBody.TextStream.Message.Chunk("second\n"),
            CommonClientResponseBody.TextStream.Message.Chunk("third\n")
        )
            .delayElements(Duration.ofSeconds(1))
            .doFinally {
                shared.tryEmit(CommonClientResponseBody.TextStream.Message.ConnectionClosed.End);
            }
            .subscribe {
                shared.tryEmit(it)
            }
        return RSocketClientResponse(10, textStream)
    }

    fun metadataPush(rsocketRequest: RSocketRequest): CommonClientResponse {
        return RSocketClientResponse(111)
    }

    private fun bodyFileHint(request: RSocketRequest): CommonClientBodyFileHint {
        val acceptMimeType = request.acceptMimeType
        val dataType = request.dataMimeTyp
        val fileName = "rsocket-${request.httpMethod}-${request.routingMetadata()[0]}"
        return if ("application/json" == acceptMimeType || "application/json" == dataType) {
            RSocketBodyFileHint.jsonBodyFileHint(fileName)
        } else {
            RSocketBodyFileHint.textBodyFileHint(fileName)
        }
    }

}