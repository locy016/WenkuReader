package com.aryan.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

@Composable
internal fun WenkuLegalDialog(onDismiss: () -> Unit) {
    val notices = listOf(
        "AGPL-3.0.txt" to "GNU AGPL v3",
        "LGPL-3.0.txt" to "GNU LGPL v3 · libmobi",
        "GPL-3.0.txt" to "GNU GPL v3",
        "THIRD-PARTY-SOURCES.txt" to stringResource(R.string.wenku_third_party_sources),
        "ANDROID-RUNTIME-SOURCES.txt" to stringResource(R.string.wenku_android_runtime_sources),
    )
    var selected by remember { mutableStateOf<Pair<String, String>?>(null) }
    val notice = selected
    val close = { if (notice == null) onDismiss() else selected = null }
    AlertDialog(
        onDismissRequest = close,
        title = { Text(notice?.second ?: stringResource(R.string.legal_licenses)) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                if (notice == null) {
                    Text(stringResource(R.string.wenku_legal_credit))
                    for (entry in notices) {
                        TextButton(onClick = { selected = entry }) { Text(entry.second) }
                    }
                } else {
                    val assets = LocalContext.current.assets
                    val failure = stringResource(R.string.wenku_license_read_error)
                    val body by produceState(initialValue = "", notice.first) {
                        value = withContext(Dispatchers.IO) {
                            try {
                                assets.open("legal/${notice.first}").bufferedReader().use { it.readText() }
                            } catch (_: IOException) {
                                failure
                            }
                        }
                    }
                    SelectionContainer { Text(body, style = MaterialTheme.typography.bodySmall) }
                }
            }
        },
        confirmButton = { TextButton(onClick = close) { Text(stringResource(R.string.action_close)) } },
    )
}
