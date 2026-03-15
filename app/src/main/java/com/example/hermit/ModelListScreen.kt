package com.example.hermit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelListScreen(
    onModelSelected: (LocalModel) -> Unit,
    onImportModel: () -> Unit,
    onBack: () -> Unit,
    onDownloadNew: () -> Unit
) {
    val context = LocalContext.current
    val modelManager = remember { ModelManager(context) }
    val downloadedModels = modelManager.getDownloadedModels()

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
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
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
                    ListItem(
                        headlineContent = { Text(model.name) },
                        supportingContent = { Text(model.path) },
                        trailingContent = {
                            Button(onClick = { onModelSelected(model) }) {
                                Text("Load")
                            }
                        }
                    )
                }
            }
        }
    }
}
