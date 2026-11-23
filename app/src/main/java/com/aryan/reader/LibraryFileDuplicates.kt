package com.aryan.reader

import com.aryan.reader.data.RecentFileItem
import com.aryan.reader.data.effectiveReadingPositionModifiedTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class LibraryFileDuplicates(
    private val sizeOf: (String) -> Long,
    private val hashOf: suspend (String) -> String?,
) {
    private data class FileSnapshot(val uri: String, val size: Long, val modified: Long)

    private val hashes = object : LinkedHashMap<FileSnapshot, String>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<FileSnapshot, String>?): Boolean = size > 1024
    }

    suspend fun groups(books: List<RecentFileItem>): List<List<RecentFileItem>> = withContext(Dispatchers.IO) {
        val candidates = books.filter { it.isAvailable && !it.isDeleted && it.uriString != null }
            .groupBy { it.type to sizeOf(it) }
        val duplicateGroups = mutableMapOf<String, List<RecentFileItem>>()
        for ((key, matchingSizes) in candidates) {
            val size = key.second
            if (size <= 0L || matchingSizes.size < 2) continue
            val verified = mutableMapOf<String, MutableList<RecentFileItem>>()
            for (book in matchingSizes) {
                val hash = digest(book, size) ?: continue
                verified.getOrPut(hash) { mutableListOf() }.add(book)
            }
            for (matches in verified.values) {
                if (matches.size > 1) matches.forEach { duplicateGroups[it.bookId] = matches }
            }
        }

        val emitted = mutableSetOf<String>()
        books.mapNotNull { book ->
            val group = duplicateGroups[book.bookId] ?: listOf(book)
            group.takeIf { emitted.add(group.first().bookId) }
        }
    }

    suspend fun existingBook(
        books: List<RecentFileItem>,
        uri: String,
        type: FileType,
        hash: String,
    ): RecentFileItem? = withContext(Dispatchers.IO) {
        val size = sizeOf(uri)
        if (size <= 0L) return@withContext null
        val matches = books.filter { book ->
            book.isAvailable && !book.isDeleted && book.uriString != null && book.type == type &&
                sizeOf(book) == size && hashOf(book.uriString) == hash
        }
        preferredLibraryCopy(matches)
    }

    private fun sizeOf(book: RecentFileItem): Long =
        book.fileSize.takeIf { it > 0L } ?: book.uriString?.let(sizeOf) ?: 0L

    private suspend fun digest(book: RecentFileItem, size: Long): String? {
        val uri = book.uriString ?: return null
        val modified = book.fileContentModifiedTimestamp
        if (modified <= 0L) return hashOf(uri)
        val snapshot = FileSnapshot(uri, size, modified)
        synchronized(hashes) { hashes[snapshot] }?.let { return it }
        return hashOf(uri)?.also { hash -> synchronized(hashes) { hashes[snapshot] = hash } }
    }
}

internal fun preferredLibraryCopy(books: List<RecentFileItem>): RecentFileItem? = books.minWithOrNull(
    compareByDescending<RecentFileItem> { it.isAvailable }
        .thenByDescending { it.effectiveReadingPositionModifiedTimestamp() }
        .thenByDescending { it.sourceFolderUri == null }
        .thenBy { it.dateAddedTimestamp }
        .thenBy { it.bookId },
)
