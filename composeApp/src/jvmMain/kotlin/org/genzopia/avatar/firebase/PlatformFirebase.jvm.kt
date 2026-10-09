package org.genzopia.avatar.firebase

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.*

private const val DATABASE_URL = "https://instagame-452906-default-rtdb.firebaseio.com"

actual fun platformInitializeFirebase(context: Any?) {
    // No Android context on desktop; dev.gitlive Firebase cannot auto-init here.
    println("ℹ️ platformInitializeFirebase(jvm) called — no platform initialization performed.")
}

actual suspend fun platformCreateAvatarWithFileReference(cloudflareFilePaths: List<String>): String? {
    return try {
        val client = HttpClient { install(ContentNegotiation) { json(Json { encodeDefaults = true; ignoreUnknownKeys = true }) } }
        val payload = AvatarPayload(cloudflareFilePaths, System.currentTimeMillis())
        val url = "$DATABASE_URL/avatars.json"
        val response: HttpResponse = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        val status = response.status
        val bodyText = response.bodyAsText()
        client.close()
        println("ℹ️ platformCreateAvatarWithFileReference(jvm) POST $url -> status=$status, body=$bodyText")
        val parsed = Json.parseToJsonElement(bodyText)
        val generatedName = parsed.jsonObject["name"]?.jsonPrimitive?.content
        if (generatedName == null) println("⚠️ platformCreateAvatarWithFileReference(jvm) didn't get generated name; response: $bodyText")
        generatedName
    } catch (e: Exception) {
        println("❌ platformCreateAvatarWithFileReference(jvm) failed: ${e.message}")
        e.printStackTrace()
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
        val status = response.status
        val bodyText = response.bodyAsText()
        client.close()
        println("ℹ️ platformAddFileToAvatarList(jvm) POST $url -> status=$status, body=$bodyText")
        val parsed = Json.parseToJsonElement(bodyText)
        val generatedId = parsed.jsonObject["name"]?.jsonPrimitive?.content
        if (generatedId == null) println("⚠️ platformAddFileToAvatarList(jvm) didn't get generated name; response: $bodyText")
        generatedId
    } catch (e: Exception) {
        println("❌ platformAddFileToAvatarList(jvm) failed: ${e.message}")
        e.printStackTrace()
        null
    }
}

@Suppress("REDUNDANT_ELSE_IN_WHEN")
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

                // Convert JsonElement 'value' into a Map<String, Any>
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
                    // fallback: store raw text
                    dataMap["value"] = value.toString()
                }

                avatarMap.putAll(dataMap)
                avatarsList.add(avatarMap)
            }
        }

        callback(if (avatarsList.isEmpty()) null else avatarsList)
        println("✅ platformGetAvatarFileList(jvm) retrieved ${avatarsList.size} files")
    } catch (e: Exception) {
        println("❌ platformGetAvatarFileList(jvm) failed: ${e.message}")
        callback(null)
    }
}
