package com.reprush.app.data.repository

import org.json.JSONObject

class PlanImportValidator {

    fun validate(jsonText: String): Result<GeminiPlan> {
        return try {
            val json = JSONObject(jsonText)

            val schemaVersion = json.optInt("schema_version", -1)
            if (schemaVersion != 1) return Result.Error("Unsupported plan format (schema_version must be 1).")

            val planName = json.optString("plan_name", "").trim()
            if (planName.isEmpty() || planName.length > 100) return Result.Error("Plan name is missing or too long.")

            val goal = json.optString("goal", "").trim()
            if (goal.isEmpty()) return Result.Error("Plan goal is missing.")

            val weeks = json.optInt("weeks", 0)
            if (weeks < 1 || weeks > 52) return Result.Error("Plan weeks must be between 1 and 52.")

            val daysPerWeek = json.optInt("days_per_week", 0)
            if (daysPerWeek < 1 || daysPerWeek > 7) return Result.Error("Days per week must be between 1 and 7.")

            val scheduleArr = json.optJSONArray("schedule")
                ?: return Result.Error("The plan has no training days. Please regenerate your plan.")
            if (scheduleArr.length() == 0) return Result.Error("The plan has no training days. Please regenerate your plan.")

            val days = mutableListOf<GeminiPlanDay>()
            for (i in 0 until scheduleArr.length()) {
                val dayObj = scheduleArr.getJSONObject(i)
                val dayNumber = dayObj.optInt("day_number", 0)
                if (dayNumber < 1) return Result.Error("Invalid day_number in schedule.")

                val dayLabel = dayObj.optString("day_label", "").trim()
                if (dayLabel.isEmpty()) return Result.Error("Missing day_label in schedule.")

                val exercisesArr = dayObj.optJSONArray("exercises")
                    ?: return Result.Error("Day $dayNumber has no exercises.")
                if (exercisesArr.length() == 0) return Result.Error("Day $dayNumber has no exercises.")

                val exercises = mutableListOf<GeminiExercise>()
                for (j in 0 until exercisesArr.length()) {
                    val exObj = exercisesArr.getJSONObject(j)
                    val exerciseName = exObj.optString("exercise_name", "").trim()
                    if (exerciseName.isEmpty()) return Result.Error("Missing exercise_name in day $dayNumber.")

                    val sets = exObj.optInt("sets", 0)
                    if (sets < 1 || sets > 20) return Result.Error("Sets must be between 1 and 20 for $exerciseName.")

                    val reps = exObj.optString("reps", "").trim()
                    if (reps.isEmpty()) return Result.Error("Missing reps for $exerciseName.")

                    val restSeconds = exObj.optInt("rest_seconds", 0)
                    if (restSeconds < 15 || restSeconds > 600) return Result.Error("rest_seconds must be 15–600 for $exerciseName.")

                    val notes = if (exObj.isNull("notes")) null else exObj.optString("notes", null)
                    if (notes != null && notes.length > 500) return Result.Error("Notes too long for $exerciseName.")

                    exercises.add(GeminiExercise(exerciseName, sets, reps, restSeconds, notes))
                }
                days.add(GeminiPlanDay(dayNumber, dayLabel, exercises))
            }

            Result.Success(GeminiPlan(schemaVersion, planName, goal, weeks, daysPerWeek, days))
        } catch (e: Exception) {
            Result.Error("Your AI trainer returned an unexpected response. Try generating your plan again.")
        }
    }
}
