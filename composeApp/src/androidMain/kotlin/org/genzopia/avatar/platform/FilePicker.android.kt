package org.genzopia.avatar.platform

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.runBlocking

actual class FilePicker {
    actual suspend fun pickFiles(): List<FileData> {
        return emptyList() // prefer compose-based rememberFilePicker on Android
    }

    actual suspend fun pickSingleFile(): FileData? {
        // Not used in non-composable Android paths; keep simple null to avoid blocking UI.
        return null
    }
}

@Composable
fun rememberFilePicker(onFilesPicked: (List<FileData>) -> Unit): () -> Unit {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri>? ->
        val list = mutableListOf<FileData>()
        uris?.forEach { uri ->
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                val fileName = getFileName(context, uri) ?: "unknown_file"

                if (bytes != null) {
                    list.add(FileData(fileName, bytes))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (list.isNotEmpty()) onFilesPicked(list)
    }

    return remember {
        { launcher.launch(arrayOf("*/*")) }
    }
}

@Composable
fun rememberSingleFilePicker(onFilePicked: (FileData) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                val fileName = getFileName(context, it) ?: "unknown_file"

                if (bytes != null) {
                    onFilePicked(FileData(fileName, bytes))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    return remember { { launcher.launch("*/*") } }
}

private fun getFileName(context: Context, uri: Uri): String? {
    var fileName: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        fileName = cursor.getString(nameIndex)
    }
    return fileName
}
