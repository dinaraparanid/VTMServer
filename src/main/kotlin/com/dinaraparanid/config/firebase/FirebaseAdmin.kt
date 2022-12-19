package com.dinaraparanid.config.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.io.FileInputStream

object FirebaseAdmin {
    lateinit var app: FirebaseApp
        private set

    fun init() {
        app = FirebaseApp.initializeApp(
            FileInputStream("vtmconverter-firebase-adminsdk.json").use { serviceAccount ->
                FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setServiceAccountId(firebaseServiceAccountId)
                    .build()
            }
        )
    }
}