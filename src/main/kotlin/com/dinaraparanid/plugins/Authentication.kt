package com.dinaraparanid.plugins

import com.dinaraparanid.auth.firebase.firebase
import com.dinaraparanid.config.firebase.FirebaseAdmin
import io.ktor.server.application.*
import io.ktor.server.auth.*

fun Application.configureAuthentication() {
    FirebaseAdmin.app
    authentication(AuthenticationConfig::firebase)
}