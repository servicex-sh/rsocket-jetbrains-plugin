package org.jetbrains.plugins.rsocket.restClient.execution

import com.intellij.httpClient.execution.common.CommonClientResponse
import com.intellij.httpClient.execution.common.CommonClientResponseBody
import com.intellij.httpClient.execution.common.RequestHandler
import com.intellij.httpClient.execution.common.RunContext

@Suppress("UnstableApiUsage")
class RSocketRequestHandler : RequestHandler<RSocketRequest> {

    override fun execute(request: RSocketRequest, runContext: RunContext): CommonClientResponse {
        return when (request.httpMethod) {
            "RPC", "RSOCKET" -> {
                // language=json
                val jsonText = """
                    {"id": 1, "nick": "linux_china" }
                """.trimIndent()
                RSocketClientResponse(111, CommonClientResponseBody.Text(jsonText))
            }
            "FNF" -> {
                RSocketClientResponse(111)
            }
            "STREAM" -> {
                RSocketClientResponse(111, CommonClientResponseBody.Text("Hello, stream!"))
            }
            else -> {
                RSocketClientResponse(111)
            }
        }
    }

    override fun prepareExecutionEnvironment(request: RSocketRequest, runContext: RunContext) {

    }
}