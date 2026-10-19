package com.taskmanager.app.ui.theme

import androidx.compose.ui.graphics.Color

// ---- Design tokens (dark-first, premium) ----
val Bg = Color(0xFF0A0C10)
val Surface = Color(0xFF12151D)
val Surface2 = Color(0xFF181D27)
val Border = Color(0x14FFFFFF) // white at 8% alpha

val Iris = Color(0xFF8B7CFF)
val IrisDeep = Color(0xFF6A5CFF)
val Cyan = Color(0xFF67E8F9) // sparing accent

val Text = Color(0xFFF2F4F8)
val Dim = Color(0xFF9AA3B2)

val Success = Color(0xFF34D399)
val Warning = Color(0xFFFBBF24)
val Danger = Color(0xFFF87171)

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
