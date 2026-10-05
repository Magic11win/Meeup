package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetupDao {
    // User Profile
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun observeUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getUserProfileOnce(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProfile(profile: UserProfileEntity)

    // Conversations
    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun observeConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    fun observeConversationById(conversationId: String): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    suspend fun getConversationByIdOnce(conversationId: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<ConversationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :conversationId")
    suspend fun markConversationRead(conversationId: String)

    // Messages
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun observeMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE deliveryStatus = 'QUEUED_OFFLINE'")
    fun observeQueuedOfflineMessages(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("UPDATE messages SET deliveryStatus = :newStatus WHERE id = :messageId")
    suspend fun updateMessageDeliveryStatus(messageId: Long, newStatus: String)

    @Query("UPDATE messages SET deliveryStatus = 'DELIVERED' WHERE deliveryStatus = 'QUEUED_OFFLINE'")
    suspend fun flushAllQueuedOfflineMessages()

    // Status Updates
    @Query("SELECT * FROM status_updates ORDER BY isMine DESC, timestamp DESC")
    fun observeStatusUpdates(): Flow<List<StatusUpdateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatusUpdate(status: StatusUpdateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatusUpdates(statuses: List<StatusUpdateEntity>)

    @Query("UPDATE status_updates SET isViewedByMe = 1, viewedCount = viewedCount + 1 WHERE id = :statusId")
    suspend fun markStatusViewed(statusId: Long)

    // Call Logs
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun observeCallLogs(): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(callLog: CallLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLogs(callLogs: List<CallLogEntity>)
}
