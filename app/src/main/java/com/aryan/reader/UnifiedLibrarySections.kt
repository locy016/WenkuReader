package com.aryan.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize
import com.aryan.reader.data.RecentFileItem
import com.aryan.reader.shared.CloudFolderSyncSelection
import com.aryan.reader.shared.SortOrder
import com.aryan.reader.shared.search.SharedGlobalSearchBookResult
import com.aryan.reader.shared.search.SharedGlobalSearchHit
import com.aryan.reader.shared.ui.SharedAndroidLibraryGroup

/**
 * Section-level composables for the library.
 *
 * Split out of [UnifiedLibraryScreen] so the screen owns destination wiring while these
 * own their own layout. Every shared collaborator is injected, so the Android
 * implementation stays the benchmark for iOS parity.
 */

@Composable
internal fun UnifiedFoldersSection(
    modifier: Modifier,
    folders: List<SyncedFolder>,
    allRecentFiles: List<RecentFileItem>,
    isLoading: Boolean,
    onAddFolder: () -> Unit,
    onScan: () -> Unit,
    onSyncMetadata: () -> Unit,
    onUpdateFolderReadingData: (SyncedFolder) -> Unit,
    onReconnectFolder: (SyncedFolder) -> Unit,
    onToggleLocalSync: (SyncedFolder, Boolean, Boolean) -> Unit,
    onEditFolderFilters: (SyncedFolder, Set<FileType>) -> Unit,
    onRemove: (SyncedFolder) -> Unit,
    cloudFolderSelection: CloudFolderSyncSelection? = null,
    cloudSyncEnabled: Boolean = false,
    isProUser: Boolean = false,
    onCloudFolderSettings: (() -> Unit)? = null,
    onIncomingCloudFolder: ((String) -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxSize()) {
        FolderSyncScreen(
            syncedFolders = folders,
            allRecentFiles = allRecentFiles,
            onAddFolderClick = onAddFolder,
            onRemoveFolderClick = onRemove,
            onFolderLocalSyncChange = onToggleLocalSync,
            onEditFolderFiltersClick = onEditFolderFilters,
            onScanNowClick = onScan,
            onSyncMetadataClick = onSyncMetadata,
            onUpdateFolderReadingData = onUpdateFolderReadingData,
            onReconnectFolder = onReconnectFolder,
            isLoading = isLoading,
            cloudFolderSelection = cloudFolderSelection,
            cloudSyncEnabled = cloudSyncEnabled,
            isProUser = isProUser,
            onCloudFolderSettingsClick = onCloudFolderSettings,
            onIncomingCloudFolderClick = onIncomingCloudFolder,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UnifiedLibraryControlsSheet(
    currentFilter: UnifiedLibraryFilter,
    currentSortOrder: SortOrder,
    canReorderGroups: Boolean,
    onFilterChanged: (UnifiedLibraryFilter) -> Unit,
    onSortChanged: (SortOrder) -> Unit,
    onAdvancedFiltersClick: () -> Unit,
    onGroupOrderClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    com.aryan.reader.shared.ui.SharedAndroidUnifiedLibraryControlsSheet(
        currentFilter = currentFilter,
        currentSortOrder = currentSortOrder,
        strings = com.aryan.reader.shared.ui.SharedAndroidUnifiedControlsStrings(
            title = stringResource(R.string.unified_library_sort_filter),
            readStatus = stringResource(R.string.filter_read_status),
            sort = stringResource(R.string.content_desc_sort),
            advancedFilters = stringResource(R.string.filter_library),
            filterLabels = UnifiedLibraryFilter.entries.associateWith { stringResource(it.labelRes) },
            sortLabels = SortOrder.entries.associateWith { stringResource(it.labelRes) },
        ),
        onFilterChanged = onFilterChanged,
        onSortChanged = onSortChanged,
        onAdvancedFilters = onAdvancedFiltersClick,
        onDismiss = onDismiss,
        additionalAction = {
            OutlinedButton(
                onClick = onGroupOrderClick,
                enabled = canReorderGroups,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.wenku_category_order)) }
        },
    )
}

@Composable
internal fun UnifiedLibraryTopBar(
    section: UnifiedLibrarySection,
    showSectionBack: Boolean,
    onNavigateBack: () -> Unit,
    onSettingsClick: () -> Unit,
    searchQuery: String?,
    onSearchQueryChange: (String) -> Unit,
    onOpenSearch: (() -> Unit)?,
    onCloseSearch: (() -> Unit)?,
    requestSearchFocus: Boolean,
    onSearchFocusHandled: () -> Unit,
    headingActions: @Composable () -> Unit,
    searchInsideFiles: Boolean = false,
    onSearchInsideFilesChange: ((Boolean) -> Unit)? = null,
) {
    val title = when (section) {
        UnifiedLibrarySection.HOME -> null
        UnifiedLibrarySection.AUDIOBOOKS -> stringResource(R.string.listen_title)
        UnifiedLibrarySection.FOLDERS -> stringResource(R.string.wenku_local_folders)
        UnifiedLibrarySection.SHELVES, UnifiedLibrarySection.CATALOGS -> null
    }
    com.aryan.reader.shared.ui.SharedAndroidUnifiedTopBar(
        title = title,
        showingShelf = showSectionBack,
        drawerDescription = "",
        backToShelvesDescription = stringResource(R.string.action_back),
        onMenu = null,
        onBackFromShelf = onNavigateBack,
        onAccount = onSettingsClick,
        accountAvatar = { Icon(Icons.Default.Settings, stringResource(R.string.settings)) },
        searchQuery = searchQuery,
        searchPlaceholder = stringResource(R.string.wenku_search_library),
        clearSearchDescription = stringResource(R.string.content_desc_clear_query),
        onSearchQueryChange = onSearchQueryChange,
        searchInsideFiles = searchInsideFiles,
        onSearchInsideFilesChange = onSearchInsideFilesChange,
        searchInsideFilesLabel = stringResource(R.string.unified_library_search_inside_files),
        libraryHeading = if (section == UnifiedLibrarySection.HOME) stringResource(R.string.wenku_library_title) else null,
        showAccount = section == UnifiedLibrarySection.HOME && onCloseSearch == null,
        onOpenSearch = onOpenSearch,
        onCloseSearch = onCloseSearch,
        openSearchDescription = stringResource(R.string.unified_library_search_books),
        closeSearchDescription = stringResource(R.string.content_desc_close_search),
        requestSearchFocus = requestSearchFocus,
        onSearchFocusHandled = onSearchFocusHandled,
        headingActions = headingActions,
    )
}

@Composable
internal fun LibraryHeadingActions(
    advancedFilterCount: Int,
    useListView: Boolean,
    onOpenControls: () -> Unit,
    onToggleListView: () -> Unit,
) {
    BadgedBox(badge = { if (advancedFilterCount > 0) Badge { Text(advancedFilterCount.toString()) } }) {
        IconButton(onClick = onOpenControls) {
            Icon(Icons.Default.FilterList, stringResource(R.string.wenku_filter_and_sort))
        }
    }
    IconButton(onClick = onToggleListView) {
        Icon(
            if (useListView) Icons.AutoMirrored.Filled.LibraryBooks else Icons.Default.FormatListNumbered,
            stringResource(if (useListView) R.string.unified_library_grid_view else R.string.unified_library_list_view),
        )
    }
}

@Composable
private fun LibraryReadingStatusButtons(
    filter: UnifiedLibraryFilter,
    onFilterChange: (UnifiedLibraryFilter) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .widthIn(max = 224.dp)
            .fillMaxWidth()
            .clip(LibrarySurfaceShape)
            .background(colors.surfaceContainerLow)
            .border(1.dp, colors.outlineVariant.copy(alpha = 0.60f), LibrarySurfaceShape)
            .selectableGroup()
            .testTag("UnifiedLibraryReadingStatus"),
    ) {
        UnifiedLibraryFilter.entries.forEach { option ->
            val selected = filter == option
            val label = stringResource(option.labelRes)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(LibrarySurfaceShape)
                    .selectable(
                        selected = selected,
                        role = Role.Tab,
                        onClick = { onFilterChange(option) },
                    )
                    .semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Box(
                        Modifier.matchParentSize()
                            .padding(3.dp)
                            .background(colors.primaryContainer, LibrarySurfaceShape),
                    )
                }
                Icon(
                    imageVector = when (option) {
                        UnifiedLibraryFilter.ALL -> Icons.AutoMirrored.Filled.LibraryBooks
                        UnifiedLibraryFilter.READING -> Icons.Default.Bookmark
                        UnifiedLibraryFilter.FINISHED -> Icons.Default.Check
                        UnifiedLibraryFilter.UNREAD -> Icons.Default.Book
                    },
                    contentDescription = null,
                    modifier = Modifier.size(21.dp),
                    tint = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun UnifiedLibraryHome(
    modifier: Modifier,
    books: List<RecentFileItem>,
    groupOrder: List<String>,
    filter: UnifiedLibraryFilter,
    query: String,
    sortOrder: SortOrder,
    advancedFilterCount: Int,
    useListView: Boolean,
    selectedBookIds: Set<String>,
    pinnedBookIds: Set<String>,
    downloadingBookIds: Set<String>,
    usePdfFileNameAsDisplayName: Boolean,
    onFilterChange: (UnifiedLibraryFilter) -> Unit,
    onControlsClick: () -> Unit,
    onAdvancedFiltersClick: () -> Unit,
    onListViewChange: (Boolean) -> Unit,
    onBookClick: (RecentFileItem) -> Unit,
    onBookLongClick: (RecentFileItem) -> Unit,
    openTabs: List<RecentFileItem>,
    tabsEnabled: Boolean,
    onCloseTab: (RecentFileItem) -> Unit,
    onCloseAllTabs: () -> Unit,
    widthSizeClass: WindowWidthSizeClass,
    isLibraryEmpty: Boolean = false,
    searchInsideFiles: Boolean = false,
    contentSearchResults: List<SharedGlobalSearchBookResult> = emptyList(),
    onContentHitClick: (RecentFileItem, SharedGlobalSearchHit, String) -> Unit = { _, _, _ -> },
) {
    // Content search replaces the grid: one row per matching book, its matched
    // text, and an expand affordance for the rest of that book's matches.
    val contentSearchActive = searchInsideFiles && query.isNotBlank()
    val context = LocalContext.current
    val bookGroups = remember(books, context, query.isBlank(), groupOrder) {
        if (query.isNotBlank()) emptyList() else {
            val grouped = books.groupBy { it.libraryBookGroup() }
            orderLibraryGroups(grouped.keys, groupOrder).map { group ->
                SharedAndroidLibraryGroup(group.id, group.label(context), libraryCategoryAccent(group.category), grouped.getValue(group))
            }
        }
    }
    com.aryan.reader.shared.ui.SharedAndroidUnifiedLibraryHome(
        books = books,
        continueReading = null,
        filter = filter,
        sortLabel = stringResource(sortOrder.labelRes),
        advancedFilterCount = advancedFilterCount,
        useListView = useListView,
        showControls = query.isBlank(),
        strings = com.aryan.reader.shared.ui.SharedAndroidUnifiedHomeStrings(
            noBooks = stringResource(when {
                contentSearchActive -> R.string.library_content_search_no_results
                isLibraryEmpty && query.isBlank() -> R.string.wenku_empty_library_title
                else -> R.string.wenku_no_matching_books
            }),
            emptyMessage = if (isLibraryEmpty && query.isBlank()) {
                stringResource(R.string.wenku_empty_library_details)
            } else null,
            filterBooks = stringResource(R.string.content_desc_filter),
            gridView = stringResource(R.string.unified_library_grid_view),
            listView = stringResource(R.string.unified_library_list_view),
            filterLabels = UnifiedLibraryFilter.entries.associateWith { stringResource(it.labelRes) },
        ),
        itemKey = { it.bookId },
        contentSearchActive = contentSearchActive,
        contentSearchResults = contentSearchResults,
        contentResultRow = { item, result, expanded, onToggleExpanded ->
            LibraryContentSearchResultRow(
                item = item,
                result = result,
                expanded = expanded,
                onToggleExpanded = onToggleExpanded,
                onClick = { onBookClick(item) },
                onLongClick = { onBookLongClick(item) },
                onHitClick = { hit -> onContentHitClick(item, hit, query) },
                usePdfFileNameAsDisplayName = usePdfFileNameAsDisplayName,
                showCover = !useListView,
                animateOpening = !useListView && selectedBookIds.isEmpty(),
            )
        },
        onFilterChange = onFilterChange,
        onControls = onControlsClick,
        onAdvancedFilters = onAdvancedFiltersClick,
        onListViewChange = onListViewChange,
        openTabsContent = {
            com.aryan.reader.shared.ui.SharedAndroidActiveTabsRow(
                openTabs = openTabs,
                tabsEnabled = tabsEnabled && query.isBlank() && openTabs.size > 1,
                activeTabsLabel = stringResource(R.string.active_tabs),
                closeAllTabsDescription = stringResource(R.string.close_all_tabs),
                closeTabDescription = stringResource(R.string.close_tab),
                itemKey = { it.bookId },
                itemTitle = { it.cardTitle(usePdfFileNameAsDisplayName) },
                onItemClick = onBookClick,
                onCloseTab = onCloseTab,
                onCloseAllTabs = onCloseAllTabs,
            )
        },
        continueCard = { _, _ -> },
        bookCard = { item ->
            Box(Modifier.fillMaxWidth()) {
                RecentFileCard(
                    item = item,
                    isSelected = item.bookId in selectedBookIds,
                    modifier = Modifier.widthIn(max = 136.dp).fillMaxWidth(),
                    onClick = { onBookClick(item) },
                    onLongClick = { onBookLongClick(item) },
                    isDownloading = item.bookId in downloadingBookIds,
                    usePdfFileNameAsDisplayName = usePdfFileNameAsDisplayName,
                    animateOpening = selectedBookIds.isEmpty(),
                    compact = true,
                )
            }
        },
        bookListItem = { item ->
            LibraryListItem(
                item = item,
                isSelected = item.bookId in selectedBookIds,
                isPinned = item.bookId in pinnedBookIds,
                onItemClick = { onBookClick(item) },
                onItemLongClick = { onBookLongClick(item) },
                isDownloading = item.bookId in downloadingBookIds,
                usePdfFileNameAsDisplayName = usePdfFileNameAsDisplayName,
            )
        },
        widthClass = when (widthSizeClass) {
            WindowWidthSizeClass.Compact -> com.aryan.reader.shared.ui.SharedAndroidHomeWidthClass.COMPACT
            WindowWidthSizeClass.Medium -> com.aryan.reader.shared.ui.SharedAndroidHomeWidthClass.MEDIUM
            else -> com.aryan.reader.shared.ui.SharedAndroidHomeWidthClass.EXPANDED
        },
        fixedBookColumns = 5,
        bookGridSpacing = when (widthSizeClass) {
            WindowWidthSizeClass.Compact -> DpSize(8.dp, 12.dp)
            WindowWidthSizeClass.Medium -> DpSize(14.dp, 16.dp)
            else -> DpSize(18.dp, 18.dp)
        },
        bookListSpacing = 8.dp,
        combineFilterAndSort = true,
        filterAndSortLabel = stringResource(R.string.wenku_filter_and_sort),
        bookGroups = bookGroups,
        density = com.aryan.reader.shared.ui.SharedAndroidHomeDensity.Standard.copy(
            filterRowTopGap = 8.dp,
            gridCardTopGap = 8.dp,
            gridContentTopPadding = 4.dp,
        ),
        statusControls = { LibraryReadingStatusButtons(filter, onFilterChange) },
        modifier = modifier,
    )
}

@Composable
internal fun LibraryGroupOrderDialog(
    groups: List<LibraryBookGroup>,
    onMove: (from: Int, to: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.wenku_category_order)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(groups.size, key = { groups[it].id }) { index ->
                    val label = groups[index].label(context)
                    ListItem(
                        headlineContent = { Text(label) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { onMove(index, index - 1) }, enabled = index > 0) {
                                    Icon(Icons.Default.KeyboardArrowUp, stringResource(R.string.wenku_category_move_up, label))
                                }
                                IconButton(onClick = { onMove(index, index + 1) }, enabled = index < groups.lastIndex) {
                                    Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.wenku_category_move_down, label))
                                }
                            }
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}

@Composable
internal fun LibraryCategoryDialog(
    currentCategoryId: String?,
    onChoose: (LibraryBookCategory?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.wenku_change_category)) },
        text = {
            val options = listOf(null) + LibraryBookCategory.entries
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(options, key = { it?.name ?: "automatic" }) { category ->
                    val label = stringResource(category?.labelRes ?: R.string.wenku_category_automatic)
                    ListItem(
                        headlineContent = { Text(label) },
                        leadingContent = { RadioButton(selected = category?.name == currentCategoryId, onClick = null) },
                        modifier = Modifier.clickable { onChoose(category) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
internal fun LibraryCloseAllTabsDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_close_all_tabs)) },
        text = { Text(stringResource(R.string.dialog_close_all_tabs_desc)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(R.string.action_close)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
