package com.aryan.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class UnifiedLibraryBackTest {

    @Test
    fun `selection clears before search and leaving a management page`() {
        assertEquals(
            LibraryBackAction.CLEAR_SELECTION,
            nextLibraryBackAction(hasSelection = true, isSearchActive = true, isManagementPage = true),
        )
    }

    @Test
    fun `open keyboard hides before search clears`() {
        assertEquals(
            LibraryBackAction.HIDE_KEYBOARD,
            nextLibraryBackAction(
                hasSelection = false,
                isSearchActive = true,
                isManagementPage = true,
                isKeyboardOpen = true,
            ),
        )
    }

    @Test
    fun `active search clears first`() {
        assertEquals(
            LibraryBackAction.CLEAR_SEARCH,
            nextLibraryBackAction(hasSelection = false, isSearchActive = true, isManagementPage = true),
        )
    }

    @Test
    fun `back from a management page returns to the library`() {
        assertEquals(
            LibraryBackAction.RETURN_TO_LIBRARY,
            nextLibraryBackAction(hasSelection = false, isSearchActive = false, isManagementPage = true),
        )
    }

    @Test
    fun `no layers means no interception`() {
        assertEquals(
            LibraryBackAction.NONE,
            nextLibraryBackAction(hasSelection = false, isSearchActive = false, isManagementPage = false),
        )
    }

    @Test
    fun `upgrading from the removed shelf section opens the library`() {
        assertEquals(
            UnifiedLibrarySection.HOME,
            androidLibrarySection(UnifiedLibrarySection.SHELVES.persistedValue),
        )
        assertEquals(
            UnifiedLibrarySection.FOLDERS,
            androidLibrarySection(UnifiedLibrarySection.FOLDERS.persistedValue),
        )
    }
}
