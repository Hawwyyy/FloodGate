package com.example.floodgate.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.example.floodgate.R
import com.example.floodgate.data.auth.AuthServiceError
import com.example.floodgate.data.auth.FirebaseAuthRepository
import com.example.floodgate.data.auth.RegistrationResult
import com.example.floodgate.data.auth.SignInResult

private enum class AuthDestination {
    SIGN_IN,
    SIGN_UP,
    SUCCESS
}

@Composable
fun FloodGateAuthApp(
    onExit: () -> Unit,
    repository: FirebaseAuthRepository = remember { FirebaseAuthRepository() }
) {
    val initialDestination = if (repository.currentUser != null) {
        AuthDestination.SUCCESS
    } else {
        AuthDestination.SIGN_IN
    }
    var destinationName by rememberSaveable { mutableStateOf(initialDestination.name) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var signInError by rememberSaveable { mutableStateOf<AuthServiceError?>(null) }
    var signUpError by rememberSaveable { mutableStateOf<AuthServiceError?>(null) }
    var registrationSucceeded by rememberSaveable { mutableStateOf(false) }

    val destination = AuthDestination.valueOf(destinationName)

    BackHandler(enabled = destination != AuthDestination.SIGN_IN || isLoading) {
        when {
            isLoading -> Unit
            destination == AuthDestination.SIGN_UP -> {
                signUpError = null
                destinationName = AuthDestination.SIGN_IN.name
            }
            else -> onExit()
        }
    }

    when (destination) {
        AuthDestination.SIGN_IN -> SignInScreen(
            isLoading = isLoading,
            authError = signInError?.let { stringResource(it.messageRes) },
            noticeMessage = if (registrationSucceeded) {
                stringResource(R.string.registration_successful)
            } else {
                null
            },
            onClearAuthError = { signInError = null },
            onBackClick = { if (!isLoading) onExit() },
            onSignInClick = { credentials ->
                if (!isLoading) {
                    signInError = null
                    registrationSucceeded = false
                    isLoading = true
                    repository.signIn(credentials) { result ->
                        isLoading = false
                        when (result) {
                            is SignInResult.Success -> {
                                destinationName = AuthDestination.SUCCESS.name
                            }
                            is SignInResult.Failure -> signInError = result.error
                        }
                    }
                }
            },
            onForgotPasswordClick = {
                signInError = AuthServiceError.SIGN_IN_FAILED
            },
            onGoogleSignInClick = {
                signInError = AuthServiceError.SIGN_IN_FAILED
            },
            onSignUpClick = {
                if (!isLoading) {
                    signInError = null
                    registrationSucceeded = false
                    destinationName = AuthDestination.SIGN_UP.name
                }
            }
        )

        AuthDestination.SIGN_UP -> SignUpScreen(
            isLoading = isLoading,
            authError = signUpError?.let { stringResource(it.messageRes) },
            onClearAuthError = { signUpError = null },
            onBackClick = {
                if (!isLoading) {
                    signUpError = null
                    destinationName = AuthDestination.SIGN_IN.name
                }
            },
            onSignUpClick = { credentials ->
                if (!isLoading) {
                    signUpError = null
                    isLoading = true
                    repository.register(credentials) { result ->
                        isLoading = false
                        when (result) {
                            is RegistrationResult.Success -> {
                                registrationSucceeded = true
                                destinationName = AuthDestination.SIGN_IN.name
                            }
                            is RegistrationResult.Failure -> signUpError = result.error
                        }
                    }
                }
            },
            onGoogleSignUpClick = {
                signUpError = AuthServiceError.REGISTRATION_FAILED
            },
            onSignInClick = {
                if (!isLoading) {
                    signUpError = null
                    destinationName = AuthDestination.SIGN_IN.name
                }
            }
        )

        AuthDestination.SUCCESS -> AuthSuccessScreen(
            email = repository.currentUser?.email.orEmpty(),
            onSignOutClick = {
                repository.signOut()
                signInError = null
                registrationSucceeded = false
                destinationName = AuthDestination.SIGN_IN.name
            }
        )
    }
}
