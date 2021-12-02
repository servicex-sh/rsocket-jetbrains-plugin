package org.jetbrains.plugins.rsocket.restClient.execution

import com.intellij.httpClient.execution.common.CommonClientResponse
import com.intellij.httpClient.execution.common.CommonClientResponseBody
import com.intellij.httpClient.execution.common.CommonClientResponseBody.TextStream.Message.Chunk
import com.intellij.httpClient.execution.common.RequestHandler
import com.intellij.httpClient.execution.common.RunContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import reactor.core.publisher.Flux
import java.time.Duration


@Suppress("UnstableApiUsage")
class RSocketRequestHandler : RequestHandler<RSocketRequest> {

    override fun execute(request: RSocketRequest, runContext: RunContext): CommonClientResponse {
        return when (request.httpMethod) {
            "RPC", "RSOCKET" -> {
                // language=json
                val jsonText = """
                    {"id": 1, "nick": "linux_china" }
                """.trimIndent()
                RSocketClientResponse(10, CommonClientResponseBody.Text(jsonText, RSocketBodyFileHint.JSON))
            }
            "FNF" -> {
                RSocketClientResponse(0)
            }
            "STREAM" -> {
                val shared = MutableSharedFlow<CommonClientResponseBody.TextStream.Message>(
                    replay = 1,
                    onBufferOverflow = BufferOverflow.DROP_OLDEST
                )
                val textStream = CommonClientResponseBody.TextStream(shared, RSocketBodyFileHint.TEXT)
                Flux.just(Chunk("first\n"), Chunk("second\n"), Chunk("third\n"))
                    .delayElements(Duration.ofSeconds(1))
                    .doFinally {
                        shared.tryEmit(CommonClientResponseBody.TextStream.Message.ConnectionClosed.End);
                    }
                    .subscribe {
                        shared.tryEmit(it)
                    }
                RSocketClientResponse(10, textStream)
            }
            else -> {
                RSocketClientResponse(0)
            }
        }
    }

    override fun prepareExecutionEnvironment(request: RSocketRequest, runContext: RunContext) {

    }
}