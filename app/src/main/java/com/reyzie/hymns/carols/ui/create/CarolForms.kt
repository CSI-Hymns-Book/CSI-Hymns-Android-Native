package com.reyzie.hymns.carols.ui.create

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.reyzie.hymns.carols.data.model.CarolSong
import com.reyzie.hymns.carols.domain.splitBilingualLyrics
import com.reyzie.hymns.data.MusicalScales

@Composable
fun CreateChurchDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Church") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Church / Group name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) onCreate(name.trim(), description.trim().ifBlank { null })
                },
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * The song form. With no [initial] song it adds a new one; with one it edits it, prefilled
 * with its title, number, scale and lyrics (the Kannada and English halves separately).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarolSongFormDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, songNumber: String?, kannada: String, english: String?, scale: String) -> Unit,
    initial: CarolSong? = null,
) {
    val (initialKannada, initialEnglish) = remember(initial?.id, initial?.lyrics) {
        initial?.let { splitBilingualLyrics(it.lyrics) } ?: ("" to null)
    }
    var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var number by remember(initial?.id) { mutableStateOf(initial?.songNumber.orEmpty()) }
    var kannada by remember(initial?.id) { mutableStateOf(initialKannada) }
    var english by remember(initial?.id) { mutableStateOf(initialEnglish.orEmpty()) }
    var scale by remember(initial?.id) { mutableStateOf(initial?.scale?.takeIf { it.isNotBlank() } ?: "C Major") }
    var scaleExpanded by remember { mutableStateOf(false) }
    val scaleOptions = remember(scale) { (MusicalScales.allScales.take(12) + scale).distinct() }
    val isEditing = initial != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Song" else "Add Song") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("Song number (optional)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(expanded = scaleExpanded, onExpandedChange = { scaleExpanded = !scaleExpanded }) {
                    OutlinedTextField(
                        value = scale,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Scale") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = scaleExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    DropdownMenu(expanded = scaleExpanded, onDismissRequest = { scaleExpanded = false }) {
                        scaleOptions.forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = { scale = option; scaleExpanded = false })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = kannada, onValueChange = { kannada = it }, label = { Text("Kannada lyrics") }, modifier = Modifier.fillMaxWidth().height(if (isEditing) 180.dp else 100.dp))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = english, onValueChange = { english = it }, label = { Text("English (optional)") }, modifier = Modifier.fillMaxWidth().height(if (isEditing) 140.dp else 80.dp))
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && kannada.isNotBlank(),
                onClick = { onSave(title.trim(), number.trim().ifBlank { null }, kannada, english.ifBlank { null }, scale) },
            ) { Text(if (isEditing) "Save changes" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun AddCarolPdfDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, songNumber: String?, uri: Uri) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) { }
            pickedUri = uri
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add PDF") },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("Song number (optional)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text(if (pickedUri != null) "PDF selected ✓" else "No PDF selected")
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { pdfPicker.launch(arrayOf("application/pdf")) }) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (pickedUri != null) "Change PDF" else "Pick PDF")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val uri = pickedUri ?: return@TextButton
                if (title.isNotBlank()) onAdd(title, number.ifBlank { null }, uri)
            }) { Text("Upload") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun ChurchAddMenuDialog(
    onDismiss: () -> Unit,
    onAddSong: () -> Unit,
    onAddPdf: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to church") },
        text = {
            Column {
                TextButton(onClick = onAddSong) {
                    Icon(Icons.Default.MusicNote, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Song")
                }
                TextButton(onClick = onAddPdf) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add PDF")
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
