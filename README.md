RSocket plugin for JetBrains IDEs
===================

<!-- Plugin description -->
**RSocket plugin** is a plugin for JetBrains IDE to execute RSocket requests in HTTP Client.

```
RPC 127.0.0.1:42252/com.example.service.HelloService.hello
Content-Type: application/json

["linux_china"]
```

<!-- Plugin description end -->

# RSocket url

* without schema means tcp connection
* http schema means WebSocket connection
* extra routing as query: `127.0.0.1:42252/com.example.service.HelloService.hello?e=xxx`

# Http Headers

* Host: localhost:42252
* From: app information, such app name, ip, datacenter etc
* Content-Type: application/json
* Metadata-Type: application/json
* Metadata: base64-string for composite metadata or text
* Accept: application/json
* Authorization: Bearer <token>
* Pragma: no-cache
* User-Agent: DemoApp/1.0.0

For more please refer https://en.wikipedia.org/wiki/List_of_HTTP_header_fields
           
**Attention**: According to [HTTP spec](https://www.rfc-editor.org/rfc/rfc7230#section-3.2), 
JSON may be used as HTTP header value, and some limitations: no "\r","\n", invisible or none-ASCII characters

# Attentions

* projectService: XxxRequestsManager that sends requests to upstream
* TextStream.withConnectionDisposable() to close the connection

# todo

* live templates for rsocket

# References

* RSocket: [https://rsocket.io/](https://rsocket.io/)
