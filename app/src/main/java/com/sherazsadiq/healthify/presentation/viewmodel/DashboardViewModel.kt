package com.sherazsadiq.healthify.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherazsadiq.healthify.data.FirebaseRepository
import com.sherazsadiq.healthify.models.DailyEntry
import com.sherazsadiq.healthify.models.HealthGoals
import com.sherazsadiq.healthify.models.SleepEntry
import com.sherazsadiq.healthify.utils.ResultState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

class DashboardViewModel : ViewModel() {

    val firebaseRepository = FirebaseRepository()

    private val _dailyEntry = MutableStateFlow<DailyEntry?>(null)
    val dailyEntry: StateFlow<DailyEntry?> = _dailyEntry

    private val _healthGoals = MutableStateFlow<HealthGoals?>(null)
    val healthGoals: StateFlow<HealthGoals?> = _healthGoals

    private val _stepIntakeData = MutableStateFlow(StepIntakeData(0, 0, 0, 0f, 0f))
    val stepIntakeData: StateFlow<StepIntakeData> = _stepIntakeData

    val waterIntakeData = MutableStateFlow(WaterIntakeData(0, 0, 0))
    val totalSleepTime = MutableStateFlow("00:00")

    private val _selectedMood = MutableStateFlow<String?>(null)
    val selectedMood: StateFlow<String?> = _selectedMood

    private val _lastSixMonthsWeights = MutableStateFlow<List<Pair<Float, String>>>(emptyList())
    val lastSixMonthsWeights: StateFlow<List<Pair<Float, String>>> = _lastSixMonthsWeights


    fun fetchTodayDailyEntry() {
        viewModelScope.launch {
            _dailyEntry.value = firebaseRepository.getTodayDailyEntry()
            updateStepIntakeData()
            updateWaterIntakeData()
            calculateTotalSleepTime()
            fetchSelectedMood()
            fetchLastSixMonthsWeights()
        }
    }

    fun fetchHealthGoals() {
        viewModelScope.launch {
            when (val result = firebaseRepository.getHealthGoals()) {
                is ResultState.Success -> {
                    _healthGoals.value = result.data
                    updateStepIntakeData()
                    updateWaterIntakeData()
                }
                is ResultState.Error -> {
                    Log.e("fetchHealthGoals", "Error: ${result.message}")
                }
                ResultState.Loading -> {
                    // Optional: Handle loading state if needed
                }
            }
        }
    }


    data class StepIntakeData(
        val currentSteps: Int,
        val goal: Int,
        val percentage: Int,
        val distanceKm: Float,
        val caloriesBurned: Float
    )


    private fun updateStepIntakeData() {
        val stepsList = _dailyEntry.value?.stepsIntake ?: emptyList()
        val currentSteps = stepsList.sumOf { it.steps }

        val goal = _healthGoals.value?.stepGoal ?: 0

        val percentage = if (goal > 0)  min((currentSteps * 100 / goal), 100).toInt() else 0

        val distanceKm = currentSteps / 1312f // or use: currentSteps * 0.00076f
        val caloriesBurned = currentSteps * 0.04f

        _stepIntakeData.value = StepIntakeData(
            currentSteps = currentSteps,
            goal = goal,
            percentage = percentage,
            distanceKm = distanceKm,
            caloriesBurned = caloriesBurned
        )
    }



    private fun updateWaterIntakeData() {
        val currentWaterIntake = _dailyEntry.value?.waterIntake?.sumOf { it.cups * 250 } ?: 0
        val waterGoal = _healthGoals.value?.waterIntakeGoal ?: 3000
        val percentage = min((currentWaterIntake.toFloat() / waterGoal) * 100f, 100f).toInt()

        val adjustWaterIntake = min(currentWaterIntake, waterGoal)

        waterIntakeData.value = WaterIntakeData(adjustWaterIntake, waterGoal, percentage)
    }

    data class WaterIntakeData(
        val currentIntake: Int,
        val goal: Int,
        val percentage: Int
    )


    fun calculateTotalSleepTime() {
        val sleepEntries = _dailyEntry.value?.sleepEntries ?: emptyList()
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        var totalMinutes = 0

        for (entry in sleepEntries) {
            try {
                val getInBedTime = dateFormat.parse(entry.getInBedTime)
                val wakeUpTime = dateFormat.parse(entry.wakeUpTime)

                if (getInBedTime != null && wakeUpTime != null) {
                    val difference =
                        (wakeUpTime.time - getInBedTime.time) / (1000 * 60) // Convert to minutes
                    totalMinutes += if (difference < 0) (difference + 24 * 60).toInt() else difference.toInt() // Handle overnight sleep
                } else {
                    // Log invalid time entries
                    println("Invalid time entry: getInBedTime=${entry.getInBedTime}, wakeUpTime=${entry.wakeUpTime}")
                }
            } catch (e: Exception) {
                // Log parsing errors
                println("Error parsing time: ${e.message}")
            }
        }

        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        totalSleepTime.value = String.format("%02d:%02d", hours, minutes)
    }



    fun fetchSelectedMood() {
        viewModelScope.launch {
            val moodEntries = _dailyEntry.value?.moodEntries ?: emptyList()
            val latestMood = moodEntries.maxByOrNull { it.timestamp } // Find the latest mood by timestamp
            _selectedMood.value = latestMood?.mood // Update the StateFlow with the latest mood
        }
    }


    fun fetchLastSixMonthsWeights() {
        viewModelScope.launch {
            val allWeights = firebaseRepository.getAllWeights()
            _lastSixMonthsWeights.value = getLastSixMonthsData(allWeights)
        }
    }

    private fun getLastSixMonthsData(weights: List<Pair<Float, String>>): List<Pair<Float, String>> {
        val currentMonthIndex = Calendar.getInstance().get(Calendar.MONTH)
        val monthNames = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )

        // Get the last 6 months
        val lastSixMonths = (currentMonthIndex - 5..currentMonthIndex).map { index ->
            if (index < 0) monthNames[12 + index] else monthNames[index % 12]
        }

        // Group weights by month and select the maximum weight for each month
        val maxWeightsByMonth = weights.groupBy { it.second }
            .mapValues { entry -> entry.value.maxByOrNull { it.first } }

        // Filter weights for the last 6 months
        return lastSixMonths.map { month ->
            maxWeightsByMonth[month] ?: Pair(0f, month) // Default to 0 if no data
        }
    }

}