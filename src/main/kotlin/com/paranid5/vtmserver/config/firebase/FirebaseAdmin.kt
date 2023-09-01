package com.paranid5.vtmserver.config.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import io.github.cdimascio.dotenv.dotenv

object FirebaseAdmin {
    private val firebaseAdminSdk by lazy {
        dotenv()["FIREBASE_ADMIN_SDK"]
    }

    lateinit var app: FirebaseApp
        private set

    fun init() {
        app = FirebaseApp.initializeApp(
            FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(firebaseAdminSdk.byteInputStream()))
                .build()
        )
    }
}