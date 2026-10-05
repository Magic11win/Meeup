package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CallLogEntity
import com.example.data.ConversationEntity
import com.example.data.MeetupRepository
import com.example.data.MessageEntity
import com.example.data.StatusUpdateEntity
import com.example.data.UserProfileEntity
import com.example.security.CryptoManager
import com.example.util.DeviceGpsCoordinate
import com.example.util.LocationServiceHelper
import com.example.util.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

data class LiveTrackedContactMarker(
    val conversationId: String,
    val name: String,
    val phoneOrGroupCode: String,
    val avatarColorHex: String,
    val isGroup: Boolean,
    val latitude: Double,
    val longitude: Double,
    val cityLabel: String,
    val speedKmh: Float,
    val headingDegrees: Float,
    val batteryPercent: Int,
    val grantedTimestamp: Long,
    val expiryLabel: String,
    val trailPoints: List<Pair<Double, Double>>
)

data class ActiveCallSession(
    val conversationId: String,
    val contactName: String,
    val contactPhone: String,
    val avatarColorHex: String,
    val isVideoCall: Boolean,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val isCameraOff: Boolean = false,
    val isEncryptedVerified: Boolean = true,
    val elapsedSeconds: Int = 0
)

class MeetupViewModel(
    private val repository: MeetupRepository,
    private val appContext: Context
) : ViewModel() {

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val conversations: StateFlow<List<ConversationEntity>> = repository.conversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val statusUpdates: StateFlow<List<StatusUpdateEntity>> = repository.statusUpdates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callLogs: StateFlow<List<CallLogEntity>> = repository.callLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val queuedOfflineMessages: StateFlow<List<MessageEntity>> = repository.queuedOfflineMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedChatFilter = MutableStateFlow("All") // "All", "Unread", "Groups", "Live Radar"
    val selectedChatFilter: StateFlow<String> = _selectedChatFilter.asStateFlow()

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    private val _activeCallSession = MutableStateFlow<ActiveCallSession?>(null)
    val activeCallSession: StateFlow<ActiveCallSession?> = _activeCallSession.asStateFlow()

    private val _viewingStatus = MutableStateFlow<StatusUpdateEntity?>(null)
    val viewingStatus: StateFlow<StatusUpdateEntity?> = _viewingStatus.asStateFlow()

    private val _myDeviceGps = MutableStateFlow(
        DeviceGpsCoordinate(
            latitude = 28.6139,
            longitude = 77.2090,
            speedKmh = 14.2f,
            bearing = 45f,
            accuracyMeters = 4.5f
        )
    )
    val myDeviceGps: StateFlow<DeviceGpsCoordinate> = _myDeviceGps.asStateFlow()

    private val _liveRadarMarkers = MutableStateFlow<List<LiveTrackedContactMarker>>(emptyList())
    val liveRadarMarkers: StateFlow<List<LiveTrackedContactMarker>> = _liveRadarMarkers.asStateFlow()

    private val _selectedRadarContactId = MutableStateFlow<String?>(null)
    val selectedRadarContactId: StateFlow<String?> = _selectedRadarContactId.asStateFlow()

    private val _typingContactMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val typingContactMap: StateFlow<Map<String, Boolean>> = _typingContactMap.asStateFlow()

    private var callTimerJob: Job? = null
    private var gpsObserverJob: Job? = null
    private var radarTick = 0

    init {
        NotificationHelper.createNotificationChannels(appContext)
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
        startLiveRadarMovementEngine()
        startObservingHardwareGpsIfPermitted()
    }

    fun startObservingHardwareGpsIfPermitted() {
        if (gpsObserverJob?.isActive == true) return
        if (LocationServiceHelper.hasLocationPermission(appContext)) {
            gpsObserverJob = viewModelScope.launch {
                LocationServiceHelper.observeRealTimeLocation(appContext).collect { coord ->
                    _myDeviceGps.value = coord
                }
            }
        }
    }

    private fun startLiveRadarMovementEngine() {
        viewModelScope.launch {
            while (isActive) {
                val allConvs = conversations.value
                // Filter strictly to contacts who have granted us permission (theyGrantedLocationToMe == true)
                // Even if permission was granted > 1 year ago, it remains valid until they revoke it!
                val permittedConvs = allConvs.filter { it.theyGrantedLocationToMe }
                radarTick++
                val angle = (radarTick * 0.14)

                val markers = permittedConvs.mapIndexed { idx, conv ->
                    val phase = angle + (idx * 1.3)
                    val radiusLat = 0.0028
                    val radiusLng = 0.0034
                    val currentLat = conv.baseLatitude + sin(phase) * radiusLat
                    val currentLng = conv.baseLongitude + cos(phase) * radiusLng

                    // Generate a 6-point breadcrumb trail showing live movement trajectory
                    val trail = (5 downTo 0).map { step ->
                        val stepPhase = phase - (step * 0.18)
                        Pair(
                            conv.baseLatitude + sin(stepPhase) * radiusLat,
                            conv.baseLongitude + cos(stepPhase) * radiusLng
                        )
                    }

                    val dynamicSpeed = if (conv.speedKmh > 0f) {
                        (conv.speedKmh + (sin(phase) * 4.5f)).toFloat().coerceAtLeast(3.5f)
                    } else {
                        12.0f
                    }
                    val heading = ((Math.toDegrees(phase) % 360.0) + 360.0).toFloat() % 360f

                    LiveTrackedContactMarker(
                        conversationId = conv.id,
                        name = conv.title,
                        phoneOrGroupCode = conv.phoneNumberOrGroupCode,
                        avatarColorHex = conv.avatarColorHex,
                        isGroup = conv.isGroup,
                        latitude = currentLat,
                        longitude = currentLng,
                        cityLabel = conv.currentCityLabel,
                        speedKmh = dynamicSpeed,
                        headingDegrees = heading,
                        batteryPercent = conv.batteryPercent,
                        grantedTimestamp = conv.theyGrantedTimestamp ?: (System.currentTimeMillis() - 31_536_000_000L),
                        expiryLabel = conv.theyGrantedExpiryLabel,
                        trailPoints = trail
                    )
                }
                _liveRadarMarkers.value = markers

                if (_selectedRadarContactId.value == null && markers.isNotEmpty()) {
                    _selectedRadarContactId.value = markers.first().conversationId
                } else if (markers.none { it.conversationId == _selectedRadarContactId.value }) {
                    _selectedRadarContactId.value = markers.firstOrNull()?.conversationId
                }

                delay(1200L)
            }
        }
    }

    fun selectRadarContact(conversationId: String?) {
        _selectedRadarContactId.value = conversationId
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateChatFilter(filter: String) {
        _selectedChatFilter.value = filter
    }

    fun openConversation(conversationId: String) {
        _activeConversationId.value = conversationId
        viewModelScope.launch {
            repository.markConversationRead(conversationId)
        }
    }

    fun closeConversation() {
        _activeConversationId.value = null
    }

    fun observeMessagesForActiveChat(conversationId: String) =
        repository.observeMessages(conversationId)

    fun observeConversationById(conversationId: String) =
        repository.observeConversation(conversationId)

    fun loginWithMobileNumber(
        countryCode: String,
        mobileNumber: String,
        displayName: String,
        aboutBio: String
    ) {
        viewModelScope.launch {
            val formattedPhone = "$countryCode ${mobileNumber.trim()}"
            val safetyKey = CryptoManager.generateSafetyNumber(formattedPhone, "MEETUP_ROOT").take(24)
            val profile = UserProfileEntity(
                phoneNumber = formattedPhone,
                displayName = displayName.trim().ifEmpty { "Meetup User" },
                aboutBio = aboutBio.trim().ifEmpty { "Available on Meetup • Live Radar Ready" },
                avatarColorHex = "#00A884",
                isLoggedIn = true,
                publicKeyFingerprint = safetyKey
            )
            repository.saveUserProfile(profile)
            repository.seedInitialDataIfEmpty()
        }
    }

    fun updateUserProfile(
        displayName: String,
        aboutBio: String,
        avatarColorHex: String,
        avatarUri: String?
    ) {
        viewModelScope.launch {
            val current = repository.getUserProfileOnce() ?: return@launch
            repository.saveUserProfile(
                current.copy(
                    displayName = displayName.trim().ifEmpty { current.displayName },
                    aboutBio = aboutBio.trim().ifEmpty { current.aboutBio },
                    avatarColorHex = avatarColorHex,
                    avatarUri = avatarUri ?: current.avatarUri
                )
            )
        }
    }

    fun updatePrivacyAndThemeSettings(
        lastSeenPrivacy: String? = null,
        profilePhotoPrivacy: String? = null,
        statusPrivacy: String? = null,
        readReceiptsEnabled: Boolean? = null,
        liveRadarMasterEnabled: Boolean? = null,
        themeMode: String? = null,
        isOfflineQueueMode: Boolean? = null
    ) {
        viewModelScope.launch {
            val current = repository.getUserProfileOnce() ?: return@launch
            val wasOffline = current.isOfflineQueueMode
            val updated = current.copy(
                lastSeenPrivacy = lastSeenPrivacy ?: current.lastSeenPrivacy,
                profilePhotoPrivacy = profilePhotoPrivacy ?: current.profilePhotoPrivacy,
                statusPrivacy = statusPrivacy ?: current.statusPrivacy,
                readReceiptsEnabled = readReceiptsEnabled ?: current.readReceiptsEnabled,
                liveRadarMasterEnabled = liveRadarMasterEnabled ?: current.liveRadarMasterEnabled,
                themeMode = themeMode ?: current.themeMode,
                isOfflineQueueMode = isOfflineQueueMode ?: current.isOfflineQueueMode
            )
            repository.saveUserProfile(updated)

            // If transitioning from Offline Queue Mode -> Online Mode, automatically flush all queued offline messages!
            if (wasOffline && isOfflineQueueMode == false) {
                repository.flushOfflineQueue()
                NotificationHelper.sendPushNotification(
                    context = appContext,
                    senderName = "Meetup Offline Sync",
                    messageBody = "All queued offline messages have been encrypted and delivered!"
                )
            }
        }
    }

    fun sendMessage(
        conversationId: String,
        plainText: String,
        messageType: String = "TEXT",
        attachmentName: String? = null,
        attachmentSizeLabel: String? = null,
        attachmentUri: String? = null,
        locationLat: Double? = null,
        locationLng: Double? = null,
        locationLabel: String? = null
    ) {
        if (plainText.isBlank() && messageType == "TEXT") return
        viewModelScope.launch {
            val profile = repository.getUserProfileOnce()
            val isOffline = profile?.isOfflineQueueMode == true
            val myPhone = profile?.phoneNumber ?: "+91 98765 43210"

            val msgId = repository.sendEncryptedMessage(
                conversationId = conversationId,
                senderName = "You",
                senderPhone = myPhone,
                isOutgoing = true,
                plainText = plainText.trim(),
                messageType = messageType,
                attachmentName = attachmentName,
                attachmentSizeLabel = attachmentSizeLabel,
                attachmentUri = attachmentUri,
                locationLat = locationLat,
                locationLng = locationLng,
                locationLabel = locationLabel,
                isOfflineMode = isOffline
            )

            // If online, progress delivery ticks (SENT -> DELIVERED -> READ) and trigger real-time reply
            if (!isOffline) {
                delay(450L)
                repository.updateMessageStatus(msgId, "DELIVERED")
                delay(550L)
                repository.updateMessageStatus(msgId, "READ")

                triggerRealTimeContactResponse(conversationId, plainText, messageType)
            }
        }
    }

    fun shareMyLiveLocationInChat(
        conversationId: String,
        grantPersistentMultiYearPermission: Boolean,
        durationLabel: String
    ) {
        viewModelScope.launch {
            startObservingHardwareGpsIfPermitted()
            val coord = _myDeviceGps.value
            if (grantPersistentMultiYearPermission) {
                repository.toggleMyPersistentLocationGrant(
                    conversationId = conversationId,
                    grantAccess = true,
                    expiryLabel = durationLabel
                )
            }
            val latFormatted = "%.5f".format(coord.latitude)
            val lngFormatted = "%.5f".format(coord.longitude)
            val desc = if (grantPersistentMultiYearPermission) {
                "📍 Live Radar Permission Granted ($durationLabel) • Can track live movement anytime"
            } else {
                "📍 Shared Current GPS Location ($latFormatted, $lngFormatted)"
            }

            sendMessage(
                conversationId = conversationId,
                plainText = desc,
                messageType = "LIVE_LOCATION",
                locationLat = coord.latitude,
                locationLng = coord.longitude,
                locationLabel = "Live GPS ($latFormatted, $lngFormatted) • $durationLabel"
            )
        }
    }

    private fun triggerRealTimeContactResponse(
        conversationId: String,
        userText: String,
        messageType: String
    ) {
        viewModelScope.launch {
            val conv = repository.getConversationOnce(conversationId) ?: return@launch
            _typingContactMap.value = _typingContactMap.value + (conversationId to true)
            delay(1300L)
            _typingContactMap.value = _typingContactMap.value - conversationId

            val responderName = if (conv.isGroup) {
                conv.memberNamesCsv.split(",").firstOrNull()?.trim() ?: conv.title
            } else {
                conv.title
            }

            val lower = userText.lowercase()
            val replyText = when {
                messageType == "LIVE_LOCATION" ->
                    "Got your Live Radar access! I've also kept my persistent location permission ON for you so you can track me anytime on the Radar map."
                messageType == "IMAGE" || messageType == "DOCUMENT" ->
                    "Received the encrypted file ($userText) safely! AES-256-GCM signature verified ✅"
                messageType == "VOICE_NOTE" ->
                    "Heard your voice note loud and clear! Let's hop on a Meetup call soon."
                lower.contains("location") || lower.contains("where") || lower.contains("kaha") || lower.contains("radar") ->
                    "Check the 'Live Radar' tab! Since I granted you persistent location permission, you can watch my live movement on the map anytime (even a year from now)."
                else ->
                    "Got it! Everything here is end-to-end encrypted on Meetup. Tap my pin on the Live Radar map if you want to check my real-time route."
            }

            repository.sendEncryptedMessage(
                conversationId = conversationId,
                senderName = responderName,
                senderPhone = conv.phoneNumberOrGroupCode,
                isOutgoing = false,
                plainText = replyText
            )

            // Trigger real Android system Push Notification
            NotificationHelper.sendPushNotification(
                context = appContext,
                senderName = if (conv.isGroup) "${conv.title} ($responderName)" else responderName,
                messageBody = replyText
            )
        }
    }

    fun toggleGrantMyLocationToContact(
        conversationId: String,
        grant: Boolean,
        expiryLabel: String = "Lifetime (Valid 1+ Year)"
    ) {
        viewModelScope.launch {
            repository.toggleMyPersistentLocationGrant(conversationId, grant, expiryLabel)
            val conv = repository.getConversationOnce(conversationId)
            if (conv != null) {
                val statusMsg = if (grant) {
                    "You granted ${conv.title} persistent Live Radar access ($expiryLabel). They can view your real-time location until you revoke it."
                } else {
                    "You revoked ${conv.title}'s access to your Live Radar location immediately."
                }
                NotificationHelper.sendPushNotification(
                    context = appContext,
                    senderName = "Meetup Radar Privacy",
                    messageBody = statusMsg,
                    isRadarAlert = true
                )
            }
        }
    }

    fun toggleContactGrantedLocationToMe(conversationId: String, grantToMe: Boolean) {
        viewModelScope.launch {
            repository.toggleTheirPersistentLocationGrant(conversationId, grantToMe)
        }
    }

    fun createNewChatOrGroup(
        title: String,
        phoneOrGroupCode: String,
        isGroup: Boolean,
        memberNamesCsv: String,
        grantPersistentLocation: Boolean,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val newId = repository.createNewChatOrGroup(
                title = title,
                phoneOrGroupCode = phoneOrGroupCode,
                isGroup = isGroup,
                memberNamesCsv = memberNamesCsv,
                grantPersistentLocation = grantPersistentLocation
            )
            onCreated(newId)
        }
    }

    fun postStatus(caption: String, colorHex: String, mediaUri: String? = null) {
        if (caption.isBlank()) return
        viewModelScope.launch {
            val profile = repository.getUserProfileOnce()
            repository.postStatusUpdate(
                authorName = profile?.displayName ?: "You",
                authorPhone = profile?.phoneNumber ?: "+91 98765 43210",
                captionText = caption.trim(),
                bgColorHex = colorHex,
                mediaUri = mediaUri
            )
        }
    }

    fun openStatusViewer(status: StatusUpdateEntity) {
        _viewingStatus.value = status
        viewModelScope.launch {
            repository.markStatusSeen(status.id)
        }
    }

    fun closeStatusViewer() {
        _viewingStatus.value = null
    }

    // Voice & Video Calling Engine
    fun startCall(
        conversationId: String,
        contactName: String,
        contactPhone: String,
        avatarColorHex: String,
        isVideoCall: Boolean
    ) {
        callTimerJob?.cancel()
        _activeCallSession.value = ActiveCallSession(
            conversationId = conversationId,
            contactName = contactName,
            contactPhone = contactPhone,
            avatarColorHex = avatarColorHex,
            isVideoCall = isVideoCall,
            elapsedSeconds = 0
        )
        callTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val current = _activeCallSession.value ?: break
                _activeCallSession.value = current.copy(elapsedSeconds = current.elapsedSeconds + 1)
            }
        }
    }

    fun toggleCallMute() {
        val current = _activeCallSession.value ?: return
        _activeCallSession.value = current.copy(isMuted = !current.isMuted)
    }

    fun toggleCallSpeaker() {
        val current = _activeCallSession.value ?: return
        _activeCallSession.value = current.copy(isSpeakerOn = !current.isSpeakerOn)
    }

    fun toggleCallVideo() {
        val current = _activeCallSession.value ?: return
        _activeCallSession.value = current.copy(isCameraOff = !current.isCameraOff)
    }

    fun endActiveCall() {
        val session = _activeCallSession.value ?: return
        callTimerJob?.cancel()
        _activeCallSession.value = null
        viewModelScope.launch {
            repository.logCall(
                conversationId = session.conversationId,
                contactName = session.contactName,
                contactPhone = session.contactPhone,
                avatarColorHex = session.avatarColorHex,
                isVideoCall = session.isVideoCall,
                durationSeconds = session.elapsedSeconds.coerceAtLeast(1)
            )
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            val current = repository.getUserProfileOnce() ?: return@launch
            repository.saveUserProfile(current.copy(isLoggedIn = false))
        }
    }
}

class MeetupViewModelFactory(
    private val repository: MeetupRepository,
    private val appContext: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MeetupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MeetupViewModel(repository, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
