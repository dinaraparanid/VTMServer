package com.dinaraparanid.config.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.io.FileInputStream

internal object FirebaseAdmin {
    private val serviceAccount = FileInputStream("vtmconverter-firebase-adminsdk.json")

    private val options = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
        .setServiceAccountId(firebaseServiceAccountId)
        .build()

    internal val app: FirebaseApp by lazy { FirebaseApp.initializeApp(options) }
}