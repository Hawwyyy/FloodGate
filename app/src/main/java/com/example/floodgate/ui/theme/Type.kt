package com.example.floodgate.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.floodgate.R

val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal)
)

val GochiHandFontFamily = FontFamily(
    Font(R.font.gochi_hand_regular, FontWeight.Normal)
)

val GlutenFontFamily = FontFamily(
    Font(R.font.gluten_regular, FontWeight.Normal)
)

val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
)

val FloodGateBrandLine = TextStyle(
    fontWeight = FontWeight.Normal,
    lineHeight = 88.sp,
    letterSpacing = 0.sp
)

val FloodGateBrandFloodInitial = SpanStyle(
    fontFamily = GochiHandFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 60.sp
)

val FloodGateBrandFloodRemainder = SpanStyle(
    fontFamily = GochiHandFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 40.sp
)

val FloodGateBrandGateInitial = SpanStyle(
    fontFamily = GlutenFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 72.sp
)

val FloodGateBrandGateRemainder = SpanStyle(
    fontFamily = GlutenFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 60.sp
)
