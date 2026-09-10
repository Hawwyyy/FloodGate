package com.example.floodgate.ui.dashboard

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.floodgate.R
import com.example.floodgate.ui.auth.AuthButton
import com.example.floodgate.ui.theme.*

private enum class DashboardDialog { LOCATION, ACTIVITY, CONTROL, DEVICES, NOTIFICATIONS, PROFILE }

/** The design's readings are explicitly sample data until a device repository is connected. */
@Composable
fun DashboardScreen(email: String, onSignOutClick: () -> Unit) {
    // Android's legacy font padding adds space absent from Figma line boxes.
    val typography = MaterialTheme.typography
    val noFontPadding = PlatformTextStyle(includeFontPadding = false)
    MaterialTheme(typography = typography.copy(
        headlineSmall = typography.headlineSmall.copy(platformStyle = noFontPadding),
        bodyLarge = typography.bodyLarge.copy(platformStyle = noFontPadding),
        bodyMedium = typography.bodyMedium.copy(platformStyle = noFontPadding),
        bodySmall = typography.bodySmall.copy(platformStyle = noFontPadding)
    )) { DashboardContent(email, onSignOutClick) }
}

@Composable
private fun DashboardContent(email: String, onSignOutClick: () -> Unit) {
    var dialog by rememberSaveable { mutableStateOf<DashboardDialog?>(null) }
    Column(
        Modifier.fillMaxSize().background(FloodGateSurfacePrimary)
            .statusBarsPadding().navigationBarsPadding().testTag("dashboard")
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = DashboardTokens.PagePadding)
                .padding(top = DashboardTokens.HeaderTopPadding, bottom = DashboardTokens.SectionGap)
        ) {
            DashboardHeader()
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().heightIn(min = DashboardTokens.TouchTarget)
                    .clickable(role = Role.Button) { dialog = DashboardDialog.LOCATION },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DashboardTokens.SmallGap)
            ) {
                Box(Modifier.size(DashboardTokens.Icon), contentAlignment = Alignment.Center) {
                    DashboardIcon(R.drawable.dashboard_location, Modifier.size(15.dp, 18.333.dp))
                }
                Text(stringResource(R.string.dashboard_location), style = MaterialTheme.typography.bodyMedium,
                    color = DashboardTokens.Body, modifier = Modifier.weight(1f, fill = false))
                DashboardIcon(R.drawable.dashboard_arrow_down)
            }
            Caption(R.string.dashboard_demo_notice, Modifier.padding(bottom = DashboardTokens.SmallGap))
            BarrierCard()
            Spacer(Modifier.height(DashboardTokens.SectionGap))
            // Stack metrics on narrow screens or with larger accessibility text instead of clipping.
            BoxWithConstraints {
                if (maxWidth < 350.dp || LocalDensity.current.fontScale > 1.15f) {
                    Column(verticalArrangement = Arrangement.spacedBy(DashboardTokens.SectionGap)) {
                        WaterLevelCard(Modifier.fillMaxWidth())
                        SystemStatusCard(Modifier.fillMaxWidth())
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        WaterLevelCard(Modifier.weight(190f))
                        SystemStatusCard(Modifier.weight(172f))
                    }
                }
            }
            Spacer(Modifier.height(DashboardTokens.SectionGap))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SectionTitle(R.string.dashboard_recent_activity, Modifier.weight(1f))
                Box(Modifier.heightIn(min = DashboardTokens.TouchTarget)
                    .clickable(role = Role.Button) { dialog = DashboardDialog.ACTIVITY },
                    contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.dashboard_see_all), color = DashboardTokens.Information,
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
            ActivityCard { dialog = DashboardDialog.ACTIVITY }
            Spacer(Modifier.height(DashboardTokens.SectionGap))
            SectionTitle(R.string.dashboard_quick_controls)
            Spacer(Modifier.height(DashboardTokens.SectionGap))
            Row(horizontalArrangement = Arrangement.spacedBy(DashboardTokens.SectionGap)) {
                AuthButton(stringResource(R.string.dashboard_deploy), true,
                    { dialog = DashboardDialog.CONTROL }, Modifier.weight(1f),
                    leadingIcon = R.drawable.dashboard_deploy)
                AuthButton(stringResource(R.string.dashboard_retract), true,
                    { dialog = DashboardDialog.CONTROL }, Modifier.weight(1f),
                    leadingIcon = R.drawable.dashboard_retract)
            }
        }
        DashboardNavigation { dialog = it }
    }
    dialog?.let { current ->
        DashboardInfoDialog(current, email, onDismiss = { dialog = null }, onSignOut = {
            dialog = null
            onSignOutClick()
        })
    }
}

@Composable
private fun DashboardHeader() {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DashboardTokens.SmallGap)) {
        // Preserve the logo's Figma crop, including its transparent source margins.
        Box(Modifier.size(36.dp, 30.dp).clipToBounds()) {
            Image(painterResource(R.drawable.dashboard_logo), null,
                Modifier.requiredSize(48.dp, 33.87.dp), contentScale = ContentScale.FillBounds)
        }
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall,
            color = FloodGateHeading)
    }
}

@Composable
internal fun BarrierCard(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(DashboardTokens.CardRadius))
        .background(DashboardTokens.Card)) {
        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(380f / 206f).clipToBounds()) {
            // Figma crops the top of a larger image, not the center of the floodwater.
            val imageWidth = maxWidth * (441f / 380f)
            Image(painterResource(R.drawable.dashboard_barrier), stringResource(R.string.dashboard_photo_description),
                Modifier.align(Alignment.TopCenter).wrapContentSize(Alignment.TopCenter, unbounded = true)
                    .requiredSize(imageWidth, imageWidth * (1150f / 1367f)),
                contentScale = ContentScale.FillBounds)
        }
        Row(Modifier.fillMaxWidth().padding(DashboardTokens.PagePadding).heightIn(min = 71.dp).height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically) {
            BarrierStatus(R.string.dashboard_barrier_status, R.string.dashboard_deployed,
                R.string.dashboard_shield_active, DashboardTokens.Information, Modifier.weight(1f))
            Spacer(Modifier.width(1.dp).fillMaxHeight().background(FloodGateDivider))
            BarrierStatus(R.string.dashboard_flood_status, R.string.dashboard_critical,
                R.string.dashboard_flood_risk, DashboardTokens.Error,
                Modifier.weight(1f).padding(start = if (LocalDensity.current.fontScale <= 1.15f) 46.dp else 16.dp))
        }
    }
}

@Composable
private fun BarrierStatus(@StringRes title: Int, @StringRes status: Int, @StringRes caption: Int,
                          color: Color, modifier: Modifier) {
    Column(modifier) {
        Text(stringResource(title), style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium, color = DashboardTokens.Body)
        Spacer(Modifier.height(DashboardTokens.SmallGap))
        Text(stringResource(status), style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold, color = color)
        Spacer(Modifier.height(DashboardTokens.CaptionGap))
        Caption(caption)
    }
}

@Composable
private fun MetricCard(modifier: Modifier, @DrawableRes icon: Int, @StringRes title: Int,
                       content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.heightIn(min = 115.dp).border(1.dp, DashboardTokens.SecondaryBorder,
        RoundedCornerShape(DashboardTokens.CardRadius)).padding(
        horizontal = DashboardTokens.PagePadding, vertical = DashboardTokens.CardVerticalPadding),
        verticalArrangement = Arrangement.Center) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DashboardTokens.SmallGap)) {
            DashboardIcon(icon)
            Text(stringResource(title), style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium, color = FloodGateNeutralDefault)
        }
        Spacer(Modifier.height(DashboardTokens.SectionGap))
        content()
    }
}

@Composable
internal fun WaterLevelCard(modifier: Modifier) {
    MetricCard(modifier, R.drawable.dashboard_water, R.string.dashboard_water_level) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.dashboard_water_reading), style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium, color = FloodGateHeading)
            Row(Modifier.clip(CircleShape).background(DashboardTokens.ErrorSurface)
                .padding(horizontal = DashboardTokens.SmallGap, vertical = DashboardTokens.CaptionGap),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DashboardTokens.SmallGap)) {
                DashboardIcon(R.drawable.dashboard_warning, Modifier.size(15.dp))
                Text(stringResource(R.string.dashboard_critical), style = captionStyle(),
                    fontWeight = FontWeight.Medium, color = DashboardTokens.Error)
            }
        }
        Spacer(Modifier.height(DashboardTokens.SmallGap))
        Caption(R.string.dashboard_water_trend)
    }
}

@Composable
internal fun SystemStatusCard(modifier: Modifier) {
    MetricCard(modifier, R.drawable.dashboard_settings, R.string.dashboard_system_status) {
        Row(Modifier.heightIn(min = 24.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DashboardTokens.SmallGap)) {
            Box(Modifier.size(15.dp).background(DashboardTokens.SuccessSurface, CircleShape),
                contentAlignment = Alignment.Center) {
                DashboardIcon(R.drawable.dashboard_online, Modifier.requiredSize(23.292.dp))
            }
            Text(stringResource(R.string.dashboard_online), style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium, color = FloodGateHeading)
        }
        Spacer(Modifier.height(DashboardTokens.SmallGap))
        Caption(R.string.dashboard_last_updated)
    }
}

@Composable
private fun ActivityCard(onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(DashboardTokens.CardRadius))
        .clip(RoundedCornerShape(DashboardTokens.CardRadius)).background(DashboardTokens.ActivitySurface)
        .clickable(role = Role.Button, onClick = onClick)
        .padding(horizontal = DashboardTokens.PagePadding, vertical = DashboardTokens.CardVerticalPadding),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DashboardTokens.SmallGap)) {
            Text(stringResource(R.string.dashboard_activity_title), style = MaterialTheme.typography.bodyMedium,
                color = DashboardTokens.Body)
            Text(stringResource(R.string.dashboard_activity_time), style = MaterialTheme.typography.bodyMedium,
                color = DashboardTokens.Body)
        }
        DashboardIcon(R.drawable.dashboard_arrow_right)
    }
}

@Composable
private fun DashboardNavigation(onSelect: (DashboardDialog) -> Unit) {
    val scrollTabs = LocalDensity.current.fontScale > 1.15f
    Column {
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(DashboardTokens.SecondaryBorder))
        // Keep full labels at larger font sizes; tabs can be swiped instead of clipping text.
        Row(Modifier.fillMaxWidth()
            .then(if (scrollTabs) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
            .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = if (scrollTabs) Arrangement.spacedBy(24.dp) else Arrangement.Start) {
            val itemModifier = if (scrollTabs) Modifier.widthIn(min = 48.dp) else Modifier.weight(1f)
            NavigationItem(R.drawable.dashboard_home, R.string.dashboard_home, itemModifier, true) { }
            NavigationItem(R.drawable.dashboard_device, R.string.dashboard_devices, itemModifier) {
                onSelect(DashboardDialog.DEVICES)
            }
            NavigationItem(R.drawable.dashboard_notification, R.string.dashboard_notification,
                if (scrollTabs) itemModifier else Modifier.weight(1.25f)) {
                onSelect(DashboardDialog.NOTIFICATIONS)
            }
            NavigationItem(R.drawable.dashboard_profile, R.string.dashboard_profile, itemModifier) {
                onSelect(DashboardDialog.PROFILE)
            }
        }
    }
}

@Composable
private fun NavigationItem(@DrawableRes icon: Int, @StringRes label: Int, modifier: Modifier,
                           isSelected: Boolean = false, onClick: () -> Unit) {
    Column(modifier.heightIn(min = DashboardTokens.TouchTarget)
        .semantics { selected = isSelected }.clickable(role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally) {
        DashboardIcon(icon, Modifier.size(DashboardTokens.NavigationIcon))
        Text(stringResource(label), style = MaterialTheme.typography.bodyMedium,
            color = DashboardTokens.Body, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun DashboardInfoDialog(dialog: DashboardDialog, email: String, onDismiss: () -> Unit,
                                onSignOut: () -> Unit) {
    val title = when (dialog) {
        DashboardDialog.LOCATION -> R.string.dashboard_location
        DashboardDialog.ACTIVITY -> R.string.dashboard_recent_activity
        DashboardDialog.CONTROL -> R.string.dashboard_no_device
        DashboardDialog.DEVICES -> R.string.dashboard_devices
        DashboardDialog.NOTIFICATIONS -> R.string.dashboard_notification
        DashboardDialog.PROFILE -> R.string.dashboard_profile
    }
    val message = when (dialog) {
        DashboardDialog.LOCATION -> R.string.dashboard_location_message
        DashboardDialog.ACTIVITY -> R.string.dashboard_activity_message
        DashboardDialog.CONTROL -> R.string.dashboard_control_message
        DashboardDialog.DEVICES -> R.string.dashboard_devices_message
        DashboardDialog.NOTIFICATIONS -> R.string.dashboard_notifications_message
        DashboardDialog.PROFILE -> R.string.dashboard_profile_message
    }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = { Text(if (dialog == DashboardDialog.PROFILE) stringResource(message, email) else stringResource(message)) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dashboard_close)) } },
        dismissButton = {
            if (dialog == DashboardDialog.PROFILE) TextButton(onClick = onSignOut) {
                Text(stringResource(R.string.dashboard_sign_out))
            }
        })
}

@Composable
private fun DashboardIcon(@DrawableRes icon: Int, modifier: Modifier = Modifier.size(DashboardTokens.Icon)) {
    Image(painterResource(icon), contentDescription = null, modifier = modifier)
}

@Composable
private fun SectionTitle(@StringRes title: Int, modifier: Modifier = Modifier) {
    Text(stringResource(title), modifier, color = DashboardTokens.Body,
        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun captionStyle() = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 14.sp)

@Composable
private fun Caption(@StringRes text: Int, modifier: Modifier = Modifier) {
    Text(stringResource(text), modifier, style = captionStyle(), color = FloodGateNeutral400)
}

@Preview(name = "Dashboard — regular phone", widthDp = 412, heightDp = 917, showBackground = true)
@Preview(name = "Dashboard — small phone", widthDp = 320, heightDp = 640, showBackground = true)
@Composable
private fun DashboardPreview() {
    FloodGateTheme { DashboardScreen("preview@example.com", {}) }
}
