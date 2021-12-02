package org.jetbrains.plugins.rsocket.restClient.execution

import com.intellij.httpClient.execution.common.RequestContext
import com.intellij.httpClient.execution.common.RequestConverter
import com.intellij.httpClient.execution.common.RequestExecutionSupport
import com.intellij.httpClient.execution.common.RequestHandler

@Suppress("UnstableApiUsage")
class RSocketRequestExecutionSupport : RequestExecutionSupport<RSocketRequest> {
    companion object {
        val RSOCKET_REQUEST_TYPES = listOf("RSOCKET", "RPC", "FNF", "STREAM", "M_PUSH")
    }

    override fun canProcess(requestContext: RequestContext): Boolean {
        return RSOCKET_REQUEST_TYPES.contains(requestContext.method)
    }

    override fun getRequestConverter(): RequestConverter<RSocketRequest> {
        return RSocketRequestConverter()
    }

    override fun getRequestHandler(): RequestHandler<RSocketRequest> {
        return RSocketRequestHandler()
    }

    override fun supportedMethods(): Collection<String> {
        return RSOCKET_REQUEST_TYPES
    }
}