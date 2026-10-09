package org.genzopia.avatar.platform

data class FileData(
    val name: String,
    val bytes: ByteArray
)

expect class FilePicker {
    suspend fun pickFiles(): List<FileData>
    suspend fun pickSingleFile(): FileData?
}
