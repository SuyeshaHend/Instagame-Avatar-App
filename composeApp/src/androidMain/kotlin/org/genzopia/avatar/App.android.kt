// androidMain/FilePicker.kt
package org.genzopia.avatar

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream

@Composable
actual fun FilePickerButton(onFilesPicked: (List<File>) -> Unit) {
    val context = LocalContext.current

    // Remember the launcher for multiple documents
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri>? ->
        val files = mutableListOf<File>()
        uris?.forEach { uri ->
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "temp_file"
            val tempFile = File(context.cacheDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            files.add(tempFile)
        }
        if (files.isNotEmpty()) onFilesPicked(files)
    }

    Button(onClick = { launcher.launch(arrayOf("*/*")) }) {
        Text("Choose Files")
    }
}
