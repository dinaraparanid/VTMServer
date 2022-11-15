package com.dinaraparanid.models

import com.google.firebase.auth.FirebaseToken
import com.google.firebase.auth.UserRecord
import io.ktor.server.auth.*
import kotlinx.serialization.Serializable

@Serializable
internal data class User(
    @JvmField val uid: String,
    @JvmField val email: String,
    @JvmField val login: String,
    @JvmField val password: String? = null
) : Principal {
    internal constructor(firebaseToken: FirebaseToken) : this(
        firebaseToken.uid,
        firebaseToken.email,
        firebaseToken.name
    )

    internal constructor(firebaseUserRecord: UserRecord) : this(
        firebaseUserRecord.uid,
        firebaseUserRecord.email,
        firebaseUserRecord.displayName
    )
}
