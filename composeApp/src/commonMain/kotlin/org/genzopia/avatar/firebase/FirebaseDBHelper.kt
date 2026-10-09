package org.genzopia.avatar.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.database
import dev.gitlive.firebase.database.FirebaseDatabase

object FirebaseHelper {
    private var database: FirebaseDatabase? = null
    private val isInitialized: Boolean get() = database != null

    private const val DATABASE_URL = "https://instagame-452906-default-rtdb.firebaseio.com"

    /** ✅ Initialize Firebase; optional platform context (Android Context) */
    fun initialize(context: Any? = null) {
        try {
            // call platform-specific initializer (Android will initialize FirebaseApp)
            try {
                platformInitializeFirebase(context)
            } catch (e: Exception) {
                println("⚠️ platformInitializeFirebase() failed: ${e.message}")
            }

            if (!isInitialized) {
                // Try to initialize dev.gitlive Firebase database after platform init
                database = try {
                    Firebase.database(DATABASE_URL)
                } catch (e: Exception) {
                    println("⚠️ dev.gitlive Firebase.database() could not be initialized: ${e.message}")
                    null
                }
            }

            if (isInitialized) println("✅ Firebase initialized successfully (dev.gitlive)") else println("ℹ️ Firebase initialized with REST fallback (no dev.gitlive instance)")
        } catch (e: Exception) {
            println("❌ Firebase initialization error: ${e.message}")
            e.printStackTrace()
        }
    }

    // Ensure database is initialized before use
    private fun ensureInitialized() {
        if (!isInitialized) initialize()
    }

    private fun getDatabase(): FirebaseDatabase {
        ensureInitialized()
        return database ?: throw IllegalStateException("dev.gitlive FirebaseDatabase not available; use REST functions")
    }

    // 🔹 Realtime Database
    suspend fun addData(path: String, data: Any) {
        try {
            getDatabase().reference(path).setValue(data)
            println("✅ Data added successfully at $path")
        } catch (e: Exception) {
            println("❌ Error adding data: ${e.message}")
        }
    }

    // Generate a unique key like Android push() method
    fun getKey(path: String = ""): String {
        return if (isInitialized) {
            val ref = if (path.isEmpty()) getDatabase().reference() else getDatabase().reference(path)
            ref.push().key ?: System.currentTimeMillis().toString()
        } else {
            // REST/local fallback
            "${System.currentTimeMillis()}-${java.util.UUID.randomUUID().toString().take(8)}"
        }
    }

    // Add data with auto-generated unique key
    suspend fun addDataWithKey(path: String, data: Any): String? {
        return try {
            if (isInitialized) {
                val key = getKey(path)
                getDatabase().reference("$path/$key").setValue(data)
                println("✅ Data added with key: $key at $path")
                key
            } else {
                // REST fallback
                addDataWithKeyRest(path, data)
            }
        } catch (e: Exception) {
            println("❌ Error adding data: ${e.message}")
            null
        }
    }

    private suspend fun addDataWithKeyRest(path: String, data: Any): String? {
        try {
            // Use platform implementation (JVM/Android actual) which uses Ktor
            if (data is Map<*, *>) {
                // if the data uses cloudflareFilePaths, write each file entry via platformAddFileToAvatarList
                val paths = (data["cloudflareFilePaths"] as? List<String>)
                if (paths != null) {
                    var firstId: String? = null
                    for (p in paths) {
                        val payload = AvatarPayload(listOf(p), System.currentTimeMillis())
                        val gen = platformAddFileToAvatarList(payload)
                        if (firstId == null && gen != null) firstId = gen
                    }
                    return firstId
                }
            }
            return null
        } catch (e: Exception) {
            println("❌ Error adding data with key (REST): ${e.message}")
            return null
        }
    }

    fun getData(path: String, key: String, callback: (String?) -> Unit) {
        try {
            val ref = getDatabase().reference("$path/$key")
            @Suppress("UNCHECKED_CAST")
            val result = ref as? String
            callback(result)
        } catch (e: Exception) {
            println("❌ Error fetching data: ${e.message}")
            callback(null)
        }
    }

    fun getData(path: String, callback: (Map<String, Any>?) -> Unit) {
        try {
            val ref = getDatabase().reference(path)
            @Suppress("UNCHECKED_CAST")
            val result = ref as? Map<String, Any>
            callback(result)
        } catch (e: Exception) {
            println("❌ Error fetching data: ${e.message}")
            callback(null)
        }
    }

    suspend fun deleteData(path: String) {
        try {
            getDatabase().reference(path).removeValue()
            println("✅ Data deleted successfully from $path")
        } catch (e: Exception) {
            println("❌ Error deleting data: ${e.message}")
        }
    }

    // Create avatar node with unique ID and Cloudflare file references (multiple)
    suspend fun createAvatarWithFileReference(cloudflareFilePaths: List<String>): String? {
        return try {
            val uniqueId = getKey("avatars")
            val avatarData = mapOf(
                "id" to uniqueId,
                "cloudflareFilePaths" to cloudflareFilePaths,
                "createdAt" to System.currentTimeMillis()
            )
            if (isInitialized) {
                getDatabase().reference("avatars/$uniqueId").setValue(avatarData)
            } else {
                // Delegate to platform-specific REST implementation
                println("ℹ️ createAvatarWithFileReference: using REST fallback for paths=$cloudflareFilePaths")
                val generatedName = platformCreateAvatarWithFileReference(cloudflareFilePaths)
                return generatedName
            }
            println("✅ Avatar created with ID: $uniqueId and file paths: $cloudflareFilePaths")
            uniqueId
        } catch (e: Exception) {
            println("❌ Error creating avatar: ${e.message}")
            null
        }
    }

    // Add file to avatar list (append to avatars array) - keep for backward compatibility
    suspend fun addFileToAvatarList(cloudflareFilePath: String): String? {
        return try {
            val filePayload = AvatarPayload(listOf(cloudflareFilePath), System.currentTimeMillis())
            val generatedId = platformAddFileToAvatarList(filePayload)
            if (generatedId != null) {
                println("✅ File added to avatar list: $cloudflareFilePath with ID: $generatedId")
            }
            generatedId
        } catch (e: Exception) {
            println("❌ Error adding file to avatar list: ${e.message}")
            null
        }
    }

    // Get all files in avatar list
    suspend fun getAvatarFileList(callback: (List<Map<String, Any>>?) -> Unit) {
        try {
            if (isInitialized) {
                val ref = getDatabase().reference("avatars")
                @Suppress("UNCHECKED_CAST")
                val result = ref as? List<Map<String, Any>>
                callback(result)
            } else {
                // REST fallback
                platformGetAvatarFileList(callback)
            }
            println("✅ Avatar file list retrieved")
        } catch (e: Exception) {
            println("❌ Error fetching avatar file list: ${e.message}")
            callback(null)
        }
    }

    // Get avatar by ID
    fun getAvatarById(avatarId: String, callback: (Map<String, Any>?) -> Unit) {
        try {
            val ref = getDatabase().reference("avatars/$avatarId")
            @Suppress("UNCHECKED_CAST")
            val result = ref as? Map<String, Any>
            callback(result)
            println("✅ Avatar retrieved: $avatarId")
        } catch (e: Exception) {
            println("❌ Error fetching avatar: ${e.message}")
            callback(null)
        }
    }

    // Get all avatars
    fun getAllAvatars(callback: (Map<String, Map<String, Any>>?) -> Unit) {
        try {
            val ref = getDatabase().reference("avatars")
            @Suppress("UNCHECKED_CAST")
            val result = ref as? Map<String, Map<String, Any>>
            callback(result)
            println("✅ All avatars retrieved")
        } catch (e: Exception) {
            println("❌ Error fetching all avatars: ${e.message}")
            callback(null)
        }
    }

    // Delete avatar by ID
    suspend fun deleteAvatarById(avatarId: String) {
        try {
            getDatabase().reference("avatars/$avatarId").removeValue()
            println("✅ Avatar deleted: $avatarId")
        } catch (e: Exception) {
            println("❌ Error deleting avatar: ${e.message}")
        }
    }
}
