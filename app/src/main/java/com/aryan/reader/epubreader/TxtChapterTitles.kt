package com.aryan.reader.epubreader

import com.aryan.reader.epub.EpubTocEntry

internal class TxtChapterTitles(
    private val bookTitle: String,
    partPaths: List<String>,
    entriesByPath: Map<String, List<EpubTocEntry>>
) {
    private data class Part(val startTitle: String, val anchoredTitles: Map<String, String>)

    private val parts = buildMap {
        var precedingTitle = bookTitle
        for (path in partPaths) {
            val entries = entriesByPath[path].orEmpty()
            val startTitle = entries.firstOrNull { it.fragmentId == null }?.label ?: precedingTitle
            val anchoredTitles = entries.mapNotNull { entry ->
                entry.fragmentId?.let { it to entry.label }
            }.toMap()
            put(path, Part(startTitle, anchoredTitles))
            precedingTitle = entries.lastOrNull()?.label ?: precedingTitle
        }
    }

    fun titleAt(path: String?, fragmentId: String?): String {
        val part = path?.let(parts::get) ?: return bookTitle
        return fragmentId?.let(part.anchoredTitles::get) ?: part.startTitle
    }
}
