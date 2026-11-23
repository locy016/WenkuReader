package com.aryan.reader.shared.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape

data class SharedLibrarySurfaceStyle(
    val shape: Shape,
    val outline: BorderStroke,
    val activeOutline: BorderStroke,
    val listOutline: BorderStroke = outline,
)

val LocalSharedLibrarySurfaceStyle = staticCompositionLocalOf<SharedLibrarySurfaceStyle?> { null }
