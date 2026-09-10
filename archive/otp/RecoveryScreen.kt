// Archived design reference; outside Android source sets and not included in the app.
package com.example.floodgate.ui.auth

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
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
    val back: () -> Unit = {
        if (!model.busy) {
            if (model.step == RecoveryStep.CODE) model.backToEmail()
            else { model.reset(); onClose() }
        }
    }
    BackHandler(onBack = back)
    var resendSeconds by remember { mutableStateOf(0) }
    LaunchedEffect(model.resendAt) {
        do {
            resendSeconds = ((model.resendAt - SystemClock.elapsedRealtime() + 999) / 1000)
                .coerceAtLeast(0).toInt()
            if (resendSeconds > 0) delay(1000)
        } while (resendSeconds > 0)
    }
    val focus = LocalFocusManager.current
    RecoveryScreen(
        step = model.step, email = model.email, code = model.code, busy = model.busy,
        errorMessage = model.errorRes?.let { stringResource(it) }, emailError = model.emailError,
        resendSeconds = resendSeconds, onEmailChange = model::editEmail, onCodeChange = model::editCode,
        onSend = { focus.clearFocus(); model.sendCode() },
        onVerify = { focus.clearFocus(); model.verifyCode() }, onBack = back
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RecoveryScreen(
    step: RecoveryStep,
    email: String = "",
    code: String = "",
    busy: Boolean = false,
    errorMessage: String? = null,
    emailError: Boolean = false,
    resendSeconds: Int = 0,
    onEmailChange: (String) -> Unit = {},
    onCodeChange: (String) -> Unit = {},
    onSend: () -> Unit = {},
    onVerify: () -> Unit = {},
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
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = screenHeight)
                    .padding(
                        top = when (step) {
                            RecoveryStep.EMAIL -> RecoveryDesign.EmailTop
                            RecoveryStep.CODE -> RecoveryDesign.CenterClearance
                            RecoveryStep.VERIFIED -> maxOf(RecoveryDesign.CenterClearance, screenHeight * 0.29f)
                        },
                        bottom = RecoveryDesign.CenterClearance
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (step == RecoveryStep.CODE) Arrangement.Center else Arrangement.Top
            ) {
                when (step) {
                    RecoveryStep.EMAIL -> {
                        RecoveryHeader(
                            stringResource(R.string.recovery_email_heading),
                            stringResource(R.string.recovery_email_caption), centered = false,
                            modifier = Modifier.align(Alignment.Start)
                        )
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
                        if (!emailError) RecoveryErrorText(errorMessage)
                        Spacer(Modifier.height(RecoveryDesign.SectionGap))
                        AuthButton(stringResource(R.string.recovery_send), true, onSend, loading = busy)
                    }
                    RecoveryStep.CODE -> {
                        RecoveryIcon(R.drawable.recovery_email, RecoveryDesign.EmailCircle)
                        Spacer(Modifier.height(RecoveryDesign.SectionGap))
                        RecoveryHeader(stringResource(R.string.recovery_verify_heading),
                            stringResource(R.string.recovery_verify_caption, maskedRecoveryEmail(email)))
                        Spacer(Modifier.height(RecoveryDesign.SectionGap))
                        RecoveryCodeField(code, onCodeChange, !busy, errorMessage, onVerify)
                        RecoveryErrorText(errorMessage)
                        Spacer(Modifier.height(RecoveryDesign.SectionGap))
                        AuthButton(stringResource(R.string.recovery_verify), true, onVerify, loading = busy)
                        Spacer(Modifier.height(RecoveryDesign.SectionGap))
                        // Wrap at large accessibility font sizes instead of clipping the link.
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
                            Text(stringResource(R.string.recovery_resend_prompt), color = FloodGateNeutral400,
                                style = MaterialTheme.typography.bodyMedium)
                            Text(
                                if (resendSeconds > 0) stringResource(R.string.recovery_resend_countdown, resendSeconds)
                                else stringResource(R.string.recovery_resend),
                                Modifier.clickable(enabled = !busy && resendSeconds == 0, role = Role.Button, onClick = onSend),
                                color = if (resendSeconds > 0) FloodGateNeutral400 else RecoveryDesign.Link,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    RecoveryStep.VERIFIED -> {
                        RecoveryIcon(R.drawable.recovery_check, RecoveryDesign.VerifiedCircle)
                        Spacer(Modifier.height(RecoveryDesign.SectionGap))
                        RecoveryHeader(stringResource(R.string.recovery_verified_heading),
                            stringResource(R.string.recovery_verified_caption))
                    }
                }
            }
            Box(
                Modifier.align(Alignment.TopStart).padding(top = RecoveryDesign.BackTop)
                    .size(AuthControlDimensions.TouchTargetSize).clip(CircleShape)
                    .clickable(enabled = !busy, role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.CenterStart
            ) {
                Image(painterResource(R.drawable.left_arrow_icon), stringResource(R.string.navigate_back),
                    Modifier.size(AuthControlDimensions.IconSize))
            }
        }
    }
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
private fun RecoveryIcon(drawable: Int, background: Color) {
    Box(Modifier.size(RecoveryDesign.CircleSize).background(background, CircleShape), contentAlignment = Alignment.Center) {
        Image(painterResource(drawable), null, Modifier.size(RecoveryDesign.IconSize))
    }
}

@Composable
private fun RecoveryErrorText(message: String?) {
    if (message != null) Text(message,
        Modifier.fillMaxWidth().padding(top = FloodGateSpacing.Xs).semantics { liveRegion = LiveRegionMode.Polite },
        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun RecoveryCodeField(value: String, onChange: (String) -> Unit, enabled: Boolean, message: String?, onDone: () -> Unit) {
    val label = stringResource(R.string.recovery_code_label)
    // A single editable field supports pasting all six digits, deletion, and a coherent TalkBack label.
    BasicTextField(
        value, onChange, enabled = enabled, singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("recovery_code")
            .semantics { contentDescription = label; if (message != null) error(message) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        decorationBox = { innerTextField ->
            Box {
                Box(Modifier.size(1.dp).alpha(0f)) { innerTextField() }
                Row(horizontalArrangement = Arrangement.spacedBy(RecoveryDesign.CodeGap)) {
                    repeat(6) { index ->
                        Box(
                            Modifier.weight(1f).height(RecoveryDesign.CodeHeight)
                                .border(1.dp, when {
                                    message != null -> MaterialTheme.colorScheme.error
                                    else -> FloodGateBorderAction
                                }, RoundedCornerShape(RecoveryDesign.CodeRadius)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(value.getOrNull(index)?.toString().orEmpty(), Modifier.clearAndSetSemantics {},
                                color = FloodGateHeading, style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }
        }
    )
}

internal fun maskedRecoveryEmail(email: String): String {
    val local = email.substringBefore('@')
    if (local.isEmpty() || '@' !in email) return email
    val visibleEnd = if (local.length > 3) local.takeLast(2) else ""
    return "${local.first()}*****$visibleEnd@${email.substringAfter('@')}"
}

// Figma recovery nodes 100:481, 89:141 and 91:269; existing auth tokens are reused elsewhere.
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
    val CodeHeight = 60.dp
    val CodeGap = 16.dp
    val CodeRadius = 4.dp
    val Link = Color(0xFF000AFF)
    val EmailCircle = Color(0xFFE5E7FF)
    val VerifiedCircle = Color(0xFFE7F6E5)
}

@Preview(name = "Recovery email", widthDp = 412, heightDp = 917)
@Composable private fun RecoveryEmailPreview() { FloodGateTheme { RecoveryScreen(RecoveryStep.EMAIL) } }
@Preview(name = "Recovery code", widthDp = 412, heightDp = 917)
@Preview(name = "Recovery code small phone", widthDp = 320, heightDp = 640)
@Composable private fun RecoveryCodePreview() { FloodGateTheme { RecoveryScreen(RecoveryStep.CODE, "hawwy@example.com") } }
@Preview(name = "Email verified", widthDp = 412, heightDp = 917)
@Composable private fun RecoveryVerifiedPreview() { FloodGateTheme { RecoveryScreen(RecoveryStep.VERIFIED) } }
