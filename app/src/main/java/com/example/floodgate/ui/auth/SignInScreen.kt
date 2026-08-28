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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.floodgate.R
import com.example.floodgate.ui.theme.FloodGateAction
import com.example.floodgate.ui.theme.FloodGateHeading
import com.example.floodgate.ui.theme.FloodGateLink
import com.example.floodgate.ui.theme.FloodGateNeutralDefault
import com.example.floodgate.ui.theme.FloodGateOnActionCaption
import com.example.floodgate.ui.theme.FloodGateSpacing
import com.example.floodgate.ui.theme.FloodGateSurfacePrimary
import com.example.floodgate.ui.theme.FloodGateTextPrimary
import com.example.floodgate.ui.theme.FloodGateTheme

@Composable
fun SignInScreen(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    authError: String? = null,
    noticeMessage: String? = null,
    onClearAuthError: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onSignInClick: (SignInCredentials) -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    onGoogleSignInClick: () -> Unit = {},
    onSignUpClick: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var fieldErrors by remember { mutableStateOf(SignInFieldErrors()) }

    val submit: () -> Unit = {
        if (!isLoading) {
            val validation = AuthValidator.validateSignIn(email = email, password = password)
            fieldErrors = validation.errors
            validation.credentials?.let(onSignInClick)
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
                val contentWidth = minOf(maxWidth, SignInDesign.ContentWidth)

                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .width(contentWidth)
                        .fillMaxHeight()
                        .statusBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = SignInDesign.ScrollBottomPadding)
                ) {
                    Spacer(modifier = Modifier.height(SignInDesign.BackTopSpacing))

                    Box(
                        modifier = Modifier
                            .size(SignInDesign.BackTouchTarget)
                            .clip(RoundedCornerShape(SignInDesign.BackTouchRadius))
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
                            modifier = Modifier.size(SignInDesign.IconSize)
                        )
                    }

                    Spacer(modifier = Modifier.height(SignInDesign.BackToHeadingSpacing))

                    Text(
                        text = stringResource(R.string.sign_in_heading),
                        color = FloodGateHeading,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(SignInDesign.HeadingToCaptionSpacing))

                    Text(
                        text = stringResource(R.string.sign_in_caption),
                        color = FloodGateOnActionCaption,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(SignInDesign.CaptionToFieldsSpacing))

                    AuthTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            fieldErrors = fieldErrors.copy(email = null)
                            onClearAuthError()
                        },
                        placeholder = stringResource(R.string.email_address),
                        enabled = !isLoading,
                        errorMessage = fieldErrors.email?.let { stringResource(it.messageRes) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(FloodGateSpacing.Sm))

                    AuthTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            fieldErrors = fieldErrors.copy(password = null)
                            onClearAuthError()
                        },
                        placeholder = stringResource(R.string.password),
                        enabled = !isLoading,
                        errorMessage = fieldErrors.password?.let { stringResource(it.messageRes) },
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { submit() }
                        ),
                        trailingIcon = if (passwordVisible) {
                            R.drawable.show_password
                        } else {
                            R.drawable.hide_icon
                        },
                        trailingIconDescription = stringResource(
                            if (passwordVisible) R.string.hide_password else R.string.show_password
                        ),
                        onTrailingIconClick = { passwordVisible = !passwordVisible }
                    )

                    if (authError != null) {
                        Spacer(modifier = Modifier.height(FloodGateSpacing.Xs))
                        Text(
                            text = authError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(FloodGateSpacing.Sm))

                    Text(
                        text = stringResource(R.string.forgot_password),
                        modifier = Modifier
                            .align(Alignment.End)
                            .height(SignInDesign.BodyLineHeight)
                            .clickable(
                                enabled = !isLoading,
                                role = Role.Button,
                                onClick = onForgotPasswordClick
                            ),
                        color = FloodGateLink,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.End
                    )

                    Spacer(modifier = Modifier.height(SignInDesign.LinkToButtonSpacing))

                    AuthButton(
                        label = stringResource(R.string.sign_in),
                        filled = true,
                        onClick = submit,
                        enabled = !isLoading,
                        loading = isLoading
                    )

                    Spacer(modifier = Modifier.height(SignInDesign.ButtonToDividerSpacing))

                    OrDivider()

                    Spacer(modifier = Modifier.height(SignInDesign.DividerToGoogleSpacing))

                    AuthButton(
                        label = stringResource(R.string.continue_with_google),
                        filled = false,
                        leadingIcon = R.drawable.google_icon,
                        onClick = onGoogleSignInClick,
                        enabled = !isLoading
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = SignInDesign.AccountPromptBottomSpacing),
                    horizontalArrangement = Arrangement.spacedBy(FloodGateSpacing.Xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.no_account),
                        color = FloodGateNeutralDefault,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.sign_up),
                        modifier = Modifier.clickable(
                            enabled = !isLoading,
                            role = Role.Button,
                            onClick = onSignUpClick
                        ),
                        color = FloodGateLink,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (noticeMessage != null) {
                    Text(
                        text = noticeMessage,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = SignInDesign.NoticeBottomSpacing)
                            .width(contentWidth)
                            .clip(MaterialTheme.shapes.medium)
                            .background(FloodGateAction)
                            .padding(FloodGateSpacing.Sm),
                        color = FloodGateTextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private object SignInDesign {
    val ContentWidth = 380.dp
    val BackTopSpacing = 8.dp
    val BackTouchTarget = 40.dp
    val BackTouchRadius = 20.dp
    val BackToHeadingSpacing = 57.dp
    val HeadingToCaptionSpacing = 6.dp
    val CaptionToFieldsSpacing = 55.dp
    val LinkToButtonSpacing = 40.dp
    val ButtonToDividerSpacing = 32.dp
    val DividerToGoogleSpacing = 32.dp
    val AccountPromptBottomSpacing = 26.dp
    val NoticeBottomSpacing = 72.dp
    val ScrollBottomPadding = 120.dp
    val BodyLineHeight = 20.dp
    val IconSize = 20.dp
}

@Preview(
    name = "Sign In",
    widthDp = 412,
    heightDp = 917,
    showBackground = true
)
@Composable
private fun SignInScreenPreview() {
    FloodGateTheme {
        SignInScreen()
    }
}
