package com.example.hermit.db

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class ChatMessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val text: String,
    val isUser: Boolean,
    val isSystem: Boolean,
    val tokensPerSecond: Float?,
    val isLiked: Boolean,
    val isDisliked: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<ChatSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession): Long

    @Delete
    suspend fun deleteSession(session: ChatSession): Int

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSession(sessionId: String): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Update
    suspend fun updateMessage(message: ChatMessageEntity): Int

    @Delete
    suspend fun deleteMessage(message: ChatMessageEntity): Int
}

@Database(entities = [ChatSession::class, ChatMessageEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hermit_chat_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
