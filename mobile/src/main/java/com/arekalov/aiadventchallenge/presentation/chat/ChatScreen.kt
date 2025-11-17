package com.arekalov.aiadventchallenge.presentation.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arekalov.aiadventchallenge.R
import com.arekalov.aiadventchallenge.domain.model.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    // Обработка SideEffects
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is ChatSideEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                ChatSideEffect.ScrollToBottom -> {
                    if (state.messages.isNotEmpty()) {
                        listState.animateScrollToItem(state.messages.size - 1)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        modifier = Modifier.imePadding()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Messages list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.messages, key = { it.id }) { message ->
                    MessageItem(message = message)
                }

                // Loading indicator
                if (state.isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                shape = RoundedCornerShape(
                                    topStart = 4.dp,
                                    topEnd = 16.dp,
                                    bottomStart = 16.dp,
                                    bottomEnd = 16.dp
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.typing_indicator),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Settings toggle button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                FilledTonalIconButton(
                    onClick = { viewModel.handleIntent(ChatIntent.ToggleSettings) }
                ) {
                    Icon(
                        imageVector = if (state.isSettingsExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = if (state.isSettingsExpanded) "Скрыть настройки" else "Показать настройки"
                    )
                }
            }
            
            // Collapsible settings
            AnimatedVisibility(
                visible = state.isSettingsExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    // Model selector
                    ModelSelector(
                        availableModels = state.availableModels,
                        selectedModelId = state.selectedModelId,
                        onModelSelected = { modelId ->
                            viewModel.handleIntent(ChatIntent.SelectModel(modelId))
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    
                    // Temperature selector
                    TemperatureSelector(
                        selectedTemperature = state.selectedTemperature,
                        onTemperatureChange = { temperature ->
                            viewModel.handleIntent(ChatIntent.UpdateTemperature(temperature))
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    
                    // Day 8: Compress history button
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Button(
                            onClick = { viewModel.handleIntent(ChatIntent.CompressHistory) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            enabled = !state.isCompressing && !state.isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (state.isCompressing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(text = "🗜️")
                                }
                                Text(
                                    text = if (state.isCompressing) "Сжатие..." else "Сжать историю",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        if (state.tokensSaved > 0) {
                            Text(
                                text = "Сэкономлено токенов: ${state.tokensSaved}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    // Day 9: Clear history button
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Button(
                            onClick = { viewModel.handleIntent(ChatIntent.ClearHistory) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            enabled = !state.isLoading && state.messages.size > 1,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🗑️")
                                Text(
                                    text = "Очистить историю",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    // Token test mode toggle
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        TextButton(
                            onClick = { viewModel.handleIntent(ChatIntent.ToggleTokenTestMode) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Token tests"
                                    )
                                    Text(
                                        text = "Тесты токенов",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Icon(
                                    imageVector = if (state.isTokenTestModeEnabled) 
                                        Icons.Default.KeyboardArrowUp 
                                    else 
                                        Icons.Default.KeyboardArrowDown,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                    
                    // Token test panel
                    AnimatedVisibility(
                        visible = state.isTokenTestModeEnabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        TokenTestPanel(
                            onTestSelected = { testType ->
                                viewModel.handleIntent(ChatIntent.SendTokenTest(testType))
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            // Token usage visualization
            TokenUsageBar(
                currentUsage = state.currentTokenUsage,
                limit = state.tokenLimit,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Input field
            MessageInput(
                inputText = state.inputText,
                onInputTextChanged = { text ->
                    viewModel.handleIntent(ChatIntent.UpdateInputText(text))
                },
                onSendClick = {
                    viewModel.handleIntent(ChatIntent.SendMessage(state.inputText))
                },
                enabled = !state.isLoading
            )
        }
    }
}

@Composable
fun MessageItem(message: Message) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (message.isSummary) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else if (message.isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            ),
            shape = RoundedCornerShape(
                topStart = if (message.isUser) 16.dp else 4.dp,
                topEnd = if (message.isUser) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Day 8: Badge для сжатых сообщений
                if (message.isSummary) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🗜️ СЖАТО",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        if (message.summarizedCount > 0) {
                            Text(
                                text = "(${message.summarizedCount} сообщ.)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (message.isSummary) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else if (message.isUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
                Spacer(modifier = Modifier.size(4.dp))
                
                // Метаинформация: время, категория, токены
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (message.isUser) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        }
                    )
                    
                    // Категория (только для сообщений ассистента)
                    if (!message.isUser && message.category.isNotEmpty()) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (message.isUser) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                            }
                        )
                        Text(
                            text = message.category,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (message.isUser) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            }
                        )
                    }
                    
                    // Метрики (только для сообщений ассистента)
                    message.metrics?.let { metrics ->
                        if (!message.isUser) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "⏱️ ${metrics.getFormattedTime()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "📥 ${metrics.inputTokens}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "📤 ${metrics.outputTokens}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    } ?: run {
                        if (!message.isUser && message.totalTokens != null) {
                            // Fallback для старых сообщений без metrics
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "${message.totalTokens} tokens",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageInput(
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = onInputTextChanged,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.input_hint)) },
                enabled = enabled,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                maxLines = 5
            )
            
            AnimatedVisibility(
                visible = inputText.isNotBlank() && enabled,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    onClick = onSendClick,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.send_button),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

// Previews
@Preview(showBackground = true)
@Composable
private fun MessageItemUserPreview() {
    MaterialTheme {
        MessageItem(
            message = Message(
                id = "1",
                text = "Это тестовое сообщение пользователя",
                isUser = true,
                timestamp = System.currentTimeMillis(),
                category = "Еда",
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MessageItemAssistantPreview() {
    MaterialTheme {
        MessageItem(
            message = Message(
                id = "2",
                text = "Это тестовое сообщение от AI ассистента с длинным текстом для проверки отображения",
                isUser = false,
                timestamp = System.currentTimeMillis(),
                category = "Развлечения",
                totalTokens = 150
            )
        )
    }
}

@Composable
fun ModelSelector(
    availableModels: List<com.arekalov.aiadventchallenge.domain.model.ModelInfo>,
    selectedModelId: String,
    onModelSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    val selectedModel = availableModels.find { it.id == selectedModelId }
        ?: availableModels.firstOrNull()
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            TextButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedModel?.displayName ?: "Выберите модель",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = selectedModel?.description ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableModels.forEach { model ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = model.displayName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = model.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            onModelSelected(model.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TemperatureSelector(
    selectedTemperature: Float,
    onTemperatureChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    val temperatureOptions = listOf(
        0.0f to "🧊 Точный (0.0)",
        0.7f to "🌤️ Сбалансированный (0.7)",
        1.0f to "🔥 Креативный (1.0)"
    )
    
    val selectedOption = temperatureOptions.find { it.first == selectedTemperature }
        ?: temperatureOptions[1]
    
    val (emoji, description) = when {
        selectedTemperature <= 0.3f -> "🧊" to "Точный режим"
        selectedTemperature < 1.0f -> "🌤️" to "Сбалансированный режим"
        else -> "🔥" to "Креативный режим"
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            TextButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "%.1f".format(selectedTemperature),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                temperatureOptions.forEach { (temp, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onTemperatureChange(temp)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MessageInputPreview() {
    MaterialTheme {
        MessageInput(
            inputText = "Пример текста",
            onInputTextChanged = {},
            onSendClick = {},
            enabled = true
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MessageInputEmptyPreview() {
    MaterialTheme {
        MessageInput(
            inputText = "",
            onInputTextChanged = {},
            onSendClick = {},
            enabled = true
        )
    }
}

@Composable
fun TokenUsageBar(
    currentUsage: Int,
    limit: Int,
    modifier: Modifier = Modifier
) {
    val progress = (currentUsage.toFloat() / limit.toFloat()).coerceIn(0f, 1f)
    val isNearLimit = progress > 0.8f
    val isOverLimit = progress >= 1.0f
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Использование токенов",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$currentUsage / $limit",
                    style = MaterialTheme.typography.labelMedium,
                    color = when {
                        isOverLimit -> MaterialTheme.colorScheme.error
                        isNearLimit -> Color(0xFFFF9800) // Orange
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    isOverLimit -> MaterialTheme.colorScheme.error
                    isNearLimit -> Color(0xFFFF9800) // Orange
                    else -> MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            if (isNearLimit) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isOverLimit) 
                        "⚠️ Превышен лимит контекста!" 
                    else 
                        "⚠️ Приближается лимит контекста",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOverLimit) 
                        MaterialTheme.colorScheme.error 
                    else 
                        Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
fun TokenTestPanel(
    onTestSelected: (TokenTestType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "🧪 Тестирование токенов",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Протестируйте поведение модели с запросами разной длины:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Test buttons
            TokenTestType.values().forEach { testType ->
                Button(
                    onClick = { onTestSelected(testType) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = TokenTestUtils.getTestDescription(testType),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = when (testType) {
                                    TokenTestType.SHORT -> Icons.Default.Check
                                    TokenTestType.MEDIUM -> Icons.Default.Info
                                    TokenTestType.LONG -> Icons.Default.Warning
                                    TokenTestType.OVERFLOW -> Icons.Default.Close
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = TokenTestUtils.getExpectedBehavior(testType),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

