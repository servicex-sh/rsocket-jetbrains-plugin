RSocket plugin for JetBrains IDEs
===================

<!-- Plugin description -->
**RSocket plugin** is a plugin for JetBrains IDE to execute RSocket requests in HTTP Client.

The following features are available for RSocket:

* Execute request/response, fireAndForget, request/Stream, metadataPush for RSocket
* Live templates: rpc, fnf, stream, metadata
* Spring Boot RSocket, Alibaba/Spring RSocket Broker support
* Code completion for RSocket headers
* Intention action to convert RSocket request to rsc CLI
* Transportation support: TCP and WebSocket

<!-- Plugin description end -->

# RSocket requests demo

```http request
### rsocket request response for Spring Boot RSocket
RSOCKET com.example.UserService.findById
Host: 127.0.0.1:42252
Content-Type: application/json

1

### Alibaba RSocket Broker
RSOCKET com.alibaba.user.UserService.findById
Host: 127.0.0.1:9999
X-AliBroker: true
Content-Type: application/json

[2]

### Spring RSocket Broker
RSOCKET pong
Host: 127.0.0.1:8001
X-ServiceName: com.example.PongService
Content-Type: application/json

"ping"
```

# RSocket URI and Routing

* without schema means tcp connection: `127.0.0.1:42252`
* http schema means WebSocket connection: `http://127.0.0.1:8080/rsocket`
* RSocket Routing:  path means first tag for routing, and query params for other tags `127.0.0.1:42252/com.example.service.HelloService.hello?e=xxx`

# Http Headers for RSocket

* Host: the target to connect by tcp `localhost:42252` or by WS `ws://localhost:8080/rsocket`
* From: app information, such app name, ip, datacenter etc
* Content-Type: data content type, and default is `application/json`
* Metadata-Type: metadata type, and default is `message/x.rsocket.composite-metadata.v0`
* Metadata: metadata for payload, and it should be base64-string for composite metadata
* Accept: Metadata Payload for acceptable data MIME Type
* Authorization: Bearer <token>

# Todo
                 
* Replace rsocket-java to rsocket-kotlin
* endpoints for Spring Boot RSocket
* codeInsight.lineMarkerProvider for @MessageMapping

# References

* RSocket: [https://rsocket.io/](https://rsocket.io/)
* rsocket-kotlin: https://github.com/rsocket/rsocket-kotlin 
