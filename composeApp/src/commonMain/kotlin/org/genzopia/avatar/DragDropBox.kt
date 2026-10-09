package org.genzopia.avatar
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.text.BasicText
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.awt.awtEventOrNull
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.unit.dp
//import java.awt.dnd.DropTargetDropEvent
//import java.awt.datatransfer.DataFlavor
//import java.io.File
//
//@Composable
//fun DragDropBox(
//    onFileDropped: (File) -> Unit
//) {
//    var hovering by remember { mutableStateOf(false) }
//
//    Box(
//        modifier = Modifier
//            .size(260.dp)
//            .border(2.dp, if (hovering) Color.Green else Color.Blue)
//            .background(Color(0x110000FF))
//            .pointerInput(Unit) {
//                awaitPointerEventScope {
//                    while (true) {
//                        val event = awaitPointerEvent()
//                        event.awtEventOrNull?.let { awtEvent ->
//                            val files = awtEvent.getTransferFiles()
//                            if (files.isNotEmpty()) {
//                                onFileDropped(files.first())
//                            }
//                        }
//                    }
//                }
//            },
//        contentAlignment = Alignment.Center
//    ) {
//        BasicText("Drag & Drop File Here")
//    }
//}
//
//// Helper extension to extract files from AWT drag events
//fun java.awt.AWTEvent.getTransferFiles(): List<File> {
//    return try {
//        (this as? DropTargetDropEvent)?.transferable
//            ?.getTransferData(DataFlavor.javaFileListFlavor)
//            ?.let { it as List<*> }
//            ?.mapNotNull { it as? File }
//            ?: emptyList()
//    } catch (e: Exception) {
//        emptyList()
//    }
//}