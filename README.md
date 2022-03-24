RSocket plugin for JetBrains IDEs
===================

<!-- Plugin description -->
**RSocket plugin** is a plugin for JetBrains IDE to execute RSocket requests in HTTP Client.

The following features are available for RSocket:

* Execute request/response, fireAndForget, request/Stream, metadataPush for RSocket
* Live templates: rpc, fnf, stream, metadata
* Spring Boot RSocket, Alibaba/Spring RSocket Broker support
* Code completion for RSocket headers
* Code completion/navigation for RSocket routing
* Intention action to convert RSocket request to rsc CLI
* Transportation support: TCP and WebSocket
* RSocket Endpoint support: Java and Kotlin
* Line marker for methods in RSocket class with API test 

### RSocket requests demo

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
```

<!-- Plugin description end -->


# RSocket URI and Routing

* without schema means tcp connection: `127.0.0.1:42252`
* http schema means WebSocket connection: `http://127.0.0.1:8080/rsocket`
* RSocket Routing:  path means first tag for routing, and query params for other tags `127.0.0.1:42252/com.example.service.HelloService.hello?e=xxx`

# Http Headers for RSocket

* Host: the target to connect by tcp `localhost:42252` or by WS `ws://localhost:8080/rsocket`
* From: app information, such app name, ip, datacenter etc
* Content-Type: data content type, and default is `application/json`
* Metadata-Type: metadata type, and default is `message/x.rsocket.composite-metadata.v0`
* Setup-Metadata: Metadata for Setup payload
* Setup-Data: Data for Setup payload
* Metadata: metadata for payload, and it should be base64-string for composite metadata
* Accept: Metadata Payload for acceptable data MIME Type
* Authorization: Bearer <token>

# Data format for Metadata and Data

* Text style: `normal text`
* Binary style: `data:application/octet-stream;base64,<base64-string>`

# References

* RSocket: [https://rsocket.io/](https://rsocket.io/)
* rsocket-kotlin: https://github.com/rsocket/rsocket-kotlin 
* Language Injection: https://plugins.jetbrains.com/docs/intellij/language-injection.html
