package org.jetbrains.plugins.rsocket.restClient.execution

import com.intellij.httpClient.execution.common.CommonClientResponse
import com.intellij.httpClient.execution.common.RequestHandler
import com.intellij.httpClient.execution.common.RunContext
import org.jetbrains.plugins.rsocket.requests.RSocketRequestManager


@Suppress("UnstableApiUsage")
class RSocketRequestHandler : RequestHandler<RSocketRequest> {

    override fun execute(request: RSocketRequest, runContext: RunContext): CommonClientResponse {
        val rsocketRequestManager = runContext.project.getService(RSocketRequestManager::class.java)
        return when (request.httpMethod) {
            "RPC", "RSOCKET" -> {
                rsocketRequestManager.requestResponse(request)
            }
            "FNF" -> {
                rsocketRequestManager.fireAndForget(request)
            }
            "M_PUSH" -> {
                rsocketRequestManager.metadataPush(request)
            }
            "STREAM" -> {
                rsocketRequestManager.requestStream(request)
            }
            else -> {
                RSocketClientResponse(0)
            }
        }
    }

    override fun prepareExecutionEnvironment(request: RSocketRequest, runContext: RunContext) {

    }
}