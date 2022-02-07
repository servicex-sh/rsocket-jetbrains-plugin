package org.jetbrains.plugins.rsocket

import com.intellij.openapi.util.IconLoader

val rsocketIcon = IconLoader.findIcon("rsocket-icon.svg")!!
val messageMappingFullName = "org.springframework.messaging.handler.annotation.MessageMapping"
val rsocketServiceFullName = "com.alibaba.rsocket.RSocketService"

val RSOCKET_REQUEST_TYPES = listOf("RSOCKET", "RPC", "FNF", "STREAM", "METADATA")
