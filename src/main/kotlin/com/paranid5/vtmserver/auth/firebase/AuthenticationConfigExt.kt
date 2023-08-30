package com.paranid5.vtmserver.auth.firebase

import com.paranid5.vtmserver.config.firebase.FirebaseAdmin
import io.ktor.server.auth.*

fun AuthenticationConfig.firebase() {
    FirebaseAdmin.init()
    register(provider = FirebaseAuthProvider(FirebaseConfig(FirebaseAuthProvider.FIREBASE_AUTH)))
}