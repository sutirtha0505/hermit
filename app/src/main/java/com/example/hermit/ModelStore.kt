package com.example.hermit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
private data class HFModelResponse(
    val id: String,
    val siblings: List<HFSibling>? = null
)

@Serializable
private data class HFSibling(
    val rfilename: String,
    val size: Long? = null
)

class ModelStore(private val client: OkHttpClient) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchAvailableModels(query: String = ""): List<RemoteModel> = withContext(Dispatchers.IO) {
        val searchParam = if (query.isNotBlank()) "search=${query}" else "search=gguf"
        val request = Request.Builder()
            .url("https://huggingface.co/api/models?$searchParam&limit=100&full=true")
            .header("User-Agent", "Hermit-Android-App")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyString = response.body?.string() ?: return@withContext emptyList()
                val hfModels = json.decodeFromString<List<HFModelResponse>>(bodyString)
                
                hfModels.flatMap { hfModel ->
                    val repo = hfModel.id
                    val ggufFiles = hfModel.siblings?.filter { 
                        it.rfilename.endsWith(".gguf", ignoreCase = true) &&
                        isAllowedQuant(it.rfilename)
                    } ?: emptyList()

                    ggufFiles.map { file ->
                        RemoteModel(
                            id = "${repo}/${file.rfilename}",
                            name = parseModelName(repo, file.rfilename),
                            repo = repo,
                            fileName = file.rfilename,
                            downloadUrl = "https://huggingface.co/${repo}/resolve/main/${file.rfilename}?download=true",
                            sizeBytes = file.size ?: 0L,
                            description = "Repository: ${repo}"
                        )
                    }
                }.filter { !it.fileName.contains("7b", true) && !it.fileName.contains("8b", true) }
                .take(50)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun isAllowedQuant(fileName: String): Boolean {
        val quants = listOf("Q4_0", "Q4_K_M", "Q5_K_M", "IQ4_XS")
        return quants.any { fileName.contains(it, ignoreCase = true) }
    }

    private fun parseModelName(repo: String, fileName: String): String {
        val cleanRepo = repo.split("/").last().replace("-GGUF", "", true)
        val quant = fileName.split(".").dropLast(1).lastOrNull() ?: ""
        return "${cleanRepo} (${quant})"
    }
}
