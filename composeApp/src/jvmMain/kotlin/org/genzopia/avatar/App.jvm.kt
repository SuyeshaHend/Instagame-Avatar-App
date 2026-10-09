// desktopMain/FilePicker.kt
package org.genzopia.avatar

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun FilePickerButton(onFilesPicked: (List<File>) -> Unit) {
    Button(onClick = {
        val dialog = FileDialog(Frame(), "Choose Files", FileDialog.LOAD)
        dialog.isMultipleMode = true
        dialog.isVisible = true
        val files = mutableListOf<File>()
        val selectedFiles = dialog.files
        if (selectedFiles != null && selectedFiles.isNotEmpty()) {
            for (f in selectedFiles) files.add(f)
        } else if (dialog.file != null) {
            files.add(File(dialog.directory, dialog.file))
        }
        if (files.isNotEmpty()) onFilesPicked(files)
    }) {
        Text("Choose Files")
    }
}
