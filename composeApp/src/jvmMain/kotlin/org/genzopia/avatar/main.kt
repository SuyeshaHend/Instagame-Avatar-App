package org.genzopia.avatar

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.genzopia.avatar.platform.FilePicker
import org.genzopia.avatar.ui.FileManagerViewModel
import org.genzopia.avatar.firebase.FirebaseHelper

fun main() = application {
    // Initialize Firebase at app startup
    try {
        FirebaseHelper.initialize()
    } catch (e: Exception) {
        println("❌ Firebase initialization failed at startup: ${e.message}")
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Cloudflare File Manager"
    ) {
        val viewModel: FileManagerViewModel = viewModel { FileManagerViewModel() }
        val scope = rememberCoroutineScope()
        val filePicker = FilePicker()

        App(
            viewModel = viewModel,
            onPickFileAt = { index ->
                scope.launch {
                    val file = filePicker.pickSingleFile()
                    if (file != null) {
                        viewModel.setSelectedFileAt(index, file)
                    }
                }
            }
        )
    }
}
