package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.MeetupDatabase
import com.example.data.MeetupRepository
import com.example.ui.screens.ActiveCallOverlayScreen
import com.example.ui.screens.CallsLogScreen
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.ChatsListScreen
import com.example.ui.screens.LiveLocationRadarScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ProfileAndSettingsScreen
import com.example.ui.screens.StatusScreen
import com.example.ui.theme.MeetupEmerald
import com.example.ui.theme.MeetupTealDark
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MeetupViewModel
import com.example.viewmodel.MeetupViewModelFactory

enum class MeetupDestination(
    val label: String,
    val activeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val inactiveIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    CHATS("Chats", Icons.Filled.Chat, Icons.Outlined.Chat),
    STATUS("Status", Icons.Outlined.DonutLarge, Icons.Outlined.DonutLarge),
    RADAR("Live Radar", Icons.Filled.MyLocation, Icons.Outlined.MyLocation),
    CALLS("Calls", Icons.Filled.Call, Icons.Outlined.Call),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = MeetupDatabase.getInstance(applicationContext)
        val repository = MeetupRepository(database.meetupDao())
        val factory = MeetupViewModelFactory(repository, applicationContext)

        setContent {
            val meetupViewModel: MeetupViewModel = viewModel(factory = factory)
            MeetupRootApp(meetupViewModel = meetupViewModel)
        }
    }
}

@Composable
fun MeetupRootApp(meetupViewModel: MeetupViewModel) {
    val userProfile by meetupViewModel.userProfile.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()

    val isDarkTheme = when (userProfile?.themeMode) {
        "Dark" -> true
        "Light" -> false
        else -> systemDark
    }

    MyApplicationTheme(darkTheme = isDarkTheme) {
        val currentProfile = userProfile
        if (currentProfile == null || !currentProfile.isLoggedIn) {
            LoginScreen(
                onLoginCompleted = { country, phone, name, bio ->
                    meetupViewModel.loginWithMobileNumber(country, phone, name, bio)
                }
            )
        } else {
            MeetupAuthenticatedHome(
                viewModel = meetupViewModel,
                isDarkTheme = isDarkTheme
            )
        }
    }
}

@Composable
private fun MeetupAuthenticatedHome(
    viewModel: MeetupViewModel,
    isDarkTheme: Boolean
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val statusUpdates by viewModel.statusUpdates.collectAsStateWithLifecycle()
    val callLogs by viewModel.callLogs.collectAsStateWithLifecycle()
    val queuedOffline by viewModel.queuedOfflineMessages.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedChatFilter.collectAsStateWithLifecycle()
    val activeConversationId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val activeCallSession by viewModel.activeCallSession.collectAsStateWithLifecycle()
    val viewingStatus by viewModel.viewingStatus.collectAsStateWithLifecycle()
    val liveRadarMarkers by viewModel.liveRadarMarkers.collectAsStateWithLifecycle()
    val selectedRadarContactId by viewModel.selectedRadarContactId.collectAsStateWithLifecycle()
    val myDeviceGps by viewModel.myDeviceGps.collectAsStateWithLifecycle()
    val typingMap by viewModel.typingContactMap.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(MeetupDestination.CHATS) }

    val totalUnread = conversations.sumOf { it.unreadCount }

    // If an active voice/video call is running, render the full-screen encrypted call overlay
    activeCallSession?.let { callSession ->
        ActiveCallOverlayScreen(
            callSession = callSession,
            onToggleMute = viewModel::toggleCallMute,
            onToggleSpeaker = viewModel::toggleCallSpeaker,
            onToggleVideo = viewModel::toggleCallVideo,
            onEndCall = viewModel::endActiveCall
        )
        return
    }

    // If a specific conversation is opened on a compact phone screen, show ChatDetailScreen
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideTablet = maxWidth >= 700.dp

        if (!isWideTablet && activeConversationId != null) {
            val convId = activeConversationId!!
            val activeConv by viewModel.observeConversationById(convId)
                .collectAsStateWithLifecycle(initialValue = conversations.find { it.id == convId })
            val activeMessages by viewModel.observeMessagesForActiveChat(convId)
                .collectAsStateWithLifecycle(initialValue = emptyList())

            activeConv?.let { conv ->
                ChatDetailScreen(
                    conversation = conv,
                    messages = activeMessages,
                    myPhone = userProfile?.phoneNumber ?: "+91 98765 43210",
                    isTyping = typingMap[conv.id] == true,
                    isOfflineMode = userProfile?.isOfflineQueueMode == true,
                    onBack = { viewModel.closeConversation() },
                    onSendText = { text -> viewModel.sendMessage(conv.id, text) },
                    onSendAttachment = { type, name, size, uri ->
                        viewModel.sendMessage(
                            conversationId = conv.id,
                            plainText = "Shared $name",
                            messageType = type,
                            attachmentName = name,
                            attachmentSizeLabel = size,
                            attachmentUri = uri
                        )
                    },
                    onShareLiveLocation = { grantPersistent, durationLabel ->
                        viewModel.shareMyLiveLocationInChat(conv.id, grantPersistent, durationLabel)
                    },
                    onToggleMyPersistentGrant = { grant ->
                        viewModel.toggleGrantMyLocationToContact(conv.id, grant)
                    },
                    onOpenLiveRadarMap = {
                        viewModel.closeConversation()
                        viewModel.selectRadarContact(conv.id)
                        currentTab = MeetupDestination.RADAR
                    },
                    onStartVoiceCall = {
                        viewModel.startCall(
                            conversationId = conv.id,
                            contactName = conv.title,
                            contactPhone = conv.phoneNumberOrGroupCode,
                            avatarColorHex = conv.avatarColorHex,
                            isVideoCall = false
                        )
                    },
                    onStartVideoCall = {
                        viewModel.startCall(
                            conversationId = conv.id,
                            contactName = conv.title,
                            contactPhone = conv.phoneNumberOrGroupCode,
                            avatarColorHex = conv.avatarColorHex,
                            isVideoCall = true
                        )
                    }
                )
            }
        } else {
            if (currentTab != MeetupDestination.CHATS) {
                BackHandler {
                    currentTab = MeetupDestination.CHATS
                }
            }

            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    MeetupTopAppBar(
                        currentTab = currentTab,
                        isDarkTheme = isDarkTheme,
                        isOfflineMode = userProfile?.isOfflineQueueMode == true,
                        liveRadarCount = liveRadarMarkers.size,
                        onToggleDarkTheme = {
                            val nextMode = if (isDarkTheme) "Light" else "Dark"
                            viewModel.updatePrivacyAndThemeSettings(themeMode = nextMode)
                        },
                        onJumpToRadar = { currentTab = MeetupDestination.RADAR }
                    )
                },
                bottomBar = {
                    if (!isWideTablet) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp
                        ) {
                            MeetupDestination.entries.forEach { dest ->
                                val selected = currentTab == dest
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = dest },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (dest == MeetupDestination.CHATS && totalUnread > 0) {
                                                    Badge(containerColor = MeetupEmerald) {
                                                        Text(totalUnread.toString(), color = Color.Black)
                                                    }
                                                } else if (dest == MeetupDestination.RADAR && liveRadarMarkers.isNotEmpty()) {
                                                    Badge(containerColor = MeetupEmerald) {
                                                        Text(liveRadarMarkers.size.toString(), color = Color.Black)
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) dest.activeIcon else dest.inactiveIcon,
                                                contentDescription = dest.label
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = dest.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.testTag("nav_tab_${dest.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isWideTablet) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            MeetupDestination.entries.forEach { dest ->
                                val selected = currentTab == dest
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { currentTab = dest },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.activeIcon else dest.inactiveIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label) },
                                    modifier = Modifier.testTag("nav_rail_${dest.name.lowercase()}")
                                )
                            }
                        }
                    }

                    // Main Content Area (List-Detail on Wide Tablets for Chats, or Full Screen on Compact)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        when (currentTab) {
                            MeetupDestination.CHATS -> {
                                if (isWideTablet && activeConversationId != null) {
                                    Row(modifier = Modifier.fillMaxSize()) {
                                        Box(modifier = Modifier.weight(0.42f)) {
                                            ChatsListScreen(
                                                conversations = conversations,
                                                searchQuery = searchQuery,
                                                selectedFilter = selectedFilter,
                                                isOfflineMode = userProfile?.isOfflineQueueMode == true,
                                                queuedOfflineCount = queuedOffline.size,
                                                typingMap = typingMap,
                                                onSearchQueryChange = viewModel::updateSearchQuery,
                                                onFilterChange = viewModel::updateChatFilter,
                                                onToggleOfflineMode = {
                                                    viewModel.updatePrivacyAndThemeSettings(isOfflineQueueMode = it)
                                                },
                                                onOpenChat = viewModel::openConversation,
                                                onOpenRadarForContact = { id ->
                                                    viewModel.selectRadarContact(id)
                                                    currentTab = MeetupDestination.RADAR
                                                },
                                                onCreateNewChatOrGroup = { title, phone, isGroup, members, grantRadar ->
                                                    viewModel.createNewChatOrGroup(
                                                        title,
                                                        phone,
                                                        isGroup,
                                                        members,
                                                        grantRadar
                                                    ) { newId ->
                                                        viewModel.openConversation(newId)
                                                    }
                                                }
                                            )
                                        }
                                        Box(modifier = Modifier.weight(0.58f)) {
                                            val convId = activeConversationId!!
                                            val activeConv by viewModel.observeConversationById(convId)
                                                .collectAsStateWithLifecycle(
                                                    initialValue = conversations.find { it.id == convId }
                                                )
                                            val activeMessages by viewModel.observeMessagesForActiveChat(convId)
                                                .collectAsStateWithLifecycle(initialValue = emptyList())
                                            activeConv?.let { conv ->
                                                ChatDetailScreen(
                                                    conversation = conv,
                                                    messages = activeMessages,
                                                    myPhone = userProfile?.phoneNumber ?: "+91 98765 43210",
                                                    isTyping = typingMap[conv.id] == true,
                                                    isOfflineMode = userProfile?.isOfflineQueueMode == true,
                                                    onBack = { viewModel.closeConversation() },
                                                    onSendText = { text -> viewModel.sendMessage(conv.id, text) },
                                                    onSendAttachment = { type, name, size, uri ->
                                                        viewModel.sendMessage(
                                                            conversationId = conv.id,
                                                            plainText = "Shared $name",
                                                            messageType = type,
                                                            attachmentName = name,
                                                            attachmentSizeLabel = size,
                                                            attachmentUri = uri
                                                        )
                                                    },
                                                    onShareLiveLocation = { grantPersistent, durationLabel ->
                                                        viewModel.shareMyLiveLocationInChat(
                                                            conv.id,
                                                            grantPersistent,
                                                            durationLabel
                                                        )
                                                    },
                                                    onToggleMyPersistentGrant = { grant ->
                                                        viewModel.toggleGrantMyLocationToContact(conv.id, grant)
                                                    },
                                                    onOpenLiveRadarMap = {
                                                        viewModel.selectRadarContact(conv.id)
                                                        currentTab = MeetupDestination.RADAR
                                                    },
                                                    onStartVoiceCall = {
                                                        viewModel.startCall(
                                                            conv.id,
                                                            conv.title,
                                                            conv.phoneNumberOrGroupCode,
                                                            conv.avatarColorHex,
                                                            false
                                                        )
                                                    },
                                                    onStartVideoCall = {
                                                        viewModel.startCall(
                                                            conv.id,
                                                            conv.title,
                                                            conv.phoneNumberOrGroupCode,
                                                            conv.avatarColorHex,
                                                            true
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    ChatsListScreen(
                                        conversations = conversations,
                                        searchQuery = searchQuery,
                                        selectedFilter = selectedFilter,
                                        isOfflineMode = userProfile?.isOfflineQueueMode == true,
                                        queuedOfflineCount = queuedOffline.size,
                                        typingMap = typingMap,
                                        onSearchQueryChange = viewModel::updateSearchQuery,
                                        onFilterChange = viewModel::updateChatFilter,
                                        onToggleOfflineMode = {
                                            viewModel.updatePrivacyAndThemeSettings(isOfflineQueueMode = it)
                                        },
                                        onOpenChat = viewModel::openConversation,
                                        onOpenRadarForContact = { id ->
                                            viewModel.selectRadarContact(id)
                                            currentTab = MeetupDestination.RADAR
                                        },
                                        onCreateNewChatOrGroup = { title, phone, isGroup, members, grantRadar ->
                                            viewModel.createNewChatOrGroup(
                                                title,
                                                phone,
                                                isGroup,
                                                members,
                                                grantRadar
                                            ) { newId ->
                                                viewModel.openConversation(newId)
                                            }
                                        }
                                    )
                                }
                            }

                            MeetupDestination.STATUS -> {
                                StatusScreen(
                                    userProfile = userProfile,
                                    statuses = statusUpdates,
                                    viewingStatus = viewingStatus,
                                    onPostStatus = { caption, color ->
                                        viewModel.postStatus(caption, color)
                                    },
                                    onOpenStatus = viewModel::openStatusViewer,
                                    onCloseStatusViewer = viewModel::closeStatusViewer
                                )
                            }

                            MeetupDestination.RADAR -> {
                                LiveLocationRadarScreen(
                                    conversations = conversations,
                                    liveMarkers = liveRadarMarkers,
                                    selectedContactId = selectedRadarContactId,
                                    myDeviceGps = myDeviceGps,
                                    onSelectContact = viewModel::selectRadarContact,
                                    onToggleMyGrantToContact = { id, grant ->
                                        viewModel.toggleGrantMyLocationToContact(id, grant)
                                    },
                                    onToggleTheirGrantToMe = { id, grant ->
                                        viewModel.toggleContactGrantedLocationToMe(id, grant)
                                    },
                                    onOpenChat = { id ->
                                        viewModel.openConversation(id)
                                        currentTab = MeetupDestination.CHATS
                                    },
                                    onLocationPermissionGranted = {
                                        viewModel.startObservingHardwareGpsIfPermitted()
                                    }
                                )
                            }

                            MeetupDestination.CALLS -> {
                                CallsLogScreen(
                                    callLogs = callLogs,
                                    onStartCall = { log, isVideo ->
                                        viewModel.startCall(
                                            conversationId = log.conversationId,
                                            contactName = log.contactName,
                                            contactPhone = log.contactPhone,
                                            avatarColorHex = log.avatarColorHex,
                                            isVideoCall = isVideo
                                        )
                                    }
                                )
                            }

                            MeetupDestination.SETTINGS -> {
                                ProfileAndSettingsScreen(
                                    userProfile = userProfile,
                                    onSaveProfile = { name, bio, color ->
                                        viewModel.updateUserProfile(name, bio, color, null)
                                    },
                                    onUpdatePrivacyAndTheme = { lastSeen, photo, status, receipts, radar, theme, offline ->
                                        viewModel.updatePrivacyAndThemeSettings(
                                            lastSeenPrivacy = lastSeen,
                                            profilePhotoPrivacy = photo,
                                            statusPrivacy = status,
                                            readReceiptsEnabled = receipts,
                                            liveRadarMasterEnabled = radar,
                                            themeMode = theme,
                                            isOfflineQueueMode = offline
                                        )
                                    },
                                    onLogout = viewModel::logoutUser
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MeetupTopAppBar(
    currentTab: MeetupDestination,
    isDarkTheme: Boolean,
    isOfflineMode: Boolean,
    liveRadarCount: Int,
    onToggleDarkTheme: () -> Unit,
    onJumpToRadar: () -> Unit
) {
    Surface(
        color = MeetupTealDark,
        contentColor = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MeetupEmerald),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Meetup Logo",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Meetup",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "End-to-End Encrypted",
                            tint = MeetupEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = "${currentTab.label} • E2EE & Live Radar",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isOfflineMode) {
                    Surface(
                        color = MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = "Offline Queue",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }

                // Live Radar Quick Pill
                Surface(
                    color = MeetupEmerald.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp),
                    onClick = onJumpToRadar
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Live Radar",
                            tint = MeetupEmerald,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$liveRadarCount Live",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Quick Light/Dark Mode Toggle Button
                IconButton(
                    onClick = onToggleDarkTheme,
                    modifier = Modifier.testTag("top_theme_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Light or Dark Theme",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
