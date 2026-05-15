package com.reprush.app.data.repository

import android.util.Log
import com.reprush.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class GeminiPlanDay(
    val dayNumber: Int,
    val dayLabel: String,
    val exercises: List<GeminiExercise>
)

data class GeminiExercise(
    val exerciseName: String,
    val sets: Int,
    val reps: String,
    val restSeconds: Int,
    val notes: String?
)

data class GeminiPlan(
    val schemaVersion: Int,
    val planName: String,
    val goal: String,
    val weeks: Int,
    val daysPerWeek: Int,
    val schedule: List<GeminiPlanDay>
)

@Singleton
class GeminiRepository @Inject constructor() {

    private val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

    suspend fun generatePlan(
        goal: String,
        daysPerWeek: Int,
        splitType: String,
        sessionDuration: Int,
        equipment: String,
        fitnessLevel: String,
        weeks: Int,
        injuries: String,
        exerciseNames: List<String>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank()) return@withContext Result.Error("Gemini API key not configured.")

            val exerciseList = exerciseNames.joinToString("\n") { "- $it" }
            val prompt = buildPrompt(goal, daysPerWeek, splitType, sessionDuration, equipment, fitnessLevel, weeks, injuries, exerciseList)

            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("maxOutputTokens", 8192)
                    put("responseMimeType", "application/json")
                    put("responseSchema", buildResponseSchema())
                })
            }

            val url = URL("$endpoint?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 60_000
            conn.readTimeout = 120_000
            conn.doOutput = true

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(requestBody.toString())
            }

            val responseCode = conn.responseCode
            val responseText = if (responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
            }
            conn.disconnect()

            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e("GeminiRepo", "Error response: $responseText")
                return@withContext Result.Error("AI trainer is temporarily unavailable. Please try again later.")
            }

            val json = JSONObject(responseText)
            val candidates = json.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.Error("AI trainer returned an unexpected response.")
            }

            val planJson = candidates.getJSONObject(0)
                .optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
                ?: return@withContext Result.Error("AI trainer returned an unexpected response.")

            Result.Success(planJson.trim())
        } catch (e: java.net.SocketTimeoutException) {
            Result.Error("Your AI trainer is taking too long to respond. Check your connection and try again.")
        } catch (e: Exception) {
            Log.e("GeminiRepo", "Exception: ${e.message}", e)
            Result.Error("AI trainer is temporarily unavailable. Please try again later.")
        }
    }

    private fun buildResponseSchema(): JSONObject = JSONObject().apply {
        put("type", "OBJECT")
        put("required", JSONArray().apply {
            put("schema_version"); put("plan_name"); put("goal")
            put("weeks"); put("days_per_week"); put("schedule")
        })
        put("properties", JSONObject().apply {
            put("schema_version", JSONObject().put("type", "INTEGER"))
            put("plan_name", JSONObject().put("type", "STRING"))
            put("goal", JSONObject().put("type", "STRING"))
            put("weeks", JSONObject().put("type", "INTEGER"))
            put("days_per_week", JSONObject().put("type", "INTEGER"))
            put("schedule", JSONObject().apply {
                put("type", "ARRAY")
                put("items", JSONObject().apply {
                    put("type", "OBJECT")
                    put("required", JSONArray().apply {
                        put("day_number"); put("day_label"); put("exercises")
                    })
                    put("properties", JSONObject().apply {
                        put("day_number", JSONObject().put("type", "INTEGER"))
                        put("day_label", JSONObject().put("type", "STRING"))
                        put("exercises", JSONObject().apply {
                            put("type", "ARRAY")
                            put("items", JSONObject().apply {
                                put("type", "OBJECT")
                                put("required", JSONArray().apply {
                                    put("exercise_name"); put("sets"); put("reps"); put("rest_seconds")
                                })
                                put("properties", JSONObject().apply {
                                    put("exercise_name", JSONObject().put("type", "STRING"))
                                    put("sets", JSONObject().put("type", "INTEGER"))
                                    put("reps", JSONObject().put("type", "STRING"))
                                    put("rest_seconds", JSONObject().put("type", "INTEGER"))
                                    put("notes", JSONObject().put("type", "STRING"))
                                })
                            })
                        })
                    })
                })
            })
        })
    }

    private fun buildPrompt(
        goal: String, daysPerWeek: Int, splitType: String, sessionDuration: Int,
        equipment: String, fitnessLevel: String, weeks: Int, injuries: String,
        exerciseList: String
    ): String = """
You are a professional fitness trainer. Generate a workout plan using ONLY the exercises from the list below.

Member profile:
- Goal: $goal
- Days per week: $daysPerWeek
- Split type: $splitType
- Session duration: $sessionDuration minutes
- Equipment available: $equipment
- Fitness level: $fitnessLevel
- Plan duration: $weeks weeks
- Injuries or restrictions: ${injuries.ifBlank { "None" }}

Available exercises (use ONLY these names exactly as written):
$exerciseList

Set schema_version to 1. The schedule must have exactly $daysPerWeek day entries. Use only exercise names from the list above.
""".trimIndent()
}
