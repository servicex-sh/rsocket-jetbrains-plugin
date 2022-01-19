package org.jetbrains.plugins.rsocket

import com.intellij.openapi.util.IconLoader

val rsocketIcon = IconLoader.findIcon("rsocket-icon.svg")!!

val RSOCKET_REQUEST_TYPES = listOf("RSOCKET", "RPC", "FNF", "STREAM", "METADATA")
