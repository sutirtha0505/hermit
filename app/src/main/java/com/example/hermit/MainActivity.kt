package com.example.hermit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mikepenz.markdown.m3.Markdown
import com.example.hermit.ui.theme.HermitTheme

class MainActivity : ComponentActivity() {
    private val chatViewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HermitTheme {
                var currentScreen by remember { mutableStateOf("chat") }

                when (currentScreen) {
                    "chat" -> ChatScreen(
                        viewModel = chatViewModel,
                        onOpenSettings = { currentScreen = "models" }
                    )
                    "models" -> ModelListScreen(
                        onModelSelected = { model, useGpu ->
                            chatViewModel.loadModel(model.path, useGpu)
                            currentScreen = "chat"
                        },
                        onImportModel = { /* TODO: File picker */ },
                        onBack = { currentScreen = "chat" },
                        onDownloadNew = { currentScreen = "download" }
                    )
                    "download" -> ModelDownloadScreen(
                        onBack = { currentScreen = "models" }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel, onOpenSettings: () -> Unit) {
    var inputText by remember { mutableStateOf("") }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val sessions by viewModel.chatSessions.collectAsState(initial = emptyList())

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Chat History", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.headlineSmall)
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("New Chat") },
                    selected = viewModel.currentSessionId == null,
                    icon = { Icon(Icons.Default.Add, contentDescription = "New Chat") },
                    onClick = { 
                        viewModel.startNewSession()
                        scope.launch { drawerState.close() } 
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                LazyColumn {
                    items(sessions) { session ->
                        NavigationDrawerItem(
                            label = { Text(session.title, maxLines = 1) },
                            selected = viewModel.currentSessionId == session.id,
                            onClick = { 
                                viewModel.loadSession(session.id)
                                scope.launch { drawerState.close() } 
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Hermit AI") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Models")
                        }
                    }
                )
            },
            bottomBar = {
                ChatInput(
                    text = inputText,
                    onTextChange = { inputText = it },
                    isGenerating = viewModel.isGenerating,
                    onSend = {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    },
                    onStop = { viewModel.stopGeneration() }
                )
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(8.dp),
                    reverseLayout = false
                ) {
                    items(viewModel.messages) { message ->
                        ChatBubble(
                            message = message,
                            onLike = { liked -> viewModel.setLike(message.id, liked) },
                            onDislike = { disliked -> viewModel.setDislike(message.id, disliked) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    onLike: (Boolean) -> Unit = {},
    onDislike: (Boolean) -> Unit = {}
) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val color = if (message.isUser || message.isSystem) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = alignment) {
        Column(horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color,
                tonalElevation = 2.dp,
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                if (message.isUser || message.isSystem) {
                    Text(
                        text = message.text,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    Markdown(
                        content = message.text,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            
            if (!message.isUser && !message.isSystem && message.text.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (message.tokensPerSecond != null) {
                        Text(
                            text = String.format("%.1f t/s", message.tokensPerSecond),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Hermit AI", message.text)
                            clipboard.setPrimaryClip(clip)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    IconButton(
                        onClick = { onLike(!message.isLiked) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (message.isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp, 
                            contentDescription = "Like",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    IconButton(
                         onClick = { onDislike(!message.isDisliked) },
                         modifier = Modifier.size(32.dp)
                    ) {
                         Icon(
                             imageVector = if (message.isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                             contentDescription = "Dislike",
                             modifier = Modifier.size(16.dp)
                         )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    isGenerating: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit
) {
    Surface(tonalElevation = 4.dp) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth().navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(if (isGenerating) "Generating..." else "Ask anything...") },
                enabled = !isGenerating,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                )
            )
            
            if (isGenerating) {
                IconButton(onClick = onStop) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop Generation", tint = MaterialTheme.colorScheme.error)
                }
            } else {
                IconButton(onClick = onSend, enabled = text.isNotBlank()) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}
