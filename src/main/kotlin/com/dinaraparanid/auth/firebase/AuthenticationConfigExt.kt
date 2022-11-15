package com.dinaraparanid.auth.firebase

import io.ktor.server.auth.*

internal fun AuthenticationConfig.firebase() =
    register(provider = FirebaseAuthProvider(FirebaseConfig(FirebaseAuthProvider.FIREBASE_AUTH)))