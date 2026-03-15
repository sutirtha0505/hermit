package com.example.hermit

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelStoreScreen(
    onBack: () -> Unit,
    viewModel: DownloadViewModel = viewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Model Store") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.searchModels() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        val models = viewModel.availableModels
        val keyboardController = LocalSoftwareKeyboardController.current

        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search Hugging Face (e.g., Llama 3)") },
                singleLine = true,
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        viewModel.searchModels()
                    }
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                trailingIcon = {
                    if (viewModel.searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.updateSearchQuery("")
                            viewModel.searchModels()
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    } else {
                        IconButton(onClick = {
                            keyboardController?.hide()
                            viewModel.searchModels()
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                }
            )
        
        if (models.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (viewModel.searchQuery.isNotEmpty()) {
                        Text("No models found for '${viewModel.searchQuery}'")
                    } else {
                        CircularProgressIndicator()
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(models) { model ->
                        StoreModelItem(
                            model = model,
                            state = viewModel.downloadStates[model.id] ?: DownloadState(),
                            isDownloaded = viewModel.isDownloaded(model),
                            onDownload = { viewModel.startDownload(model) },
                            onCancel = { viewModel.cancelDownload(model.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StoreModelItem(
    model: RemoteModel,
    state: DownloadState,
    isDownloaded: Boolean,
    onDownload: () -> Unit,
    onCancel: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Repo: ${model.repo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                
                if (isDownloaded && !state.isDownloading) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Installed",
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else if (state.isDownloading) {
                    IconButton(onClick = onCancel) {
                        Icon(imageVector = Icons.Default.Cancel, contentDescription = "Cancel")
                    }
                } else {
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Default.Download, contentDescription = "Download")
                    }
                }
            }

            if (state.isDownloading || state.progress > 0) {
                Spacer(Modifier.height(16.dp))
                val animatedProgress by animateFloatAsState(targetValue = state.progress, label = "progress")
                
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (state.progress >= 1f) "Finished" else "Downloading...",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "${(state.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
            
            state.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
