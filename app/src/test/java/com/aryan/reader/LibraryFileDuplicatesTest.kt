package com.aryan.reader

import com.aryan.reader.data.RecentFileItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryFileDuplicatesTest {
    private val imported = book("imported")
    private val folder = book("folder").copy(sourceFolderUri = "file:///folder")
    private val other = book("other").copy(displayName = imported.displayName)

    @Test
    fun groupsIdenticalFilesAcrossSourcesAndKeepsSameNameDifferentContent() = runBlocking {
        val matcher = LibraryFileDuplicates(sizeOf = { 42L }, hashOf = { if (it == other.uriString) "different" else "same" })

        assertEquals(listOf(listOf(imported, folder), listOf(other)), matcher.groups(listOf(imported, folder, other)))
    }

    @Test
    fun importReusesFolderCopyOnlyWhenContentMatches() = runBlocking {
        val matcher = LibraryFileDuplicates(sizeOf = { 42L }, hashOf = { "contents" })

        assertEquals(folder, matcher.existingBook(listOf(folder), "file:///new-name.txt", FileType.TXT, "contents"))
        assertNull(matcher.existingBook(listOf(folder), "file:///new-name.txt", FileType.TXT, "changed"))
    }

    @Test
    fun inaccessibleFileIsNeverTreatedAsAnIdenticalCopy() = runBlocking {
        val matcher = LibraryFileDuplicates(sizeOf = { 42L }, hashOf = { if (it == folder.uriString) null else "same" })

        assertEquals(listOf(listOf(imported), listOf(folder)), matcher.groups(listOf(imported, folder)))
        assertNull(matcher.existingBook(listOf(folder), "file:///new-name.txt", FileType.TXT, "same"))
    }

    @Test
    fun duplicateWithLatestReadingPositionTakesPrecedence() {
        assertEquals(imported, preferredLibraryCopy(listOf(folder, imported)))
        val inProgress = folder.copy(progressPercentage = 31f, readingPositionModifiedTimestamp = 100)
        assertEquals(inProgress, preferredLibraryCopy(listOf(imported, inProgress)))
    }

    private fun book(id: String) = RecentFileItem(
        bookId = id,
        uriString = "file:///$id.txt",
        type = FileType.TXT,
        displayName = "$id.txt",
        timestamp = 1L,
        fileSize = 42L,
        fileContentModifiedTimestamp = 1L,
    )
}
