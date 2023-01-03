package com.dinaraparanid.config.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

object FirebaseAdmin {
    private val firebaseServiceAccountId = System.getenv("FIREBASE_SERVICE_ACCOUNT_ID")

    private val firebaseAdminSdkJson = System.getenv("FIREBASE_ADMIN_SDK_JSON")

    lateinit var app: FirebaseApp
        private set

    fun init() {
        app = FirebaseApp.initializeApp(
            FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(firebaseAdminSdkJson.byteInputStream()))
                .setServiceAccountId(firebaseServiceAccountId)
                .build()
        )
    }
}