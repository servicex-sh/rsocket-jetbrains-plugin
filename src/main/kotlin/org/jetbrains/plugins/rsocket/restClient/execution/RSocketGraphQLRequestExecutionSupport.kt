package org.jetbrains.plugins.rsocket.restClient.execution

import com.intellij.httpClient.execution.common.RequestContext
import com.intellij.httpClient.execution.common.RequestConverter
import com.intellij.httpClient.execution.common.RequestExecutionSupport
import com.intellij.httpClient.execution.common.RequestHandler

@Suppress("UnstableApiUsage")
class RSocketGraphQLRequestExecutionSupport : RequestExecutionSupport<RSocketRequest> {

    companion object {
        val RSOCKET_SCHEMAS = listOf("rsocket", "rsocketws", "rsocketwss")
    }

    override fun canProcess(requestContext: RequestContext): Boolean {
        val method = requestContext.method
        val schema = requestContext.scheme
        return method == "GRAPHQL" && schema != null && RSOCKET_SCHEMAS.contains(schema)

    }

    override fun getRequestConverter(): RequestConverter<RSocketRequest> {
        return RSocketRequestConverter()
    }

    override fun getRequestHandler(): RequestHandler<RSocketRequest> {
        return RSocketRequestHandler()
    }

    override fun supportedMethods(): Collection<String> {
        return listOf("GRAPHQL")
    }

    override val needsScheme: Boolean
        get() = true
    override val supportedSchemes: List<String>
        get() = RSOCKET_SCHEMAS
}