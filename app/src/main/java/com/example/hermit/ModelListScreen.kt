package com.example.hermit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelListScreen(
    onModelSelected: (LocalModel) -> Unit,
    onImportModel: () -> Unit,
    onBack: () -> Unit,
    onDownloadNew: () -> Unit,
    chatViewModel: ChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val modelManager = remember { ModelManager(context) }
    var downloadedModels by remember { mutableStateOf(modelManager.getDownloadedModels()) }
    var modelToDelete by remember { mutableStateOf<LocalModel?>(null) }

    fun refreshModels() {
        downloadedModels = modelManager.getDownloadedModels()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Local Models") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onImportModel) {
                        Icon(Icons.Default.FileOpen, contentDescription = "Import Model")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onDownloadNew) {
                Icon(Icons.Default.Add, contentDescription = "Download New")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (downloadedModels.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "No models found.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = onDownloadNew) {
                                Text("Download a Model")
                            }
                        }
                    }
                }
            } else {
                items(downloadedModels) { model ->
                    val isLoaded = chatViewModel.isModelLoaded && chatViewModel.loadedModelPath == model.path
                    
                    ListItem(
                        headlineContent = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(model.name)
                                if (isLoaded) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = MaterialTheme.shapes.extraSmall
                                    ) {
                                        Text(
                                            "LOADED",
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        },
                        supportingContent = { Text(model.path, style = MaterialTheme.typography.bodySmall) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isLoaded) {
                                    IconButton(onClick = { chatViewModel.unloadModel() }) {
                                        Icon(Icons.Default.Stop, contentDescription = "Unload", tint = MaterialTheme.colorScheme.error)
                                    }
                                } else {
                                    IconButton(onClick = { modelToDelete = model }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    Button(onClick = { onModelSelected(model) }) {
                                        Text("Load")
                                    }
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (modelToDelete != null) {
        AlertDialog(
            onDismissRequest = { modelToDelete = null },
            title = { Text("Delete Model") },
            text = { Text("Are you sure you want to delete ${modelToDelete?.name}? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val model = modelToDelete
                        if (model != null) {
                            modelManager.deleteModel(model.path)
                            refreshModels()
                        }
                        modelToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { modelToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
