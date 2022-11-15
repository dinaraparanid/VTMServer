package com.dinaraparanid.auth.firebase

import com.dinaraparanid.models.User
import com.google.firebase.auth.FirebaseToken
import io.ktor.server.auth.*

internal class FirebaseConfig(name: String?) : AuthenticationProvider.Config(name) {
    internal val firebaseAuthenticationFunction: AuthenticationFunction<FirebaseToken> = { token ->
        token.email?.let { User(token) }
    }
}