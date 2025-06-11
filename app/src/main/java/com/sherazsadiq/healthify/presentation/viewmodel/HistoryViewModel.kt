package com.sherazsadiq.healthify.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sherazsadiq.healthify.models.DailyEntry
import com.sherazsadiq.healthify.models.DailySummary
import com.sherazsadiq.healthify.utils.ResultState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

class HistoryViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _summaryData = MutableStateFlow<ResultState<List<DailySummary>>>(ResultState.Loading)
    val summaryData: StateFlow<ResultState<List<DailySummary>>> = _summaryData

    fun fetchRangeData(start: String, end: String) {
        viewModelScope.launch {
            _summaryData.value = ResultState.Loading

            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val dateList = getDateRange(start, end)
                val summaries = mutableListOf<DailySummary>()

                for (date in dateList) {
                    val snapshot = firestore.collection("users")
                        .document(userId)
                        .collection("data")
                        .document(date)
                        .get()
                        .await()

                    val entry = snapshot.toObject(DailyEntry::class.java)
                    entry?.let {
                        val totalSteps = it.stepsIntake.sumOf { step -> step.steps }

                        val totalWater = it.waterIntake.sumOf { water -> water.cups }

                        val totalSleepHours = it.sleepEntries.sumOf { sleep ->
                            try {
                                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                                val inTime = sdf.parse(sleep.getInBedTime)
                                val outTime = sdf.parse(sleep.wakeUpTime)
                                val duration = if (inTime != null && outTime != null) {
                                    val hours = ((outTime.time - inTime.time).toDouble() / (1000 * 60 * 60))
                                    if (hours < 0) hours + 24 else hours
                                } else 0.0
                                duration
                            } catch (e: Exception) {
                                0.0
                            }
                        }


                        val moodList = it.moodEntries.map { mood -> mood.mood }

                        val weight = it.weight.toFloatOrNull() ?: 0f

                        summaries.add(
                            DailySummary(
                                date = date,
                                totalSteps = totalSteps,
                                totalWaterCups = totalWater,
                                totalSleepHours = totalSleepHours.toFloat(),
                                moodList = moodList,
                                weight = weight
                            )
                        )
                    }
                }



                _summaryData.value = ResultState.Success(summaries)
            } catch (e: Exception) {
                _summaryData.value = ResultState.Error(e.message ?: "Something went wrong")
            }
        }
    }

    private fun getDateRange(startDate: String, endDate: String): List<String> {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val start = format.parse(startDate)!!
        val end = format.parse(endDate)!!
        val dates = mutableListOf<String>()
        var cal = Calendar.getInstance().apply { time = start }

        while (!cal.time.after(end)) {
            dates.add(format.format(cal.time))
            cal.add(Calendar.DATE, 1)
        }
        return dates
    }
}
