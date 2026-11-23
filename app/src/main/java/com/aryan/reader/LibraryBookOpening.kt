package com.aryan.reader

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.aryan.reader.data.RecentFileItem
import kotlinx.coroutines.delay

internal const val BookOpeningDuration = 340
private const val CoverSourceLifetimeMillis = 10_000L

internal class LibraryBookOpening {
    var source by mutableStateOf<LibraryCoverSource?>(null)
        private set

    fun request(item: RecentFileItem, bounds: Rect, tap: LibraryCoverTap) {
        source = LibraryCoverSource(item, bounds, tap, SystemClock.uptimeMillis())
    }

    fun clear(expected: LibraryCoverSource?) {
        if (source === expected) source = null
    }

    suspend fun expire(expected: LibraryCoverSource) {
        delay((CoverSourceLifetimeMillis - (SystemClock.uptimeMillis() - expected.capturedAt)).coerceAtLeast(0L))
        clear(expected)
    }
}

internal data class LibraryCoverSource(
    val item: RecentFileItem,
    val bounds: Rect,
    val tap: LibraryCoverTap,
    val capturedAt: Long,
) {
    fun canAnimate(bookId: String?): Boolean =
        item.bookId == bookId && SystemClock.uptimeMillis() - capturedAt < CoverSourceLifetimeMillis
}

internal val LocalLibraryBookOpening = compositionLocalOf<LibraryBookOpening?> { null }

internal class LibraryCoverTap(private val opening: LibraryBookOpening) {
    var coordinates: LayoutCoordinates? = null
    var concealed by mutableStateOf(false)

    fun record(item: RecentFileItem) {
        val cover = coordinates?.takeIf { it.isAttached } ?: return
        val bounds = cover.boundsInRoot()
        // A partly scrolled thumbnail should not grow from a clipped rectangle.
        if (bounds.width >= cover.size.width - 1 && bounds.height >= cover.size.height - 1 &&
            bounds.width > 0 && bounds.height > 0
        ) {
            opening.request(item, bounds, this)
        }
    }
}

@Composable
internal fun rememberLibraryCoverTap(item: RecentFileItem, enabled: Boolean): LibraryCoverTap? {
    val opening = LocalLibraryBookOpening.current
    val isReaderBook = item.type.readerSurfaceOnAndroid() != null
    return remember(opening, enabled, item.bookId, item.isAvailable, isReaderBook) {
        if (opening != null && enabled && item.isAvailable && isReaderBook) LibraryCoverTap(opening) else null
    }
}

internal fun Modifier.recordLibraryCover(tap: LibraryCoverTap?): Modifier {
    if (tap == null) return this
    return onGloballyPositioned { tap.coordinates = it }.drawWithContent {
        if (!tap.concealed) drawContent()
    }
}

@Composable
internal fun LibraryBookOpeningOverlay(source: LibraryCoverSource, onFinished: () -> Unit) {
    val progress = remember(source) { Animatable(0f) }
    LaunchedEffect(source) {
        progress.animateTo(1f, tween(BookOpeningDuration, easing = LinearEasing))
        onFinished()
    }
    DisposableEffect(source) {
        source.tap.concealed = true
        onDispose {
            source.tap.concealed = false
            onFinished()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = AbsoluteAlignment.TopLeft) {
        val density = LocalDensity.current
        val bounds = source.bounds
        val sourceWidth = with(density) { bounds.width.toDp() }
        val sourceHeight = with(density) { bounds.height.toDp() }
        val aspectRatio = bounds.width / bounds.height
        val targetWidth = (maxWidth * 0.43f).coerceIn(130.dp, 216.dp)
            .coerceAtMost(maxWidth * 0.65f)
            .coerceAtMost(maxHeight * (0.54f * aspectRatio))
        val targetScale = targetWidth / sourceWidth
        val targetX = with(density) { (maxWidth - targetWidth).toPx() * 0.5f }
        val targetY = with(density) { (maxHeight - sourceHeight * targetScale).toPx() * 0.45f }

        Box(
            Modifier.size(sourceWidth, sourceHeight).graphicsLayer {
                val lift = FastOutSlowInEasing.transform((progress.value / 0.75f).coerceIn(0f, 1f))
                val zoom = lerp(1f, targetScale, lift)
                transformOrigin = TransformOrigin(0f, 0f)
                translationX = lerp(bounds.left, targetX, lift)
                translationY = lerp(bounds.top, targetY, lift)
                scaleX = zoom
                scaleY = zoom
                alpha = 1f - ((progress.value - 0.60f) / 0.40f).coerceIn(0f, 1f)
            },
        ) {
            Box(
                Modifier
                    .fillMaxSize(0.94f)
                    .graphicsLayer {
                        alpha = ((progress.value - 0.22f) / 0.28f).coerceIn(0f, 1f)
                    }
                    .background(
                        Brush.horizontalGradient(
                            0f to Color(0xFFE1D8CA),
                            0.08f to Color(0xFFFAF7F0),
                            0.96f to Color(0xFFFAF7F0),
                            1f to Color(0xFFE4DBCD),
                        ),
                    ),
            )
            ThemedBookCover(
                item = source.item,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    val hinge = ((progress.value - 0.24f) / 0.76f).coerceIn(0f, 1f)
                    transformOrigin = TransformOrigin(0.025f, 0.5f)
                    cameraDistance = bounds.width * 2f
                    rotationY = -84f * FastOutSlowInEasing.transform(hinge)
                },
            )
        }
    }
}
