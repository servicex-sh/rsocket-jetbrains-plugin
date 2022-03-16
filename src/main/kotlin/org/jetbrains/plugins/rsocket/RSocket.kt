package org.jetbrains.plugins.rsocket

import com.intellij.openapi.util.IconLoader

val rsocketIcon = IconLoader.getIcon("rsocket-icon.svg", RSocketBundle::class.java)
const val messageMappingFullName = "org.springframework.messaging.handler.annotation.MessageMapping"
const val rsocketServiceFullName = "com.alibaba.rsocket.RSocketService"

val RSOCKET_REQUEST_TYPES = setOf("RSOCKET", "RPC", "FNF", "STREAM", "METADATA")
