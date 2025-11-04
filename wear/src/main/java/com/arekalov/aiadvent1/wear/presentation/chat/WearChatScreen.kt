package com.arekalov.aiadvent1.wear.presentation.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.material.dialog.Dialog
import com.arekalov.aiadvent1.domain.model.Message
import kotlin.random.Random

@Composable
fun WearChatScreen(
    viewModel: WearChatViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberScalingLazyListState()
    var showTopicDialog by remember { mutableStateOf(false) }

    val topics = listOf(
        "Расскажи анекдот про работу",
        "Расскажи смешную историю про учёбу",
        "Придумай анекдот про отношения",
        "Расскажи анекдот про программистов",
        "Придумай смешную историю про путешествия",
        "Расскажи анекдот про домашних животных",
        "Придумай смешную историю про спорт",
        "Расскажи анекдот про еду",
        "Придумай анекдот про технологии",
        "Расскажи смешную историю про семью"
    )

    // Автопрокрутка к последнему сообщению
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size)
        }
    }

    Scaffold(
        timeText = { TimeText() },
        vignette = { 
            Vignette(vignettePosition = VignettePosition.TopAndBottom) 
        },
        positionIndicator = {
            PositionIndicator(scalingLazyListState = listState)
        }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(
                top = 32.dp,
                bottom = 32.dp,
                start = 8.dp,
                end = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Сообщения
            items(uiState.messages, key = { it.id }) { message ->
                WearMessageItem(message = message)
            }

            // Индикатор загрузки
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp
                        )
                    }
                }
            }

            // Кнопка "Выбрать тему"
            item {
                Chip(
                    onClick = { showTopicDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading,
                    label = {
                        Text(
                            text = "📝 Выбрать тему",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = ChipDefaults.primaryChipColors()
                )
            }

            // Кнопка "Случайный анекдот"
            item {
                Chip(
                    onClick = { 
                        val randomTopic = topics[Random.nextInt(topics.size)]
                        viewModel.sendMessage(randomTopic)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading,
                    label = {
                        Text(
                            text = "🎲 Случайный анекдот",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = ChipDefaults.secondaryChipColors()
                )
            }
        }
    }

    // Диалог выбора темы - показываем как отдельный экран в списке
    if (showTopicDialog) {
        Dialog(
            showDialog = true,
            onDismissRequest = { showTopicDialog = false }
        ) {
            Scaffold(
                timeText = { TimeText() },
                vignette = {
                    Vignette(vignettePosition = VignettePosition.TopAndBottom)
                },
                positionIndicator = {
                    PositionIndicator(scalingLazyListState = rememberScalingLazyListState())
                }
            ) {
                ScalingLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        Text(
                            text = "Выберите тему:",
                            style = MaterialTheme.typography.title3,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    items(topics.size) { index ->
                        Chip(
                            onClick = {
                                viewModel.sendMessage(topics[index])
                                showTopicDialog = false
                            },
                            label = {
                                Text(
                                    text = topics[index].replace("Расскажи анекдот про ", "")
                                        .replace("Расскажи смешную историю про ", "")
                                        .replace("Придумай анекдот про ", "")
                                        .replace("Придумай смешную историю про ", "")
                                        .replaceFirstChar { it.uppercase() },
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = ChipDefaults.primaryChipColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    // Показываем ошибку если есть
    if (uiState.error != null) {
        LaunchedEffect(uiState.error) {
            viewModel.clearError()
        }
    }
}

@Composable
fun WearMessageItem(message: Message) {
    Card(
        onClick = { /* Можно добавить действия */ },
        modifier = Modifier.fillMaxWidth(),
        backgroundPainter = if (message.isUser) {
            androidx.wear.compose.material.CardDefaults.cardBackgroundPainter(
                startBackgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.3f),
                endBackgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.2f)
            )
        } else {
            androidx.wear.compose.material.CardDefaults.cardBackgroundPainter(
                startBackgroundColor = MaterialTheme.colors.surface,
                endBackgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.8f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = if (message.isUser) "Вы" else "AI",
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message.text,
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Start,
                maxLines = 10,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
