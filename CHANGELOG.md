<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# RSocket plugin Changelog

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
