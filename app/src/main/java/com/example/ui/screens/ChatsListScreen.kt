package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ConversationEntity
import com.example.ui.components.ContactAvatar
import com.example.ui.components.EncryptedSecurityPill
import com.example.ui.components.formatChatTimestamp
import com.example.ui.theme.MeetupEmerald
import com.example.ui.theme.MeetupTealDark

@Composable
fun ChatsListScreen(
    conversations: List<ConversationEntity>,
    searchQuery: String,
    selectedFilter: String,
    isOfflineMode: Boolean,
    queuedOfflineCount: Int,
    typingMap: Map<String, Boolean>,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onToggleOfflineMode: (Boolean) -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenRadarForContact: (String) -> Unit,
    onCreateNewChatOrGroup: (title: String, phone: String, isGroup: Boolean, members: String, grantRadar: Boolean) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var createGroupMode by remember { mutableStateOf(false) }

    val filteredConversations = conversations.filter { conv ->
        val matchesQuery = searchQuery.isBlank() ||
            conv.title.contains(searchQuery, ignoreCase = true) ||
            conv.lastMessagePreview.contains(searchQuery, ignoreCase = true) ||
            conv.phoneNumberOrGroupCode.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            "Unread" -> conv.unreadCount > 0
            "Groups" -> conv.isGroup
            "Live Radar" -> conv.theyGrantedLocationToMe || conv.iGrantedLocationToThem
            else -> true
        }
        matchesQuery && matchesFilter
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search & Offline Mode Controls Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search encrypted chats, groups, or radar...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chat_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips + Offline Queue Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("All", "Unread", "Groups", "Live Radar").forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { onFilterChange(filter) },
                            label = { Text(filter) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MeetupTealDark,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("filter_chip_${filter.lowercase().replace(" ", "_")}")
                        )
                    }

                    FilterChip(
                        selected = isOfflineMode,
                        onClick = { onToggleOfflineMode(!isOfflineMode) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isOfflineMode) Icons.Default.CloudOff else Icons.Default.CloudQueue,
                                contentDescription = "Offline Mode",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                if (isOfflineMode) {
                                    "Offline Queue ON ($queuedOfflineCount)"
                                } else {
                                    "Online Sync"
                                }
                            )
                        },
                        modifier = Modifier.testTag("offline_mode_chip")
                    )
                }
            }

            if (isOfflineMode) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Offline Messaging Active: New messages are encrypted & queued locally ($queuedOfflineCount pending).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { onToggleOfflineMode(false) }) {
                            Text("Go Online & Sync")
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("conversations_list"),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(filteredConversations, key = { it.id }) { conv ->
                    val isTyping = typingMap[conv.id] == true
                    ConversationRowItem(
                        conversation = conv,
                        isTyping = isTyping,
                        onClick = { onOpenChat(conv.id) },
                        onRadarBadgeClick = { onOpenRadarForContact(conv.id) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 82.dp, end = 16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EncryptedSecurityPill(
                            text = "Your personal messages & live radar are end-to-end encrypted"
                        )
                    }
                }
            }
        }

        // Floating Action Buttons for New Group & New Chat
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    createGroupMode = true
                    showCreateDialog = true
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("fab_new_group")
            ) {
                Icon(imageVector = Icons.Default.GroupAdd, contentDescription = "New Encrypted Group")
            }

            FloatingActionButton(
                onClick = {
                    createGroupMode = false
                    showCreateDialog = true
                },
                containerColor = MeetupEmerald,
                contentColor = Color.Black,
                modifier = Modifier.testTag("fab_new_chat")
            ) {
                Icon(imageVector = Icons.Default.AddComment, contentDescription = "New Chat")
            }
        }
    }

    if (showCreateDialog) {
        NewChatOrGroupDialog(
            isGroupInitial = createGroupMode,
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, phone, isGroup, members, grantRadar ->
                showCreateDialog = false
                onCreateNewChatOrGroup(title, phone, isGroup, members, grantRadar)
            }
        )
    }
}

@Composable
private fun ConversationRowItem(
    conversation: ConversationEntity,
    isTyping: Boolean,
    onClick: () -> Unit,
    onRadarBadgeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("conversation_row_${conversation.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactAvatar(
            name = conversation.title,
            colorHex = conversation.avatarColorHex,
            isGroup = conversation.isGroup,
            isOnline = conversation.isOnline,
            hasLiveRadarActive = conversation.theyGrantedLocationToMe
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = conversation.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (conversation.isPinned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Text(
                    text = formatChatTimestamp(conversation.lastMessageTimestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (conversation.unreadCount > 0) {
                        MeetupEmerald
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isTyping) "typing encrypted message..." else conversation.lastMessagePreview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isTyping) {
                        MeetupEmerald
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (isTyping || conversation.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (conversation.theyGrantedLocationToMe) {
                        Surface(
                            color = MeetupEmerald.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.clickable { onRadarBadgeClick() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "Live Radar Trackable",
                                    tint = MeetupTealDark,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "LIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MeetupTealDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (conversation.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(MeetupEmerald),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = conversation.unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewChatOrGroupDialog(
    isGroupInitial: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (title: String, phone: String, isGroup: Boolean, members: String, grantRadar: Boolean) -> Unit
) {
    var isGroup by remember { mutableStateOf(isGroupInitial) }
    var nameInput by remember { mutableStateOf("") }
    var phoneOrGroupCode by remember { mutableStateOf(if (isGroupInitial) "GRP-NEW-108" else "+91 98110 22334") }
    var membersInput by remember { mutableStateOf("Aarav, Priya, Kabir, You") }
    var grantPersistentLocation by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isGroup) "Create Encrypted Group Chat" else "Start New Encrypted Chat")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isGroup,
                        onClick = { isGroup = false },
                        label = { Text("1-on-1 Contact") }
                    )
                    FilterChip(
                        selected = isGroup,
                        onClick = { isGroup = true },
                        label = { Text("Group Chat") }
                    )
                }

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(if (isGroup) "Group Subject / Name" else "Contact Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_chat_name_input")
                )

                OutlinedTextField(
                    value = phoneOrGroupCode,
                    onValueChange = { phoneOrGroupCode = it },
                    label = { Text(if (isGroup) "Group Invite Code" else "Mobile Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isGroup) {
                    OutlinedTextField(
                        value = membersInput,
                        onValueChange = { membersInput = it },
                        label = { Text("Group Members (comma-separated)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Grant Persistent Live Location",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Allow mutual real-time radar tracking anytime (valid 1+ year until revoked)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = grantPersistentLocation,
                            onCheckedChange = { grantPersistentLocation = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = nameInput.trim().ifEmpty {
                        if (isGroup) "New Meetup Group" else "New Contact"
                    }
                    onConfirm(finalName, phoneOrGroupCode, isGroup, membersInput, grantPersistentLocation)
                },
                modifier = Modifier.testTag("confirm_create_chat_button")
            ) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
