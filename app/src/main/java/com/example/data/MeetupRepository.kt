package com.example.data

import com.example.security.CryptoManager
import kotlinx.coroutines.flow.Flow

class MeetupRepository(private val dao: MeetupDao) {

    val userProfile: Flow<UserProfileEntity?> = dao.observeUserProfile()
    val conversations: Flow<List<ConversationEntity>> = dao.observeConversations()
    val statusUpdates: Flow<List<StatusUpdateEntity>> = dao.observeStatusUpdates()
    val callLogs: Flow<List<CallLogEntity>> = dao.observeCallLogs()
    val queuedOfflineMessages: Flow<List<MessageEntity>> = dao.observeQueuedOfflineMessages()

    fun observeMessages(conversationId: String): Flow<List<MessageEntity>> =
        dao.observeMessagesForConversation(conversationId)

    fun observeConversation(conversationId: String): Flow<ConversationEntity?> =
        dao.observeConversationById(conversationId)

    suspend fun getUserProfileOnce(): UserProfileEntity? = dao.getUserProfileOnce()

    suspend fun getConversationOnce(conversationId: String): ConversationEntity? =
        dao.getConversationByIdOnce(conversationId)

    suspend fun saveUserProfile(profile: UserProfileEntity) {
        dao.upsertUserProfile(profile)
    }

    suspend fun markConversationRead(conversationId: String) {
        dao.markConversationRead(conversationId)
    }

    suspend fun sendEncryptedMessage(
        conversationId: String,
        senderName: String,
        senderPhone: String,
        isOutgoing: Boolean,
        plainText: String,
        messageType: String = "TEXT",
        attachmentName: String? = null,
        attachmentSizeLabel: String? = null,
        attachmentUri: String? = null,
        locationLat: Double? = null,
        locationLng: Double? = null,
        locationLabel: String? = null,
        isOfflineMode: Boolean = false
    ): Long {
        val payload = CryptoManager.encrypt(plainText, conversationId)
        val now = System.currentTimeMillis()
        val initialStatus = when {
            !isOutgoing -> "READ"
            isOfflineMode -> "QUEUED_OFFLINE"
            else -> "SENT"
        }

        val message = MessageEntity(
            conversationId = conversationId,
            senderName = senderName,
            senderPhone = senderPhone,
            isOutgoing = isOutgoing,
            plainText = plainText,
            encryptedCipherText = payload.cipherTextBase64,
            encryptionFingerprint = payload.keyFingerprint,
            messageType = messageType,
            attachmentName = attachmentName,
            attachmentSizeLabel = attachmentSizeLabel,
            attachmentUri = attachmentUri,
            locationLat = locationLat,
            locationLng = locationLng,
            locationLabel = locationLabel,
            timestamp = now,
            deliveryStatus = initialStatus
        )
        val msgId = dao.insertMessage(message)

        val conv = dao.getConversationByIdOnce(conversationId)
        if (conv != null) {
            val previewPrefix = if (isOutgoing) "You: " else if (conv.isGroup) "$senderName: " else ""
            val updatedUnread = if (isOutgoing) 0 else conv.unreadCount + 1
            dao.updateConversation(
                conv.copy(
                    lastMessagePreview = "$previewPrefix$plainText",
                    lastMessageTimestamp = now,
                    unreadCount = updatedUnread
                )
            )
        }
        return msgId
    }

    suspend fun updateMessageStatus(messageId: Long, status: String) {
        dao.updateMessageDeliveryStatus(messageId, status)
    }

    suspend fun flushOfflineQueue() {
        dao.flushAllQueuedOfflineMessages()
    }

    suspend fun toggleMyPersistentLocationGrant(
        conversationId: String,
        grantAccess: Boolean,
        expiryLabel: String = "Lifetime (Valid 1+ Year)"
    ) {
        val conv = dao.getConversationByIdOnce(conversationId) ?: return
        val now = System.currentTimeMillis()
        dao.updateConversation(
            conv.copy(
                iGrantedLocationToThem = grantAccess,
                iGrantedTimestamp = if (grantAccess) now else null,
                iGrantedExpiryLabel = expiryLabel
            )
        )
    }

    suspend fun toggleTheirPersistentLocationGrant(
        conversationId: String,
        grantAccessToMe: Boolean
    ) {
        val conv = dao.getConversationByIdOnce(conversationId) ?: return
        val now = System.currentTimeMillis()
        dao.updateConversation(
            conv.copy(
                theyGrantedLocationToMe = grantAccessToMe,
                theyGrantedTimestamp = if (grantAccessToMe) now - 31_536_000_000L else null, // 1 year ago!
                theyGrantedExpiryLabel = "Valid 1+ Year (No Expiry)"
            )
        )
    }

    suspend fun createNewChatOrGroup(
        title: String,
        phoneOrGroupCode: String,
        isGroup: Boolean,
        memberNamesCsv: String,
        grantPersistentLocation: Boolean
    ): String {
        val id = "conv_${System.currentTimeMillis()}"
        val now = System.currentTimeMillis()
        val colors = listOf("#00A884", "#0284C7", "#7C3AED", "#EA580C", "#D97706", "#E11D48")
        val chosenColor = colors[kotlin.math.abs(title.hashCode()) % colors.size]
        val newConv = ConversationEntity(
            id = id,
            title = title,
            phoneNumberOrGroupCode = phoneOrGroupCode,
            isGroup = isGroup,
            memberNamesCsv = memberNamesCsv,
            avatarColorHex = chosenColor,
            lastMessagePreview = "🔒 End-to-end encrypted chat started",
            lastMessageTimestamp = now,
            unreadCount = 0,
            isOnline = true,
            statusText = if (isGroup) "Encrypted Group • $memberNamesCsv" else "Available on Meetup",
            iGrantedLocationToThem = grantPersistentLocation,
            iGrantedTimestamp = if (grantPersistentLocation) now else null,
            theyGrantedLocationToMe = grantPersistentLocation,
            theyGrantedTimestamp = if (grantPersistentLocation) now else null,
            baseLatitude = 28.6139 + ((title.hashCode() % 40) * 0.002),
            baseLongitude = 77.2090 + ((title.hashCode() % 35) * 0.002),
            currentCityLabel = "Indiranagar, Bengaluru"
        )
        dao.upsertConversation(newConv)

        sendEncryptedMessage(
            conversationId = id,
            senderName = "System",
            senderPhone = phoneOrGroupCode,
            isOutgoing = true,
            plainText = if (isGroup) {
                "Welcome to $title! Group messages and files are AES-256-GCM encrypted."
            } else {
                "Hi $title! Let's connect on Meetup."
            }
        )
        return id
    }

    suspend fun postStatusUpdate(
        authorName: String,
        authorPhone: String,
        captionText: String,
        bgColorHex: String,
        mediaUri: String? = null
    ) {
        dao.insertStatusUpdate(
            StatusUpdateEntity(
                authorName = authorName,
                authorPhone = authorPhone,
                isMine = true,
                captionText = captionText,
                backgroundColorHex = bgColorHex,
                mediaUri = mediaUri,
                timestamp = System.currentTimeMillis(),
                viewedCount = 1,
                isViewedByMe = true
            )
        )
    }

    suspend fun markStatusSeen(statusId: Long) {
        dao.markStatusViewed(statusId)
    }

    suspend fun logCall(
        conversationId: String,
        contactName: String,
        contactPhone: String,
        avatarColorHex: String,
        isVideoCall: Boolean,
        durationSeconds: Int
    ) {
        dao.insertCallLog(
            CallLogEntity(
                conversationId = conversationId,
                contactName = contactName,
                contactPhone = contactPhone,
                avatarColorHex = avatarColorHex,
                isVideoCall = isVideoCall,
                isIncoming = false,
                isMissed = false,
                durationSeconds = durationSeconds,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun seedInitialDataIfEmpty() {
        val existing = dao.getConversationByIdOnce("conv_aarav")
        if (existing != null) return

        val now = System.currentTimeMillis()
        val oneYearTwoMonthsAgo = now - (380L * 24L * 3600L * 1000L) // > 1 year ago to prove multi-year persistence!
        val sixMonthsAgo = now - (185L * 24L * 3600L * 1000L)

        val initialConversations = listOf(
            ConversationEntity(
                id = "conv_aarav",
                title = "Aarav Sharma",
                phoneNumberOrGroupCode = "+91 98201 44510",
                isGroup = false,
                avatarColorHex = "#00A884",
                lastMessagePreview = "📍 Live Location active • Reaching CyberHub in 8 mins",
                lastMessageTimestamp = now - 180_000L,
                unreadCount = 2,
                isOnline = true,
                statusText = "Building scalable systems 🚀 | Live Radar ON",
                iGrantedLocationToThem = true,
                iGrantedTimestamp = oneYearTwoMonthsAgo,
                iGrantedExpiryLabel = "Lifetime (Granted 1 yr 2 mos ago)",
                theyGrantedLocationToMe = true,
                theyGrantedTimestamp = oneYearTwoMonthsAgo,
                theyGrantedExpiryLabel = "Persistent Access (Granted 14 mos ago)",
                baseLatitude = 28.4950,
                baseLongitude = 77.0895,
                currentCityLabel = "DLF CyberHub, Gurugram",
                speedKmh = 38.4f,
                batteryPercent = 91,
                isPinned = true
            ),
            ConversationEntity(
                id = "conv_group_squad",
                title = "Weekend Roadtrip Squad 🏔️",
                phoneNumberOrGroupCode = "GRP-8821-MEET",
                isGroup = true,
                memberNamesCsv = "Aarav, Priya, Kabir, Zoya, You",
                avatarColorHex = "#0284C7",
                lastMessagePreview = "Priya: Shared 4K_Itinerary_Map.pdf (2.4 MB)",
                lastMessageTimestamp = now - 900_000L,
                unreadCount = 5,
                isOnline = true,
                statusText = "5 participants • End-to-End Encrypted Group",
                iGrantedLocationToThem = true,
                iGrantedTimestamp = sixMonthsAgo,
                iGrantedExpiryLabel = "Group Radar Active (6 mos)",
                theyGrantedLocationToMe = true,
                theyGrantedTimestamp = sixMonthsAgo,
                theyGrantedExpiryLabel = "Persistent Group Radar",
                baseLatitude = 30.0869,
                baseLongitude = 78.2676,
                currentCityLabel = "NH-34 Highway towards Rishikesh",
                speedKmh = 62.0f,
                batteryPercent = 78,
                isPinned = true
            ),
            ConversationEntity(
                id = "conv_priya",
                title = "Priya Verma",
                phoneNumberOrGroupCode = "+91 97112 88392",
                isGroup = false,
                avatarColorHex = "#7C3AED",
                lastMessagePreview = "You can check my live radar anytime, permission is always on for you!",
                lastMessageTimestamp = now - 3_600_000L,
                unreadCount = 0,
                isOnline = true,
                statusText = "Product Designer ✨ Coffee & Code",
                iGrantedLocationToThem = true,
                iGrantedTimestamp = oneYearTwoMonthsAgo,
                iGrantedExpiryLabel = "Lifetime (Never Expires)",
                theyGrantedLocationToMe = true,
                theyGrantedTimestamp = oneYearTwoMonthsAgo,
                theyGrantedExpiryLabel = "Persistent (Granted 1+ Year Ago)",
                baseLatitude = 19.0760,
                baseLongitude = 72.8777,
                currentCityLabel = "Bandra West, Mumbai",
                speedKmh = 18.2f,
                batteryPercent = 84
            ),
            ConversationEntity(
                id = "conv_kabir",
                title = "Kabir Khan",
                phoneNumberOrGroupCode = "+91 98180 77123",
                isGroup = false,
                avatarColorHex = "#EA580C",
                lastMessagePreview = "Voice note (0:42) • Call me when you're free",
                lastMessageTimestamp = now - 14_400_000L,
                unreadCount = 0,
                isOnline = false,
                statusText = "At the gym 🏋️‍♂️",
                iGrantedLocationToThem = false,
                iGrantedTimestamp = null,
                theyGrantedLocationToMe = false,
                theyGrantedTimestamp = null,
                baseLatitude = 12.9716,
                baseLongitude = 77.5946,
                currentCityLabel = "MG Road, Bengaluru",
                speedKmh = 0.0f,
                batteryPercent = 64
            ),
            ConversationEntity(
                id = "conv_founders",
                title = "Meetup Core Architecture ⚡",
                phoneNumberOrGroupCode = "GRP-CORE-990",
                isGroup = true,
                memberNamesCsv = "Rohan, Ananya, DevOps Bot, You",
                avatarColorHex = "#D97706",
                lastMessagePreview = "Rohan: E2EE key rotation & offline sync verified!",
                lastMessageTimestamp = now - 42_000_000L,
                unreadCount = 0,
                isOnline = true,
                statusText = "Engineering & Security Team",
                iGrantedLocationToThem = false,
                theyGrantedLocationToMe = true,
                theyGrantedTimestamp = sixMonthsAgo,
                theyGrantedExpiryLabel = "Persistent Team Beacon",
                baseLatitude = 17.4435,
                baseLongitude = 78.3772,
                currentCityLabel = "HITEC City, Hyderabad",
                speedKmh = 11.5f,
                batteryPercent = 95
            )
        )
        dao.insertConversations(initialConversations)

        // Seed encrypted messages for Aarav
        val aaravMsgs = listOf(
            createSeededMessage(
                convId = "conv_aarav",
                sender = "Aarav Sharma",
                phone = "+91 98201 44510",
                isOutgoing = false,
                text = "Hey! Remember I granted you permanent Live Location permission over a year ago? You can still track my live movement on the Radar tab anytime!",
                time = now - 720_000L
            ),
            createSeededMessage(
                convId = "conv_aarav",
                sender = "You",
                phone = "+91 98765 43210",
                isOutgoing = true,
                text = "Yes! I just checked the Live Radar map — I can see your live speed and battery moving near CyberHub right now.",
                time = now - 540_000L
            ),
            createSeededMessage(
                convId = "conv_aarav",
                sender = "Aarav Sharma",
                phone = "+91 98201 44510",
                isOutgoing = false,
                text = "📍 Live Location active • Reaching CyberHub in 8 mins",
                type = "LIVE_LOCATION",
                lat = 28.4950,
                lng = 77.0895,
                locLabel = "DLF CyberHub, Gurugram (Persistent Permission Active)",
                time = now - 180_000L
            )
        )

        // Seed encrypted messages for Weekend Roadtrip Squad
        val groupMsgs = listOf(
            createSeededMessage(
                convId = "conv_group_squad",
                sender = "Kabir",
                phone = "+91 98180 77123",
                isOutgoing = false,
                text = "Everyone turn on your Meetup persistent location permission for the highway convoy!",
                time = now - 1_800_000L
            ),
            createSeededMessage(
                convId = "conv_group_squad",
                sender = "You",
                phone = "+91 98765 43210",
                isOutgoing = true,
                text = "Done! Mine is enabled for the whole group.",
                time = now - 1_200_000L
            ),
            createSeededMessage(
                convId = "conv_group_squad",
                sender = "Priya",
                phone = "+91 97112 88392",
                isOutgoing = false,
                text = "Shared 4K_Itinerary_Map.pdf",
                type = "DOCUMENT",
                attachmentName = "4K_Itinerary_Map.pdf",
                attachmentSize = "2.4 MB • PDF Document",
                time = now - 900_000L
            )
        )

        // Seed encrypted messages for Priya
        val priyaMsgs = listOf(
            createSeededMessage(
                convId = "conv_priya",
                sender = "Priya Verma",
                phone = "+91 97112 88392",
                isOutgoing = false,
                text = "You can check my live radar anytime, permission is always on for you!",
                time = now - 3_600_000L
            )
        )

        dao.insertMessages(aaravMsgs + groupMsgs + priyaMsgs)

        // Seed Status Updates
        dao.insertStatusUpdates(
            listOf(
                StatusUpdateEntity(
                    authorName = "Aarav Sharma",
                    authorPhone = "+91 98201 44510",
                    isMine = false,
                    captionText = "Cruising through Gurugram CyberHub 🚗💨 Live Radar ON for trusted friends!",
                    backgroundColorHex = "#075E54",
                    timestamp = now - 1_800_000L,
                    viewedCount = 29,
                    isViewedByMe = false
                ),
                StatusUpdateEntity(
                    authorName = "Priya Verma",
                    authorPhone = "+91 97112 88392",
                    isMine = false,
                    captionText = "Sunset at Bandra Worli Sea Link 🌅✨ Encrypted stories on Meetup!",
                    backgroundColorHex = "#7C3AED",
                    timestamp = now - 5_400_000L,
                    viewedCount = 42,
                    isViewedByMe = false
                ),
                StatusUpdateEntity(
                    authorName = "Kabir Khan",
                    authorPhone = "+91 98180 77123",
                    isMine = false,
                    captionText = "Weekend trek prep complete 🏔️🎒 Who's joining the live convoy?",
                    backgroundColorHex = "#EA580C",
                    timestamp = now - 12_000_000L,
                    viewedCount = 19,
                    isViewedByMe = true
                )
            )
        )

        // Seed Call Logs
        dao.insertCallLogs(
            listOf(
                CallLogEntity(
                    conversationId = "conv_aarav",
                    contactName = "Aarav Sharma",
                    contactPhone = "+91 98201 44510",
                    avatarColorHex = "#00A884",
                    isVideoCall = true,
                    isIncoming = true,
                    isMissed = false,
                    durationSeconds = 312,
                    timestamp = now - 3_200_000L
                ),
                CallLogEntity(
                    conversationId = "conv_priya",
                    contactName = "Priya Verma",
                    contactPhone = "+91 97112 88392",
                    avatarColorHex = "#7C3AED",
                    isVideoCall = false,
                    isIncoming = false,
                    isMissed = false,
                    durationSeconds = 184,
                    timestamp = now - 18_000_000L
                ),
                CallLogEntity(
                    conversationId = "conv_kabir",
                    contactName = "Kabir Khan",
                    contactPhone = "+91 98180 77123",
                    avatarColorHex = "#EA580C",
                    isVideoCall = true,
                    isIncoming = true,
                    isMissed = true,
                    durationSeconds = 0,
                    timestamp = now - 48_000_000L
                )
            )
        )
    }

    private fun createSeededMessage(
        convId: String,
        sender: String,
        phone: String,
        isOutgoing: Boolean,
        text: String,
        type: String = "TEXT",
        attachmentName: String? = null,
        attachmentSize: String? = null,
        lat: Double? = null,
        lng: Double? = null,
        locLabel: String? = null,
        time: Long
    ): MessageEntity {
        val enc = CryptoManager.encrypt(text, convId)
        return MessageEntity(
            conversationId = convId,
            senderName = sender,
            senderPhone = phone,
            isOutgoing = isOutgoing,
            plainText = text,
            encryptedCipherText = enc.cipherTextBase64,
            encryptionFingerprint = enc.keyFingerprint,
            messageType = type,
            attachmentName = attachmentName,
            attachmentSizeLabel = attachmentSize,
            locationLat = lat,
            locationLng = lng,
            locationLabel = locLabel,
            timestamp = time,
            deliveryStatus = "READ"
        )
    }
}
