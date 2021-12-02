package org.jetbrains.plugins.rsocket.requests

import com.intellij.httpClient.execution.common.CommonClientBodyFileHint
import com.intellij.httpClient.execution.common.CommonClientResponse
import com.intellij.httpClient.execution.common.CommonClientResponseBody
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.util.io.toByteArray
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import io.rsocket.RSocket
import io.rsocket.core.RSocketConnector
import io.rsocket.metadata.CompositeMetadataCodec
import io.rsocket.metadata.TaggingMetadataCodec
import io.rsocket.metadata.WellKnownMimeType
import io.rsocket.transport.netty.client.TcpClientTransport
import io.rsocket.util.DefaultPayload
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.jetbrains.plugins.rsocket.restClient.execution.RSocketBodyFileHint
import org.jetbrains.plugins.rsocket.restClient.execution.RSocketRequest
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.*


@Suppress("UnstableApiUsage")
class RSocketRequestManager(private val project: Project) : Disposable {
    override fun dispose() {
    }

    fun requestResponse(rsocketRequest: RSocketRequest): CommonClientResponse {
        val dataMimeType = rsocketRequest.acceptMimeTypeHint()
        val text: String?
        var clientRSocket: RSocket? = null;
        try {
            //val clientTransport: WebsocketClientTransport = WebsocketClientTransport.create(URI.create("tcp://localhost:8080/rsocket"))
            val clientTransport = TcpClientTransport.create("127.0.0.1", 42252)
            clientRSocket = RSocketConnector.create()
                .dataMimeType(rsocketRequest.dataMimeTyp)
                .metadataMimeType(rsocketRequest.metadataMimeTyp)
                .connect(clientTransport)
                .block()!!
            val compositeMetadataBuffer = ByteBufAllocator.DEFAULT.compositeBuffer()
            val routingMetaData = TaggingMetadataCodec.createTaggingContent(ByteBufAllocator.DEFAULT, listOf("findBook"))
            CompositeMetadataCodec.encodeAndAddMetadata(
                compositeMetadataBuffer, ByteBufAllocator.DEFAULT,
                WellKnownMimeType.MESSAGE_RSOCKET_ROUTING,
                routingMetaData
            )
            val payload = DefaultPayload.create(Unpooled.wrappedBuffer("book-1".toByteArray()), compositeMetadataBuffer);
            val result = clientRSocket.requestResponse(payload).block()
            if (result == null) {
                return RSocketClientResponse(
                    CommonClientResponseBody.Empty(), "text/plain",
                    "ERROR", "No result returned"
                )
            } else {
                text = if (dataMimeType.contains("text") || dataMimeType.contains("json") || dataMimeType.contains("xml")) {
                    result.dataUtf8
                } else {
                    val bytes = result.data.toByteArray()
                    Base64.getEncoder().encodeToString(bytes)
                }
            }
        } catch (e: Exception) {
            return RSocketClientResponse(
                CommonClientResponseBody.Empty(), "text/plain",
                "ERROR", e.stackTraceToString()
            )
        } finally {
            clientRSocket?.dispose()
        }
        return RSocketClientResponse(CommonClientResponseBody.Text(text ?: "", bodyFileHint(rsocketRequest)), dataMimeType)
    }

    fun fireAndForget(rsocketRequest: RSocketRequest): CommonClientResponse {
        return RSocketClientResponse()
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
        val dataMimeType = rsocketRequest.acceptMimeTypeHint()
        return RSocketClientResponse(textStream, dataMimeType)
    }

    fun metadataPush(rsocketRequest: RSocketRequest): CommonClientResponse {
        return RSocketClientResponse()
    }

    private fun bodyFileHint(request: RSocketRequest): CommonClientBodyFileHint {
        val acceptMimeType = request.acceptMimeTypeHint()
        val fileName = "rsocket-${request.httpMethod}-${request.routingMetadata()[0]}"
        return if ("application/json" == acceptMimeType) {
            RSocketBodyFileHint.jsonBodyFileHint(fileName)
        } else {
            RSocketBodyFileHint.textBodyFileHint(fileName)
        }
    }

}