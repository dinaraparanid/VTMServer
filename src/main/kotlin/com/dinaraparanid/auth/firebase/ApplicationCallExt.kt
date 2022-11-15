package com.dinaraparanid.auth.firebase

import com.google.firebase.auth.AuthErrorCode
import com.google.firebase.auth.FirebaseAuthException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import java.util.Locale

internal inline val ApplicationCall.authHeader
    get() = request.parseAuthorizationHeaderOrNull()

private inline val AuthErrorCode.respondMessage
    get() = javaClass.simpleName
        .toString()
        .lowercase(Locale.getDefault())
        .replace('_', ' ')

internal suspend fun ApplicationCall.respondAuthError(error: FirebaseAuthException) = error.authErrorCode.run {
    suspend fun respondError(statusCode: HttpStatusCode) = respond(statusCode, respondMessage)

    when (this) {
        AuthErrorCode.CONFIGURATION_NOT_FOUND -> respondError(HttpStatusCode.NotFound)
        AuthErrorCode.EMAIL_NOT_FOUND -> respondError(HttpStatusCode.NotFound)
        AuthErrorCode.TENANT_NOT_FOUND -> respondError(HttpStatusCode.NotFound)
        AuthErrorCode.UNAUTHORIZED_CONTINUE_URL -> respondError(HttpStatusCode.Unauthorized)
        AuthErrorCode.USER_NOT_FOUND -> respondError(HttpStatusCode.NotFound)
        else -> respondError(HttpStatusCode.BadRequest)
    }
}