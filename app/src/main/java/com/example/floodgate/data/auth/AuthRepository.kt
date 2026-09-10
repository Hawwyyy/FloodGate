package com.example.floodgate.data.auth

import com.example.floodgate.ui.auth.SignInCredentials
import com.example.floodgate.ui.auth.SignUpCredentials

/** Authentication contract keeps navigation testable without creating real Firebase accounts. */
interface AuthRepository {
    val currentUser: AuthenticatedUser?
    fun register(credentials: SignUpCredentials, onResult: (RegistrationResult) -> Unit)
    fun signIn(credentials: SignInCredentials, onResult: (SignInResult) -> Unit)
    fun signOut()
}
