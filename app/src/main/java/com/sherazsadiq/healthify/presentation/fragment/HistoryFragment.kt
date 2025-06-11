package com.sherazsadiq.healthify.presentation.fragment


import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.sherazsadiq.healthify.R
import com.sherazsadiq.healthify.databinding.FragmentHistoryBinding
import com.sherazsadiq.healthify.models.DailySummary
import com.sherazsadiq.healthify.presentation.viewmodel.HistoryViewModel
import com.sherazsadiq.healthify.utils.ResultState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.appcompat.app.AlertDialog
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.data.Entry


class HistoryFragment : Fragment() {

    private lateinit var binding: FragmentHistoryBinding
    private val historyViewModel: HistoryViewModel by viewModels()
    private var loadingDialog: AlertDialog? = null
    private var isStartDateSelected = false
    private var isEndDateSelected = false




    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupDatePickers()
        setupContinueButton()
        observeHistoryData()
    }

    private fun setupDatePickers() {
        val today = MaterialDatePicker.todayInUtcMilliseconds()

        val startPicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Start Date")
            .build()

        val endConstraints = CalendarConstraints.Builder()
            .setEnd(today)
            .build()

        val endPicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select End Date")
            .setCalendarConstraints(endConstraints)
            .build()

        binding.startDateEditText.setOnClickListener {
            startPicker.show(parentFragmentManager, "start_date_picker")
        }

        binding.endDateEditText.setOnClickListener {
            endPicker.show(parentFragmentManager, "end_date_picker")
        }

        startPicker.addOnPositiveButtonClickListener { millis ->
            val date = dateFormat.format(Date(millis))
            binding.startDateEditText.setText(date)
            isStartDateSelected = true

            if(isEndDateSelected) {
                binding.contButton.visibility = View.VISIBLE // Show button when both dates are selected
            } else {
                binding.contButton.visibility = View.GONE // Hide button until end date is selected
            }

        }

        endPicker.addOnPositiveButtonClickListener { millis ->
            val selectedDate = Date(millis)
            val todayDate = Date(MaterialDatePicker.todayInUtcMilliseconds())

            if (selectedDate.after(todayDate)) {
                Toast.makeText(requireContext(), "Future date not allowed.", Toast.LENGTH_SHORT).show()
                val todayMillis = MaterialDatePicker.todayInUtcMilliseconds()
                val todayFormatted = dateFormat.format(Date(todayMillis))
                binding.endDateEditText.setText(todayFormatted)
            } else {
                val formatted = dateFormat.format(selectedDate)
                binding.endDateEditText.setText(formatted)
            }

            isEndDateSelected = true

            if(isStartDateSelected) {
                binding.contButton.visibility = View.VISIBLE // Show button when both dates are selected
            } else {
                binding.contButton.visibility = View.GONE // Hide button until start date is selected
            }

        }
    }

    private fun setupContinueButton() {
        binding.contButton.setOnClickListener {
            val start = binding.startDateEditText.text?.toString()
            val end = binding.endDateEditText.text?.toString()

            if (!start.isNullOrEmpty() && !end.isNullOrEmpty()) {
                binding.contButton.visibility = View.GONE // Hide button on click
                showLoadingDialog() // Show loading dialog
                historyViewModel.fetchRangeData(start, end)
            }
        }
    }

    private fun observeHistoryData() {
        lifecycleScope.launch {
            historyViewModel.summaryData.collect { state ->
                when (state) {
                    is ResultState.Loading -> {
                        // Dialog is already shown on button click
                    }

                    is ResultState.Success -> {
                        hideLoadingDialog()
                        val data = state.data


                        setupStepsChart(data)
                        setupWaterChart(data)
                        setupSleepChart(data)
                        setupMoodChart(data)
                        setupWeightChart(data)

                    }

                    is ResultState.Error -> {
                        hideLoadingDialog()
                        Toast.makeText(requireContext(), "Error: ${state.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }




    private fun setupStepsChart(data: List<DailySummary>) {
        val entries = data.mapIndexed { index, summary ->
            BarEntry(index.toFloat(), summary.totalSteps.toFloat())
        }

        val labels = data.map { it.date.takeLast(2) }
        val dataSet = BarDataSet(entries, "Steps")
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.step_chart)
        dataSet.valueTextSize = 10f

        var textColor = if (isNightMode()) Color.WHITE else Color.BLACK
        dataSet.valueTextColor = textColor

        val barData = BarData(dataSet)
        barData.barWidth = 0.15f

        val chart = binding.stepsBarChart
        chart.data = barData

        chart.apply {
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                labelRotationAngle = -45f
                textSize = 10f
                setTextColor(textColor)
            }
            axisLeft.apply {
                axisMinimum = 0f
                setTextColor(textColor)
            }
            axisRight.isEnabled = false
            legend.isEnabled = false
            description.isEnabled = false
            setVisibleXRangeMaximum(7f)
            setScaleEnabled(false)
            isDragEnabled = true
            invalidate()
        }
    }


    private fun setupWaterChart(data: List<DailySummary>) {
        val entries = data.mapIndexed { index, summary ->
            val ml = summary.totalWaterCups * 250f // 1 cup = 250 ml
            BarEntry(index.toFloat(), ml)
        }

        val labels = data.map { it.date.takeLast(2) } // Show only DD as label

        val dataSet = BarDataSet(entries, "Water (ml)")
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.water_chart)
        dataSet.valueTextSize = 10f

        var textColor = if (isNightMode()) Color.WHITE else Color.BLACK
        dataSet.valueTextColor = textColor

        val barData = BarData(dataSet)
        barData.barWidth = 0.15f

        val waterBarChart = binding.waterBarChart // Make sure this exists in XML and is of type BarChart
        waterBarChart.data = barData

        waterBarChart.apply {
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                labelRotationAngle = -45f
                textSize = 10f
                setTextColor(textColor)
            }
            axisLeft.apply{
                axisMinimum = 0f
                setTextColor(textColor)
            }
            axisRight.isEnabled = false
            description.isEnabled = false
            legend.isEnabled = false
            setVisibleXRangeMaximum(7f)
            setScaleEnabled(false)
            isDragEnabled = true
            invalidate()
        }
    }

    private fun setupSleepChart(data: List<DailySummary>) {
        val entries = data.mapIndexed { index, summary ->
            BarEntry(index.toFloat(), summary.totalSleepHours)
        }

        val labels = data.map { it.date.takeLast(2) } // Show only day (DD)

        val dataSet = BarDataSet(entries, "Sleep (hrs)")
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.light_primary)
        dataSet.valueTextSize = 10f

        var textColor = if (isNightMode()) Color.WHITE else Color.BLACK
        dataSet.valueTextColor = textColor

        val barData = BarData(dataSet)
        barData.barWidth = 0.15f

        val sleepBarChart = binding.sleepBarChart // Ensure this ID exists in XML
        sleepBarChart.data = barData

        sleepBarChart.apply {
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                labelRotationAngle = -45f
                textSize = 10f
                setTextColor(textColor)
            }
            axisLeft.apply{
                axisMinimum = 0f
                setTextColor(textColor)
            }
            axisRight.isEnabled = false
            description.isEnabled = false
            legend.isEnabled = false
            setVisibleXRangeMaximum(7f)
            setScaleEnabled(false)
            isDragEnabled = true
            invalidate()
        }
    }


    private fun setupMoodChart(data: List<DailySummary>) {
        val moodLevels = listOf("Awful", "Bad", "OK", "Good", "Great")
        val moodEmojis = mapOf(
            "Awful" to "😢",
            "Bad" to "😡",
            "OK" to "😐",
            "Good" to "😀",
            "Great" to "😄"
        )

        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()
        val emojiLabels = mutableListOf<String>()

        data.forEachIndexed { index, summary ->
            val lastMood = summary.moodList.lastOrNull()
            if (lastMood != null && moodLevels.contains(lastMood)) {
                val yIndex = moodLevels.indexOf(lastMood).toFloat()
                entries.add(Entry(index.toFloat(), yIndex))
                labels.add(summary.date.takeLast(2)) // Show just DD
                emojiLabels.add(moodEmojis[lastMood] ?: "")
            }
        }


        val dataSet = LineDataSet(entries, "Mood")
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(true) // To show emoji as value label
        dataSet.setDrawIcons(false)
        dataSet.color = Color.TRANSPARENT // No lines
        dataSet.lineWidth = 0f
        dataSet.valueTextSize = 16f
        dataSet.valueTextColor = ContextCompat.getColor(requireContext(), R.color.light_primary)

        // Set emojis as value labels
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getPointLabel(entry: Entry?): String {
                val index = entries.indexOf(entry)
                return emojiLabels.getOrNull(index) ?: ""
            }
        }


        var textColor = if (isNightMode()) Color.WHITE else Color.BLACK
        dataSet.valueTextColor = textColor

        val lineData = LineData(dataSet)

        val chart = binding.moodLineChart
        chart.data = lineData

        chart.apply {
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                labelRotationAngle = -45f
                textSize = 10f
                setTextColor(textColor)

            }

            axisLeft.apply {
                granularity = 1f
                axisMinimum = 0f
                axisMaximum = (moodLevels.size - 1).toFloat()
                valueFormatter = IndexAxisValueFormatter(moodLevels)
                textSize = 12f
                setTextColor(textColor)
            }

            axisRight.isEnabled = false
            legend.isEnabled = false
            description.isEnabled = false
            setScaleEnabled(false)
            setTouchEnabled(false)
            invalidate()
        }
    }

    private fun setupWeightChart(data: List<DailySummary>) {
        val entries = data.mapIndexed { index, summary ->
            Entry(index.toFloat(), summary.weight)
        }

        val labels = data.map { it.date.takeLast(2) } // Show only day (DD) on X-axis

        val dataSet = LineDataSet(entries, "Weight")
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.light_primary)
        dataSet.valueTextSize = 10f
        dataSet.setDrawFilled(true)
        dataSet.setDrawCircles(true)
        dataSet.setDrawValues(true)
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.light_primary))

        val lineData = LineData(dataSet)

        val weightLineChart = binding.weightLineChart // Make sure it's a LineChart in your XML
        weightLineChart.data = lineData

        var textColor = if (isNightMode()) Color.WHITE else Color.BLACK
        dataSet.valueTextColor = textColor

        weightLineChart.apply {
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                labelRotationAngle = -45f
                textSize = 10f
                setTextColor(textColor)
            }
            axisLeft.apply{
                axisMinimum = 0f
                setTextColor(textColor)
            }
            axisRight.isEnabled = false
            description.isEnabled = false
            legend.isEnabled = false
            setVisibleXRangeMaximum(7f)
            setScaleEnabled(false)
            isDragEnabled = true
            invalidate()
        }
    }



    private fun isNightMode(): Boolean {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }





    private fun showLoadingDialog() {
        if (loadingDialog == null) {
            val dialogView = layoutInflater.inflate(R.layout.dialog_loading, null)

            loadingDialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create()
        }

        loadingDialog?.show()
    }


    private fun hideLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }


}
