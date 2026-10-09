package org.genzopia.avatar.ui

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.genzopia.avatar.data.api.CloudflareApiClient
import org.genzopia.avatar.data.model.FileItem
import org.genzopia.avatar.firebase.FirebaseHelper
import org.genzopia.avatar.platform.FileData

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val message: String) : UiState()
    data class Error(val message: String) : UiState()
}

class FileManagerViewModel : ViewModel() {
    private val apiClient = CloudflareApiClient()

    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _selectedFiles = MutableStateFlow<List<FileData>>(emptyList())
    val selectedFiles: StateFlow<List<FileData>> = _selectedFiles.asStateFlow()

    private val _avatars = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val avatars: StateFlow<List<Map<String, Any>>> = _avatars.asStateFlow()

    fun setSelectedFileAt(index: Int, fileData: FileData) {
        val current = _selectedFiles.value.toMutableList()
        if (current.size <= index) {
            current.add(fileData)
        } else {
            current[index] = fileData
        }
        _selectedFiles.value = current
    }

    fun removeSelectedFileAt(index: Int) {
        val current = _selectedFiles.value.toMutableList()
        if (index < current.size) {
            current.removeAt(index)
            _selectedFiles.value = current
        }
    }

    fun uploadFiles(uploadName: String, uploadPath: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val uploadedUrls = mutableListOf<String>()

            selectedFiles.value.forEachIndexed { index, fileData ->
                try {
                    // Generate unique name for each file to avoid overwriting
                    val timestamp = System.currentTimeMillis()
                    val originalName = fileData.name
                    val nameWithoutExtension = originalName.substringBeforeLast(".")
                    val extension = originalName.substringAfterLast(".", "")

                    val uniqueName = if (extension.isNotEmpty()) {
                        "${nameWithoutExtension}_${timestamp}.$extension"
                    } else {
                        "${originalName}_$timestamp"
                    }

                    println("📤 Uploading file $index: original=$originalName, unique=$uniqueName")

                    val result = apiClient.uploadFile(
                        fileBytes = fileData.bytes,
                        fileName = originalName,  // Keep original for Content-Type
                        name = uniqueName,        // Use unique name to avoid collisions
                        path = uploadPath
                    )

                    result.onSuccess { response ->
                        println("✅ Upload successful: ${response.url}")
                        response.url?.let { url ->
                            uploadedUrls.add(url)
                        }
                    }.onFailure { error ->
                        println("❌ Upload failed: ${error.message}")
                    }
                } catch (e: Exception) {
                    println("❌ Exception during upload: ${e.message}")
                }
            }

            // If we have uploaded files, create avatar entries
            if (uploadedUrls.isNotEmpty()) {
                try {
                    val avatarId = FirebaseHelper.createAvatarWithFileReference(uploadedUrls)
                    _uiState.value = UiState.Success("Uploaded ${uploadedUrls.size} files and created avatar: $avatarId")
                } catch (e: Exception) {
                    _uiState.value = UiState.Error("Upload succeeded but failed to create avatar: ${e.message}")
                }
            } else {
                _uiState.value = UiState.Error("No files were uploaded successfully")
            }

            // Clear selected files after upload
            _selectedFiles.value = emptyList()
        }
    }

    fun loadFiles(path: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val result = apiClient.listFiles(path)
                result.onSuccess { response ->
                    _files.value = response.files
                    _uiState.value = UiState.Success("Loaded ${response.files.size} files")
                }.onFailure { error ->
                    _uiState.value = UiState.Error("Failed to load files: ${error.message}")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Exception: ${e.message}")
            }
        }
    }

    fun loadAvatarsFromDb() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            FirebaseHelper.getAvatarFileList { avatarsList ->
                viewModelScope.launch {
                    avatarsList?.let {
                        _avatars.value = it
                        _uiState.value = UiState.Success("Loaded ${it.size} avatars")
                    } ?: run {
                        _uiState.value = UiState.Error("No avatars found")
                    }
                }
            }
        }
    }

    fun deleteFile(path: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val result = apiClient.deleteFile(path)
                result.onSuccess { response ->
                    // Remove from local list
                    _files.value = _files.value.filter { it.key != path }
                    _uiState.value = UiState.Success("Deleted file: $path")
                }.onFailure { error ->
                    _uiState.value = UiState.Error("Delete failed: ${error.message}")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Exception: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        apiClient.close()
    }
}