package yos.music.player.ui.pages

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import yos.music.player.R
import yos.music.player.ui.widgets.basic.Title

private const val MEDIA_FOLDER_PREFERENCES = "session_media_folders"

private data class LocalMediaFolder(val name: String, val audiobookCount: Int, val podcastCount: Int)

@Composable
fun Media() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences(MEDIA_FOLDER_PREFERENCES, 0) }
    var folders by remember {
        mutableStateOf(preferences.getStringSet("folders", emptySet()).orEmpty())
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        folders = folders + uri.toString()
        preferences.edit().putStringSet("folders", folders).apply()
    }

    Title(
        title = stringResource(R.string.page_media_title),
        content = {
            item("media-folder-action") {
                Button(
                    onClick = { folderPicker.launch(null) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                ) { Text(stringResource(R.string.page_media_add_folder)) }
            }
            if (folders.isEmpty()) {
                item("media-empty") {
                    Text(
                        stringResource(R.string.page_media_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            } else {
                folders.sorted().forEach { folder ->
                    item(folder) { MediaFolderRow(Uri.parse(folder)) }
                }
            }
        }
    )
}

@Composable
private fun MediaFolderRow(uri: Uri) {
    val context = LocalContext.current
    var mediaFolder by remember(uri) {
        mutableStateOf(LocalMediaFolder(uri.lastPathSegment.orEmpty(), 0, 0))
    }
    LaunchedEffect(uri) {
        mediaFolder = withContext(Dispatchers.IO) {
            val root = DocumentFile.fromTreeUri(context, uri)
            val files = root?.listFiles().orEmpty().flatMap { child ->
                if (child.isDirectory) child.listFiles().toList() else listOf(child)
            }.filter { it.type?.startsWith("audio/") == true }
            val podcasts = files.count { file ->
                file.name.orEmpty().contains("podcast", ignoreCase = true) ||
                    file.parentFile?.name.orEmpty().contains("podcast", ignoreCase = true)
            }
            LocalMediaFolder(
                name = root?.name.orEmpty().ifBlank { uri.lastPathSegment.orEmpty() },
                audiobookCount = files.size - podcasts,
                podcastCount = podcasts
            )
        }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
        Text("▮", color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(mediaFolder.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${stringResource(R.string.page_media_audiobooks)} ${mediaFolder.audiobookCount}  ·  ${stringResource(R.string.page_media_podcasts)} ${mediaFolder.podcastCount}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}