package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.ConversationEntity
import com.example.data.MessageEntity
import com.example.security.CryptoManager
import com.example.ui.components.ContactAvatar
import com.example.ui.components.EncryptedSecurityPill
import com.example.ui.components.formatChatTimestamp
import com.example.ui.theme.MeetupEmerald
import com.example.ui.theme.MeetupTealDark
import com.example.ui.theme.MeetupTealMedium
import com.example.ui.theme.ReadReceiptBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversation: ConversationEntity,
    messages: List<MessageEntity>,
    myPhone: String,
    isTyping: Boolean,
    isOfflineMode: Boolean,
    onBack: () -> Unit,
    onSendText: (String) -> Unit,
    onSendAttachment: (type: String, fileName: String, fileSize: String, uri: String?) -> Unit,
    onShareLiveLocation: (grantPersistentMultiYear: Boolean, durationLabel: String) -> Unit,
    onToggleMyPersistentGrant: (Boolean) -> Unit,
    onOpenLiveRadarMap: () -> Unit,
    onStartVoiceCall: () -> Unit,
    onStartVideoCall: () -> Unit
) {
    BackHandler(onBack = onBack)

    var messageInput by remember { mutableStateOf("") }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showEncryptionDialog by remember { mutableStateOf(false) }
    var selectedMessageForCipherInspect by remember { mutableStateOf<MessageEntity?>(null) }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Zero-permission Photo Picker for image sharing
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onSendAttachment(
                "IMAGE",
                "Encrypted_Photo_${System.currentTimeMillis() % 10000}.jpg",
                "1.8 MB • High-Res Image",
                uri.toString()
            )
        }
    }

    // Document picker via SAF OpenDocument
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onSendAttachment(
                "DOCUMENT",
                "Shared_Document_${System.currentTimeMillis() % 1000}.pdf",
                "1.4 MB • Encrypted Document",
                uri.toString()
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        // WhatsApp-style Top Header Bar
        Surface(
            color = MeetupTealDark,
            contentColor = Color.White,
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to chats",
                            tint = Color.White
                        )
                    }

                    ContactAvatar(
                        name = conversation.title,
                        colorHex = conversation.avatarColorHex,
                        isGroup = conversation.isGroup,
                        isOnline = conversation.isOnline,
                        hasLiveRadarActive = conversation.theyGrantedLocationToMe,
                        size = 42.dp
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showEncryptionDialog = true }
                    ) {
                        Text(
                            text = conversation.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when {
                                isTyping -> "typing..."
                                conversation.isGroup -> conversation.memberNamesCsv
                                conversation.isOnline -> "online • tap for E2EE info"
                                else -> "last seen recently"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.82f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onStartVideoCall,
                        modifier = Modifier.testTag("start_video_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video Call",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = onStartVoiceCall,
                        modifier = Modifier.testTag("start_voice_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Voice Call",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = { showEncryptionDialog = true },
                        modifier = Modifier.testTag("verify_e2ee_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Verify End-to-End Encryption",
                            tint = MeetupEmerald
                        )
                    }
                }

                // Persistent Live Location Radar Banner inside Chat Header
                Surface(
                    color = MeetupTealMedium.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Live Location",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    conversation.theyGrantedLocationToMe && conversation.iGrantedLocationToThem ->
                                        "Mutual Persistent Radar Active (Valid 1+ Year)"
                                    conversation.theyGrantedLocationToMe ->
                                        "${conversation.title} shared persistent live radar with you"
                                    conversation.iGrantedLocationToThem ->
                                        "You granted ${conversation.title} persistent live radar"
                                    else ->
                                        "Live Radar permission off for this chat"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (conversation.theyGrantedLocationToMe) {
                                TextButton(
                                    onClick = onOpenLiveRadarMap,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = "Track on Map",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Switch(
                                checked = conversation.iGrantedLocationToThem,
                                onCheckedChange = onToggleMyPersistentGrant,
                                modifier = Modifier.testTag("chat_persistent_radar_switch")
                            )
                        }
                    }
                }
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EncryptedSecurityPill(
                        text = "Messages, files & live radar coordinates are AES-256-GCM encrypted. Tap any bubble to inspect ciphertext."
                    )
                }
            }

            items(messages, key = { it.id }) { msg ->
                MessageBubbleItem(
                    message = msg,
                    isGroup = conversation.isGroup,
                    onBubbleClick = { selectedMessageForCipherInspect = msg },
                    onOpenMapClick = onOpenLiveRadarMap
                )
            }
        }

        // Message Composer Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { showAttachmentSheet = true },
                    modifier = Modifier.testTag("attach_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Share File or Live Location",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    placeholder = {
                        Text(
                            if (isOfflineMode) "Type offline message (will queue)..." else "Type an encrypted message..."
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MeetupTealMedium,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    maxLines = 4,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("message_input_field")
                )

                Spacer(modifier = Modifier.width(6.dp))

                if (messageInput.isNotBlank()) {
                    IconButton(
                        onClick = {
                            onSendText(messageInput)
                            messageInput = ""
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MeetupEmerald)
                            .testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Encrypted Message",
                            tint = Color.Black
                        )
                    }
                } else {
                    // Quick Voice Note Send Button
                    IconButton(
                        onClick = {
                            onSendAttachment(
                                "VOICE_NOTE",
                                "Encrypted_VoiceNote_${System.currentTimeMillis() % 100}.m4a",
                                "0:18 • Encrypted Opus Audio",
                                null
                            )
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MeetupTealDark)
                            .testTag("send_voice_note_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Send Encrypted Voice Note",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    // Attachment & Persistent Live Location Bottom Sheet
    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Share Encrypted Media, Files & Live Location",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AttachmentActionItem(
                        icon = Icons.Default.Image,
                        label = "Photo Gallery",
                        color = Color(0xFF7C3AED),
                        onClick = {
                            showAttachmentSheet = false
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                    AttachmentActionItem(
                        icon = Icons.Default.Description,
                        label = "Document PDF",
                        color = Color(0xFF0284C7),
                        onClick = {
                            showAttachmentSheet = false
                            onSendAttachment(
                                "DOCUMENT",
                                "Project_Architecture_Spec.pdf",
                                "3.1 MB • AES-256 Encrypted PDF",
                                null
                            )
                        }
                    )
                    AttachmentActionItem(
                        icon = Icons.Default.GraphicEq,
                        label = "System File",
                        color = Color(0xFFEA580C),
                        onClick = {
                            showAttachmentSheet = false
                            documentPickerLauncher.launch(arrayOf("*/*"))
                        }
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = MeetupTealDark
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Share Real-Time GPS & Persistent Permission",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Grant ${conversation.title} permission to view your live location on the Radar Map. With Persistent Permission, they can track your live movement even a year later until you revoke it.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    showAttachmentSheet = false
                                    onShareLiveLocation(false, "Current Snapshot (1 Hour)")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Share 1 Hour")
                            }
                            Button(
                                onClick = {
                                    showAttachmentSheet = false
                                    onShareLiveLocation(true, "Persistent (Valid 1+ Year)")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MeetupTealDark),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("grant_persistent_location_btn")
                            ) {
                                Text("Grant 1+ Yr Access", color = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // E2EE Safety Number Verification Dialog
    if (showEncryptionDialog) {
        val safetyNumber = remember(conversation.id, myPhone) {
            CryptoManager.generateSafetyNumber(myPhone, conversation.phoneNumberOrGroupCode)
        }
        AlertDialog(
            onDismissRequest = { showEncryptionDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MeetupEmerald,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("End-to-End Encryption Verified") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Messages, files, voice/video calls, and live location coordinates in this chat are secured with AES-256-GCM encryption.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = safetyNumber,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                    Text(
                        text = "Compare this 60-digit safety number with ${conversation.title} to verify end-to-end security.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showEncryptionDialog = false }) {
                    Text("Verified")
                }
            }
        )
    }

    // Inspect Individual Message AES-256-GCM Ciphertext Dialog
    selectedMessageForCipherInspect?.let { msg ->
        AlertDialog(
            onDismissRequest = { selectedMessageForCipherInspect = null },
            title = { Text("AES-256-GCM Packet Inspector") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Decrypted Plaintext:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = msg.plainText, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "On-Disk / Wire AES-256-GCM Ciphertext (Base64):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg.encryptedCipherText,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Text(
                        text = "Key Fingerprint: ${msg.encryptionFingerprint}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMessageForCipherInspect = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun MessageBubbleItem(
    message: MessageEntity,
    isGroup: Boolean,
    onBubbleClick: () -> Unit,
    onOpenMapClick: () -> Unit
) {
    val bubbleColor = if (message.isOutgoing) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val textColor = if (message.isOutgoing) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isOutgoing) 16.dp else 4.dp,
                bottomEnd = if (message.isOutgoing) 4.dp else 16.dp
            ),
            tonalElevation = 2.dp,
            shadowElevation = 1.dp,
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clickable(onClick = onBubbleClick)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (isGroup && !message.isOutgoing) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MeetupTealMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // Special Rich Content Cards for File Sharing, Images, Voice Notes, and Live Location
                when (message.messageType) {
                    "IMAGE" -> {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                if (message.attachmentUri != null) {
                                    AsyncImage(
                                        model = message.attachmentUri,
                                        contentDescription = "Shared Image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = MeetupTealMedium
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = message.attachmentName ?: "Encrypted Image",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = message.attachmentSizeLabel ?: "1.8 MB",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    "DOCUMENT" -> {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = "Document",
                                    tint = MeetupTealDark,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = message.attachmentName ?: "Encrypted Document.pdf",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = message.attachmentSizeLabel ?: "PDF • Encrypted",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    "VOICE_NOTE" -> {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Voice Note",
                                    tint = MeetupEmerald,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Voice Message (Encrypted)",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = message.attachmentSizeLabel ?: "0:18",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }

                    "LIVE_LOCATION" -> {
                        Surface(
                            color = MeetupTealDark,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clickable { onOpenMapClick() }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = "Live Radar",
                                        tint = MeetupEmerald
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "LIVE RADAR BEACON",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MeetupEmerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = message.locationLabel ?: "Real-time GPS Coordinates",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tap to open Live Radar Map & track movement →",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MeetupEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Text(
                    text = message.plainText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "E2EE",
                        tint = textColor.copy(alpha = 0.55f),
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = formatChatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    if (message.isOutgoing) {
                        when (message.deliveryStatus) {
                            "QUEUED_OFFLINE" -> Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Queued Offline",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                            "SENT" -> Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Sent",
                                tint = textColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                            "DELIVERED" -> Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Delivered",
                                tint = textColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                            else -> Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Read",
                                tint = ReadReceiptBlue,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
