package org.jetbrains.plugins.rsocket

import io.rsocket.ConnectionSetupPayload
import io.rsocket.Payload
import io.rsocket.RSocket
import io.rsocket.core.RSocketServer
import io.rsocket.metadata.CompositeMetadata
import io.rsocket.transport.netty.server.TcpServerTransport
import io.rsocket.util.DefaultPayload
import reactor.core.publisher.Flux
import reactor.core.publisher.Hooks
import reactor.core.publisher.Mono

fun main() {
    Hooks.onErrorDropped { }
    val closeableChannel = RSocketServer.create()
        .acceptor { setup: ConnectionSetupPayload, sendingSocket: RSocket ->
            printCompositeMetadata(setup)
            Mono.just(object : RSocket {
                override fun requestResponse(payload: Payload): Mono<Payload> {
                    printCompositeMetadata(payload)
                    return Mono.just(DefaultPayload.create("{id: 2}"))
                }

                override fun requestStream(payload: Payload): Flux<Payload> {
                    return Flux.just(DefaultPayload.create("{id: 1}"), DefaultPayload.create("{id: 2}"))
                }

                override fun fireAndForget(payload: Payload): Mono<Void> {
                    return Mono.empty();
                }

                override fun metadataPush(payload: Payload): Mono<Void> {
                    return Mono.empty()
                }
            })
        }
        .bind(TcpServerTransport.create("0.0.0.0", 42252))
        //.bind(WebsocketServerTransport.create("0.0.0.0", 42252))
        .block()
    println("RSocket responder is listening on 42252!")
    closeableChannel.onClose().block()
}


fun printCompositeMetadata(payload: Payload) {
    println(payload.metadataUtf8)
    val entries = CompositeMetadata(payload.metadata(), false)
    for (entry in entries) {
        println(entry.mimeType)
    }
}