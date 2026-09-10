package com.example.floodgate.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.floodgate.R
import com.example.floodgate.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun RecoveryFlow(model: RecoveryViewModel, onClose: () -> Unit) {
    val close: () -> Unit = { model.reset(); onClose() }
    BackHandler(onBack = close)
    var resendSeconds by remember(model.resendAt) { mutableStateOf(model.remainingResendSeconds()) }
    LaunchedEffect(model.resendAt) {
        while (true) {
            resendSeconds = model.remainingResendSeconds()
            if (resendSeconds == 0) break
            delay(1000)
        }
    }
    val focus = LocalFocusManager.current
    RecoveryScreen(
        step = model.step, email = model.email, busy = model.busy,
        errorMessage = model.errorRes?.let { stringResource(it) }, emailError = model.emailError,
        noticeMessage = model.noticeRes?.let { stringResource(it) }, resendSeconds = resendSeconds,
        onEmailChange = model::editEmail,
        onSend = { focus.clearFocus(); model.sendResetLink() }, onBack = close
    )
}

@Composable
internal fun RecoveryScreen(
    step: RecoveryStep,
    email: String = "",
    busy: Boolean = false,
    errorMessage: String? = null,
    emailError: Boolean = false,
    noticeMessage: String? = null,
    resendSeconds: Int = 0,
    onEmailChange: (String) -> Unit = {},
    onSend: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        BoxWithConstraints(
            Modifier.fillMaxSize().clip(MaterialTheme.shapes.extraLarge)
                .background(FloodGateSurfacePrimary).statusBarsPadding()
                .navigationBarsPadding().imePadding().padding(horizontal = FloodGateSpacing.Sm)
        ) {
            val contentWidth = minOf(maxWidth, RecoveryDesign.ContentWidth)
            val screenHeight = maxHeight
            Column(
                Modifier.align(Alignment.TopCenter).width(contentWidth)
                    .verticalScroll(rememberScrollState()).heightIn(min = screenHeight)
                    .padding(top = if (step == RecoveryStep.EMAIL) RecoveryDesign.EmailTop else RecoveryDesign.CenterClearance,
                        bottom = RecoveryDesign.CenterClearance),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (step == RecoveryStep.CHECK_EMAIL) Arrangement.Center else Arrangement.Top
            ) {
                when (step) {
                    RecoveryStep.EMAIL -> ForgotPasswordContent(email, onEmailChange, busy,
                        errorMessage, emailError, onSend)
                    RecoveryStep.CHECK_EMAIL -> CheckEmailContent(email, busy, errorMessage,
                        noticeMessage, resendSeconds, onSend, onBack)
                }
            }
            Box(
                Modifier.align(Alignment.TopStart).padding(top = RecoveryDesign.BackTop)
                    .size(AuthControlDimensions.TouchTargetSize).clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.CenterStart
            ) {
                Image(painterResource(R.drawable.left_arrow_icon), stringResource(R.string.navigate_back),
                    Modifier.size(AuthControlDimensions.IconSize))
            }
        }
    }
}

@Composable
private fun ColumnScope.ForgotPasswordContent(
    email: String, onEmailChange: (String) -> Unit, busy: Boolean,
    errorMessage: String?, emailError: Boolean, onSend: () -> Unit
) {
    RecoveryHeader(stringResource(R.string.reset_heading), stringResource(R.string.reset_caption),
        centered = false, modifier = Modifier.align(Alignment.Start))
    Spacer(Modifier.height(RecoveryDesign.SectionGap))
    Text(stringResource(R.string.recovery_email_label), Modifier.align(Alignment.Start),
        color = FloodGateNeutral400, style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(RecoveryDesign.CaptionGap))
    AuthTextField(
        value = email, onValueChange = onEmailChange,
        placeholder = stringResource(R.string.email_address), enabled = !busy,
        errorMessage = if (emailError) errorMessage else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSend() })
    )
    if (!emailError) RecoveryMessage(errorMessage, isError = true)
    Spacer(Modifier.height(RecoveryDesign.SectionGap))
    AuthButton(stringResource(R.string.reset_send), true, onSend,
        modifier = Modifier.testTag("reset_send"), loading = busy)
}

@Composable
private fun CheckEmailContent(
    email: String, busy: Boolean, errorMessage: String?, noticeMessage: String?,
    resendSeconds: Int, onResend: () -> Unit, onBack: () -> Unit
) {
    Box(Modifier.size(RecoveryDesign.CircleSize).background(RecoveryDesign.EmailCircle, CircleShape),
        contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.recovery_email), null, Modifier.size(RecoveryDesign.IconSize))
    }
    Spacer(Modifier.height(RecoveryDesign.SectionGap))
    RecoveryHeader(stringResource(R.string.reset_check_heading),
        stringResource(R.string.reset_check_caption, maskedRecoveryEmail(email)))
    Spacer(Modifier.height(RecoveryDesign.SectionGap))
    AuthButton(stringResource(R.string.reset_back_to_sign_in), true, onBack)
    Spacer(Modifier.height(RecoveryDesign.CaptionGap))
    Box(
        Modifier.fillMaxWidth().heightIn(min = AuthControlDimensions.TouchTargetSize)
            .clip(MaterialTheme.shapes.medium).testTag("reset_resend")
            .clickable(enabled = !busy && resendSeconds == 0, role = Role.Button, onClick = onResend),
        contentAlignment = Alignment.Center
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(AuthControlDimensions.IconSize),
                color = FloodGateAction, strokeWidth = AuthControlDimensions.ProgressStrokeWidth)
        } else {
            Text(
                if (resendSeconds > 0) stringResource(R.string.reset_resend_countdown,
                    resendSeconds / 60, resendSeconds % 60) else stringResource(R.string.reset_resend),
                color = if (resendSeconds > 0) FloodGateNeutral400 else FloodGateLink,
                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center
            )
        }
    }
    RecoveryMessage(errorMessage, isError = true)
    RecoveryMessage(noticeMessage, isError = false)
}

@Composable
private fun RecoveryHeader(title: String, caption: String, centered: Boolean = true, modifier: Modifier = Modifier) {
    Column(modifier.widthIn(max = RecoveryDesign.HeaderWidth),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
        val alignment = if (centered) TextAlign.Center else TextAlign.Start
        Text(title, color = FloodGateHeading, style = MaterialTheme.typography.headlineSmall, textAlign = alignment)
        Spacer(Modifier.height(RecoveryDesign.CaptionGap))
        Text(caption, color = FloodGateNeutral400, style = MaterialTheme.typography.bodyMedium, textAlign = alignment)
    }
}

@Composable
private fun RecoveryMessage(message: String?, isError: Boolean) {
    if (message != null) Text(message,
        Modifier.fillMaxWidth().padding(top = FloodGateSpacing.Xs).semantics { liveRegion = LiveRegionMode.Polite },
        color = if (isError) MaterialTheme.colorScheme.error else FloodGateAction,
        style = MaterialTheme.typography.bodySmall)
}

/** Fixed masking length avoids revealing the full local-part length. */
internal fun maskedRecoveryEmail(email: String): String {
    val local = email.substringBefore('@')
    if (local.isEmpty() || '@' !in email) return "******"
    return "${local.first()}******@${email.substringAfter('@')}"
}

// Preserve the existing recovery layout, icon and shared auth styling.
private object RecoveryDesign {
    val ContentWidth = 380.dp
    val HeaderWidth = 316.dp
    val SectionGap = 24.dp
    val CaptionGap = 8.dp
    val BackTop = 24.dp
    val EmailTop = 82.dp
    val CenterClearance = 88.dp
    val CircleSize = 62.dp
    val IconSize = 30.dp
    val EmailCircle = Color(0xFFE5E7FF)
}

@Preview(name = "Forgot Password", widthDp = 412, heightDp = 917)
@Composable private fun ForgotPasswordPreview() { FloodGateTheme { RecoveryScreen(RecoveryStep.EMAIL) } }
@Preview(name = "Check Your Email", widthDp = 412, heightDp = 917)
@Preview(name = "Check Your Email small phone", widthDp = 320, heightDp = 640)
@Composable private fun CheckEmailPreview() { FloodGateTheme { RecoveryScreen(RecoveryStep.CHECK_EMAIL, "harrison@gmail.com") } }
