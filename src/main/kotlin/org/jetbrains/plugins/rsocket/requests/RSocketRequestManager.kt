package org.jetbrains.plugins.rsocket.requests

import com.intellij.httpClient.execution.common.CommonClientBodyFileHint
import com.intellij.httpClient.execution.common.CommonClientResponse
import com.intellij.httpClient.execution.common.CommonClientResponseBody
import com.intellij.httpClient.execution.common.CommonClientResponseBody.TextStream
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.util.io.toByteArray
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import io.rsocket.Payload
import io.rsocket.RSocket
import io.rsocket.core.RSocketConnector
import io.rsocket.metadata.CompositeMetadataCodec
import io.rsocket.metadata.TaggingMetadataCodec
import io.rsocket.metadata.WellKnownMimeType
import io.rsocket.transport.ClientTransport
import io.rsocket.transport.netty.client.TcpClientTransport
import io.rsocket.transport.netty.client.WebsocketClientTransport
import io.rsocket.util.DefaultPayload
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.jetbrains.plugins.rsocket.restClient.execution.RSocketBodyFileHint
import org.jetbrains.plugins.rsocket.restClient.execution.RSocketRequest
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
            clientRSocket = createRSocket(rsocketRequest)
            val result = clientRSocket.requestResponse(createPayload(rsocketRequest)).block()!!
            text = convertPayloadText(dataMimeType, result)
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
        var clientRSocket: RSocket? = null;
        try {
            clientRSocket = createRSocket(rsocketRequest)
            clientRSocket.fireAndForget(createPayload(rsocketRequest)).block()
        } catch (e: Exception) {
            return RSocketClientResponse(
                CommonClientResponseBody.Empty(), "text/plain",
                "ERROR", e.stackTraceToString()
            )
        } finally {
            clientRSocket?.dispose()
        }
        return RSocketClientResponse()
    }

    fun requestStream(rsocketRequest: RSocketRequest): CommonClientResponse {
        val dataMimeType = rsocketRequest.acceptMimeTypeHint()
        var clientRSocket: RSocket? = null
        val disposeRSocket = Disposable {
            if (clientRSocket != null && clientRSocket!!.isDisposed) {
                clientRSocket!!.dispose()
            }
        }
        val shared = MutableSharedFlow<TextStream.Message>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val textStream = TextStream(shared, bodyFileHint(rsocketRequest)).withConnectionDisposable(disposeRSocket)
        try {
            clientRSocket = createRSocket(rsocketRequest)
            clientRSocket.requestStream(createPayload(rsocketRequest))
                .doFinally {
                    shared.tryEmit(TextStream.Message.ConnectionClosed.End)
                }
                .doOnError {
                    shared.tryEmit(TextStream.Message.ConnectionClosed.WithError(it))
                }
                .subscribe {
                    println("ooooo")
                    val data = convertPayloadText(dataMimeType, it)
                    shared.tryEmit(TextStream.Message.Chunk(data))
                }
        } catch (e: Exception) {
            return RSocketClientResponse(
                CommonClientResponseBody.Empty(), "text/plain",
                "ERROR", e.stackTraceToString()
            )
        }
        return RSocketClientResponse(textStream, dataMimeType)
    }

    fun metadataPush(rsocketRequest: RSocketRequest): CommonClientResponse {
        if (rsocketRequest.textToSend != null) {
            var clientRSocket: RSocket? = null
            try {
                clientRSocket = createRSocket(rsocketRequest)
                DefaultPayload.create(Unpooled.EMPTY_BUFFER, Unpooled.wrappedBuffer(rsocketRequest.textToSend.toByteArray()))
                clientRSocket.metadataPush(createPayload(rsocketRequest)).block()
            } catch (e: Exception) {
                return RSocketClientResponse(
                    CommonClientResponseBody.Empty(), "text/plain",
                    "ERROR", e.stackTraceToString()
                )
            } finally {
                clientRSocket?.dispose()
            }
        }
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

    private fun createRSocket(rsocketRequest: RSocketRequest): RSocket {
        val rsocketURI = rsocketRequest.rsocketURI
        val clientTransport: ClientTransport = if (rsocketURI.scheme == "tcp") {
            TcpClientTransport.create(rsocketURI.host, rsocketURI.port)
        } else {
            WebsocketClientTransport.create(rsocketURI)
        }
        return RSocketConnector.create()
            .dataMimeType(rsocketRequest.dataMimeTyp)
            .metadataMimeType(rsocketRequest.metadataMimeTyp)
            .connect(clientTransport)
            .block()!!
    }

    private fun createPayload(rsocketRequest: RSocketRequest): Payload {
        val compositeMetadataBuffer = compositeMetadata(rsocketRequest)
        val dataBuf = if (rsocketRequest.textToSend != null) {
            Unpooled.wrappedBuffer(rsocketRequest.textToSend.toByteArray())
        } else {
            Unpooled.EMPTY_BUFFER
        }
        return DefaultPayload.create(dataBuf, compositeMetadataBuffer)
    }

    private fun compositeMetadata(rsocketRequest: RSocketRequest): ByteBuf {
        val compositeMetadataBuffer = ByteBufAllocator.DEFAULT.compositeBuffer()
        if (rsocketRequest.routingMetadata()[0].isNotEmpty()) {
            val routingMetaData = TaggingMetadataCodec.createTaggingContent(ByteBufAllocator.DEFAULT, rsocketRequest.routingMetadata())
            CompositeMetadataCodec.encodeAndAddMetadata(
                compositeMetadataBuffer, ByteBufAllocator.DEFAULT,
                WellKnownMimeType.MESSAGE_RSOCKET_ROUTING,
                routingMetaData
            )
        }
        return compositeMetadataBuffer
    }

    private fun convertPayloadText(dataMimeType: String, payload: Payload): String {
        return if (dataMimeType.contains("text") || dataMimeType.contains("json") || dataMimeType.contains("xml")) {
            payload.dataUtf8
        } else {
            Base64.getEncoder().encodeToString(payload.data.toByteArray())
        }
    }

}