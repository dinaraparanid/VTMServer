package com.paranid5.vtmserver.data

import com.google.firebase.auth.FirebaseToken
import com.google.firebase.auth.UserRecord
import io.ktor.server.auth.*
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @JvmField val uid: String? = null,
    @JvmField val email: String,
    @JvmField val login: String,
) : Principal {
    constructor(firebaseToken: FirebaseToken) : this(
        firebaseToken.uid,
        firebaseToken.email,
        firebaseToken.name ?: firebaseToken.email
    )

    constructor(firebaseUserRecord: UserRecord) : this(
        firebaseUserRecord.uid,
        firebaseUserRecord.email,
        firebaseUserRecord.displayName ?: firebaseUserRecord.email
    )
}
