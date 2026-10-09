package org.genzopia.avatar.firebase

expect fun platformInitializeFirebase(context: Any? = null)

expect suspend fun platformCreateAvatarWithFileReference(cloudflareFilePaths: List<String>): String?

expect suspend fun platformAddFileToAvatarList(payload: AvatarPayload): String?

expect suspend fun platformGetAvatarFileList(callback: (List<Map<String, Any>>?) -> Unit)
