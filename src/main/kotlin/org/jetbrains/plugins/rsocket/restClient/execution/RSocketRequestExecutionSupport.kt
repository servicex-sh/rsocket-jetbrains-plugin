package org.jetbrains.plugins.rsocket.restClient.execution

import com.intellij.httpClient.execution.common.RequestContext
import com.intellij.httpClient.execution.common.RequestConverter
import com.intellij.httpClient.execution.common.RequestExecutionSupport
import com.intellij.httpClient.execution.common.RequestHandler
import org.jetbrains.plugins.rsocket.RSOCKET_REQUEST_TYPES

@Suppress("UnstableApiUsage")
class RSocketRequestExecutionSupport : RequestExecutionSupport<RSocketRequest> {

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

    override val needsScheme: Boolean
        get() = false
    override val supportedSchemes: List<String>
        get() = listOf("tcp", "ws", "wss")
}