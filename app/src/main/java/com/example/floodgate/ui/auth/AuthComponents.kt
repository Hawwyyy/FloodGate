package com.example.floodgate.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.floodgate.R
import com.example.floodgate.ui.theme.FloodGateAction
import com.example.floodgate.ui.theme.FloodGateBorderAction
import com.example.floodgate.ui.theme.FloodGateDivider
import com.example.floodgate.ui.theme.FloodGateHeading
import com.example.floodgate.ui.theme.FloodGateNeutral400
import com.example.floodgate.ui.theme.FloodGateSpacing
import com.example.floodgate.ui.theme.FloodGateSurfacePrimary
import com.example.floodgate.ui.theme.FloodGateTextPrimary

@Composable
internal fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
    errorMessage: String? = null,
    trailingIcon: Int? = null,
    trailingIconDescription: String? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    defaultBorderColor: Color = FloodGateBorderAction
) {
    val shape = MaterialTheme.shapes.medium
    val borderColor = if (errorMessage != null) {
        MaterialTheme.colorScheme.error
    } else {
        defaultBorderColor
    }

    Column(modifier = modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(AuthControlDimensions.FieldHeight)
                .clip(shape)
                .background(FloodGateSurfacePrimary)
                .border(AuthControlDimensions.BorderWidth, borderColor, shape)
                .semantics {
                    if (errorMessage != null) error(errorMessage)
                },
            enabled = enabled,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = FloodGateHeading),
            singleLine = true,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            cursorBrush = SolidColor(FloodGateAction),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = FloodGateSpacing.Sm,
                            end = if (trailingIcon != null) {
                                AuthControlDimensions.TrailingTouchEndPadding
                            } else {
                                FloodGateSpacing.Sm
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = FloodGateNeutral400,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        innerTextField()
                    }

                    if (trailingIcon != null) {
                        Spacer(modifier = Modifier.width(FloodGateSpacing.Xs))
                        Box(
                            modifier = Modifier
                                .size(AuthControlDimensions.TouchTargetSize)
                                .then(
                                    if (onTrailingIconClick != null) {
                                        Modifier.clickable(
                                            enabled = enabled,
                                            role = Role.Button,
                                            onClick = onTrailingIconClick
                                        )
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(trailingIcon),
                                contentDescription = trailingIconDescription,
                                modifier = Modifier.size(AuthControlDimensions.IconSize)
                            )
                        }
                    }
                }
            }
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                modifier = Modifier.padding(top = FloodGateSpacing.Xxs),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
internal fun AuthButton(
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: Int? = null,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val shape = MaterialTheme.shapes.medium
    val interactionEnabled = enabled && !loading
    val buttonDecoration = if (filled) {
        Modifier.background(FloodGateAction)
    } else {
        Modifier
            .background(FloodGateSurfacePrimary)
            .border(AuthControlDimensions.BorderWidth, FloodGateBorderAction, shape)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AuthControlDimensions.ButtonHeight)
            .clip(shape)
            .then(buttonDecoration)
            .alpha(if (interactionEnabled) 1f else AuthControlDimensions.DisabledAlpha)
            .clickable(
                enabled = interactionEnabled,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(AuthControlDimensions.IconSize),
                color = if (filled) FloodGateTextPrimary else FloodGateAction,
                strokeWidth = AuthControlDimensions.ProgressStrokeWidth
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(FloodGateSpacing.Xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    Image(
                        painter = painterResource(leadingIcon),
                        contentDescription = null,
                        modifier = Modifier.size(AuthControlDimensions.IconSize)
                    )
                }
                Text(
                    text = label,
                    color = if (filled) FloodGateTextPrimary else FloodGateHeading,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
internal fun OrDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AuthControlDimensions.DividerHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = Modifier
                .weight(1f)
                .height(AuthControlDimensions.BorderWidth)
                .background(FloodGateDivider)
        )
        Text(
            text = stringResource(R.string.or),
            modifier = Modifier.padding(horizontal = AuthControlDimensions.DividerLabelPadding),
            color = FloodGateNeutral400,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(
            modifier = Modifier
                .weight(1f)
                .height(AuthControlDimensions.BorderWidth)
                .background(FloodGateDivider)
        )
    }
}

internal object AuthControlDimensions {
    val FieldHeight = 54.dp
    val ButtonHeight = 52.dp
    val DividerHeight = 16.dp
    val IconSize = 20.dp
    val TouchTargetSize = 48.dp
    val TrailingTouchEndPadding = 2.dp
    val DividerLabelPadding = 9.dp
    val BorderWidth = 1.dp
    val ProgressStrokeWidth = 2.dp
    const val DisabledAlpha = 0.65f
}
