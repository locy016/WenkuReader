package com.aryan.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aryan.reader.data.RecentFileItem
import java.io.File
import kotlin.math.absoluteValue

@Composable
fun ThemedBookCover(
    item: RecentFileItem,
    modifier: Modifier = Modifier,
    contentDescription: String? = item.displayName,
    contentScale: ContentScale = ContentScale.Crop,
    showFileType: Boolean = false,
) {
    val context = LocalContext.current
    val coverDescription = contentDescription
    val coverFile = remember(item.type, item.coverImagePath) {
        item.coverImagePath
            ?.takeIf { item.type != FileType.TXT && it.isNotBlank() }
            ?.let(::File)
            ?.takeIf { it.isFile }
    }
    // Stable request identity: without remember, every recomposition builds a
    // new ImageRequest and Coil restarts the load (flicker + decode churn).
    // Coil sizes the decode to the laid-out AsyncImage automatically.
    val coverRequest = remember(coverFile) {
        coverFile?.let {
            ImageRequest.Builder(context)
                .data(it)
                .memoryCacheKey(it.path)
                .crossfade(true)
                .allowHardware(true)
                .build()
        }
    }

    val category = item.libraryBookGroup().category
    val palette = item.coverPalette(category)
    BoxWithConstraints(modifier = modifier) {
        val edge = (maxWidth * 0.018f).coerceIn(1.dp, 2.5.dp)
        val shadow = (maxWidth * 0.028f).coerceIn(1.dp, 4.dp)
        Box(
            Modifier
                .fillMaxSize()
                .absolutePadding(right = edge + shadow * 1.5f, bottom = edge + shadow * 2f),
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .absoluteOffset(x = edge, y = edge)
                    .dropShadow(
                        shape = LibrarySurfaceShape,
                        shadow = Shadow(
                            radius = shadow,
                            color = CoverShadow,
                            offset = DpOffset(shadow * 0.35f, shadow * 0.7f),
                        ),
                    )
                    .clip(LibrarySurfaceShape)
                    .drawWithCache {
                        val pages = Brush.verticalGradient(
                            0f to Color(0xFFF4F0E8),
                            0.92f to Color(0xFFF4F0E8),
                            1f to Color(0xFFC6BFB3),
                        )
                        onDrawBehind {
                            drawRect(pages)
                            drawRect(
                                color = Color(0xFFA69F93),
                                topLeft = Offset(0f, size.height - 0.5.dp.toPx()),
                                size = Size(size.width, 0.5.dp.toPx()),
                            )
                        }
                    },
            )
            val face = Modifier.matchParentSize()
                .border(
                    0.5.dp,
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.65f), palette.accent.copy(alpha = 0.45f)),
                    ),
                    LibrarySurfaceShape,
                )
                .clip(LibrarySurfaceShape)
                .coverSpine(palette, category.artwork)
            // Generated art is only a fallback: composing it under an opaque
            // cover wastes text measurement and paint on every card.
            if (coverRequest != null) {
                AsyncImage(
                    model = coverRequest,
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = face.background(palette.paper),
                )
            } else {
                GeneratedBookCover(
                    item = item,
                    palette = palette,
                    showFileType = showFileType,
                    modifier = face.clearAndSetSemantics {
                        coverDescription?.let { this.contentDescription = it }
                    },
                )
            }
            if (showFileType) {
                FileTypeBadge(
                    type = item.type,
                    modifier = Modifier.align(Alignment.BottomStart).padding(3.dp),
                    overlay = true,
                    compact = true,
                )
            }
        }
    }
}

@Composable
private fun GeneratedBookCover(
    item: RecentFileItem,
    palette: CoverPalette,
    showFileType: Boolean,
    modifier: Modifier = Modifier
) {
    val title = remember(item.customName, item.title, item.displayName) { item.coverTitle() }
    val author = remember(item.author) { item.coverAuthor() }
    val paperGradient = remember(palette.paper) {
        Brush.linearGradient(
            0f to lerp(Color.White, palette.paper, 0.08f),
            0.62f to lerp(Color.White, palette.paper, 0.40f),
            1f to palette.paper,
        )
    }

    BoxWithConstraints(modifier = modifier.background(paperGradient)) {
        val compact = maxWidth < 64.dp || maxHeight < 85.dp
        val titleSize = (maxWidth.value * 0.10f).coerceIn(7f, 16f)
        val maxTitleLines = if (compact) 2 else 3
        val titleStyle = LocalTextStyle.current.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = titleSize.sp,
            lineHeight = (titleSize * 1.26f).sp,
            letterSpacing = 0.sp,
            lineBreak = LineBreak.Heading,
            textAlign = TextAlign.Center,
        )
        val titleWidth = with(LocalDensity.current) { (maxWidth * 0.78f).roundToPx() }
        val measurer = rememberTextMeasurer(cacheSize = 1)
        val balancedTitle = remember(title, titleWidth, titleStyle, maxTitleLines, measurer) {
            if (titleWidth > 0 && title.length in 6..24 && title.all { Character.isIdeographic(it.code) }) {
                val layout = measurer.measure(
                    title,
                    style = titleStyle,
                    maxLines = maxTitleLines,
                    constraints = Constraints(maxWidth = titleWidth),
                )
                if (layout.lineCount > 1 && !layout.hasVisualOverflow) {
                    splitBalancedCoverTitle(title, layout.lineCount)
                } else title
            } else title
        }
        val textArea = Modifier.padding(start = maxWidth * 0.13f, end = maxWidth * 0.09f)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = maxHeight * 0.14f)
                .height(maxHeight * 0.46f)
                .then(textArea),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = balancedTitle,
                modifier = Modifier.fillMaxWidth(),
                style = titleStyle,
                color = CoverInk,
                maxLines = maxTitleLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (author != null && maxWidth >= 54.dp && maxHeight >= 76.dp) {
            val authorSize = (titleSize * 0.72f).coerceIn(7f, 11f)
            val badgeClearance = if (showFileType) {
                with(LocalDensity.current) { CoverFileTypeLineHeight.toDp() } + 6.dp
            } else 0.dp
            Text(
                text = author,
                modifier = Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = maxOf(maxHeight * 0.13f, badgeClearance))
                    .then(textArea),
                color = lerp(CoverInk, palette.paper, 0.26f),
                style = titleStyle.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = authorSize.sp,
                    lineHeight = (authorSize * 1.25f).sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val CoverInk = Color(0xFF273242)
private val CoverShadow = Color(0x60262F3B)

private data class CoverPalette(val paper: Color, val accent: Color)

private val coverPalettes = listOf(
    CoverPalette(Color(0xFFE7F6EF), Color(0xFF8CAEA0)),
    CoverPalette(Color(0xFFE9F3FC), Color(0xFF8FB3D4)),
    CoverPalette(Color(0xFFFFF1E9), Color(0xFFDDA88B)),
    CoverPalette(Color(0xFFF2EDFC), Color(0xFFAE9DCB)),
    CoverPalette(Color(0xFFEEF6E2), Color(0xFFABB775)),
    CoverPalette(Color(0xFFFFF7E2), Color(0xFFC7AC79)),
    CoverPalette(Color(0xFFE7F6F7), Color(0xFF84BAAF)),
    CoverPalette(Color(0xFFFCECF2), Color(0xFFD09BAE)),
)

internal fun libraryCategoryAccent(category: LibraryBookCategory): Color =
    coverPalettes[category.paletteIndex].accent

private fun RecentFileItem.coverPalette(category: LibraryBookCategory): CoverPalette {
    val family = coverPalettes[category.paletteIndex]
    val variation = coverPalettes[coverColorSeed() % coverPalettes.size]
    return CoverPalette(
        lerp(family.paper, variation.paper, 0.18f),
        lerp(family.accent, variation.accent, 0.18f),
    )
}

private fun splitBalancedCoverTitle(title: String, lines: Int): String = buildString {
    val baseLength = title.length / lines
    val remainder = title.length % lines
    var offset = 0
    repeat(lines) { line ->
        if (line > 0) append('\n')
        val length = baseLength + if (line < remainder) 1 else 0
        append(title, offset, offset + length)
        offset += length
    }
}

private fun Modifier.coverSpine(palette: CoverPalette, binding: CoverBinding): Modifier = drawWithCache {
    val spineWidth = (size.width * binding.widthFraction).coerceIn(2.dp.toPx(), 9.dp.toPx())
    val accent = palette.accent.copy(alpha = binding.tintOpacity)
    val spine = Brush.horizontalGradient(
        0f to CoverInk.copy(alpha = 0.21f),
        0.17f to Color.White.copy(alpha = 0.40f),
        0.32f to accent,
        0.80f to accent,
        0.90f to CoverInk.copy(alpha = 0.15f),
        1f to Color.Transparent,
        endX = spineWidth,
    )
    onDrawWithContent {
        drawContent()
        drawRect(spine, size = Size(spineWidth, size.height))
    }
}

private fun RecentFileItem.coverColorSeed(): Int {
    val hash = (bookId.ifBlank { displayName }).hashCode()
    return if (hash == Int.MIN_VALUE) 0 else hash.absoluteValue
}

private fun RecentFileItem.coverTitle(): String {
    val displayTitle = cardTitle()
    return if (displayTitle == displayName || displayTitle.equals("content", ignoreCase = true)) {
        displayName.substringBeforeLast('.', missingDelimiterValue = displayName)
    } else displayTitle
}

private fun RecentFileItem.coverAuthor(): String? {
    return author
        ?.trim()
        ?.takeIf { it.isNotBlank() && !it.equals("Unknown", ignoreCase = true) }
}
