package com.sherazsadiq.healthify.models

data class DailyEntry(
    val stepsIntake: List<StepEntry> = emptyList(),
    val waterIntake: List<WaterEntry> = emptyList(),
    val sleepEntries: List<SleepEntry> = emptyList(),
    val moodEntries: List<MoodEntry> = emptyList(),  // Added mood entries list
    val weight: String = ""
)

data class StepEntry(
    val steps: Int = 0,
    val timestamp: Long = 0L
)

data class WaterEntry(
    val cups: Int = 0,
    val time: String = "" // Format: "HH:mm" or ISO timestamp
)

data class SleepEntry(
    val getInBedTime: String = "",
    val wakeUpTime: String = "" // Format: "HH:mm" or ISO timestamp
)

data class MoodEntry(
    val mood: String = "",
    val timestamp: Long = 0L // Store the timestamp of when the mood was entered
)

data class ReminderTime(
    val hour: Int = 0,    // 0 to 12
    val minute: Int = 0,  // 0 to 59
    val amPm: String = "AM" // "AM" or "PM"
)


data class DailySummary(
    val date: String,
    val totalSteps: Int = 0,
    val totalWaterCups: Int = 0,
    val totalSleepHours: Float = 0f,
    val moodList: List<String> = emptyList(),
    val weight: Float = 0f
)
