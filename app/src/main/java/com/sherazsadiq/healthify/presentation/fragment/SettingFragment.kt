package com.sherazsadiq.healthify.presentation.fragment

import SettingViewModel
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TimePicker
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sherazsadiq.healthify.R
import com.sherazsadiq.healthify.data.FirebaseRepository
import com.sherazsadiq.healthify.databinding.DialogReminderInputBinding
import com.sherazsadiq.healthify.databinding.FragmentAddBinding
import com.sherazsadiq.healthify.databinding.FragmentSettingBinding
import com.sherazsadiq.healthify.utils.ReminderWorker
import com.sherazsadiq.healthify.utils.ResultState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.max

class SettingFragment : Fragment() {

    private lateinit var binding: FragmentSettingBinding

    // Initial goal values
    private var currentStepGoal = 6000
    private var currentWaterGoal = 3000
    // ViewModel
    private val settingViewModel: SettingViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        loadUI()
        observeReminderTime()
        observeHealthGoals()
        settingViewModel.fetchReminderTime()
        settingViewModel.fetchHealthGoals()

        // Click listener for setting reminder time
        binding.reminderTimeLayout.setOnClickListener {
            showTimePicker()
        }

        // Collect toast messages
        lifecycleScope.launch {
            settingViewModel.toastMessageState.collect { toastMessage ->
                toastMessage?.let {
                    // Show toast message
                    val message = it.message
                    val duration = if (it.isError) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                    Toast.makeText(requireContext(), message, duration).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun loadUI() {
        lifecycleScope.launch {
            // We are now observing the health goals from ViewModel instead of direct Firebase calls
            settingViewModel.healthGoalState.collectLatest { state ->
                if (state.isLoading) {
                    // Show loading state if necessary
                } else if (state.error != null) {
                    // Handle error if necessary
                } else {
                    // Set health goal values to UI
                    currentStepGoal = state.stepGoal
                    currentWaterGoal = state.waterGoal
                    setupGoalButtons()
                }
            }
        }
    }

    private fun observeReminderTime() {
        lifecycleScope.launch {
            settingViewModel.reminderTimeState.collectLatest { state ->
                if (state.isLoading) {
                    binding.reminderTimeValue.text = "Loading..."
                } else if (state.error != null) {
                    binding.reminderTimeValue.text = "Not set"
                } else {
                    binding.reminderTimeValue.text = state.timeFormatted
                }
            }
        }
    }

    private fun observeHealthGoals() {
        lifecycleScope.launch {
            settingViewModel.healthGoalState.collectLatest { state ->
                if (!state.isLoading && state.error == null) {
                    // Display health goal values (already updated in `loadUI`)
                    updateStepGoalText()
                    updateWaterGoalText()
                }
            }
        }
    }

    private fun showTimePicker() {
        val dialogBinding = DialogReminderInputBinding.inflate(layoutInflater)

        dialogBinding.tpReminder.setIs24HourView(false) // Set to 12-hour format

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialogBinding.btnSaveReminder.setOnClickListener {
            val selectedHour = dialogBinding.tpReminder.hour
            val selectedMinute = dialogBinding.tpReminder.minute
            val amPm = if (selectedHour >= 12) "PM" else "AM"

            val formattedTime = String.format(
                "%02d:%02d %s",
                if (selectedHour > 12) selectedHour - 12 else if (selectedHour == 0) 12 else selectedHour,
                selectedMinute,
                amPm
            )
            binding.reminderTimeValue.text = formattedTime

            // Save the reminder time using ViewModel
            settingViewModel.saveReminderTime(
                if (selectedHour > 12) selectedHour - 12 else if (selectedHour == 0) 12 else selectedHour,
                selectedMinute,
                amPm
            )

            // Schedule the reminder
            scheduleReminder(selectedHour, selectedMinute, amPm)

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun scheduleReminder(hour: Int, minute: Int, amPm: String) {
        val now = Calendar.getInstance()
        val target = now.clone() as Calendar

        // Convert 12-hour format to 24-hour format
        val targetHour = if (amPm == "PM" && hour != 12) hour + 12 else if (amPm == "AM" && hour == 12) 0 else hour

        target.set(Calendar.HOUR_OF_DAY, targetHour)
        target.set(Calendar.MINUTE, minute)
        target.set(Calendar.SECOND, 0)

        // If the time is in the past, schedule for the next day
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }

        val delayMillis = target.timeInMillis - now.timeInMillis
        val delayMinutes = TimeUnit.MILLISECONDS.toMinutes(delayMillis)

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(requireContext()).enqueueUniqueWork(
            "daily_reminder",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun setupGoalButtons() {
        binding.increaseStepsGoalButton.setOnClickListener {
            currentStepGoal = max(0, currentStepGoal - 500)
            updateStepGoalText()
            settingViewModel.saveStepGoal(currentStepGoal)
        }

        binding.decreaseStepsGoalButton.setOnClickListener {
            currentStepGoal += 500
            updateStepGoalText()
            settingViewModel.saveStepGoal(currentStepGoal)
        }

        binding.increaseWaterIntakeGoalButton.setOnClickListener {
            currentWaterGoal = max(0, currentWaterGoal - 250)
            updateWaterGoalText()
            settingViewModel.saveWaterGoal(currentWaterGoal)
        }

        binding.decreaseWaterIntakeGoalButton.setOnClickListener {
            currentWaterGoal += 250
            updateWaterGoalText()
            settingViewModel.saveWaterGoal(currentWaterGoal)
        }
    }

    private fun updateStepGoalText() {
        binding.stepsGoalText.setText("$currentStepGoal Steps")
    }

    private fun updateWaterGoalText() {
        binding.waterIntakeText.setText("$currentWaterGoal ml")
    }
}
