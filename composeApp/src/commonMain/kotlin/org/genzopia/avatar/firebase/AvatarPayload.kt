package org.genzopia.avatar.firebase

import kotlinx.serialization.Serializable

@Serializable
data class AvatarPayload(
    val cloudflareFilePaths: List<String>,
    val createdAt: Long
)
