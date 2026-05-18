package com.reprush.app.ui.member.chat

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.reprush.app.BuildConfig
import com.reprush.app.data.local.dao.UserDao
import com.reprush.app.data.local.dao.WorkoutPlanDao
import com.reprush.app.data.local.entity.ChatMessageEntity
import com.reprush.app.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userDao: UserDao,
    private val workoutPlanDao: WorkoutPlanDao,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _messages = MutableLiveData<List<ChatMessageEntity>>(emptyList())
    val messages: LiveData<List<ChatMessageEntity>> = _messages

    private val _isTyping = MutableLiveData(false)
    val isTyping: LiveData<Boolean> = _isTyping

    private val endpoint =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"

    fun loadHistory() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val history = chatRepository.getMessages(uid)
            _messages.postValue(history)
        }
    }

    fun sendMessage(text: String) {
        val uid = auth.currentUser?.uid ?: return
        if (text.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            val userMsg = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                userId = uid,
                role = "user",
                content = text.trim(),
                createdAt = System.currentTimeMillis()
            )
            chatRepository.insertMessage(userMsg)
            val currentList = _messages.value.orEmpty() + userMsg
            _messages.postValue(currentList)
            _isTyping.postValue(true)

            val responseText = callGemini(uid, currentList)

            val aiMsg = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                userId = uid,
                role = "model",
                content = responseText,
                createdAt = System.currentTimeMillis()
            )
            chatRepository.insertMessage(aiMsg)
            _messages.postValue(currentList + aiMsg)
            _isTyping.postValue(false)
        }
    }

    private suspend fun callGemini(uid: String, allMessages: List<ChatMessageEntity>): String =
        withContext(Dispatchers.IO) {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank()) return@withContext "AI trainer not configured."

                val user = userDao.getUserById(uid)
                val plan = workoutPlanDao.getActivePlan(uid)
                val systemPrompt = buildSystemPrompt(user, plan)

                val last10 = allMessages.takeLast(10)
                val contentsArray = JSONArray()

                for (msg in last10) {
                    val role = if (msg.role == "model") "model" else "user"
                    contentsArray.put(JSONObject().apply {
                        put("role", role)
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", msg.content))
                        })
                    })
                }

                val requestBody = JSONObject().apply {
                    put("system_instruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", systemPrompt))
                        })
                    })
                    put("contents", contentsArray)
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.7)
                        put("maxOutputTokens", 512)
                    })
                }

                val url = URL("$endpoint?key=$apiKey")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 30_000
                conn.readTimeout = 60_000
                conn.doOutput = true

                OutputStreamWriter(conn.outputStream).use { it.write(requestBody.toString()) }

                val responseCode = conn.responseCode
                val responseText = if (responseCode == HttpURLConnection.HTTP_OK) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                }
                conn.disconnect()

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e("ChatViewModel", "Gemini error: $responseText")
                    return@withContext "Sorry, I'm having trouble connecting right now. Please try again."
                }

                val json = JSONObject(responseText)
                json.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")
                    ?: "I didn't get a response. Please try again."

            } catch (e: java.net.SocketTimeoutException) {
                "Request timed out. Please check your connection and try again."
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Exception: ${e.message}", e)
                "Sorry, something went wrong. Please try again."
            }
        }

    private fun buildSystemPrompt(
        user: com.reprush.app.data.local.entity.UserEntity?,
        plan: com.reprush.app.data.local.entity.WorkoutPlanEntity?
    ): String {
        val fitnessLevel = user?.fitnessLevel ?: "intermediate"
        val goal = user?.primaryGoal ?: "general fitness"
        val equipment = user?.availableEquipment ?: "gym equipment"
        val planName = plan?.planName ?: "no active plan"
        val daysPerWeek = plan?.daysPerWeek ?: 0

        return "You are RepRush AI Trainer. " +
                "The member's fitness level is $fitnessLevel, " +
                "their primary goal is $goal, " +
                "available equipment is $equipment. " +
                "Their active plan is \"$planName\"" +
                (if (daysPerWeek > 0) " ($daysPerWeek days/week)" else "") +
                ". Provide concise, actionable fitness advice. Keep responses under 150 words."
    }
}
