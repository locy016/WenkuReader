package com.aryan.reader.shared.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Exact Android Home recent-book card chrome with platform cover/status slots. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SharedAndroidHomeRecentCard(
    bookId: String,
    title: String,
    author: String,
    progressPercent: Int?,
    isAvailable: Boolean,
    isDownloading: Boolean,
    isSelected: Boolean,
    showStatusBadges: Boolean,
    unavailableDescription: String,
    selectedDescription: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    cover: @Composable (Modifier) -> Unit,
    statusBadges: @Composable (Modifier) -> Unit,
    fileTypeBadge: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val libraryStyle = LocalSharedLibrarySurfaceStyle.current
    val cardShape = libraryStyle?.shape ?: RectangleShape
    ElevatedCard(
        modifier = modifier
            .testTag("HomeRecentFileCard_$bookId")
            .graphicsLayer { alpha = if (isAvailable) 1f else 0.8f }
            .then(
                when {
                    !isSelected -> Modifier
                    libraryStyle != null -> Modifier.border(libraryStyle.activeOutline, cardShape)
                    else -> Modifier.border(2.dp, MaterialTheme.colorScheme.primary)
                },
            )
            .clip(cardShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = cardShape,
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().aspectRatio(0.74f)) {
                cover(Modifier.fillMaxSize())
                if (showStatusBadges) statusBadges(Modifier.align(Alignment.TopStart).padding(10.dp))
                if (!isAvailable) {
                    Box(
                        Modifier.matchParentSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isDownloading) CircularProgressIndicator(color = Color.White)
                        else Icon(Icons.Default.Info, unavailableDescription, Modifier.size(48.dp), tint = Color.White)
                    }
                }
                if (isSelected) {
                    Box(
                        Modifier.matchParentSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Check,
                            selectedDescription,
                            Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary, CircleShape).padding(8.dp),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
            Column(
                Modifier.fillMaxWidth().padding(top = if (compact) 4.dp else 7.dp, bottom = 3.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    title,
                    style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (author.isNotBlank()) {
                    Spacer(Modifier.height(if (compact) 1.dp else 4.dp))
                    Text(
                        author,
                        style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (fileTypeBadge != null || progressPercent != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = if (compact) 2.dp else 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        fileTypeBadge?.invoke()
                        progressPercent?.let {
                            Text(
                                "$it%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
