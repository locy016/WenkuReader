/**
 * Shared Android chrome still owned by this file. The filename is a historical artifact: the
 * `HomeScreen` this was named for was retired when Library Beta replaced it, and is now gone
 * (see `docs/shared-parity-migration.md` C4). What remains is the set of composables that its
 * neighbours still call, so the name no longer describes the contents.
 *
 * Live callers, for anyone re-homing these:
 * - `SettingsScreen` — the dialogs, `DeviceManagementScreen`, `AppThemeBottomSheet`,
 *   `LanguageSelectionDialog`
 * - `UnifiedLibraryScreen` — `AppDrawerContent`
 *
 * Splitting this into per-concern files is mechanical and safe, but it is pure relocation with no
 * behavior change, so it is deliberately not done here: renaming churn is not worth a diff's worth
 * of review on its own. Do it when one of these symbols next needs to move for a real reason.
 */
@file:Suppress("DEPRECATION")

package com.aryan.reader

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.aryan.reader.data.RecentFileItem
import com.aryan.reader.shared.ui.SharedMobileLanguageSelectionList
import com.aryan.reader.shared.ui.SharedAppThemeBottomSheet
import java.text.SimpleDateFormat
import java.util.Locale

internal fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentFileCard(
    item: RecentFileItem,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    isPinned: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isDownloading: Boolean,
    usePdfFileNameAsDisplayName: Boolean = false,
    animateOpening: Boolean = false,
    compact: Boolean = false,
) {
    val progressPercent = item.progressPercentage?.takeIf { it > 0f }?.coerceIn(0f, 100f)?.toInt()
    val authorText = item.cardAuthor()
    val coverTap = rememberLibraryCoverTap(item, animateOpening)
    com.aryan.reader.shared.ui.SharedAndroidHomeRecentCard(
        bookId = item.bookId,
        title = item.cardTitle(usePdfFileNameAsDisplayName),
        author = authorText,
        progressPercent = progressPercent,
        isAvailable = item.isAvailable,
        isDownloading = isDownloading,
        isSelected = isSelected,
        showStatusBadges = isPinned,
        unavailableDescription = stringResource(R.string.not_available_locally),
        selectedDescription = stringResource(R.string.content_desc_selected),
        onClick = {
            coverTap?.record(item)
            onClick()
        },
        onLongClick = onLongClick,
        modifier = modifier,
        compact = compact,
        cover = { coverModifier ->
            ThemedBookCover(
                item = item,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = coverModifier.recordLibraryCover(coverTap),
                showFileType = true,
            )
        },
        statusBadges = { badgeModifier ->
            FileStatusBadges(
                item = item,
                isPinned = isPinned,
                overlay = true,
                modifier = badgeModifier,
            )
        },
    )
}

@Composable
fun UpgradeDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.VerifiedUser, contentDescription = null) },
        title = { Text(stringResource(R.string.dialog_unlock_pro)) },
        text = { Text(stringResource(R.string.dialog_unlock_pro_desc)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_upgrade)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        })
}

@Composable
fun SignOutConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_confirm_sign_out)) },
        text = { Text(stringResource(R.string.dialog_confirm_sign_out_desc)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.drawer_sign_out))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        })
}

@Composable
fun DeviceManagementScreen(
    devices: List<DeviceItem>, onRemoveDevice: (String) -> Unit, isReplacing: Boolean
) {
    val dateFormatter = remember {
        SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.device_limit_reached),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.device_limit_reached_desc),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (isReplacing) {
                CircularProgressIndicator()
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(devices, key = { it.deviceId }) { device ->
                        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PhoneAndroid, contentDescription = stringResource(R.string.content_desc_device))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(device.deviceName, fontWeight = FontWeight.SemiBold)
                                    device.lastSeen?.let {
                                        Text(
                                            stringResource(R.string.last_seen, dateFormatter.format(it)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                TextButton(onClick = { onRemoveDevice(device.deviceId) }) {
                                    Text(stringResource(R.string.action_remove))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClearAllDataConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Info, contentDescription = null) },
        title = { Text(stringResource(R.string.dialog_destructive_action)) },
        text = { Text(stringResource(R.string.dialog_destructive_action_desc)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        })
}

@Composable
fun DangerousFolderActionDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.action_confirm_clear))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun ExternalFileBehaviorDialog(
    currentBehavior: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.options_external_file_behavior)) },
        text = {
            Column(modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                val options = listOf(
                    Triple("ASK", R.string.external_file_behavior_ask, R.string.external_file_behavior_ask_desc),
                    Triple("KEEP", R.string.external_file_behavior_keep, R.string.external_file_behavior_keep_desc),
                    Triple("DELETE", R.string.external_file_behavior_delete, R.string.external_file_behavior_delete_desc),
                    Triple("TEMPORARY", R.string.external_file_behavior_temporary, R.string.external_file_behavior_temporary_desc)
                )
                options.forEach { (value, labelRes, descriptionRes) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value); onDismiss() }
                            .padding(vertical = 12.dp)
                    ) {
                        RadioButton(selected = currentBehavior == value, onClick = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(labelRes))
                            Text(
                                text = stringResource(descriptionRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
fun StrictFilterConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_strict_file_filter_title)) },
        text = { Text(stringResource(R.string.dialog_strict_file_filter_desc)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_enable)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

/**
 * App theme bottom sheet.
 *
 * Rendering lives in shared (`SharedAppThemeBottomSheet`), which owns the mode
 * / contrast / text-dim / seed-color controls *and* the custom-theme create
 * dialog. This wrapper only reads Android's [ReaderScreenState] and forwards the
 * ViewModel mutators.
 */
@Composable
fun AppThemeBottomSheet(
    uiState: ReaderScreenState,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onContrastOptionChanged: (AppContrastOption) -> Unit,
    onTextDimFactorLightChanged: (Float) -> Unit,
    onTextDimFactorDarkChanged: (Float) -> Unit,
    onSeedColorChanged: (Color?) -> Unit,
    onCustomThemeAdded: (CustomAppTheme) -> Unit,
    onCustomThemeDeleted: (String) -> Unit,
    onDismiss: () -> Unit
) {
    SharedAppThemeBottomSheet(
        appThemeMode = uiState.appThemeMode,
        appContrastOption = uiState.appContrastOption,
        appTextDimFactorLight = uiState.appTextDimFactorLight,
        appTextDimFactorDark = uiState.appTextDimFactorDark,
        appSeedColor = uiState.appSeedColor,
        customAppThemes = uiState.customAppThemes,
        onThemeModeChanged = onThemeModeChanged,
        onContrastOptionChanged = onContrastOptionChanged,
        onTextDimFactorLightChanged = onTextDimFactorLightChanged,
        onTextDimFactorDarkChanged = onTextDimFactorDarkChanged,
        onSeedColorChanged = onSeedColorChanged,
        onCustomThemeAdded = onCustomThemeAdded,
        onCustomThemeDeleted = onCustomThemeDeleted,
        onDismiss = onDismiss
    )
}
@Composable
fun LanguageSelectionDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentTag = if (!currentLocales.isEmpty) currentLocales.get(0)?.toLanguageTag() else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.options_language)) },
        text = {
            SharedMobileLanguageSelectionList(
                selectedTag = currentTag,
                onSelect = { tag ->
                    val locales = tag?.let { LocaleListCompat.forLanguageTags(it) }
                        ?: LocaleListCompat.getEmptyLocaleList()
                    AppCompatDelegate.setApplicationLocales(locales)
                    onDismiss()
                    context.findActivity()?.recreate()
                },
                maxListHeight = 360.dp
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
