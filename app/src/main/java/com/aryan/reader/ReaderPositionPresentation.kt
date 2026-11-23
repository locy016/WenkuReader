package com.aryan.reader

import android.content.Context
import androidx.core.content.edit
import com.aryan.reader.data.RecentFileItem
import java.text.NumberFormat

private fun Context.readerPositionLabels() =
    getSharedPreferences("wenku_reader_position_labels", Context.MODE_PRIVATE)

internal fun RecentFileItem.saveChapterLabel(context: Context, label: String?) {
    if (label.isNullOrBlank()) return
    val value = "${chapterLabelPositionKey()}|$label"
    val labels = context.readerPositionLabels()
    if (labels.getString(bookId, null) != value) labels.edit { putString(bookId, value) }
}

private fun RecentFileItem.chapterLabelPositionKey(): String =
    "$lastChapterIndex:$locatorBlockIndex:$locatorCharOffset:$readingPositionModifiedTimestamp"

internal fun RecentFileItem.readingPositionLabel(context: Context): String? {
    if (type in PDF_VIEWER_FILE_TYPES) {
        return lastPage?.takeIf { it >= 0 }?.let { context.getString(R.string.wenku_reading_page, it + 1) }
    }
    val storedLabel = context.readerPositionLabels().getString(bookId, null)
    val label = storedLabel?.takeIf { it.substringBefore('|') == chapterLabelPositionKey() }
        ?.substringAfter('|')
    return label ?: lastChapterIndex
        ?.takeIf { type == FileType.EPUB && it >= 0 }
        ?.let { context.getString(R.string.wenku_reading_chapter, it + 1) }
}

internal fun RecentFileItem.readingProgressLabel(context: Context): String? {
    val position = readingPositionLabel(context)
    val progress = progressPercentage?.takeIf { it.isFinite() }?.coerceIn(0f, 100f)?.let {
        val formatter = NumberFormat.getPercentInstance(context.resources.configuration.locales[0])
        formatter.maximumFractionDigits = 1
        formatter.format(it / 100.0)
    }
    return listOfNotNull(position, progress).joinToString(" · ").ifBlank { null }
}
