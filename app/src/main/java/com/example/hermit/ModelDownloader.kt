package com.example.hermit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile

class ModelDownloader(private val client: OkHttpClient) {

    fun download(url: String, outputFile: File): Flow<DownloadState> = flow {
        val existingLength = if (outputFile.exists()) outputFile.length() else 0L
        
        emit(DownloadState(isDownloading = true, downloadedBytes = existingLength))

        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Hermit-Android-App")
            .addHeader("Accept", "*/*")
            .header("Range", "bytes=$existingLength-")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 206) {
                    emit(DownloadState(error = "Server error: ${response.code}", isDownloading = false))
                    return@flow
                }

                val body = response.body
                if (body == null) {
                    emit(DownloadState(error = "Empty response body", isDownloading = false))
                    return@flow
                }

                val totalBytes = (body.contentLength() + existingLength)
                val inputStream = body.byteStream()
                
                // Ensure parent directory exists
                outputFile.parentFile?.mkdirs()

                val raf = RandomAccessFile(outputFile, "rw")
                raf.seek(existingLength)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var currentDownloaded = existingLength

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    raf.write(buffer, 0, bytesRead)
                    currentDownloaded += bytesRead
                    
                    val progress = if (totalBytes > 0) currentDownloaded.toFloat() / totalBytes else 0f
                    emit(DownloadState(
                        progress = progress,
                        isDownloading = true,
                        downloadedBytes = currentDownloaded,
                        totalBytes = totalBytes
                    ))
                }
                
                raf.close()
                emit(DownloadState(progress = 1f, isDownloading = false, downloadedBytes = totalBytes, totalBytes = totalBytes))
            }
        } catch (e: Exception) {
            emit(DownloadState(error = e.localizedMessage ?: "Unknown error", isDownloading = false))
        }
    }.flowOn(Dispatchers.IO)
}
