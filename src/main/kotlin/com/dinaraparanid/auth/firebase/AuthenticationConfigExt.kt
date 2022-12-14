package com.dinaraparanid.auth.firebase

import com.dinaraparanid.config.firebase.FirebaseAdmin
import io.ktor.server.auth.*

fun AuthenticationConfig.firebase() {
    FirebaseAdmin.init()
    register(provider = FirebaseAuthProvider(FirebaseConfig(FirebaseAuthProvider.FIREBASE_AUTH)))
}