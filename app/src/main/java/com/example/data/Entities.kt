package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val phoneNumber: String,
    val displayName: String,
    val aboutBio: String,
    val avatarColorHex: String,
    val avatarUri: String? = null,
    val isLoggedIn: Boolean = true,
    // Privacy settings
    val lastSeenPrivacy: String = "Everyone", // Everyone, My Contacts, Nobody
    val profilePhotoPrivacy: String = "Everyone",
    val statusPrivacy: String = "My Contacts",
    val readReceiptsEnabled: Boolean = true,
    val liveRadarMasterEnabled: Boolean = true,
    // Theme Mode: "System", "Light", "Dark"
    val themeMode: String = "System",
    // Offline Simulation Toggle to demonstrate offline message queueing
    val isOfflineQueueMode: Boolean = false,
    val publicKeyFingerprint: String = "94A1 7C20 E81B 44F9 390D"
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val phoneNumberOrGroupCode: String,
    val isGroup: Boolean = false,
    val memberNamesCsv: String = "", // For groups: "Aarav, Priya, Kabir, You"
    val avatarColorHex: String = "#00A884",
    val lastMessagePreview: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isOnline: Boolean = true,
    val statusText: String = "Hey there! I am using Meetup.",
    // Persistent Live Location Permission System:
    // 1. iGrantedLocationToThem: I allowed this contact to track my live location anytime (even 1+ year later)
    val iGrantedLocationToThem: Boolean = false,
    val iGrantedTimestamp: Long? = null,
    val iGrantedExpiryLabel: String = "Lifetime (Until Revoked)",
    // 2. theyGrantedLocationToMe: This contact allowed me to view their live location on the Radar Map anytime
    val theyGrantedLocationToMe: Boolean = false,
    val theyGrantedTimestamp: Long? = null,
    val theyGrantedExpiryLabel: String = "Valid 1+ Year (No Expiry)",
    // Current live coordinates & telemetry for Radar tracking
    val baseLatitude: Double = 28.6139,
    val baseLongitude: Double = 77.2090,
    val currentCityLabel: String = "Connaught Place, New Delhi",
    val speedKmh: Float = 24.5f,
    val batteryPercent: Int = 86,
    val isPinned: Boolean = false
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: String,
    val senderName: String,
    val senderPhone: String,
    val isOutgoing: Boolean,
    // Decrypted text for fast indexing + real AES-256-GCM ciphertext stored alongside it
    val plainText: String,
    val encryptedCipherText: String,
    val encryptionFingerprint: String,
    // Message Type: "TEXT", "IMAGE", "DOCUMENT", "VOICE_NOTE", "LIVE_LOCATION"
    val messageType: String = "TEXT",
    val attachmentName: String? = null,
    val attachmentSizeLabel: String? = null,
    val attachmentUri: String? = null,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationLabel: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    // Delivery Status: "QUEUED_OFFLINE", "SENT", "DELIVERED", "READ"
    val deliveryStatus: String = "READ"
)

@Entity(tableName = "status_updates")
data class StatusUpdateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val authorName: String,
    val authorPhone: String,
    val isMine: Boolean,
    val captionText: String,
    val backgroundColorHex: String,
    val mediaUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val viewedCount: Int = 14,
    val isViewedByMe: Boolean = false
)

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: String,
    val contactName: String,
    val contactPhone: String,
    val avatarColorHex: String,
    val isVideoCall: Boolean,
    val isIncoming: Boolean,
    val isMissed: Boolean = false,
    val durationSeconds: Int = 145,
    val timestamp: Long = System.currentTimeMillis()
)
