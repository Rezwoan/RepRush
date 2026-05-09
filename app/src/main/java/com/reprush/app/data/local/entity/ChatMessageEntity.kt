package com.reprush.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val role: String,
    val content: String,
    val createdAt: Long
)