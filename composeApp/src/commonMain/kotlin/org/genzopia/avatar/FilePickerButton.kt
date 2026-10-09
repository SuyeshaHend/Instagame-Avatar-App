package org.genzopia.avatar

import androidx.compose.runtime.Composable
import java.io.File

@Composable
expect fun FilePickerButton(onFilesPicked: (List<File>) -> Unit)
