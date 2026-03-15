package com.example.hermit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nativelib.NativeLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.hermit.db.*

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val isSystem: Boolean = false,
    val tokensPerSecond: Float? = null,
    val isLiked: Boolean = false,
    val isDisliked: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val nativeLib = NativeLib()
    private val dao = AppDatabase.getDatabase(application).chatDao()

    val chatSessions: Flow<List<ChatSession>> = dao.getAllSessions()
    
    var currentSessionId by mutableStateOf<String?>(null)
        private set

    var messages = mutableStateListOf<ChatMessage>()
        private set
    
    var isModelLoaded by mutableStateOf(false)
        private set

    var isGenerating by mutableStateOf(false)
        private set

    private var generationStartTime: Long = 0
    private var generatedTokensCount: Int = 0

    init {
        nativeLib.setTokenListener { token ->
            generatedTokensCount++
            // Look for the last message (which should be the assistant's empty placeholder)
            val lastIdx = messages.lastIndex
            if (lastIdx >= 0 && !messages[lastIdx].isUser) {
                messages[lastIdx] = messages[lastIdx].copy(
                    text = messages[lastIdx].text + token
                )
            }
        }
    }

    fun startNewSession() {
        if (isGenerating) return
        currentSessionId = null
        messages.clear()
        messages.add(ChatMessage(text = "System: Switched to a new empty chat.", isUser = false, isSystem = true))
    }

    fun loadSession(sessionId: String) {
        if (isGenerating) return
        viewModelScope.launch(Dispatchers.IO) {
            val history = dao.getMessagesForSession(sessionId)
            withContext(Dispatchers.Main) {
                messages.clear()
                currentSessionId = sessionId
                history.forEach { entity ->
                    messages.add(
                        ChatMessage(
                            id = entity.id,
                            text = entity.text,
                            isUser = entity.isUser,
                            isSystem = entity.isSystem,
                            tokensPerSecond = entity.tokensPerSecond,
                            isLiked = entity.isLiked,
                            isDisliked = entity.isDisliked
                        )
                    )
                }
            }
        }
    }

    fun loadModel(path: String) {
        val modelName = path.substringAfterLast("/")
        messages.add(ChatMessage(text = "System: ⏳ Initializing engine and loading model: $modelName. This may take a moment...", isUser = false, isSystem = true))
        
        viewModelScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val result = nativeLib.loadModel(path)
            val loadTimeMs = System.currentTimeMillis() - startTime
            
            if (result == 0) {
                isModelLoaded = true
                withContext(Dispatchers.Main) {
                    messages.add(ChatMessage(text = "System: ✅ Model loaded successfully!\n- Name: $modelName\n- Load time: ${loadTimeMs}ms\n- Context window: 2048 tokens\n- Backend: CPU", isUser = false, isSystem = true))
                }
            } else {
                withContext(Dispatchers.Main) {
                    messages.add(ChatMessage(text = "System: ❌ Failed to load model.", isUser = false, isSystem = true))
                }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        if (!isModelLoaded || isGenerating) return
        
        val userMsgId = java.util.UUID.randomUUID().toString()
        val assistantMsgId = java.util.UUID.randomUUID().toString()
        
        val userMsg = ChatMessage(id = userMsgId, text = text, isUser = true)
        messages.add(userMsg)
        
        // Add empty assistant message placeholder
        val assistantMsg = ChatMessage(id = assistantMsgId, text = "", isUser = false)
        messages.add(assistantMsg)

        isGenerating = true
        generationStartTime = System.currentTimeMillis()
        generatedTokensCount = 0

        viewModelScope.launch(Dispatchers.IO) {
            // Persist session if it's new
            if (currentSessionId == null) {
                val newSessionId = java.util.UUID.randomUUID().toString()
                val title = if (text.length > 30) text.take(30) + "..." else text
                dao.insertSession(ChatSession(id = newSessionId, title = title))
                withContext(Dispatchers.Main) { currentSessionId = newSessionId }
            }
            
            val sid = currentSessionId ?: return@launch
            
            // Persist user message
            dao.insertMessage(
                ChatMessageEntity(
                    id = userMsgId,
                    sessionId = sid,
                    text = text,
                    isUser = true,
                    isSystem = false,
                    tokensPerSecond = null,
                    isLiked = false,
                    isDisliked = false
                )
            )
            nativeLib.completion(text)
            
            withContext(Dispatchers.Main) {
                isGenerating = false
                val timeSpentMs = System.currentTimeMillis() - generationStartTime
                var finalTps: Float? = null
                if (timeSpentMs > 0 && generatedTokensCount > 0) {
                    finalTps = (generatedTokensCount * 1000f) / timeSpentMs
                    val lastIdx = messages.lastIndex
                    if (lastIdx >= 0 && !messages[lastIdx].isUser) {
                        messages[lastIdx] = messages[lastIdx].copy(
                            tokensPerSecond = finalTps
                        )
                    }
                }
                
                // Persist final assistant message
                val lastIdx = messages.lastIndex
                if (lastIdx >= 0 && !messages[lastIdx].isUser) {
                    val finalMsg = messages[lastIdx]
                    viewModelScope.launch(Dispatchers.IO) {
                        dao.insertMessage(
                            ChatMessageEntity(
                                id = assistantMsgId,
                                sessionId = sid,
                                text = finalMsg.text,
                                isUser = false,
                                isSystem = false,
                                tokensPerSecond = finalTps,
                                isLiked = false,
                                isDisliked = false
                            )
                        )
                    }
                }
            }
        }
    }

    private fun persistMessageUpdate(message: ChatMessage) {
        val sid = currentSessionId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateMessage(
                ChatMessageEntity(
                    id = message.id,
                    sessionId = sid,
                    text = message.text,
                    isUser = message.isUser,
                    isSystem = message.isSystem,
                    tokensPerSecond = message.tokensPerSecond,
                    isLiked = message.isLiked,
                    isDisliked = message.isDisliked
                )
            )
        }
    }

    fun deleteSession(session: ChatSession) {
        if (currentSessionId == session.id) {
            startNewSession()
        }
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteSession(session)
        }
    }

    fun setLike(messageId: String, liked: Boolean) {
        val idx = messages.indexOfFirst { it.id == messageId }
        if (idx != -1) {
            val msg = messages[idx]
            val updated = msg.copy(isLiked = liked, isDisliked = if (liked) false else msg.isDisliked)
            messages[idx] = updated
            persistMessageUpdate(updated)
        }
    }

    fun setDislike(messageId: String, disliked: Boolean) {
        val idx = messages.indexOfFirst { it.id == messageId }
        if (idx != -1) {
            val msg = messages[idx]
            val updated = msg.copy(isDisliked = disliked, isLiked = if (disliked) false else msg.isLiked)
            messages[idx] = updated
            persistMessageUpdate(updated)
        }
    }

    fun stopGeneration() {
        if (isGenerating) {
            nativeLib.stopGeneration()
        }
    }
}
