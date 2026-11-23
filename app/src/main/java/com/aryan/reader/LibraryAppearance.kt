package com.aryan.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aryan.reader.shared.ui.SharedLibrarySurfaceStyle

internal val LibrarySurfaceShape = RoundedCornerShape(4.dp)

@Composable
internal fun librarySurfaceStyle(): SharedLibrarySurfaceStyle {
    val colors = MaterialTheme.colorScheme
    return remember(colors) {
        SharedLibrarySurfaceStyle(
            shape = LibrarySurfaceShape,
            outline = BorderStroke(
                1.dp,
                Brush.linearGradient(
                    listOf(colors.outline.copy(alpha = 0.70f), colors.outlineVariant.copy(alpha = 0.56f)),
                ),
            ),
            activeOutline = BorderStroke(
                1.dp,
                Brush.linearGradient(
                    listOf(colors.primary.copy(alpha = 0.88f), colors.tertiary.copy(alpha = 0.60f)),
                ),
            ),
        )
    }
}

@Composable
internal fun LibraryFloatingActions(onImport: () -> Unit, onContinueReading: (() -> Unit)?) {
    val style = librarySurfaceStyle()
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        LibraryFloatingIcon(
            icon = Icons.Outlined.FileOpen,
            description = stringResource(R.string.unified_library_import),
            style = style,
            onClick = onImport,
            modifier = Modifier.testTag("UnifiedLibraryImport"),
        )
        if (onContinueReading != null) {
            LibraryFloatingIcon(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                description = stringResource(R.string.unified_library_continue_reading),
                style = style,
                onClick = onContinueReading,
                modifier = Modifier.testTag("UnifiedLibraryContinueReading"),
            )
        }
    }
}

@Composable
private fun LibraryFloatingIcon(
    icon: ImageVector,
    description: String,
    style: SharedLibrarySurfaceStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = style.shape,
        border = style.outline,
        color = colors.surface.copy(alpha = 0.88f),
        contentColor = colors.onSurface,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(22.dp))
        }
    }
}
