package com.aryan.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
            listOutline = BorderStroke(0.5.dp, colors.outlineVariant.copy(alpha = 0.55f)),
        )
    }
}

@Composable
internal fun LibraryHomeActions(onImport: () -> Unit, onContinueReading: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            LibraryHomeAction(
                icon = Icons.Outlined.FileOpen,
                label = stringResource(R.string.unified_library_import),
                onClick = onImport,
                modifier = Modifier.testTag("UnifiedLibraryImport"),
            )
        }
        if (onContinueReading != null) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                LibraryHomeAction(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    label = stringResource(R.string.unified_library_continue_reading),
                    onClick = onContinueReading,
                    emphasized = true,
                    modifier = Modifier.testTag("UnifiedLibraryContinueReading"),
                )
            }
        }
    }
}

@Composable
private fun LibraryHomeAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp).semantics { role = Role.Button },
        shape = LibrarySurfaceShape,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.60f)),
        color = if (emphasized) colors.primaryContainer else colors.surfaceContainerHigh,
        contentColor = if (emphasized) colors.onPrimaryContainer else colors.onSurface,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
