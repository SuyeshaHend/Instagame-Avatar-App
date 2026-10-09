package org.genzopia.avatar.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FileItem(
    val key: String,
    val size: Long,
    val etag: String
)

@Serializable
data class FileListResponse(
    val success: Boolean,
    val files: List<FileItem>
)

@Serializable
data class UploadResponse(
    val success: Boolean,
    val url: String? = null,
    val key: String? = null,
    val name: String? = null,
    val path: String? = null,
    val message: String? = null
)

@Serializable
data class DeleteResponse(
    val success: Boolean,
    val deleted: List<String>? = null,
    val message: String? = null
)
