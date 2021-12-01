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

* Content-Type: application/json
* Metadata-Type: application/json
* Accept: application/json
* Authorization: Bearer <token>
* Pragma: no-cache
* User-Agent: DemoApp/1.0.0

For more please refer https://en.wikipedia.org/wiki/List_of_HTTP_header_fields

# todo

* live templates for rsocket

# References

* RSocket: [https://rsocket.io/](https://rsocket.io/)
