package com.example.floodgate.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.floodgate.R
import com.example.floodgate.ui.theme.FloodGateHeading
import com.example.floodgate.ui.theme.FloodGateNeutralDefault
import com.example.floodgate.ui.theme.FloodGateSpacing
import com.example.floodgate.ui.theme.FloodGateSurfacePrimary
import com.example.floodgate.ui.theme.FloodGateTheme

@Composable
fun AuthSuccessScreen(
    email: String,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(FloodGateSurfacePrimary)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = FloodGateSpacing.Sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.login_successful),
                color = FloodGateHeading,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            if (email.isNotBlank()) {
                Spacer(modifier = Modifier.padding(top = FloodGateSpacing.Xs))
                Text(
                    text = email,
                    color = FloodGateNeutralDefault,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.padding(top = 32.dp))
            AuthButton(
                label = stringResource(R.string.sign_out),
                filled = true,
                onClick = onSignOutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp)
            )
        }
    }
}

@Preview(widthDp = 412, heightDp = 917, showBackground = true)
@Composable
private fun AuthSuccessScreenPreview() {
    FloodGateTheme {
        AuthSuccessScreen(email = "user@gmail.com", onSignOutClick = {})
    }
}
