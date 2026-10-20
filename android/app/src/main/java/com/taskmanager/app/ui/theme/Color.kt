package com.taskmanager.app.ui.theme

import androidx.compose.ui.graphics.Color

// ---- Design tokens: "Midnight" (GitHub Primer dark palette) ----
val Bg = Color(0xFF0D1117)
val Surface = Color(0xFF161B22)
val Surface2 = Color(0xFF1C2128)
val Border = Color(0x14FFFFFF) // white at 8% alpha

val Iris = Color(0xFF58A6FF) // Primer accent blue
val IrisDeep = Color(0xFF1F6FEB) // Primer accent blue, deep
val Cyan = Color(0xFF818CF8) // soft periwinkle, sparing accent

val Text = Color(0xFFE6EDF3)
val Dim = Color(0xFF7D8590)

val Success = Color(0xFF3FB950)
val Warning = Color(0xFFD29922)
val Danger = Color(0xFFF85149)

// Keep-style note colors (unchanged behavior)
val NoteColors = listOf(
    "#FFFFFF",
    "#FFF7AE",
    "#F8BBD0",
    "#C8E6C9",
    "#B3E5FC",
    "#E1BEE7",
    "#FFCC80"
)

// Soft tints used for pills / icon wells
fun Color.soft(alpha: Float = 0.14f): Color = copy(alpha = alpha)
