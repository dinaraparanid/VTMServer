package com.dinaraparanid.plugins

import com.dinaraparanid.auth.firebase.firebase
import io.ktor.server.application.*
import io.ktor.server.auth.*

fun Application.configureAuthentication() = authentication(AuthenticationConfig::firebase)