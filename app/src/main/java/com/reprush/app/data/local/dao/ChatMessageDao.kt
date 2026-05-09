package com.reprush.app.data.local.dao

import androidx.room.*
import com.reprush.app.data.local.entity.ChatMessageEntity

@Dao
interface ChatMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("SELECT * FROM chat_messages WHERE userId = :userId ORDER BY createdAt ASC")
    suspend fun getMessagesForUser(userId: String): List<ChatMessageEntity>

    @Query("DELETE FROM chat_messages WHERE userId = :userId")
    suspend fun clearChatForUser(userId: String)
}
