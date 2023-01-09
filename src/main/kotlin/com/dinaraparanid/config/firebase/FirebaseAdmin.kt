package com.dinaraparanid.config.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

object FirebaseAdmin {
    private val firebaseServiceAccountId = System.getenv("FIREBASE_SERVICE_ACCOUNT_ID")
    private val firebaseAdminSdk = System.getenv("FIREBASE_ADMIN_SDK")

    lateinit var app: FirebaseApp
        private set

    fun init() {
        app = FirebaseApp.initializeApp(
            FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(firebaseAdminSdk.byteInputStream()))
                .setServiceAccountId(firebaseServiceAccountId)
                .build()
        )
    }
}