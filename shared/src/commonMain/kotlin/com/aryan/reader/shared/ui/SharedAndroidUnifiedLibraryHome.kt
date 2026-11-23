package com.aryan.reader.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import com.aryan.reader.shared.search.SharedGlobalSearchBookResult
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

data class SharedAndroidUnifiedHomeStrings(
    val noBooks: String,
    val filterBooks: String,
    val gridView: String,
    val listView: String,
    val filterLabels: Map<MobileUnifiedLibraryFilter, String>,
    val emptyMessage: String? = null,
)

data class SharedAndroidLibraryGroup<T>(
    val id: String,
    val title: String,
    val accent: Color,
    val books: List<T>,
)

@Composable
fun <T> SharedAndroidUnifiedLibraryHome(
    books: List<T>,
    continueReading: T?,
    filter: MobileUnifiedLibraryFilter,
    sortLabel: String,
    advancedFilterCount: Int,
    useListView: Boolean,
    strings: SharedAndroidUnifiedHomeStrings,
    itemKey: (T) -> String,
    onFilterChange: (MobileUnifiedLibraryFilter) -> Unit,
    onControls: () -> Unit,
    onAdvancedFilters: () -> Unit,
    onListViewChange: (Boolean) -> Unit,
    continueCard: @Composable (T, Modifier) -> Unit,
    bookCard: @Composable (T) -> Unit,
    bookListItem: @Composable (T) -> Unit,
    widthClass: SharedAndroidHomeWidthClass = SharedAndroidHomeWidthClass.COMPACT,
    /** Host override (Duo). Null keeps the shared width-class mapping. */
    gridCells: GridCells? = null,
    /** Host override (Duo). Standard keeps today's spacing on every other device. */
    density: SharedAndroidHomeDensity = SharedAndroidHomeDensity.Standard,
    /** Rendered above the continue-reading card; empty when multi-tab reading is idle. */
    openTabsContent: @Composable () -> Unit = {},
    /**
     * Inside-file content search. When [contentSearchActive] is true the grid
     * is replaced by one row per matching book, expanded to show more of that
     * book's matches on demand.
     */
    contentSearchActive: Boolean = false,
    contentSearchResults: List<SharedGlobalSearchBookResult> = emptyList(),
    contentResultRow: @Composable (T, SharedGlobalSearchBookResult, Boolean, () -> Unit) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier,
    showControls: Boolean = true,
    combineFilterAndSort: Boolean = false,
    filterAndSortLabel: String = strings.filterBooks,
    bookGroups: List<SharedAndroidLibraryGroup<T>> = emptyList(),
    bookGridSpacing: DpSize = DpSize(SharedMobileBookGridSpacing, SharedMobileBookGridSpacing),
    bookListSpacing: Dp = 12.dp,
    fixedBookColumns: Int? = null,
    statusControls: (@Composable () -> Unit)? = null,
) {
    val libraryStyle = LocalSharedLibrarySurfaceStyle.current
    val resolvedGridCells = fixedBookColumns?.let { GridCells.Fixed(it) } ?: gridCells ?: widthClass.bookGridCells()
    val libraryListState = rememberLazyListState()
    val libraryGridState = rememberLazyGridState()
    val searchListState = rememberLazyListState()
    val searchGridState = rememberLazyGridState()
    // Expansion lives here so the row composable stays stateless and the host
    // only supplies layout.
    val expandedBooks = remember { mutableStateMapOf<String, Boolean>() }
    val contentResultsById = remember(contentSearchResults) {
        contentSearchResults.associateBy { it.bookId }
    }
    val filterControl: @Composable () -> Unit = {
        BadgedBox(badge = { if (advancedFilterCount > 0) Badge { Text(advancedFilterCount.toString()) } }) {
            IconButton(onClick = if (combineFilterAndSort) onControls else onAdvancedFilters) {
                Icon(Icons.Default.FilterList, contentDescription = filterAndSortLabel)
            }
        }
    }
    val viewControl: @Composable () -> Unit = {
        IconButton(onClick = { onListViewChange(!useListView) }) {
            Icon(
                if (useListView) Icons.AutoMirrored.Filled.LibraryBooks else Icons.Default.FormatListNumbered,
                contentDescription = if (useListView) strings.gridView else strings.listView,
            )
        }
    }
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        openTabsContent()
        continueReading?.let { continueCard(it, Modifier.padding(top = density.continueCardTopGap)) }
        if (showControls && statusControls != null) {
            Box(Modifier.fillMaxWidth().padding(top = density.filterRowTopGap)) {
                statusControls()
            }
        } else if (showControls) Row(
            modifier = Modifier.fillMaxWidth().padding(top = density.filterRowTopGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                items(MobileUnifiedLibraryFilter.entries) { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { onFilterChange(option) },
                        label = { Text(strings.filterLabels.getValue(option)) },
                        shape = libraryStyle?.shape ?: FilterChipDefaults.shape,
                        border = libraryStyle?.let { if (filter == option) it.activeOutline else it.outline }
                            ?: FilterChipDefaults.filterChipBorder(enabled = true, selected = filter == option),
                    )
                }
                if (!combineFilterAndSort) {
                    item { AssistChip(onClick = onControls, label = { Text(sortLabel) }) }
                    item { filterControl() }
                    if (!contentSearchActive) item { viewControl() }
                }
            }
            if (combineFilterAndSort) {
                filterControl()
                if (!contentSearchActive) viewControl()
            }
        }
        if (books.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.noBooks, style = MaterialTheme.typography.titleMedium)
                    strings.emptyMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else if (contentSearchActive) {
            Surface(
                Modifier.weight(1f).padding(top = density.gridCardTopGap),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = if (combineFilterAndSort) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = if (combineFilterAndSort) 0.dp else 1.dp,
            ) {
                LazyColumn(
                    state = searchListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp, density.gridContentTopPadding, 12.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(bookListSpacing),
                ) {
                    // Iterate the merged book list, not the content results:
                    // books matching metadata only must still appear, just
                    // without the matched-text body.
                    items(books, key = itemKey) { book ->
                        val result = contentResultsById[itemKey(book)]
                        if (result != null) {
                            val expanded = expandedBooks[result.bookId] == true
                            contentResultRow(
                                book,
                                result,
                                expanded,
                                { expandedBooks[result.bookId] = !expanded },
                            )
                        } else {
                            bookListItem(book)
                        }
                    }
                }
            }
        } else {
            Surface(
                Modifier.weight(1f).padding(top = density.gridCardTopGap),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = if (combineFilterAndSort) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = if (combineFilterAndSort) 0.dp else 1.dp,
            ) {
                if (useListView) {
                    LazyColumn(
                        state = if (showControls) libraryListState else searchListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(if (combineFilterAndSort) 0.dp else 12.dp, density.gridContentTopPadding, if (combineFilterAndSort) 0.dp else 12.dp, 96.dp),
                        verticalArrangement = Arrangement.spacedBy(bookListSpacing),
                    ) {
                        if (bookGroups.isEmpty()) {
                            items(books, key = itemKey) { bookListItem(it) }
                        } else {
                            bookGroups.forEach { group ->
                                item(key = "category:${group.id}") { LibraryGroupHeading(group) }
                                items(group.books, key = itemKey) { bookListItem(it) }
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = resolvedGridCells,
                        state = if (showControls) libraryGridState else searchGridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(if (combineFilterAndSort) 0.dp else 12.dp, density.gridContentTopPadding, if (combineFilterAndSort) 0.dp else 12.dp, 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(bookGridSpacing.width),
                        verticalArrangement = Arrangement.spacedBy(bookGridSpacing.height),
                    ) {
                        if (bookGroups.isEmpty()) {
                            items(books, key = itemKey) { bookCard(it) }
                        } else {
                            var nextSmallGroupOnRight = false
                            bookGroups.forEach { group ->
                                if (widthClass != SharedAndroidHomeWidthClass.COMPACT && group.books.size <= 2) {
                                    val right = nextSmallGroupOnRight
                                    nextSmallGroupOnRight = !right
                                    item(
                                        key = "small-category:${group.id}",
                                        span = {
                                            GridItemSpan(smallLibraryGroupSpan(maxLineSpan, right))
                                        },
                                    ) {
                                        SmallLibraryGridGroup(
                                            group,
                                            widthClass,
                                            bookGridSpacing,
                                            fixedBookColumns?.let { smallLibraryGroupSpan(it, right) },
                                            bookCard,
                                        )
                                    }
                                } else {
                                    nextSmallGroupOnRight = false
                                    item(key = "category:${group.id}", span = { GridItemSpan(maxLineSpan) }) {
                                        LibraryGroupHeading(group)
                                    }
                                    items(group.books, key = itemKey) { bookCard(it) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun smallLibraryGroupSpan(totalColumns: Int, right: Boolean): Int {
    if (totalColumns < 4) return totalColumns
    val leftColumns = totalColumns / 2
    return if (right) totalColumns - leftColumns else leftColumns
}

@Composable
private fun <T> SmallLibraryGridGroup(
    group: SharedAndroidLibraryGroup<T>,
    widthClass: SharedAndroidHomeWidthClass,
    spacing: DpSize,
    fixedGroupColumns: Int?,
    bookCard: @Composable (T) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = spacing.width
        val minimumColumns = if (gap == SharedMobileBookGridSpacing) 3 else group.books.size
        val columns = fixedGroupColumns ?: widthClass.bookGridColumns(maxWidth, gap, minimumColumns = minimumColumns)
        val cardWidth = (maxWidth - gap * (columns - 1)) / columns
        Column(verticalArrangement = Arrangement.spacedBy(maxOf(12.dp, spacing.height * 0.65f))) {
            LibraryGroupHeading(group)
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                group.books.forEach { book ->
                    Box(Modifier.width(cardWidth)) { bookCard(book) }
                }
            }
        }
    }
}

@Composable
private fun LibraryGroupHeading(group: SharedAndroidLibraryGroup<*>) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp).semantics { heading() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(Modifier.size(4.dp, 16.dp).background(group.accent, MaterialTheme.shapes.extraSmall))
        Text(group.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun <T> SharedAndroidUnifiedLibrarySearch(
    books: List<T>,
    query: String,
    searchPlaceholder: String,
    clearDescription: String,
    closeDescription: String,
    resultLabel: String,
    noResultsLabel: String,
    showSearchField: Boolean = true,
    itemKey: (T) -> String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    bookCard: @Composable (T) -> Unit,
    widthClass: SharedAndroidHomeWidthClass = SharedAndroidHomeWidthClass.COMPACT,
    /** Host override (Duo). Null keeps the shared width-class mapping. */
    gridCells: GridCells? = null,
    modifier: Modifier = Modifier,
) {
    val focusRequester = androidx.compose.runtime.remember { FocusRequester() }
    if (showSearchField) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        if (showSearchField) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f).focusRequester(focusRequester).testTag("UnifiedLibrarySearch"),
                    placeholder = { Text(searchPlaceholder) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Close, clearDescription) }
                    },
                    singleLine = true,
                )
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, closeDescription) }
            }
        }
        Text(resultLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
        if (books.isEmpty() && query.isNotBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(noResultsLabel, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            LazyVerticalGrid(
                columns = gridCells ?: widthClass.bookGridCells(),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) { items(books, key = itemKey) { bookCard(it) } }
        }
    }
}
