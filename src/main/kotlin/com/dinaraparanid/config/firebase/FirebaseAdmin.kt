package com.dinaraparanid.config.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.io.FileInputStream

internal object FirebaseAdmin {
    internal lateinit var app: FirebaseApp
        private set

    internal fun init() {
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