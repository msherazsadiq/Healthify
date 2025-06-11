package com.sherazsadiq.healthify.presentation.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sherazsadiq.healthify.R
import com.sherazsadiq.healthify.data.FirebaseRepository
import com.sherazsadiq.healthify.databinding.FragmentAddBinding
import com.sherazsadiq.healthify.utils.ResultState
import kotlinx.coroutines.launch


class AddFragment : Fragment() {
    private lateinit var binding: FragmentAddBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // Initialize view binding (assuming it's set up)
        binding = FragmentAddBinding.bind(view)

        binding.entrySteps.setOnClickListener { showStepsDialog() }
        binding.entryWater.setOnClickListener { showWaterDialog() }
        binding.entrySleep.setOnClickListener { showSleepDialog() }
        binding.entryMood.setOnClickListener { showMoodDialog() }
        binding.entryWeight.setOnClickListener { showWeightDialog() }


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentAddBinding.inflate(inflater, container, false)
        return binding.root
    }


    private fun showStepsDialog(){

        val dialogView = layoutInflater.inflate(R.layout.dialog_step_input, null)
        val editTextSteps = dialogView.findViewById<EditText>(R.id.etStepsInput)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveStep)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()


        btnSave.setOnClickListener {
            val steps = editTextSteps.text.toString().toIntOrNull() ?: 0
            if (steps <= 0) {
                Toast.makeText(requireContext(), "Enter valid cup count", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Call FirebaseRepository
            lifecycleScope.launch {
                val result = FirebaseRepository().saveStepIntake(steps)
                when (result) {
                    is ResultState.Success -> {
                        Toast.makeText(requireContext(), "Saved: $steps Steps", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    is ResultState.Error -> {
                        Toast.makeText(requireContext(), "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }

        dialog.show()
    }




    private fun showWaterDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_water_input, null)
        val editTextCups = dialogView.findViewById<EditText>(R.id.etCupsInput)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveWater)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnSave.setOnClickListener {
            val cups = editTextCups.text.toString().toIntOrNull() ?: 0
            if (cups <= 0) {
                Toast.makeText(requireContext(), "Enter valid cup count", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Call FirebaseRepository
            lifecycleScope.launch {
                val result = FirebaseRepository().saveWaterIntake(cups)
                when (result) {
                    is ResultState.Success -> {
                        Toast.makeText(requireContext(), "Saved: $cups cups", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    is ResultState.Error -> {
                        Toast.makeText(requireContext(), "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }

        dialog.show()
    }




    private fun showSleepDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_sleep_input, null)
        val tpBed = dialogView.findViewById<TimePicker>(R.id.tpBedTime)
        val tpWake = dialogView.findViewById<TimePicker>(R.id.tpWakeUpTime)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveSleep)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnSave.setOnClickListener {
            val bedTime = "${tpBed.hour}:${tpBed.minute}"
            val wakeTime = "${tpWake.hour}:${tpWake.minute}"

            lifecycleScope.launch {
                val result = FirebaseRepository().saveSleepEntry(bedTime, wakeTime)
                when (result) {
                    is ResultState.Success -> Toast.makeText(requireContext(), "Sleep entry saved!", Toast.LENGTH_SHORT).show()
                    is ResultState.Error -> Toast.makeText(requireContext(), "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                    else -> {}
                }
            }

            dialog.dismiss()
        }


        dialog.show()
    }


    private fun showMoodDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_mood_input, null)
        val container = dialogView.findViewById<LinearLayout>(R.id.moodEmojiContainer)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveMood)
        var selectedMood: String? = null

        for (i in 0 until container.childCount) {
            val moodItem = container.getChildAt(i) as LinearLayout
            moodItem.alpha = 0.5f

            moodItem.setOnClickListener {
                // Reset selection style
                for (j in 0 until container.childCount) {
                    container.getChildAt(j).alpha = 0.5f
                }

                // Highlight selected
                moodItem.alpha = 1f

                // Get mood label text (second TextView)
                val moodLabel = moodItem.getChildAt(1) as TextView
                selectedMood = moodLabel.text.toString()
            }
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnSave.setOnClickListener {
            if (selectedMood != null) {
                lifecycleScope.launch {
                    val result = FirebaseRepository().saveMoodEntry(selectedMood!!)
                    when (result) {
                        is ResultState.Success -> Toast.makeText(requireContext(), "Mood saved!", Toast.LENGTH_SHORT).show()
                        is ResultState.Error -> Toast.makeText(requireContext(), "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                        else -> {}
                    }
                }
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Select a mood!", Toast.LENGTH_SHORT).show()
            }
        }


        dialog.show()
    }




    private fun showWeightDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_weight_input, null)
        val etWeight = dialogView.findViewById<EditText>(R.id.etWeightInput)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveWeight)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnSave.setOnClickListener {
            val weight = etWeight.text.toString().toFloatOrNull()
            if (weight != null) {
                lifecycleScope.launch {
                    val result = FirebaseRepository().saveWeight(weight)
                    when (result) {
                        is ResultState.Success -> Toast.makeText(requireContext(), "Weight saved!", Toast.LENGTH_SHORT).show()
                        is ResultState.Error -> Toast.makeText(requireContext(), "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                        else -> {}
                    }
                }
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Enter valid weight", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }








}