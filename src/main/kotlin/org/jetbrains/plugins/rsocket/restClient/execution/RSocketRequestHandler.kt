package org.jetbrains.plugins.rsocket.restClient.execution

import com.intellij.httpClient.execution.common.CommonClientResponse
import com.intellij.httpClient.execution.common.CommonClientResponseBody
import com.intellij.httpClient.execution.common.CommonClientResponseBody.TextStream.Message.Chunk
import com.intellij.httpClient.execution.common.RequestHandler
import com.intellij.httpClient.execution.common.RunContext
import com.intellij.util.concurrency.AppExecutorUtil
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.plus
import kotlinx.coroutines.reactive.asFlow
import reactor.core.publisher.Flux


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
                val flow = Flux.just(Chunk("first"), Chunk("second"), Chunk("third")).asFlow();
                val dispatcher = AppExecutorUtil.getAppExecutorService().asCoroutineDispatcher()
                val scope = CoroutineScope(dispatcher).plus(CoroutineName("RSocket stream call"))
                val messages = flow.shareIn(scope, SharingStarted.Lazily, 100)
                val textStream = CommonClientResponseBody.TextStream(messages, RSocketBodyFileHint.TEXT);
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