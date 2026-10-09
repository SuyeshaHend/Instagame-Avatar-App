package org.genzopia.avatar.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.HttpResponse
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.*

actual fun platformInitializeFirebase(context: Any?) {
    try {
        val ctx = context as? Context
        if (ctx != null) {
            FirebaseApp.initializeApp(ctx)
            println("✅ Android FirebaseApp initialized")
        } else {
            println("⚠️ platformInitializeFirebase called without Android Context")
        }
    } catch (e: Exception) {
        println("❌ platformInitializeFirebase(android) failed: ${e.message}")
    }
}

private const val DATABASE_URL = "https://instagame-452906-default-rtdb.firebaseio.com"

// ADD THIS MISSING FUNCTION:
actual suspend fun platformCreateAvatarWithFileReference(cloudflareFilePaths: List<String>): String? {
    return try {
        val client = HttpClient { install(ContentNegotiation) { json(Json { encodeDefaults = true; ignoreUnknownKeys = true }) } }
        val payload = AvatarPayload(cloudflareFilePaths, System.currentTimeMillis())
        val url = "$DATABASE_URL/avatars.json"
        val response: HttpResponse = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        val bodyText = response.bodyAsText()
        client.close()
        val parsed = Json.parseToJsonElement(bodyText)
        val generatedName = parsed.jsonObject["name"]?.jsonPrimitive?.content
        generatedName
    } catch (e: Exception) {
        println("❌ platformCreateAvatarWithFileReference(android) failed: ${e.message}")
        null
    }
}

actual suspend fun platformAddFileToAvatarList(payload: AvatarPayload): String? {
    return try {
        val client = HttpClient { install(ContentNegotiation) { json(Json { encodeDefaults = true; ignoreUnknownKeys = true }) } }
        val url = "$DATABASE_URL/avatars.json"
        val response: HttpResponse = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        val bodyText = response.bodyAsText()
        client.close()
        val parsed = Json.parseToJsonElement(bodyText)
        val generatedId = parsed.jsonObject["name"]?.jsonPrimitive?.content
        generatedId
    } catch (e: Exception) {
        println("❌ platformAddFileToAvatarList(android) failed: ${e.message}")
        null
    }
}

actual suspend fun platformGetAvatarFileList(callback: (List<Map<String, Any>>?) -> Unit) {
    try {
        val client = HttpClient { install(ContentNegotiation) { json() } }
        val url = "$DATABASE_URL/avatars.json"
        val response: HttpResponse = client.get(url)
        val bodyText = response.bodyAsText()
        client.close()

        val parsed = Json.parseToJsonElement(bodyText)
        @Suppress("UNCHECKED_CAST")
        val avatarsList = mutableListOf<Map<String, Any>>()

        if (parsed.jsonObject.isNotEmpty()) {
            parsed.jsonObject.forEach { (key, value) ->
                val avatarMap = mutableMapOf<String, Any>("id" to key)
                // convert value manually
                val dataMap = mutableMapOf<String, Any>()
                if (value is JsonObject) {
                    value.forEach { k, v ->
                        val anyValue: Any = when (v) {
                            is JsonPrimitive -> {
                                when {
                                    v.isString -> v.content
                                    v.booleanOrNull != null -> v.boolean
                                    v.longOrNull != null -> v.long
                                    v.doubleOrNull != null -> v.double
                                    else -> v.content
                                }
                            }
                            is JsonObject -> v.toString()
                            is JsonArray -> v.map { it.toString() }
                            else -> "null"
                        }
                        dataMap[k] = anyValue
                    }
                } else {
                    dataMap["value"] = value.toString()
                }
                avatarMap.putAll(dataMap)
                avatarsList.add(avatarMap)
            }
        }

        callback(if (avatarsList.isEmpty()) null else avatarsList)
        println("✅ platformGetAvatarFileList(android) retrieved ${avatarsList.size} files")
    } catch (e: Exception) {
        println("❌ platformGetAvatarFileList(android) failed: ${e.message}")
        callback(null)
    }
}