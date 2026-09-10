package com.example.floodgate.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.floodgate.R
import com.example.floodgate.ui.components.FloodGateBrandDimensions
import com.example.floodgate.ui.components.FloodGateBrandName
import com.example.floodgate.ui.theme.FloodGateHeading
import com.example.floodgate.ui.theme.FloodGateLink
import com.example.floodgate.ui.theme.FloodGateNeutral100
import com.example.floodgate.ui.theme.FloodGateNeutralDefault
import com.example.floodgate.ui.theme.FloodGateOnActionCaption
import com.example.floodgate.ui.theme.FloodGateSpacing
import com.example.floodgate.ui.theme.FloodGateSurfacePrimary
import com.example.floodgate.ui.theme.FloodGateTheme

@Composable
fun SignUpScreen(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    authError: String? = null,
    onClearAuthError: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onSignUpClick: (SignUpCredentials) -> Unit = {},
    onGoogleSignUpClick: () -> Unit = {},
    onSignInClick: () -> Unit = {}
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var fieldErrors by remember { mutableStateOf(SignUpFieldErrors()) }

    val submit: () -> Unit = {
        if (!isLoading) {
            val validation = AuthValidator.validateSignUp(
                firstName = firstName,
                lastName = lastName,
                email = email,
                password = password,
                confirmPassword = confirmPassword
            )
            fieldErrors = validation.errors
            validation.credentials?.let(onSignUpClick)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(FloodGateSurfacePrimary)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = FloodGateSpacing.Sm)
            ) {
                val contentWidth = minOf(maxWidth, SignUpDesign.ContentWidth)

                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .width(contentWidth)
                        .statusBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = SignUpDesign.ScreenBottomSpacing)
                ) {
                    Spacer(modifier = Modifier.height(SignUpDesign.BackTopSpacing))

                    Box(
                        modifier = Modifier
                            .size(SignUpDesign.BackTouchTarget)
                            .clip(RoundedCornerShape(SignUpDesign.BackTouchRadius))
                            .clickable(
                                enabled = !isLoading,
                                role = Role.Button,
                                onClick = onBackClick
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Image(
                            painter = painterResource(R.drawable.left_arrow_icon),
                            contentDescription = stringResource(R.string.navigate_back),
                            modifier = Modifier.size(AuthControlDimensions.IconSize)
                        )
                    }

                    Spacer(modifier = Modifier.height(SignUpDesign.BackToBrandSpacing))

                    FloodGateBrandName(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(FloodGateBrandDimensions.Width)
                            .height(FloodGateBrandDimensions.Height)
                    )

                    Spacer(modifier = Modifier.height(SignUpDesign.BrandToCardSpacing))

                    SignUpCard(
                        firstName = firstName,
                        onFirstNameChange = {
                            firstName = it
                            fieldErrors = fieldErrors.copy(firstName = null)
                            onClearAuthError()
                        },
                        lastName = lastName,
                        onLastNameChange = {
                            lastName = it
                            fieldErrors = fieldErrors.copy(lastName = null)
                            onClearAuthError()
                        },
                        email = email,
                        onEmailChange = {
                            email = it
                            fieldErrors = fieldErrors.copy(email = null)
                            onClearAuthError()
                        },
                        password = password,
                        onPasswordChange = {
                            password = it
                            fieldErrors = fieldErrors.copy(
                                password = AuthValidator.validateNewPassword(it)
                            )
                            onClearAuthError()
                        },
                        passwordVisible = passwordVisible,
                        onPasswordVisibilityChange = { passwordVisible = !passwordVisible },
                        confirmPassword = confirmPassword,
                        onConfirmPasswordChange = {
                            confirmPassword = it
                            fieldErrors = fieldErrors.copy(confirmPassword = null)
                            onClearAuthError()
                        },
                        confirmPasswordVisible = confirmPasswordVisible,
                        onConfirmPasswordVisibilityChange = {
                            confirmPasswordVisible = !confirmPasswordVisible
                        },
                        errors = fieldErrors,
                        isLoading = isLoading,
                        authError = authError,
                        onSignUpClick = submit,
                        onGoogleSignUpClick = onGoogleSignUpClick,
                        onSignInClick = onSignInClick
                    )
                }
            }
        }
    }
}

@Composable
private fun SignUpCard(
    firstName: String,
    onFirstNameChange: (String) -> Unit,
    lastName: String,
    onLastNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: () -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    confirmPasswordVisible: Boolean,
    onConfirmPasswordVisibilityChange: () -> Unit,
    errors: SignUpFieldErrors,
    isLoading: Boolean,
    authError: String?,
    onSignUpClick: () -> Unit,
    onGoogleSignUpClick: () -> Unit,
    onSignInClick: () -> Unit
) {
    val cardShape = MaterialTheme.shapes.extraLarge

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SignUpDesign.CardHeight)
            .shadow(SignUpDesign.CardElevation, cardShape)
            .clip(cardShape)
            .background(FloodGateNeutral100)
            .padding(horizontal = SignUpDesign.CardHorizontalPadding)
    ) {
        Spacer(modifier = Modifier.height(SignUpDesign.CardTopPadding))

        Text(
            text = stringResource(R.string.create_account),
            color = FloodGateHeading,
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(FloodGateSpacing.Xxs))

        Text(
            text = stringResource(R.string.create_account_caption),
            color = FloodGateOnActionCaption,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(SignUpDesign.HeaderToFieldsSpacing))

        AuthTextField(
            value = firstName,
            onValueChange = onFirstNameChange,
            placeholder = stringResource(R.string.first_name),
            enabled = !isLoading,
            errorMessage = errors.firstName?.let { stringResource(it.messageRes) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(FloodGateSpacing.Sm))

        AuthTextField(
            value = lastName,
            onValueChange = onLastNameChange,
            placeholder = stringResource(R.string.last_name),
            enabled = !isLoading,
            errorMessage = errors.lastName?.let { stringResource(it.messageRes) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(FloodGateSpacing.Sm))

        AuthTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = stringResource(R.string.email_address),
            enabled = !isLoading,
            errorMessage = errors.email?.let { stringResource(it.messageRes) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(FloodGateSpacing.Sm))

        AuthTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = stringResource(R.string.password),
            enabled = !isLoading,
            errorMessage = errors.password
                .map { stringResource(it.messageRes) }
                .takeIf { it.isNotEmpty() }
                ?.joinToString("\n"),
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            trailingIcon = if (passwordVisible) {
                R.drawable.show_password
            } else {
                R.drawable.hide_icon
            },
            trailingIconDescription = stringResource(
                if (passwordVisible) R.string.hide_password else R.string.show_password
            ),
            onTrailingIconClick = onPasswordVisibilityChange
        )

        Spacer(modifier = Modifier.height(FloodGateSpacing.Sm))

        AuthTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            placeholder = stringResource(R.string.confirm_password),
            enabled = !isLoading,
            errorMessage = errors.confirmPassword?.let { stringResource(it.messageRes) },
            visualTransformation = if (confirmPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onSignUpClick() }),
            trailingIcon = if (confirmPasswordVisible) {
                R.drawable.show_password
            } else {
                R.drawable.hide_icon
            },
            trailingIconDescription = stringResource(
                if (confirmPasswordVisible) R.string.hide_password else R.string.show_password
            ),
            onTrailingIconClick = onConfirmPasswordVisibilityChange
        )

        if (authError != null) {
            Spacer(modifier = Modifier.height(FloodGateSpacing.Xs))
            Text(
                text = authError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(SignUpDesign.FieldsToButtonSpacing))

        AuthButton(
            label = stringResource(R.string.sign_up_title_case),
            filled = true,
            onClick = onSignUpClick,
            enabled = !isLoading,
            loading = isLoading
        )

        Spacer(modifier = Modifier.height(FloodGateSpacing.Sm))

        OrDivider()

        Spacer(modifier = Modifier.height(FloodGateSpacing.Sm))

        AuthButton(
            label = stringResource(R.string.continue_with_google),
            filled = false,
            leadingIcon = R.drawable.google_icon,
            onClick = onGoogleSignUpClick,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(FloodGateSpacing.Sm))

        AuthAccountPrompt(
            prompt = R.string.already_have_account,
            action = R.string.sign_in_link,
            onActionClick = onSignInClick,
            enabled = !isLoading,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(FloodGateSpacing.Sm))
    }
}

private object SignUpDesign {
    val ContentWidth = 380.dp
    val BackTopSpacing = 8.dp
    val BackTouchTarget = 40.dp
    val BackTouchRadius = 20.dp
    val BackToBrandSpacing = 6.dp
    val BrandToCardSpacing = 13.dp
    val ScreenBottomSpacing = 60.dp
    val CardHeight = 658.dp
    val CardElevation = 8.dp
    val CardHorizontalPadding = 24.dp
    val CardTopPadding = 24.dp
    val HeaderToFieldsSpacing = 24.dp
    val FieldsToButtonSpacing = 24.dp
}

@Preview(
    name = "Sign Up",
    widthDp = 412,
    heightDp = 917,
    showBackground = true
)
@Preview(
    name = "Sign Up — small phone",
    widthDp = 320,
    heightDp = 640,
    showBackground = true
)
@Composable
private fun SignUpScreenPreview() {
    FloodGateTheme {
        SignUpScreen()
    }
}
