package com.paranid5.vtmserver.plugins

import com.paranid5.vtmserver.auth.firebase.firebase
import io.ktor.server.application.*
import io.ktor.server.auth.*

fun Application.configureAuthentication() = authentication(AuthenticationConfig::firebase)