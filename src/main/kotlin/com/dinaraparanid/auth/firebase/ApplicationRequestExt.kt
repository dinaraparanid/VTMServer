package com.dinaraparanid.auth.firebase

import io.ktor.server.auth.*
import io.ktor.server.request.*

internal fun ApplicationRequest.parseAuthorizationHeaderOrNull() = try {
    parseAuthorizationHeader()
} catch (ex: IllegalArgumentException) {
    null
}