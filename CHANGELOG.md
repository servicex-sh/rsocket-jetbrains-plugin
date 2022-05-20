<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# RSocket plugin Changelog

## [0.10.2]

### Added

- Compatible with JetBrains IDEs 2022.*

## [0.10.1]

### Added

- Update to RSocket Java SDK 1.1.2
- RSocket Kotlin support: `io.rsocket.kotlin.RSocket` annotated as request request
- Add `GRAPHQLRS` method for GraphQL over RSocket: https://docs.spring.io/spring-graphql/docs/1.0.0-RC1/reference/html/#server-rsocket

```http request
### GraphQL over RSocket
GRAPHQLRS graphql
Host: ws://localhost:8080/rsocket
Content-Type: application/graphql

query {
    findBook(id: "book-1") { id name }
}

### GraphQL subscription over RSocket Stream
GRAPHQLRS graphql
Host: ws://localhost:8080/rsocket
Content-Type: application/graphql

subscription { greetings }
```

## [0.10.0]

### Added

- Add application/graphql support:

```http request
### GraphQL over RSocket
RSOCKET graphql
Host: ws://localhost:8080/rsocket
Content-Type: application/graphql

query {
    findBook(id: "book-1") { id name }
}
```

If you want to use GraphQL variables, please add `X-GraphQL-Variables: {id: 1}` HTTP header.

## [0.9.0]

### Added

- Microservices Annotator support for RSocket and RSocketRequester interfaces

## [0.8.0]

### Added

- Line marker for methods in RSocket class with API test

## [0.7.0]

### Added

- Bug Fix for METADATA_PUSH
- IntelliJ IDEA 2022.1 compatible

## [0.6.0]

### Added

- RSocket Endpoints support
- Code completion/navigation for RSocket routing

## [0.5.0]

### Added

- Add support for Metadata-Type with `application/json`
- Add Setup-Data and Setup-Metadata RSocket headers
- Data format for Metadata and Data: `normal text` or `data:application/octet-stream;base64,<base64-string>`

## [0.4.0]

### Added

- Add `Hooks.onErrorDropped` to suppress noisy warning
- PSI read operations moved to application.runReadAction{} to suppress concurrent exception
- Optimization for RSocket WebSocket URL, please use `Host: ws://localhost:8080/rsocket` for WS

## [0.3.0]

### Added

- Bug fix for RSocket WebSocket URL, please use `Host: ws://localhost:8080/rsocket` for WS

## [0.2.0]

### Added

- Add custom HTTP headers for RSocket
- Code completion for RSocket headers
- Intention action to convert RSocket request to rsc CLI

## [0.1.0]

### Added

- RSocket requests support in HTTP Client
- Live templates: rpc, fnf, stream, metadata
- Spring Boot RSocket, Alibaba/Spring RSocket Broker support
