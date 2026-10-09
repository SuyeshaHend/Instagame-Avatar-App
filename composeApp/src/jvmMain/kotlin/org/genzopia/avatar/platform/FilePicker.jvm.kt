package org.genzopia.avatar.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.JFileChooser

actual class FilePicker {
    actual suspend fun pickFiles(): List<FileData> = withContext(Dispatchers.IO) {
        val fileChooser = JFileChooser()
        fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
        fileChooser.isMultiSelectionEnabled = true

        val result = fileChooser.showOpenDialog(null)
        val list = mutableListOf<FileData>()

        if (result == JFileChooser.APPROVE_OPTION) {
            val files = fileChooser.selectedFiles
            files.forEach { file ->
                list.add(
                    FileData(
                        name = file.name,
                        bytes = file.readBytes()
                    )
                )
            }
        }

        list
    }

    actual suspend fun pickSingleFile(): FileData? = withContext(Dispatchers.IO) {
        val fileChooser = JFileChooser()
        fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
        fileChooser.isMultiSelectionEnabled = false

        val result = fileChooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            val file = fileChooser.selectedFile
            FileData(name = file.name, bytes = file.readBytes())
        } else {
            null
        }
    }
}
