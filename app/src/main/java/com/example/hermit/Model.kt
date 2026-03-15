package com.example.hermit

import kotlinx.serialization.Serializable

@Serializable
data class RemoteModel(
    val id: String,
    val name: String,
    val repo: String,
    val fileName: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val description: String? = null
)

data class DownloadState(
    val progress: Float = 0f,
    val isDownloading: Boolean = false,
    val error: String? = null,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0
)

data class LocalModel(
    val name: String,
    val path: String,
    val isDownloaded: Boolean = false
)
