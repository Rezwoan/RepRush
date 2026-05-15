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

    private val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"

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
                    put("temperature", 0.7)
                    put("maxOutputTokens", 8192)
                })
            }

            val url = URL("$endpoint?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 30_000
            conn.readTimeout = 30_000
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

            val content = candidates.getJSONObject(0)
                .optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
                ?: return@withContext Result.Error("AI trainer returned an unexpected response.")

            val cleanedJson = extractJson(content)
            Result.Success(cleanedJson)
        } catch (e: java.net.SocketTimeoutException) {
            Result.Error("Your AI trainer is taking too long to respond. Check your connection and try again.")
        } catch (e: Exception) {
            Log.e("GeminiRepo", "Exception: ${e.message}", e)
            Result.Error("AI trainer is temporarily unavailable. Please try again later.")
        }
    }

    private fun buildPrompt(
        goal: String, daysPerWeek: Int, splitType: String, sessionDuration: Int,
        equipment: String, fitnessLevel: String, weeks: Int, injuries: String,
        exerciseList: String
    ): String = """
You are a professional fitness trainer. Generate a workout plan using ONLY the exercises from the list below. Return ONLY valid JSON with no explanation, markdown, or text outside the JSON object.

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

Return this exact JSON structure:
{
  "schema_version": 1,
  "plan_name": "string",
  "goal": "string",
  "weeks": number,
  "days_per_week": number,
  "schedule": [
    {
      "day_number": number,
      "day_label": "string",
      "exercises": [
        {
          "exercise_name": "string",
          "sets": number,
          "reps": "string",
          "rest_seconds": number,
          "notes": "string or null"
        }
      ]
    }
  ]
}
""".trimIndent()

    private fun extractJson(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start != -1 && end != -1 && end > start) text.substring(start, end + 1) else text
    }
}
