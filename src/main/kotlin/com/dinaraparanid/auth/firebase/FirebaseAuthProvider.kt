package com.dinaraparanid.auth.firebase

import com.dinaraparanid.config.firebase.FirebaseAdmin
import com.google.firebase.auth.*
import io.ktor.http.auth.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FirebaseAuthProvider(config: FirebaseConfig) : AuthenticationProvider(config) {
    private val authFunction = config.firebaseAuthenticationFunction

    companion object {
        const val FIREBASE_AUTH = "FIREBASE_AUTH"
        private const val FirebaseJWTAuthKey = "FirebaseAuth"
        private val firebaseAuth = FirebaseAuth.getInstance(FirebaseAdmin.app)

        private fun bearerAuthChallenge() =
            HttpAuthHeader.Parameterized("Bearer", mapOf(HttpAuthHeader.Parameters.Realm to FIREBASE_AUTH))

        private suspend fun verifyFirebaseIdToken(
            call: ApplicationCall,
            authHeader: HttpAuthHeader,
            getTokenData: suspend ApplicationCall.(FirebaseToken) -> Principal?
        ): Principal? {
            val token = when {
                authHeader.authScheme == "Bearer" && authHeader is HttpAuthHeader.Single ->
                    kotlin.runCatching {
                        withContext(Dispatchers.IO) { firebaseAuth.verifyIdToken(authHeader.blob) }
                    }.getOrElse { null }

                else -> null
            } ?: return null

            return getTokenData(call, token)
        }
    }

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val token = context.call.authHeader

        if (token == null) {
            context.challenge(FirebaseJWTAuthKey, AuthenticationFailedCause.InvalidCredentials) { challengeFunc, call ->
                challengeFunc.complete()
                call.respond(UnauthorizedResponse(bearerAuthChallenge()))
            }
            return
        }

        try {
            verifyFirebaseIdToken(context.call, token, authFunction)?.let(context::principal)
        } catch (cause: Throwable) {
            val message = cause.message ?: cause.javaClass.simpleName
            context.error(FirebaseJWTAuthKey, AuthenticationFailedCause.Error(message))
        }
    }
}