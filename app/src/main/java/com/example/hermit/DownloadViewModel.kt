package com.example.hermit

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class DownloadViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ModelRepository(application)
    private val client = OkHttpClient()
    private val downloader = ModelDownloader(client)
    private val store = ModelStore(client)
    
    val availableModels = mutableStateListOf<RemoteModel>()
    val downloadStates = mutableStateMapOf<String, DownloadState>()
    private val activeJobs = mutableStateMapOf<String, Job>()

    var searchQuery by mutableStateOf("")
        private set

    init {
        fetchModels()
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun searchModels() {
        fetchModels(searchQuery)
    }

    fun fetchModels(query: String = "") {
        viewModelScope.launch {
            val models = store.fetchAvailableModels(query)
            availableModels.clear()
            availableModels.addAll(models)
        }
    }

    fun startDownload(model: RemoteModel) {
        if (activeJobs.containsKey(model.id)) return

        val job = viewModelScope.launch {
            val file = repository.getModelFile(model.fileName)
            downloader.download(model.downloadUrl, file).collectLatest { state ->
                downloadStates[model.id] = state
                if (!state.isDownloading && state.error == null && state.progress == 1f) {
                    activeJobs.remove(model.id)
                }
            }
        }
        activeJobs[model.id] = job
    }

    fun cancelDownload(modelId: String) {
        activeJobs[modelId]?.cancel()
        activeJobs.remove(modelId)
        val currentState = downloadStates[modelId]
        if (currentState != null) {
            downloadStates[modelId] = currentState.copy(isDownloading = false)
        }
    }

    fun isDownloaded(model: RemoteModel): Boolean {
        return repository.isModelDownloaded(model.fileName)
    }

    override fun onCleared() {
        super.onCleared()
        activeJobs.values.forEach { it.cancel() }
    }
}
