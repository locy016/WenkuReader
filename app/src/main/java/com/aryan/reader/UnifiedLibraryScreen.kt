package com.aryan.reader

import android.app.Activity
import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.aryan.reader.data.RecentFileItem
import com.aryan.reader.data.AppDatabase
import com.aryan.reader.data.AudiobookImporter
import com.aryan.reader.audiobook.AudiobookController
import com.aryan.reader.audiobook.ListenOverlayHost
import com.aryan.reader.shared.AnnotationExportFormat
import com.aryan.reader.shared.search.SharedGlobalSearchBookResult
import com.aryan.reader.shared.ui.SharedAnnotationExportFormatDialog
import com.aryan.reader.shared.ui.sharedAnnotationExportFormatOptions
import com.aryan.reader.shared.ui.activeFilterCount
import com.aryan.reader.shared.ui.matchesMobileUnifiedLibraryFilter
import com.aryan.reader.shared.ui.mobileUnifiedContinueReadingBook
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

internal typealias UnifiedLibrarySection = com.aryan.reader.shared.ui.MobileUnifiedLibrarySection
internal typealias UnifiedLibraryFilter = com.aryan.reader.shared.ui.MobileUnifiedLibraryFilter

@androidx.annotation.OptIn(UnstableApi::class)
internal fun shouldAutoStartTtsAudiobook(
    requestedBookId: String?,
    playback: com.aryan.reader.tts.TtsPlaybackManager.TtsState
): Boolean = requestedBookId == null ||
    playback.playbackSource != "AUDIOBOOK_TTS" ||
    playback.bookId != requestedBookId

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun UnifiedLibraryScreen(
    viewModel: MainViewModel,
    navController: NavHostController,
    widthSizeClass: WindowWidthSizeClass,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val groupOrder by viewModel.libraryGroupOrder.collectAsStateWithLifecycle()
    val orderedGroups = remember(uiState.rawLibraryFiles, groupOrder) {
        orderLibraryGroups(uiState.rawLibraryFiles.map { it.libraryBookGroup() }.toSet(), groupOrder)
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audiobookImporter = remember(context) { AudiobookImporter(context.applicationContext) }
    val importedAudiobooks by remember(context) { AppDatabase.getDatabase(context).audiobookDao().observeAll() }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val bookTtsProgress by remember(context) { AppDatabase.getDatabase(context).bookTtsListeningProgressDao().observeAll() }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val globalAudiobookController = remember(context) { AudiobookController(context) }
    val importedPlayback by globalAudiobookController.state.collectAsStateWithLifecycle()
    val ttsPlayback by viewModel.ttsController.ttsState.collectAsStateWithLifecycle()
    // Publisher narration outlives the player sheet by design (ListenOverlayHost
    // is process-wide), so the mini player observes it directly: collapsing the
    // sheet must leave a way back, exactly like the TTS service state above.
    val listenOverlayHost = remember(context) { ListenOverlayHost.get(context.applicationContext) }
    val overlayPlayback by listenOverlayHost.playback.collectAsStateWithLifecycle()
    val overlayBook by listenOverlayHost.book.collectAsStateWithLifecycle()
    val overlayChapter by listenOverlayHost.chapterIndex.collectAsStateWithLifecycle()
    val overlayProgress by listenOverlayHost.progress.collectAsStateWithLifecycle()
    LaunchedEffect(importedAudiobooks.isNotEmpty()) {
        if (importedAudiobooks.isNotEmpty()) globalAudiobookController.connectSession()
    }
    DisposableEffect(globalAudiobookController) { onDispose(globalAudiobookController::release) }
    val activeListeningItemId = when {
        ttsPlayback.playbackSource == "AUDIOBOOK_TTS" ->
            ttsPlayback.bookId?.let { "tts-$it" }

        importedPlayback.bookId != null ->
            importedPlayback.bookId

        else -> null
    }
    var filter by rememberSaveable { mutableStateOf(UnifiedLibraryFilter.ALL) }
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var requestSearchFocus by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    // Filtering + sorting is O(n log n) on Main; debounce keystrokes so each
    // character doesn't pay a full pass. The field itself stays instant —
    // only the list lags up to 300ms behind fast typing.
    var debouncedQuery by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(query) {
        delay(300)
        debouncedQuery = query
    }
    // Content-index search follows the same debounce; the engine publishes
    // matching book ids into uiState for the union in visibleBooks.
    LaunchedEffect(debouncedQuery, uiState.libraryState.searchInsideFiles) {
        viewModel.searchLibraryContent(debouncedQuery)
    }
    var showLibraryControls by rememberSaveable { mutableStateOf(false) }
    var showGroupOrder by rememberSaveable { mutableStateOf(false) }
    var showAdvancedFilters by rememberSaveable { mutableStateOf(false) }
    var showPermanentDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    var showCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var showSelectedBookInfo by rememberSaveable { mutableStateOf(false) }
    var selectedBookForInfo by remember { mutableStateOf<RecentFileItem?>(null) }
    var pendingSaveOriginalItem by remember { mutableStateOf<RecentFileItem?>(null) }
    var pendingAnnotationExportText by remember { mutableStateOf<String?>(null) }
    var showAnnotationExportFormatDialogFor by remember { mutableStateOf<RecentFileItem?>(null) }
    var showAudiobookAddSheet by rememberSaveable { mutableStateOf(false) }
    var showTtsBookPicker by rememberSaveable {
        mutableStateOf(false)
    }
    var audiobookPlayerItem by remember { mutableStateOf<AudiobookUiItem?>(null) }
    var showCloseAllTabsDialog by remember { mutableStateOf(false) }
    val canUseCloudFolderSync = uiState.canUseCloudFolderSync()
    val canPullToSync = com.aryan.reader.shared.ui.canPullToSyncLibrary(
        cloudSyncEnabled = uiState.isSyncEnabled,
        foldersWithLocalSyncEnabled = uiState.syncedFolders.map { it.localSyncEnabled },
    )
    var cloudFolderSelection by remember(uiState.currentUser?.uid) {
        mutableStateOf(viewModel.cloudFolderSyncSelection())
    }

    LaunchedEffect(uiState.currentUser?.uid) {
        cloudFolderSelection = viewModel.cloudFolderSyncSelection()
    }
    LaunchedEffect(Unit) {
        CloudFolderSyncEvents.stateChanged.collect {
            cloudFolderSelection = viewModel.cloudFolderSyncSelection()
        }
    }

    // Drive consent is requested by the ViewModel but the intent must be launched by
    // whichever host screen is mounted, so this observer follows Home into Library Beta.
    val drivePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onDrivePermissionResult(result.data)
        } else {
            Timber.w("Google Sign In for Drive failed with result code: ${result.resultCode}")
            viewModel.onDrivePermissionFlowCancelled()
        }
    }
    LaunchedEffect(uiState.isRequestingDrivePermission) {
        if (uiState.isRequestingDrivePermission) {
            drivePermissionLauncher.launch(viewModel.getDriveSignInIntent(context))
        }
    }

    val filePicker = rememberFilePickerLauncher(viewModel::onFilesSelected)
    var reconnectFolderUri by rememberSaveable { mutableStateOf<String?>(null) }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val reconnecting = reconnectFolderUri
        reconnectFolderUri = null
        if (uri != null) {
            if (reconnecting == null) viewModel.addSyncedFolder(uri)
            else viewModel.reconnectSyncedFolder(reconnecting, uri)
        }
    }
    val audiobookPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            scope.launch {
                audiobookImporter.import(it)
                    .onSuccess { imported -> viewModel.showBanner(context.getString(R.string.audiobooks_imported, imported.book.title ?: imported.book.displayName)) }
                    .onFailure { error -> viewModel.showBanner(error.message ?: context.getString(R.string.audiobooks_import_failed), isError = true) }
            }
        }
    }
    val audiobookMultiplePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) scope.launch {
            val results = audiobookImporter.importAll(uris)
            val imported = results.count { it.isSuccess }
            val failed = results.size - imported
            viewModel.showBanner("Imported $imported audiobook${if (imported == 1) "" else "s"}${if (failed > 0) "; $failed failed" else ""}", isError = imported == 0)
        }
    }
    val audiobookFolderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { folderUri -> scope.launch {
            val results = audiobookImporter.importFolder(folderUri)
            val imported = results.count { it.isSuccess }
            val failed = results.size - imported
            viewModel.showBanner("Imported $imported audiobook${if (imported == 1) "" else "s"}${if (failed > 0) "; $failed failed" else ""}", isError = imported == 0)
        } }
    }
    val saveOriginalLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { destination ->
        val item = pendingSaveOriginalItem
        pendingSaveOriginalItem = null
        if (destination != null && item?.uriString != null) {
            viewModel.saveOriginalFile(item.uriString.toUri(), destination)
        }
    }
    val saveMarkdownAnnotationsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(AnnotationExportFormat.MARKDOWN.mimeType)
    ) { destination ->
        val contents = pendingAnnotationExportText
        pendingAnnotationExportText = null
        if (destination != null && contents != null) viewModel.saveAnnotationExport(contents, destination)
    }
    val saveTextAnnotationsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(AnnotationExportFormat.TEXT.mimeType)
    ) { destination ->
        val contents = pendingAnnotationExportText
        pendingAnnotationExportText = null
        if (destination != null && contents != null) viewModel.saveAnnotationExport(contents, destination)
    }
    val saveJsonAnnotationsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(AnnotationExportFormat.JSON.mimeType)
    ) { destination ->
        val contents = pendingAnnotationExportText
        pendingAnnotationExportText = null
        if (destination != null && contents != null) viewModel.saveAnnotationExport(contents, destination)
    }
    val saveCsvAnnotationsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(AnnotationExportFormat.CSV.mimeType)
    ) { destination ->
        val contents = pendingAnnotationExportText
        pendingAnnotationExportText = null
        if (destination != null && contents != null) viewModel.saveAnnotationExport(contents, destination)
    }

    fun launchDocumentPicker(onUnavailable: () -> Unit = {}, launch: () -> Unit) {
        try {
            launch()
        } catch (_: ActivityNotFoundException) {
            onUnavailable()
            viewModel.showBanner(context.getString(R.string.document_picker_unavailable), isError = true)
        }
    }

    fun exportAnnotations(item: RecentFileItem, format: AnnotationExportFormat) {
        viewModel.prepareAnnotationExport(item, format) { prepared ->
            pendingAnnotationExportText = prepared.contents
            when (format) {
                AnnotationExportFormat.MARKDOWN -> launchDocumentPicker(
                    onUnavailable = { pendingAnnotationExportText = null }
                ) { saveMarkdownAnnotationsLauncher.launch(prepared.fileName) }
                AnnotationExportFormat.TEXT -> launchDocumentPicker(
                    onUnavailable = { pendingAnnotationExportText = null }
                ) { saveTextAnnotationsLauncher.launch(prepared.fileName) }
                AnnotationExportFormat.JSON -> launchDocumentPicker(
                    onUnavailable = { pendingAnnotationExportText = null }
                ) { saveJsonAnnotationsLauncher.launch(prepared.fileName) }
                AnnotationExportFormat.CSV -> launchDocumentPicker(
                    onUnavailable = { pendingAnnotationExportText = null }
                ) { saveCsvAnnotationsLauncher.launch(prepared.fileName) }
            }
        }
    }

    fun shareOriginal(item: RecentFileItem) {
        val sourceUri = item.uriString?.toUri() ?: return
        if (!item.canExportOriginalFile()) return
        scope.launch {
            viewModel.shareOriginalFile(
                activityContext = context,
                sourceUri = sourceUri,
                fileType = item.type,
                filename = item.suggestedOriginalFileName()
            )
        }
    }
    val visibleBooks = remember(
        uiState.rawLibraryFiles,
        uiState.libraryFilters,
        uiState.sortOrder,
        filter,
        debouncedQuery,
        uiState.libraryState.searchInsideFiles,
        uiState.libraryState.contentSearchResults,
    ) {
        val searching = debouncedQuery.isNotBlank()
        filterUnifiedLibraryBooks(
            if (searching) uiState.rawLibraryFiles else applyLibraryFilters(uiState.rawLibraryFiles, uiState.libraryFilters),
            if (searching) UnifiedLibraryFilter.ALL else filter,
            debouncedQuery,
            uiState.libraryState.searchInsideFiles,
            uiState.libraryState.contentSearchResults,
            uiState.sortOrder,
        )
    }
    val section = androidLibrarySection(uiState.unifiedLibrarySection)
    // Keyed on the list identity: recomputes exactly when the library changes
    // (including position writes, which replace the item), but skips the O(n)
    // scan on unrelated recompositions (scroll or keystrokes).
    // Uses the unfiltered collection so an active filter never hides resume.
    val continueReading = remember(uiState.rawLibraryFiles) {
        findContinueReadingBook(uiState.rawLibraryFiles)
    }
    val advancedFilterCount = uiState.libraryFilters.activeFilterCount()
    val selectedItems = uiState.contextualActionItems

    val focusManager = LocalFocusManager.current
    val isManagementPage = section != UnifiedLibrarySection.HOME

    fun closeSearch() {
        focusManager.clearFocus()
        requestSearchFocus = false
        isSearchExpanded = false
        query = ""
        debouncedQuery = ""
    }

    fun selectSection(destination: UnifiedLibrarySection) {
        if (section == destination) return
        closeSearch()
        viewModel.clearContextualAction()
        viewModel.setUnifiedLibrarySection(destination.persistedValue)
    }

    val importFiles = {
        launchDocumentPicker {
            filePicker.launch(if (uiState.useStrictFileFilter) MainViewModel.SUPPORTED_MIME_TYPES else arrayOf("*/*"))
        }
    }
    fun openLibraryBook(item: RecentFileItem) {
        if (item.type == FileType.AUDIOBOOK) {
            importedAudiobooks.firstOrNull { it.bookId == item.bookId }?.let { audiobookPlayerItem = it.toUiItem() }
        } else {
            viewModel.onRecentFileClicked(item)
        }
    }
    val isEmptyLibrary = uiState.rawLibraryFiles.isEmpty()
    // Keyboard visibility as state: back dismisses the IME first (keeping
    // the query + results) so there is a way to hide the keyboard without
    // leaving search. Reads the IME inset directly so no shared-API change
    // is needed; dialogs still win because they register back later (LIFO).
    val isKeyboardOpen = WindowInsets.isImeVisible
    val backAction = nextLibraryBackAction(
        hasSelection = selectedItems.isNotEmpty(),
        isKeyboardOpen = isKeyboardOpen,
        isSearchActive = section == UnifiedLibrarySection.HOME && isSearchExpanded,
        isManagementPage = isManagementPage,
    )
    BackHandler(enabled = backAction != LibraryBackAction.NONE) {
        when (backAction) {
            LibraryBackAction.CLEAR_SELECTION -> viewModel.clearContextualAction()
            LibraryBackAction.HIDE_KEYBOARD -> focusManager.clearFocus()
            LibraryBackAction.CLEAR_SEARCH -> closeSearch()
            LibraryBackAction.RETURN_TO_LIBRARY -> selectSection(UnifiedLibrarySection.HOME)
            LibraryBackAction.NONE -> Unit
        }
    }

    com.aryan.reader.shared.ui.SharedAndroidUnifiedScaffold(
        section = section,
        importDescription = stringResource(R.string.unified_library_import),
        addAudiobookDescription = stringResource(R.string.listen_add),
        onImport = importFiles,
        homeFabPosition = FabPosition.Center,
        homeFloatingActions = {
            LibraryFloatingActions(
                onImport = importFiles,
                onContinueReading = continueReading?.let { book -> { openLibraryBook(book) } },
            )
        },
        showFloatingActionButton = selectedItems.isEmpty() && !isKeyboardOpen,
        onAddAudiobook = { showAudiobookAddSheet = true },
        canPullToSync = canPullToSync,
        isRefreshing = uiState.isRefreshing,
        onRefresh = viewModel::refreshLibrary,
        bottomBar = {
            Column {
                val activeTtsBook = ttsPlayback.bookId
                    ?.takeIf { ttsPlayback.playbackSource == "AUDIOBOOK_TTS" }
                    ?.let { id -> uiState.rawLibraryFiles.firstOrNull { it.bookId == id } }
                val activeImported = importedPlayback.bookId
                    ?.let { id -> importedAudiobooks.firstOrNull { it.bookId == id } }
                // Narration continues with no TTS service session, so it gets
                // the first slot: the other branches are necessarily idle then.
                val activeOverlayBook = overlayBook
                    ?.takeIf { overlayPlayback.hasBook }
                    ?.let { listenBook ->
                        uiState.rawLibraryFiles.firstOrNull { it.bookId == listenBook.bookId }
                            ?.toTtsAudiobookUiItem(
                                overlayProgress?.takeIf { it.bookId == listenBook.bookId }
                                    ?: bookTtsProgress.firstOrNull { it.bookId == listenBook.bookId }
                            )
                            ?.copy(
                                chapter = "Narration · Chapter ${(overlayChapter.takeIf { it >= 0 } ?: 0) + 1}",
                                narrator = listenBook.mediaOverlay.narrator ?: "Publisher narration",
                            )
                    }
                when {
                    activeOverlayBook != null -> {
                        AudiobookMiniPlayer(
                            item = activeOverlayBook,
                            isPlaying = overlayPlayback.isPlaying,
                            progress = activeOverlayBook.progress,
                            onTogglePlay = { listenOverlayHost.toggle() },
                            onExpand = {
                                audiobookPlayerItem = activeOverlayBook
                            },
                            onStop = { listenOverlayHost.stop() }
                        )
                    }
                    activeTtsBook != null -> {
                        val item = activeTtsBook.toTtsAudiobookUiItem(bookTtsProgress.firstOrNull { it.bookId == activeTtsBook.bookId })
                            .copy(chapter = ttsPlayback.chapterTitle ?: "Listening with TTS")
                        AudiobookMiniPlayer(
                            item = item,
                            isPlaying = ttsPlayback.isPlaying,
                            progress = item.progress,
                            onTogglePlay = {
                                if (
                                    ttsPlayback.isPlaying ||
                                    ttsPlayback.isLoading
                                ) {
                                    viewModel.ttsController.pause()
                                } else {
                                    viewModel.ttsController.resume()
                                }
                            },
                            onExpand = {
                                audiobookPlayerItem = item
                            },
                            onStop = viewModel.ttsController::stop
                        )
                    }
                    activeImported != null -> {
                        val item = activeImported.toUiItem()
                        val liveProgress = if (importedPlayback.durationMs > 0L) {
                            importedPlayback.positionMs.toFloat() /
                                    importedPlayback.durationMs.toFloat()
                        } else {
                            item.progress
                        }
                        AudiobookMiniPlayer(
                            item = item,
                            isPlaying = importedPlayback.isPlaying,
                            progress = liveProgress,
                            onTogglePlay = {
                                globalAudiobookController.togglePlay(
                                    onBeforePlay = {
                                        viewModel.ttsController.stop()
                                        listenOverlayHost.stop()
                                    }
                                )
                            },
                            onExpand = {
                                audiobookPlayerItem = item
                            },
                            onStop = globalAudiobookController::stop
                        )
                    }
                }
            }
        },
        topBar = {
            if (selectedItems.isNotEmpty()) {
                ContextualTopAppBar(
                    selectedItemCount = selectedItems.size,
                    onNavIconClick = viewModel::clearContextualAction,
                    onInfoClick = selectedItems.singleOrNull()?.let { item ->
                        {
                            selectedBookForInfo = item
                            showSelectedBookInfo = true
                        }
                    },
                    onTagClick = { viewModel.openTagSelection(selectedItems.map { it.bookId }.toSet()) },
                    onPinClick = { viewModel.togglePinForContextualItems(isHome = false) },
                    onSelectAllClick = { viewModel.selectAllLibraryFiles(visibleBooks) },
                    onSaveClick = selectedItems.singleOrNull()
                        ?.takeIf { it.canExportOriginalFile() }
                        ?.let { item ->
                            {
                                pendingSaveOriginalItem = item
                                launchDocumentPicker(
                                    onUnavailable = { pendingSaveOriginalItem = null }
                                ) { saveOriginalLauncher.launch(item.suggestedOriginalFileName()) }
                            }
                        },
                    onShareClick = selectedItems.singleOrNull()
                        ?.takeIf { it.canExportOriginalFile() }
                        ?.let { item -> { shareOriginal(item) } },
                    onExportAnnotationsClick = selectedItems.singleOrNull()
                        ?.let { item -> { showAnnotationExportFormatDialogFor = item } },
                    onDeleteClick = viewModel::removeContextualBooksFromLibrary,
                    onDeleteOriginalClick = { showPermanentDeleteConfirmation = true },
                    onCategoryClick = { showCategoryDialog = true },
                    compactSelectionActions = true,
                    overflowDeleteLabelRes = R.string.wenku_remove_from_library,
                    onClearSelectionClick = viewModel::clearContextualAction
                )
            } else {
                UnifiedLibraryTopBar(
                    section = section,
                    showSectionBack = isManagementPage,
                    onNavigateBack = { selectSection(UnifiedLibrarySection.HOME) },
                    onSettingsClick = {
                        navController.navigateIfReady(com.aryan.reader.shared.ui.SharedMobileAppDestination.SETTINGS)
                    },
                    searchQuery = query.takeIf { section == UnifiedLibrarySection.HOME && isSearchExpanded },
                    onSearchQueryChange = { query = it },
                    onOpenSearch = if (section == UnifiedLibrarySection.HOME && !isSearchExpanded) {
                        {
                            isSearchExpanded = true
                            requestSearchFocus = true
                        }
                    } else null,
                    onCloseSearch = if (section == UnifiedLibrarySection.HOME && isSearchExpanded) ::closeSearch else null,
                    requestSearchFocus = requestSearchFocus,
                    onSearchFocusHandled = { requestSearchFocus = false },
                    headingActions = {
                        if (!isEmptyLibrary) {
                            LibraryHeadingActions(
                                advancedFilterCount = advancedFilterCount,
                                useListView = uiState.unifiedLibraryListView,
                                onOpenControls = { showLibraryControls = true },
                                onToggleListView = { viewModel.setUnifiedLibraryListView(!uiState.unifiedLibraryListView) },
                            )
                        }
                    },
                    searchInsideFiles = uiState.libraryState.searchInsideFiles,
                    onSearchInsideFilesChange = viewModel::setSearchInsideFiles,
                )
            }
        },
        sectionContent = { displayedSection, padding ->
            when (displayedSection) {
                UnifiedLibrarySection.HOME -> if (isEmptyLibrary && query.isBlank()) {
                    EmptyState(
                        title = stringResource(R.string.your_library_empty),
                        message = stringResource(R.string.wenku_empty_library_details),
                        onSelectFileClick = importFiles,
                        primaryButtonText = null,
                        secondaryButtonText = stringResource(R.string.wenku_local_folders),
                        onSecondaryClick = { selectSection(UnifiedLibrarySection.FOLDERS) },
                        modifier = Modifier.fillMaxSize().padding(padding),
                    )
                } else UnifiedLibraryHome(
                    modifier = Modifier.padding(padding),
                    books = visibleBooks,
                    groupOrder = groupOrder,
                    filter = filter,
                    query = debouncedQuery,
                    sortOrder = uiState.sortOrder,
                    advancedFilterCount = advancedFilterCount,
                    useListView = uiState.unifiedLibraryListView,
                    selectedBookIds = uiState.contextualActionItems.mapTo(mutableSetOf()) { it.bookId },
                    pinnedBookIds = uiState.pinnedLibraryBookIds,
                    downloadingBookIds = uiState.downloadingBookIds,
                    usePdfFileNameAsDisplayName = uiState.usePdfFileNameAsDisplayName,
                    onFilterChange = { filter = it },
                    onControlsClick = { showLibraryControls = true },
                    onAdvancedFiltersClick = { showAdvancedFilters = true },
                    onListViewChange = viewModel::setUnifiedLibraryListView,
                    widthSizeClass = widthSizeClass,
                    isLibraryEmpty = uiState.rawLibraryFiles.isEmpty(),
                    onBookClick = ::openLibraryBook,
                    onBookLongClick = viewModel::onRecentItemLongPress,
                    onContentHitClick = { item, hit, hitQuery ->
                        viewModel.openLibrarySearchHit(item, hit, hitQuery)
                    },
                    openTabs = uiState.openTabs,
                    tabsEnabled = uiState.isTabsEnabled,
                    onCloseTab = { viewModel.closeTab(it.bookId) },
                    onCloseAllTabs = { showCloseAllTabsDialog = true },
                    searchInsideFiles = uiState.libraryState.searchInsideFiles,
                    contentSearchResults = uiState.libraryState.contentSearchResults,
                )
                UnifiedLibrarySection.AUDIOBOOKS -> AudiobooksLibrarySection(
                    modifier = Modifier.padding(padding),
                    audiobooks = importedAudiobooks,
                    libraryBooks = uiState.rawLibraryFiles,
                    ttsProgress = bookTtsProgress,
                    playback = importedPlayback,
                    ttsPlayback = com.aryan.reader.shared.SharedBookTtsListenState(
                        connected = ttsPlayback.playbackSource == "AUDIOBOOK_TTS",
                        bookId = ttsPlayback.bookId,
                        isPlaying = ttsPlayback.isPlaying,
                        isLoading = ttsPlayback.isLoading,
                    ),
                    activeItemId = activeListeningItemId,
                    onAudiobookClick = { item ->
                        val shouldStart = item.isTts &&
                                shouldAutoStartTtsAudiobook(item.sourceBook?.bookId, ttsPlayback)

                        if (shouldStart) {
                            viewModel.ttsController.connect()
                        }

                        audiobookPlayerItem =
                            if (item.isTts && !shouldStart) item.copy(autoStart = false)
                            else item
                    },
                    onListenWithTtsClick = { book ->
                        // Card tap only opens the player. Playback starts from
                        // a play button (row or sheet), never from opening.
                        audiobookPlayerItem = book
                            .toTtsAudiobookUiItem(
                                bookTtsProgress.firstOrNull { it.bookId == book.bookId }
                            )
                            .copy(autoStart = false)
                    },
                    onPlayTtsClick = { book ->
                        viewModel.ttsController.connect()
                        audiobookPlayerItem = book
                            .toTtsAudiobookUiItem(
                                bookTtsProgress.firstOrNull { it.bookId == book.bookId }
                            )
                            .copy(autoStart = true)
                    },
                    onPlayAudiobookClick = { item ->
                        audiobookPlayerItem = item.copy(autoStart = true)
                    },
                    onAddAudiobookClick = {
                        showAudiobookAddSheet = true
                    }
                )
                UnifiedLibrarySection.FOLDERS -> UnifiedFoldersSection(
                    modifier = Modifier.padding(padding),
                    folders = uiState.syncedFolders,
                    allRecentFiles = uiState.rawLibraryFiles,
                    isLoading = uiState.isLoading,
                    onAddFolder = { launchDocumentPicker { folderPicker.launch(null) } },
                    onScan = viewModel::scanSyncedFolder,
                    onSyncMetadata = viewModel::syncFolderMetadata,
                    onUpdateFolderReadingData = viewModel::updateLocalFolderReadingData,
                    onReconnectFolder = { folder ->
                        reconnectFolderUri = folder.uriString
                        launchDocumentPicker(onUnavailable = { reconnectFolderUri = null }) {
                            folderPicker.launch(folder.uriString.toUri())
                        }
                    },
                    onToggleLocalSync = viewModel::setFolderLocalSyncEnabled,
                    onEditFolderFilters = viewModel::updateFolderFilters,
                    onRemove = viewModel::removeSyncedFolder,
                    cloudFolderSelection = cloudFolderSelection.takeIf { canUseCloudFolderSync },
                    cloudSyncEnabled = uiState.isSyncEnabled,
                    isProUser = canUseCloudFolderSync,
                    onCloudFolderSettings = if (canUseCloudFolderSync) {
                        {
                            navController.navigateIfReady(com.aryan.reader.shared.ui.SharedMobileAppDestination.FOLDER_SYNC_SETTINGS)
                        }
                    } else null,
                    onIncomingCloudFolder = if (canUseCloudFolderSync) {
                        { rootId -> viewModel.showIncomingCloudFolderPrompt(rootId) }
                    } else null,
                )
                UnifiedLibrarySection.SHELVES, UnifiedLibrarySection.CATALOGS -> Unit
            }
        },
    )

    if (showLibraryControls) {
        UnifiedLibraryControlsSheet(
            currentFilter = filter,
            currentSortOrder = uiState.sortOrder,
            canReorderGroups = orderedGroups.size > 1,
            onFilterChanged = { filter = it },
            onSortChanged = viewModel::setSortOrder,
            onAdvancedFiltersClick = {
                showLibraryControls = false
                showAdvancedFilters = true
            },
            onGroupOrderClick = {
                showLibraryControls = false
                showGroupOrder = true
            },
            onDismiss = { showLibraryControls = false }
        )
    }
    if (showGroupOrder) {
        LibraryGroupOrderDialog(
            groups = orderedGroups,
            onMove = { from, to -> viewModel.moveLibraryGroup(orderedGroups, from, to) },
            onDismiss = { showGroupOrder = false },
        )
    }
    if (showAudiobookAddSheet) {
        AudiobookAddSheet(
            onChooseFile = {
                showAudiobookAddSheet = false
                launchDocumentPicker {
                    audiobookPicker.launch(
                        arrayOf(
                            "audio/*",
                            "application/octet-stream"
                        )
                    )
                }
            },
            onChooseMultiple = {
                showAudiobookAddSheet = false
                launchDocumentPicker {
                    audiobookMultiplePicker.launch(
                        arrayOf(
                            "audio/*",
                            "application/octet-stream"
                        )
                    )
                }
            },
            onChooseFolder = {
                showAudiobookAddSheet = false
                launchDocumentPicker { audiobookFolderPicker.launch(null) }
            },
            onChooseTtsBook = {
                showAudiobookAddSheet = false
                showTtsBookPicker = true
            },
            onDismiss = {
                showAudiobookAddSheet = false
            }
        )
    }
    if (showTtsBookPicker) {
        TtsBookPickerSheet(
            books = uiState.rawLibraryFiles,
            onBookSelected = { book ->
                showTtsBookPicker = false

                val shouldStart =
                    shouldAutoStartTtsAudiobook(
                        book.bookId,
                        ttsPlayback
                    )

                if (shouldStart) {
                    viewModel.ttsController.connect()
                }

                audiobookPlayerItem =
                    book.toTtsAudiobookUiItem(
                        bookTtsProgress.firstOrNull {
                            it.bookId == book.bookId
                        }
                    ).copy(
                        autoStart = shouldStart
                    )
            },
            onDismiss = {
                showTtsBookPicker = false
            }
        )
    }
    audiobookPlayerItem?.let { item ->
        AudiobookPlayerSheet(
            item = item,
            onBeforePlay = {
                when (
                    com.aryan.reader.shared.sharedListeningHandoff(
                        com.aryan.reader.shared.SharedListeningTarget.IMPORTED_AUDIOBOOK,
                    )
                ) {
                    // An imported audiobook wins over generated listening of
                    // either kind: TTS synthesis and publisher narration both
                    // stop, so two real-audio sources never play at once.
                    com.aryan.reader.shared.SharedListeningHandoff.STOP_TTS -> {
                        viewModel.ttsController.stop()
                        listenOverlayHost.stop()
                    }
                    com.aryan.reader.shared.SharedListeningHandoff.STOP_AUDIOBOOK -> Unit
                }
            },
            onDismiss = { audiobookPlayerItem = null },
            getAuthToken = viewModel::getAuthToken,
        )
    }
    if (showAdvancedFilters) {
        LibraryFilterSheet(
            filters = uiState.libraryFilters,
            allTags = uiState.allTags,
            syncedFolders = uiState.syncedFolders,
            onApply = { filters ->
                filter = filters.readStatus.toUnifiedLibraryFilter()
                viewModel.updateLibraryFilters(filters)
            },
            onDismiss = { showAdvancedFilters = false }
        )
    }
    if (showCategoryDialog && selectedItems.isNotEmpty()) {
        LibraryCategoryDialog(
            currentCategoryId = selectedItems.map { it.libraryCategoryId }.distinct().singleOrNull(),
            onChoose = { category ->
                showCategoryDialog = false
                viewModel.setContextualLibraryCategory(category)
            },
            onDismiss = { showCategoryDialog = false },
        )
    }
    if (showPermanentDeleteConfirmation) {
        DeleteConfirmationDialog(
            count = selectedItems.size,
            onConfirm = {
                viewModel.deleteContextualItemsPermanently()
                showPermanentDeleteConfirmation = false
            },
            onDismiss = { showPermanentDeleteConfirmation = false },
            isPermanentDelete = true,
            containsFolderItems = selectedItems.any { it.sourceFolderUri != null },
            affectedNames = selectedItems.take(5).joinToString("\n") { it.displayName },
        )
    }
    if (showCloseAllTabsDialog) {
        LibraryCloseAllTabsDialog(
            onConfirm = {
                viewModel.closeAllTabs()
                showCloseAllTabsDialog = false
            },
            onDismiss = { showCloseAllTabsDialog = false }
        )
    }
    showAnnotationExportFormatDialogFor?.let { item ->
        SharedAnnotationExportFormatDialog(
            title = stringResource(R.string.dialog_export_annotations_title),
            cancelLabel = stringResource(R.string.action_cancel),
            options = sharedAnnotationExportFormatOptions(
                markdownLabel = stringResource(R.string.export_annotations_markdown),
                markdownDescription = stringResource(R.string.export_annotations_markdown_description),
                textLabel = stringResource(R.string.export_annotations_text),
                textDescription = stringResource(R.string.export_annotations_text_description),
                jsonLabel = stringResource(R.string.export_annotations_json),
                jsonDescription = stringResource(R.string.export_annotations_json_description),
                csvLabel = stringResource(R.string.export_annotations_csv),
                csvDescription = stringResource(R.string.export_annotations_csv_description)
            ),
            onDismiss = { showAnnotationExportFormatDialogFor = null },
            onExport = { format ->
                showAnnotationExportFormatDialogFor = null
                exportAnnotations(item, format)
            }
        )
    }
    HydratedFileInfoDialog(
        item = selectedBookForInfo,
        isVisible = showSelectedBookInfo,
        uiState = uiState,
        viewModel = viewModel,
        onDismiss = {
            showSelectedBookInfo = false
            selectedBookForInfo = null
        },
        onOpenTags = { bookId -> viewModel.openTagSelection(setOf(bookId)) }
    )
    CustomTopBanner(bannerMessage = uiState.bannerMessage)
}

internal val UnifiedLibraryFilter.labelRes: Int
    get() = when (this) {
        UnifiedLibraryFilter.ALL -> R.string.unified_library_all
        UnifiedLibraryFilter.READING -> R.string.unified_library_reading
        UnifiedLibraryFilter.FINISHED -> R.string.unified_library_finished
        UnifiedLibraryFilter.UNREAD -> R.string.unified_library_unread
    }

internal fun ReadStatusFilter.toUnifiedLibraryFilter(): UnifiedLibraryFilter = when (this) {
    ReadStatusFilter.ALL -> UnifiedLibraryFilter.ALL
    ReadStatusFilter.UNREAD -> UnifiedLibraryFilter.UNREAD
    ReadStatusFilter.IN_PROGRESS -> UnifiedLibraryFilter.READING
    ReadStatusFilter.COMPLETED -> UnifiedLibraryFilter.FINISHED
}

internal fun findContinueReadingBook(books: List<RecentFileItem>): RecentFileItem? {
    if (books.isEmpty()) return null
    val mapper = SharedProjectionBookItemCache(maxEntries = books.size)
    val picked = mobileUnifiedContinueReadingBook(mapper.map(books)) ?: return null
    return books.firstOrNull { it.bookId == picked.id }
}

/** Innermost-first system-back priority for the unified library. */
internal enum class LibraryBackAction {
    CLEAR_SELECTION,
    HIDE_KEYBOARD,
    CLEAR_SEARCH,
    RETURN_TO_LIBRARY,
    NONE,
}

internal fun nextLibraryBackAction(
    hasSelection: Boolean,
    isSearchActive: Boolean,
    isManagementPage: Boolean,
    isKeyboardOpen: Boolean = false,
): LibraryBackAction = when {
    hasSelection -> LibraryBackAction.CLEAR_SELECTION
    isKeyboardOpen -> LibraryBackAction.HIDE_KEYBOARD
    isSearchActive -> LibraryBackAction.CLEAR_SEARCH
    isManagementPage -> LibraryBackAction.RETURN_TO_LIBRARY
    else -> LibraryBackAction.NONE
}

internal fun filterUnifiedLibraryBooks(
    books: List<RecentFileItem>,
    filter: UnifiedLibraryFilter,
    query: String,
    searchInsideFiles: Boolean = false,
    contentSearchResults: List<SharedGlobalSearchBookResult> = emptyList(),
    sortOrder: SortOrder = SortOrder.RECENT,
): List<RecentFileItem> {
    if (books.isEmpty()) return emptyList()
    val normalizedQuery = query.trim()
    val byId = books.associateBy { it.bookId }
    val sharedBooks = SharedProjectionBookItemCache(maxEntries = books.size).map(books)
    val metadataBooks = sharedBooks
        .filter { it.matchesMobileUnifiedLibraryFilter(filter, normalizedQuery) }
        .mapNotNull { byId[it.id] }
    if (!searchInsideFiles || normalizedQuery.isBlank() || contentSearchResults.isEmpty()) {
        return sortFiles(metadataBooks, sortOrder)
    }
    // Content-only books follow metadata hits but still respect the active
    // status filter (blank query = filter check without text matching).
    // Relevance wins inside this group: best content match first, otherwise a
    // strong hit sits below a book the user merely opened last.
    val metadataIds = metadataBooks.mapTo(HashSet()) { it.bookId }
    val contentScores = contentSearchResults.associate { it.bookId to it.score }
    val contentOnly = sharedBooks
        .filter { it.id !in metadataIds && it.matchesMobileUnifiedLibraryFilter(filter, "") }
        .sortedByDescending { contentScores[it.id] ?: Double.NEGATIVE_INFINITY }
        .mapNotNull { byId[it.id] }
    return sortFiles(metadataBooks, sortOrder) + contentOnly
}
