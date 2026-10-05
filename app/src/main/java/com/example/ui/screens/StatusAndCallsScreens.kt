package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.CallLogEntity
import com.example.data.StatusUpdateEntity
import com.example.data.UserProfileEntity
import com.example.ui.components.ContactAvatar
import com.example.ui.components.EncryptedSecurityPill
import com.example.ui.components.formatChatTimestamp
import com.example.ui.components.parseHexColor
import com.example.ui.theme.DangerRed
import com.example.ui.theme.MeetupEmerald
import com.example.ui.theme.MeetupTealDark

@Composable
fun StatusScreen(
    userProfile: UserProfileEntity?,
    statuses: List<StatusUpdateEntity>,
    viewingStatus: StatusUpdateEntity?,
    onPostStatus: (caption: String, colorHex: String) -> Unit,
    onOpenStatus: (StatusUpdateEntity) -> Unit,
    onCloseStatusViewer: () -> Unit
) {
    var showCreateStatusDialog by remember { mutableStateOf(false) }

    val myStatuses = statuses.filter { it.isMine }
    val contactStatuses = statuses.filter { !it.isMine }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("status_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // My Status Card
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (myStatuses.isNotEmpty()) {
                                onOpenStatus(myStatuses.first())
                            } else {
                                showCreateStatusDialog = true
                            }
                        }
                        .testTag("my_status_card")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            ContactAvatar(
                                name = userProfile?.displayName ?: "My Status",
                                colorHex = userProfile?.avatarColorHex ?: "#00A884",
                                size = 54.dp
                            )
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(MeetupEmerald)
                                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Status",
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "My Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (myStatuses.isNotEmpty()) {
                                    "Latest: ${myStatuses.first().captionText}"
                                } else {
                                    "Tap to add an encrypted 24h status update"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { showCreateStatusDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Compose Status",
                                tint = MeetupTealDark
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Recent Encrypted Updates",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(contactStatuses, key = { it.id }) { status ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenStatus(status) }
                        .testTag("status_item_${status.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .border(
                                    width = 2.5.dp,
                                    color = if (status.isViewedByMe) {
                                        MaterialTheme.colorScheme.outlineVariant
                                    } else {
                                        MeetupEmerald
                                    },
                                    shape = CircleShape
                                )
                                .padding(4.dp)
                        ) {
                            ContactAvatar(
                                name = status.authorName,
                                colorHex = status.backgroundColorHex,
                                size = 48.dp
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = status.authorName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = status.captionText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Text(
                                text = formatChatTimestamp(status.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EncryptedSecurityPill(text = "Your status updates are end-to-end encrypted")
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateStatusDialog = true },
            containerColor = MeetupEmerald,
            contentColor = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("fab_post_status")
        ) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = "Post Status")
        }

        // Full-Screen Story Viewer Overlay
        if (viewingStatus != null) {
            BackHandler(onBack = onCloseStatusViewer)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(parseHexColor(viewingStatus.backgroundColorHex, MeetupTealDark))
                    .statusBarsPadding()
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    LinearProgressIndicator(
                        progress = { 0.85f },
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ContactAvatar(
                                name = viewingStatus.authorName,
                                colorHex = "#00A884",
                                size = 40.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = viewingStatus.authorName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formatChatTimestamp(viewingStatus.timestamp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                        IconButton(onClick = onCloseStatusViewer) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Status",
                                tint = Color.White
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = viewingStatus.captionText,
                            style = MaterialTheme.typography.displayLarge,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Views",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${viewingStatus.viewedCount} encrypted views",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    if (showCreateStatusDialog) {
        var caption by remember { mutableStateOf("") }
        val colorChoices = listOf("#075E54", "#7C3AED", "#0284C7", "#EA580C", "#E11D48")
        var selectedColor by remember { mutableStateOf(colorChoices.first()) }

        AlertDialog(
            onDismissRequest = { showCreateStatusDialog = false },
            title = { Text("Post Encrypted Status Update") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        label = { Text("What's on your mind?") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("status_caption_input")
                    )

                    Text("Background Theme Color:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        colorChoices.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(hex))
                                    .border(
                                        width = if (selectedColor == hex) 3.dp else 0.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (caption.isNotBlank()) {
                            onPostStatus(caption, selectedColor)
                            showCreateStatusDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_post_status_button")
                ) {
                    Text("Share Status")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateStatusDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CallsLogScreen(
    callLogs: List<CallLogEntity>,
    onStartCall: (CallLogEntity, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("calls_log_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MeetupTealDark
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "All Meetup Voice & HD Video Calls are protected with real-time SRTP End-to-End Encryption.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        items(callLogs, key = { it.id }) { log ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ContactAvatar(
                    name = log.contactName,
                    colorHex = log.avatarColorHex,
                    size = 50.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = log.contactName,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (log.isMissed) DangerRed else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when {
                                log.isMissed -> Icons.Default.CallMissed
                                log.isIncoming -> Icons.Default.CallReceived
                                else -> Icons.Default.CallMade
                            },
                            contentDescription = null,
                            tint = if (log.isMissed) DangerRed else MeetupEmerald,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val mins = log.durationSeconds / 60
                        val secs = log.durationSeconds % 60
                        val durStr = if (log.isMissed) "Missed Call" else "${mins}m ${secs}s"
                        Text(
                            text = "${formatChatTimestamp(log.timestamp)} • $durStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { onStartCall(log, false) },
                    modifier = Modifier.testTag("call_voice_${log.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Voice Call",
                        tint = MeetupTealDark
                    )
                }

                IconButton(
                    onClick = { onStartCall(log, true) },
                    modifier = Modifier.testTag("call_video_${log.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Video Call",
                        tint = MeetupTealDark
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        }
    }
}
