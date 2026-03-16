package com.example.hermit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

@Serializable
private data class HFModelResponse(
    val id: String,
    val siblings: List<HFSibling>? = null
)

@Serializable
private data class HFSibling(
    val rfilename: String
)

class ModelStore(private val client: OkHttpClient) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchAvailableModels(query: String = ""): List<RemoteModel> =
        withContext(Dispatchers.IO) {
            val searchTerm = if (query.isNotBlank()) "$query gguf" else "gguf"
            val encodedSearch = try {
                URLEncoder.encode(searchTerm, "UTF-8")
            } catch (_: Exception) {
                searchTerm
            }

            val request = Request.Builder()
                .url("https://huggingface.co/api/models?search=$encodedSearch&limit=100&full=true")
                .header("User-Agent", "Hermit-Android-App")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext emptyList()
                    val body = response.body?.string() ?: return@withContext emptyList()
                    val hfModels = json.decodeFromString<List<HFModelResponse>>(body)

                    val models = hfModels.flatMap { repo ->
                        val repoId = repo.id
                        val ggufFiles = repo.siblings?.filter {
                            it.rfilename.endsWith(".gguf", true) &&
                                    isAllowedQuant(it.rfilename) &&
                                    isMobileSafe(it.rfilename)
                        } ?: emptyList()

                        ggufFiles.map { file ->
                            val url = "https://huggingface.co/$repoId/resolve/main/${file.rfilename}"
                            RemoteModel(
                                id = "$repoId/${file.rfilename}",
                                name = parseModelName(repoId, file.rfilename),
                                repo = repoId,
                                fileName = file.rfilename,
                                downloadUrl = url,
                                sizeBytes = 0L,
                                description = "Repository: $repoId"
                            )
                        }
                    }.take(20)

                    fetchSizes(models)
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

    private suspend fun fetchSizes(models: List<RemoteModel>): List<RemoteModel> =
        coroutineScope {
            models.map { model ->
                async {
                    val size = fetchFileSize(model.downloadUrl)
                    model.copy(sizeBytes = size)
                }
            }.awaitAll()
        }

    suspend fun fetchFileSize(url: String): Long =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .head()
                    .header("User-Agent", "Hermit-Android-App")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext 0L
                    val linked = response.header("X-Linked-Size")?.toLongOrNull()
                    val normal = response.header("Content-Length")?.toLongOrNull()
                    linked ?: normal ?: 0L
                }
            } catch (_: Exception) {
                0L
            }
        }

    private fun isAllowedQuant(fileName: String): Boolean {
        val allowed = listOf(
            "Q4_0", "Q4_K_M", "Q4_K_S",
            "Q5_0", "Q5_K_M", "Q5_K_S",
            "Q3_K_M", "Q3_K_S",
            "IQ4_XS", "IQ4_NL",
            "Q8_0"
        )
        return allowed.any { fileName.contains(it, true) }
    }

    private fun isMobileSafe(fileName: String): Boolean {
        // Exclude models larger than 14B parameters which are usually too heavy for mobile RAM
        val banned = listOf(
            "30b", "34b", "70b", "120b"
        )
        return banned.none { fileName.contains(it, true) }
    }

    private fun parseModelName(repo: String, fileName: String): String {
        val repoName = repo.substringAfter("/")
        val cleanFileName = fileName.removeSuffix(".gguf").removeSuffix(".GGUF")
        
        val commonQuants = listOf("Q4_K_M", "Q4_0", "Q5_K_M", "Q3_K_M", "Q8_0", "IQ4_XS")
        val foundQuant = commonQuants.find { fileName.contains(it, true) }
        
        val quantDisplay = foundQuant ?: cleanFileName.substringAfterLast("-").substringAfterLast(".")
        return "$repoName ($quantDisplay)"
    }
}
