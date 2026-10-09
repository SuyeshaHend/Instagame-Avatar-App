package org.genzopia.avatar

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import org.genzopia.avatar.ui.FileManagerScreen
import org.genzopia.avatar.ui.FileManagerViewModel
import org.genzopia.avatar.firebase.FirebaseHelper

@Composable
fun App(
    viewModel: FileManagerViewModel,
    onPickFileAt: (Int) -> Unit
) {
    // Initialize Firebase when the app first loads
    LaunchedEffect(Unit) {
        FirebaseHelper.initialize()
    }

    MaterialTheme {
        FileManagerScreen(
            viewModel = viewModel,
            onPickFileAt = onPickFileAt
        )
    }
}
