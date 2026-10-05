package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserProfileEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        StatusUpdateEntity::class,
        CallLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MeetupDatabase : RoomDatabase() {
    abstract fun meetupDao(): MeetupDao

    companion object {
        @Volatile
        private var INSTANCE: MeetupDatabase? = null

        fun getInstance(context: Context): MeetupDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MeetupDatabase::class.java,
                    "meetup_encrypted_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
