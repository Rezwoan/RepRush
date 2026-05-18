package com.reprush.app.data.repository

import com.reprush.app.data.local.dao.ChatMessageDao
import com.reprush.app.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatMessageDao: ChatMessageDao
) {
    suspend fun getMessages(userId: String): List<ChatMessageEntity> =
        withContext(Dispatchers.IO) {
            chatMessageDao.getMessagesForUser(userId)
        }

    suspend fun insertMessage(message: ChatMessageEntity) =
        withContext(Dispatchers.IO) {
            chatMessageDao.insertMessage(message)
        }

    suspend fun clearChat(userId: String) =
        withContext(Dispatchers.IO) {
            chatMessageDao.clearChatForUser(userId)
        }
}
