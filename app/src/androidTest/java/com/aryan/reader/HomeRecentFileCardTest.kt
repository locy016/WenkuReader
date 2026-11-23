package com.aryan.reader

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aryan.reader.data.RecentFileItem
import com.aryan.reader.shared.ui.SharedAndroidHomeWidthClass
import com.aryan.reader.shared.ui.SharedAndroidLibraryGroup
import com.aryan.reader.shared.ui.SharedAndroidUnifiedHomeStrings
import com.aryan.reader.shared.ui.SharedAndroidUnifiedLibraryHome
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeRecentFileCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun recentFileCardShowsProgressAndUnavailableState() {
        val item = recentBook(
            bookId = "home_unavailable_epub",
            title = "Unavailable Field Guide",
            author = "Casey Example",
            progress = 42f,
            isAvailable = false
        )

        setRecentFileCard(item = item)

        composeTestRule.onNodeWithTag("HomeRecentFileCard_home_unavailable_epub").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unavailable Field Guide").assertIsDisplayed()
        composeTestRule.onNodeWithText("Casey Example").assertIsDisplayed()
        composeTestRule.onNodeWithText("42%").assertIsDisplayed()
        composeTestRule.onAllNodesWithContentDescription(text(R.string.not_available_locally))[0]
            .assertIsDisplayed()
    }

    @Test
    fun recentFileCardClickLongClickAndSelectedOverlayWork() {
        val item = recentBook(
            bookId = "home_selected_pdf",
            title = "Selected Position Notes",
            author = "Morgan Example",
            progress = 7f
        )
        var clicked = false
        var longClicked = false

        setRecentFileCard(
            item = item,
            isSelected = true,
            onClick = { clicked = true },
            onLongClick = { longClicked = true }
        )

        composeTestRule.onAllNodesWithContentDescription(text(R.string.content_desc_selected))[0]
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("HomeRecentFileCard_home_selected_pdf").performClick()
        composeTestRule.onNodeWithTag("HomeRecentFileCard_home_selected_pdf").performTouchInput {
            down(center)
            advanceEventTime(600)
            up()
        }

        assertThat(clicked).isTrue()
        assertThat(longClicked).isTrue()
    }

    @Test
    fun availableAndUnavailableCardsHaveTheSameHeight() {
        val available = recentBook("same_height_available", "Available", "A", 20f)
        val unavailable = recentBook("same_height_unavailable", "Unavailable", "B", 20f, isAvailable = false)

        composeTestRule.setContent {
            MaterialTheme {
                Row(modifier = androidx.compose.ui.Modifier.width(280.dp)) {
                    RecentFileCard(
                        item = available,
                        isSelected = false,
                        modifier = androidx.compose.ui.Modifier.weight(1f),
                        onClick = {}, onLongClick = {}, isDownloading = false
                    )
                    RecentFileCard(
                        item = unavailable,
                        isSelected = false,
                        modifier = androidx.compose.ui.Modifier.weight(1f),
                        onClick = {}, onLongClick = {}, isDownloading = false
                    )
                }
            }
        }

        val availableHeight = composeTestRule.onNodeWithTag("HomeRecentFileCard_same_height_available")
            .fetchSemanticsNode().boundsInRoot.height
        val unavailableHeight = composeTestRule.onNodeWithTag("HomeRecentFileCard_same_height_unavailable")
            .fetchSemanticsNode().boundsInRoot.height

        // Two weight(1f) cards split the row's width, which can differ by a single pixel when the total
        // width is odd; the aspect-ratio cover then differs by ~1px. The point of the test is that
        // the unavailable overlay does not change the card's height, not that it is pixel-identical.
        assertThat(availableHeight).isWithin(1f).of(unavailableHeight)
    }

    @Test
    fun gridAndListShowFileTypesAtTheBottomStartOfTheirCovers() {
        val gridBook = recentBook("grid", "Grid book", "Example Author", 0f)
        val listBook = recentBook("list", "List book", "", 0f).copy(type = FileType.PDF)
        composeTestRule.setContent {
            MaterialTheme {
                Column {
                    RecentFileCard(
                        item = gridBook,
                        isSelected = false,
                        modifier = Modifier.width(98.dp),
                        onClick = {},
                        onLongClick = {},
                        isDownloading = false,
                        compact = true,
                    )
                    LibraryListItem(
                        item = listBook,
                        isSelected = false,
                        onItemClick = {},
                        onItemLongClick = {},
                        isDownloading = false,
                    )
                }
            }
        }

        val gridTitle = boundsOf("Grid book")
        val gridBadge = boundsOf("EPUB")
        assertThat(gridBadge.bottom).isLessThan(gridTitle.top)
        assertThat(gridBadge.center.x).isLessThan(gridTitle.center.x)

        val listCover = composeTestRule.onNodeWithContentDescription(listBook.displayName, useUnmergedTree = true)
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val listBadge = boundsOf("PDF")
        assertThat(listBadge.center.x).isLessThan(listCover.center.x)
        assertThat(listBadge.top).isGreaterThan(listCover.center.y)
        assertThat(listBadge.bottom).isLessThan(listCover.bottom)
    }

    @Test
    fun fixedFiveColumnGridStartsEachCategoryOnItsOwnRow() {
        val ebooks = listOf("Book 1", "Book 2", "Book 3", "Book 4", "Book 5", "Book 6", "Book 7")
        val documents = listOf("Document")
        composeTestRule.setContent {
            MaterialTheme {
                SharedAndroidUnifiedLibraryHome(
                    books = ebooks + documents,
                    continueReading = null,
                    filter = UnifiedLibraryFilter.ALL,
                    sortLabel = "Recent",
                    advancedFilterCount = 0,
                    useListView = false,
                    strings = SharedAndroidUnifiedHomeStrings("Empty", "Filter", "Grid", "List", emptyMap()),
                    itemKey = { it },
                    onFilterChange = {},
                    onControls = {},
                    onAdvancedFilters = {},
                    onListViewChange = {},
                    continueCard = { _, _ -> },
                    bookCard = { Text(it, Modifier.height(32.dp)) },
                    bookListItem = { Text(it) },
                    widthClass = SharedAndroidHomeWidthClass.MEDIUM,
                    fixedBookColumns = 5,
                    showControls = false,
                    bookGroups = listOf(
                        SharedAndroidLibraryGroup("ebooks", "E-books", Color.Green, ebooks),
                        SharedAndroidLibraryGroup("documents", "Documents", Color.Blue, documents),
                    ),
                )
            }
        }

        assertThat(boundsOf("Book 5").top).isEqualTo(boundsOf("Book 1").top)
        assertThat(boundsOf("Book 6").top).isGreaterThan(boundsOf("Book 5").top)
        assertThat(boundsOf("Documents").top).isGreaterThan(boundsOf("Book 7").bottom)
        assertThat(boundsOf("Document").left).isEqualTo(boundsOf("Book 1").left)
    }

    private fun boundsOf(text: String) = composeTestRule.onNodeWithText(text, useUnmergedTree = true)
        .assertIsDisplayed().fetchSemanticsNode().boundsInRoot

    private fun setRecentFileCard(
        item: RecentFileItem,
        isSelected: Boolean = false,
        isPinned: Boolean = false,
        onClick: () -> Unit = {},
        onLongClick: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            MaterialTheme {
                RecentFileCard(
                    item = item,
                    isSelected = isSelected,
                    isPinned = isPinned,
                    onClick = onClick,
                    onLongClick = onLongClick,
                    isDownloading = false,
                    usePdfFileNameAsDisplayName = false
                )
            }
        }
    }

    private fun recentBook(
        bookId: String,
        title: String,
        author: String,
        progress: Float,
        isAvailable: Boolean = true
    ): RecentFileItem {
        return RecentFileItem(
            bookId = bookId,
            uriString = "content://home-test/$bookId",
            type = FileType.EPUB,
            displayName = "$bookId.epub",
            timestamp = 1_000L,
            title = title,
            author = author,
            progressPercentage = progress,
            isRecent = true,
            isAvailable = isAvailable
        )
    }

    private fun text(resId: Int): String {
        return context.getString(resId)
    }
}
