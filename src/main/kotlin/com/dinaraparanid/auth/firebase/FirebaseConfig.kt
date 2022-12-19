package com.dinaraparanid.auth.firebase

import com.dinaraparanid.models.User
import com.google.firebase.auth.FirebaseToken
import io.ktor.server.auth.*

class FirebaseConfig(name: String?) : AuthenticationProvider.Config(name) {
    val firebaseAuthenticationFunction: AuthenticationFunction<FirebaseToken> = { token ->
        token.email?.let { User(token) }
    }
}