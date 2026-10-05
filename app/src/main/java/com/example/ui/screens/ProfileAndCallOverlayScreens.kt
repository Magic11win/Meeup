package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.UserProfileEntity
import com.example.ui.components.ContactAvatar
import com.example.ui.components.parseHexColor
import com.example.ui.theme.DangerRed
import com.example.ui.theme.MeetupEmerald
import com.example.ui.theme.MeetupTealDark
import com.example.util.NotificationHelper
import com.example.viewmodel.ActiveCallSession

@Composable
fun ProfileAndSettingsScreen(
    userProfile: UserProfileEntity?,
    onSaveProfile: (name: String, bio: String, colorHex: String) -> Unit,
    onUpdatePrivacyAndTheme: (
        lastSeen: String?,
        photoPrivacy: String?,
        statusPrivacy: String?,
        readReceipts: Boolean?,
        liveRadarMaster: Boolean?,
        themeMode: String?,
        offlineMode: Boolean?
    ) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var displayName by remember(userProfile?.displayName) {
        mutableStateOf(userProfile?.displayName ?: "Aryan Kapoor")
    }
    var aboutBio by remember(userProfile?.aboutBio) {
        mutableStateOf(userProfile?.aboutBio ?: "Available on Meetup • Live Radar Ready")
    }
    var selectedColor by remember(userProfile?.avatarColorHex) {
        mutableStateOf(userProfile?.avatarColorHex ?: "#00A884")
    }
    var profileSavedBanner by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            NotificationHelper.sendPushNotification(
                context = context,
                senderName = "Meetup Push Alerts Enabled",
                messageBody = "Real-time encrypted message & Live Radar alerts are active!"
            )
        }
    }

    val avatarPalette = listOf("#00A884", "#0284C7", "#7C3AED", "#EA580C", "#D97706", "#E11D48")
    val privacyOptions = listOf("Everyone", "My Contacts", "Nobody")
    val themeOptions = listOf("System", "Light", "Dark")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Customization Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ContactAvatar(
                        name = displayName,
                        colorHex = selectedColor,
                        hasLiveRadarActive = userProfile?.liveRadarMasterEnabled == true,
                        size = 82.dp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = userProfile?.phoneNumber ?: "+91 98765 43210",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Avatar Color Customizer
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        avatarPalette.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(hex))
                                    .border(
                                        width = if (selectedColor == hex) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = hex }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = {
                            displayName = it
                            profileSavedBanner = false
                        },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = aboutBio,
                        onValueChange = {
                            aboutBio = it
                            profileSavedBanner = false
                        },
                        label = { Text("About / Status Bio") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_bio_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            onSaveProfile(displayName, aboutBio, selectedColor)
                            profileSavedBanner = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MeetupTealDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_profile_button")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (profileSavedBanner) "Profile Saved ✓" else "Save Profile Customisation",
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Theme Mode Selection (Light / Dark / System)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = null,
                            tint = MeetupTealDark
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Appearance & Theme Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        themeOptions.forEach { mode ->
                            FilterChip(
                                selected = (userProfile?.themeMode ?: "System") == mode,
                                onClick = {
                                    onUpdatePrivacyAndTheme(null, null, null, null, null, mode, null)
                                },
                                label = { Text("$mode Mode") },
                                modifier = Modifier.testTag("theme_mode_${mode.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // User Privacy Settings Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PrivacyTip,
                            contentDescription = null,
                            tint = MeetupTealDark
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "User Privacy & Security Controls",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    PrivacySelectorRow(
                        title = "Who can see my Last Seen & Online",
                        selected = userProfile?.lastSeenPrivacy ?: "Everyone",
                        options = privacyOptions,
                        onSelect = {
                            onUpdatePrivacyAndTheme(it, null, null, null, null, null, null)
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    PrivacySelectorRow(
                        title = "Who can see my Profile Photo",
                        selected = userProfile?.profilePhotoPrivacy ?: "Everyone",
                        options = privacyOptions,
                        onSelect = {
                            onUpdatePrivacyAndTheme(null, it, null, null, null, null, null)
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    PrivacySelectorRow(
                        title = "Who can view my Status Updates",
                        selected = userProfile?.statusPrivacy ?: "My Contacts",
                        options = privacyOptions,
                        onSelect = {
                            onUpdatePrivacyAndTheme(null, null, it, null, null, null, null)
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Read Receipts (Blue Ticks)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "If turned off, you won't send or receive Read Receipts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = userProfile?.readReceiptsEnabled ?: true,
                            onCheckedChange = {
                                onUpdatePrivacyAndTheme(null, null, null, it, null, null, null)
                            },
                            modifier = Modifier.testTag("read_receipts_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Master Live Location Radar",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Broadcast encrypted GPS to contacts you have granted persistent access",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = userProfile?.liveRadarMasterEnabled ?: true,
                            onCheckedChange = {
                                onUpdatePrivacyAndTheme(null, null, null, null, it, null, null)
                            },
                            modifier = Modifier.testTag("master_radar_switch")
                        )
                    }
                }
            }
        }

        // Push Notification Test & E2EE Public Key Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MeetupTealDark
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Push Notifications & E2EE Identity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Your Public Key Fingerprint: ${userProfile?.publicKeyFingerprint ?: "94A1 7C20 E81B 44F9"}",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !NotificationHelper.hasNotificationPermission(context)
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    NotificationHelper.sendPushNotification(
                                        context = context,
                                        senderName = "Aarav Sharma (Meetup)",
                                        messageBody = "🔔 Encrypted Push Alert: Hey! Check my live movement on the Meetup Radar map!"
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MeetupEmerald),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_push_notification_button")
                        ) {
                            Text("Test Push Alert", color = Color.Black)
                        }

                        OutlinedButton(
                            onClick = onLogout,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Switch Number")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacySelectorRow(
    title: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelect(option) },
                    label = { Text(option) }
                )
            }
        }
    }
}

@Composable
fun ActiveCallOverlayScreen(
    callSession: ActiveCallSession,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleVideo: () -> Unit,
    onEndCall: () -> Unit
) {
    BackHandler(onBack = onEndCall)

    val mins = callSession.elapsedSeconds / 60
    val secs = callSession.elapsedSeconds % 60
    val timerText = "%02d:%02d".format(mins, secs)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF062C26),
                        Color(0xFF0B141A),
                        Color(0xFF071014)
                    )
                )
            )
            .statusBarsPadding()
            .padding(24.dp)
            .testTag("active_call_overlay")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top E2EE Badge
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MeetupEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "End-to-End Encrypted ${if (callSession.isVideoCall) "HD Video" else "Voice"} Call",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = callSession.contactName,
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = timerText,
                    style = MaterialTheme.typography.titleLarge,
                    color = MeetupEmerald
                )
            }

            // Center Avatar or Simulated Video Feed Preview
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ContactAvatar(
                    name = callSession.contactName,
                    colorHex = callSession.avatarColorHex,
                    hasLiveRadarActive = true,
                    size = 148.dp
                )

                if (callSession.isVideoCall && !callSession.isCameraOff) {
                    // Self-Preview Picture-in-Picture Card
                    Surface(
                        color = Color(0xFF1F2C34),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(width = 100.dp, height = 140.dp)
                            .border(2.dp, MeetupEmerald, RoundedCornerShape(16.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "You (HD)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Bottom Call Control Bar
            Surface(
                color = Color(0xFF1F2C34),
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleSpeaker,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                if (callSession.isSpeakerOn) Color.White.copy(alpha = 0.25f) else Color.Transparent
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Toggle Speaker",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = onToggleVideo,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                if (callSession.isCameraOff) Color.White.copy(alpha = 0.25f) else Color.Transparent
                            )
                    ) {
                        Icon(
                            imageVector = if (callSession.isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                            contentDescription = "Toggle Camera",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                if (callSession.isMuted) Color.White.copy(alpha = 0.25f) else Color.Transparent
                            )
                    ) {
                        Icon(
                            imageVector = if (callSession.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Toggle Mute",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = onEndCall,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(DangerRed)
                            .testTag("end_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
