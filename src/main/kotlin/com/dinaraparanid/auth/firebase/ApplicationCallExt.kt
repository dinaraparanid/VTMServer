package com.dinaraparanid.auth.firebase

import io.ktor.server.application.*
import io.ktor.server.auth.*

inline val ApplicationCall.authHeader
    get() = request.parseAuthorizationHeader()