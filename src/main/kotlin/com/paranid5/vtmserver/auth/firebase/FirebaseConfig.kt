package com.paranid5.vtmserver.auth.firebase

import com.paranid5.vtmserver.data.User
import com.google.firebase.auth.FirebaseToken
import io.ktor.server.auth.*

class FirebaseConfig(name: String?) : AuthenticationProvider.Config(name) {
    val firebaseAuthenticationFunction: AuthenticationFunction<FirebaseToken> = { token ->
        token.email?.let { User(token) }
    }
}