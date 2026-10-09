package org.genzopia.avatar

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import org.genzopia.avatar.platform.rememberSingleFilePicker
import org.genzopia.avatar.ui.FileManagerViewModel
import org.genzopia.avatar.firebase.FirebaseHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase before setting content, pass Android Context
        FirebaseHelper.initialize(this)

        setContent {
            val viewModel: FileManagerViewModel = viewModel()

            val pickSlot0 = rememberSingleFilePicker { fileData ->
                viewModel.setSelectedFileAt(0, fileData)
                Toast.makeText(this, "Selected: ${fileData.name}", Toast.LENGTH_SHORT).show()
            }

            val pickSlot1 = rememberSingleFilePicker { fileData ->
                viewModel.setSelectedFileAt(1, fileData)
                Toast.makeText(this, "Selected: ${fileData.name}", Toast.LENGTH_SHORT).show()
            }

            App(
                viewModel = viewModel,
                onPickFileAt = { index ->
                    if (index == 0) pickSlot0() else pickSlot1()
                }
            )
        }
    }
}
