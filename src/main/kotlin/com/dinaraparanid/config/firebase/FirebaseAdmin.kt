package com.dinaraparanid.config.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.io.FileInputStream

object FirebaseAdmin {
    private val firebaseServiceAccountId = System.getenv("FIREBASE_SERVICE_ACCOUNT_ID")

    lateinit var app: FirebaseApp
        private set

    fun init() {
        app = FirebaseApp.initializeApp(
            FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(FileInputStream("vtmconverter-firebase-adminsdk.json")))
                .setServiceAccountId(firebaseServiceAccountId)
                .build()
        )
    }
}