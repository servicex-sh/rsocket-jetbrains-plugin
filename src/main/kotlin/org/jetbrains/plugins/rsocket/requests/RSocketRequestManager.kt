package org.jetbrains.plugins.rsocket.requests

import com.intellij.httpClient.execution.common.CommonClientBodyFileHint
import com.intellij.httpClient.execution.common.CommonClientResponse
import com.intellij.httpClient.execution.common.CommonClientResponseBody
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.util.io.toByteArray
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.CompositeByteBuf
import io.netty.buffer.Unpooled
import io.rsocket.Payload
import io.rsocket.RSocket
import io.rsocket.broker.common.Id
import io.rsocket.broker.common.MimeTypes
import io.rsocket.broker.common.Tags
import io.rsocket.broker.common.WellKnownKey
import io.rsocket.broker.frames.Address
import io.rsocket.broker.frames.AddressFlyweight
import io.rsocket.broker.frames.RouteSetupFlyweight
import io.rsocket.core.RSocketConnector
import io.rsocket.metadata.CompositeMetadataCodec.encodeAndAddMetadata
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
import kotlin.experimental.or


@Suppress("UnstableApiUsage")
class RSocketRequestManager(private val project: Project) : Disposable {
    private var appId = UUID.randomUUID().toString()

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
        val shared = MutableSharedFlow<CommonClientResponseBody.TextStream.Message>(
            replay = 1000,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val textStream = CommonClientResponseBody.TextStream(shared, bodyFileHint(rsocketRequest)).withConnectionDisposable(disposeRSocket)
        try {
            clientRSocket = createRSocket(rsocketRequest)
            clientRSocket.requestStream(createPayload(rsocketRequest))
                .doFinally {
                    shared.tryEmit(CommonClientResponseBody.TextStream.Message.ConnectionClosed.End)
                    clientRSocket.dispose()
                }
                .doOnError {
                    shared.tryEmit(CommonClientResponseBody.TextStream.Message.ConnectionClosed.WithError(it))
                }
                .subscribe {
                    val data = convertPayloadText(dataMimeType, it)
                    shared.tryEmit(CommonClientResponseBody.TextStream.Message.Chunk(data + "\n\n"))
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
        var setupPayload: Payload? = null
        if (rsocketRequest.headers != null) {
            if (rsocketRequest.isAliBroker()) {
                setupPayload = createSetupPayloadForAliBroker()
            } else if (rsocketRequest.isSpringBroker()) {
                setupPayload = createSetupPayloadForSpringBroker(Id.from(appId))
            }
        }
        if (setupPayload == null) {
            setupPayload = DefaultPayload.create(Unpooled.EMPTY_BUFFER)
        }
        return RSocketConnector.create()
            .dataMimeType(rsocketRequest.dataMimeTyp)
            .metadataMimeType(rsocketRequest.metadataMimeTyp)
            .setupPayload(setupPayload!!)
            .connect(clientTransport)
            .block()!!
    }


    private fun createSetupPayloadForSpringBroker(routeId: Id): Payload {
        appId = UUID.randomUUID().toString()
        val allocator = ByteBufAllocator.DEFAULT
        val routeSetup = RouteSetupFlyweight.encode(allocator, routeId, "rsocket-jetbrains-plugin", Tags.empty(), 0)
        val setupMetadata: CompositeByteBuf = allocator.compositeBuffer()
        encodeAndAddMetadata(setupMetadata, allocator, MimeTypes.BROKER_FRAME_MIME_TYPE, routeSetup)
        return DefaultPayload.create(Unpooled.EMPTY_BUFFER, setupMetadata)
    }

    private fun createSetupPayloadForAliBroker(): Payload {
        val allocator = ByteBufAllocator.DEFAULT
        val setupMetadata: CompositeByteBuf = allocator.compositeBuffer()
        val appInfo = """{"name": "rsocket-jetbrains-plugin"}""".toByteArray()
        encodeAndAddMetadata(setupMetadata, allocator, "message/x.rsocket.application+json", Unpooled.wrappedBuffer(appInfo))
        return DefaultPayload.create(Unpooled.EMPTY_BUFFER, setupMetadata)
    }

    private fun createPayload(rsocketRequest: RSocketRequest): Payload {
        val compositeMetadataBuffer = compositeMetadata(rsocketRequest)
        val dataBuf = if (rsocketRequest.textToSend != null) {
            Unpooled.wrappedBuffer(rsocketRequest.textToSend.toByteArray())
        } else {
            Unpooled.EMPTY_BUFFER
        }
        if (rsocketRequest.isSpringBroker()) {
            encodeAddressMetadata(Id.from(appId), compositeMetadataBuffer, rsocketRequest)
        }
        return DefaultPayload.create(dataBuf, compositeMetadataBuffer)
    }

    private fun compositeMetadata(rsocketRequest: RSocketRequest): CompositeByteBuf {
        val compositeMetadataBuffer = ByteBufAllocator.DEFAULT.compositeBuffer()
        if (rsocketRequest.routingMetadata()[0].isNotEmpty()) {
            val routingMetaData = TaggingMetadataCodec.createTaggingContent(ByteBufAllocator.DEFAULT, rsocketRequest.routingMetadata())
            encodeAndAddMetadata(
                compositeMetadataBuffer, ByteBufAllocator.DEFAULT,
                WellKnownMimeType.MESSAGE_RSOCKET_ROUTING,
                routingMetaData
            )
            val dataType = WellKnownMimeType.fromString(rsocketRequest.dataMimeTyp);
            encodeAndAddMetadata(
                compositeMetadataBuffer, ByteBufAllocator.DEFAULT,
                WellKnownMimeType.MESSAGE_RSOCKET_MIMETYPE,
                Unpooled.wrappedBuffer(byteArrayOf(dataType.identifier.or(0x80.toByte())))
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

    /**
     * encode address metadata for Spring RSocket Broker
     */
    private fun encodeAddressMetadata(routeId: Id, metadataHolder: CompositeByteBuf, request: RSocketRequest) {
        val builder: Address.Builder = Address.from(routeId)
        request.headers?.forEach { (key, value) ->
            if (key.startsWith("X-")) {
                val keyName = key.substring(2)
                val knownKey = WellKnownKey.fromMimeType("io.rsocket.broker.$keyName")
                if (knownKey != WellKnownKey.UNPARSEABLE_KEY) {
                    builder.with(knownKey, value.trim())
                } else {
                    builder.with(keyName, value.trim())
                }
            }
        }
        val address = builder.build()
        val byteBuf = AddressFlyweight.encode(ByteBufAllocator.DEFAULT, address.originRouteId, address.metadata, address.tags, address.flags)
        encodeAndAddMetadata(metadataHolder, ByteBufAllocator.DEFAULT, MimeTypes.BROKER_FRAME_MIME_TYPE, byteBuf)
    }

}