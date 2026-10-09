package org.genzopia.avatar.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.genzopia.avatar.data.model.FileItem
import androidx.compose.foundation.background
import org.genzopia.avatar.firebase.FirebaseHelper
import kotlinx.coroutines.launch

private val OrangePrimary = Color(0xFFFF5E0E)
private val BorderOrange = OrangePrimary.copy(alpha = 0.45f)
val LightCardBg = Color(0xFFFFF6F0) // soft warm light background


@Composable
fun FileManagerScreen(
    viewModel: FileManagerViewModel,
    onPickFileAt: (Int) -> Unit
) {
    val files by viewModel.files.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val scope = rememberCoroutineScope()

    var listPathInput by remember { mutableStateOf("hariom") }
    var uploadNameInput by remember { mutableStateOf("file_name") }
    var uploadPathInput by remember { mutableStateOf("hariom/file_name") }
    var deletePathInput by remember { mutableStateOf("hariom/example.pdf") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightCardBg)
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp) // overall container
                .padding(16.dp)
                .border(
                    width = 1.5.dp,
                    color = BorderOrange,
                    shape = MaterialTheme.shapes.large
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            // Heading (slightly narrower)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cloudflare Worker File Manager",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            // Status
            when (val state = uiState) {
                is UiState.Loading -> LinearProgressIndicator()
                is UiState.Success -> StatusCard(state.message, false)
                is UiState.Error -> StatusCard(state.message, true)
                UiState.Idle -> {}
            }

            // Upload Section (wider than heading)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Upload File", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    // Two file slot buttons: show file name if present, otherwise placeholder
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (i in 0..1) {
                            val file = selectedFiles.getOrNull(i)
                            Column {
                                Button(
                                    onClick = { onPickFileAt(i) },
                                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                    modifier = Modifier.widthIn(min = 120.dp)
                                ) {
                                    Text(file?.name ?: "Pick File ${i + 1}")
                                }

                                Spacer(Modifier.height(6.dp))

                                OutlinedButton(
                                    onClick = { viewModel.removeSelectedFileAt(i) },
                                    enabled = file != null
                                ) {
                                    Text("Clear")
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    OrangeOutlinedField(
                        value = uploadNameInput,
                        onValueChange = { uploadNameInput = it },
                        label = "Name"
                    )

                    Spacer(Modifier.height(10.dp))

                    OrangeOutlinedField(
                        value = uploadPathInput,
                        onValueChange = { uploadPathInput = it },
                        label = "Path"
                    )

                    Spacer(Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Keep a main pick files button for convenience
                        Button(
                            onClick = { onPickFileAt(0) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrangePrimary
                            )
                        ) {
                            Text("Pick Files")
                        }

                        Button(
                            onClick = {
                                viewModel.uploadFiles(uploadNameInput, uploadPathInput)
                            },
                            enabled = selectedFiles.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrangePrimary,
                                disabledContainerColor = OrangePrimary.copy(alpha = 0.4f)
                            )
                        ) {
                            Text("Upload")
                        }
                    }
                }
            }

            // List Files
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("List Files", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    OrangeOutlinedField(
                        value = listPathInput,
                        onValueChange = { listPathInput = it },
                        label = "Path"
                    )

                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.loadFiles(listPathInput) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangePrimary
                        )
                    ) {
                        Text("Load Files")
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.loadAvatarsFromDb() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangePrimary
                        )
                    ) {
                        Text("Load Avatar entries from DB")
                    }
                }
            }

            // Show Avatars from DB
            val avatars by viewModel.avatars.collectAsState()
            if (avatars.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Avatars in DB (${avatars.size})", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))

                        avatars.forEach { avatar ->
                            Text(avatar["id"]?.toString() ?: "no-id")
                            val paths = avatar["cloudflareFilePaths"] as? List<*>
                            paths?.forEach { p -> Text(p.toString()) }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Files
            if (files.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Files (${files.size})")
                        Spacer(Modifier.height(8.dp))

                        files.forEach {
                            FileItemCard(it) {
                                viewModel.deleteFile(it.key)
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Delete Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Delete File", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    OrangeOutlinedField(
                        value = deletePathInput,
                        onValueChange = { deletePathInput = it },
                        label = "File Path"
                    )

                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.deleteFile(deletePathInput) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun OrangeOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = OrangePrimary,
            unfocusedBorderColor = BorderOrange,
            focusedLabelColor = OrangePrimary,
            cursorColor = OrangePrimary
        )
    )
}

@Composable
private fun StatusCard(message: String, isError: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isError)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun FileItemCard(
    file: FileItem,
    onDelete: () -> Unit
) {
    Card {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(file.key)
                Text("Size: ${formatFileSize(file.size)}")
            }

            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        }
    }
}

fun formatFileSize(bytes: Long): String =
    when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
