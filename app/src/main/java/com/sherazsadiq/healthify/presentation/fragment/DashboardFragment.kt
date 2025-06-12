package com.sherazsadiq.healthify.presentation.fragment

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.sherazsadiq.healthify.R
import com.sherazsadiq.healthify.databinding.FragmentDashboardBinding
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.sherazsadiq.healthify.presentation.viewmodel.AuthViewModel
import com.sherazsadiq.healthify.presentation.viewmodel.DashboardViewModel
import com.sherazsadiq.healthify.presentation.viewmodel.WeightMarkerView
import com.sherazsadiq.healthify.utils.ReminderWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


class DashboardFragment : Fragment() {
    private lateinit var binding: FragmentDashboardBinding
    private val dashboardViewModel by viewModels<DashboardViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {


        // Initialize view binding (assuming it's set up)
        binding = FragmentDashboardBinding.bind(view)

        // Fetch data
        dashboardViewModel.fetchTodayDailyEntry()
        dashboardViewModel.fetchHealthGoals()


        lifecycleScope.launch {
            dashboardViewModel.stepIntakeData.collect { data ->

                if (data.goal > 0) {
                    setupStepPieChart(binding.stepsPieChart, data.currentSteps, data.goal)
                }

                binding.stepsDistanceText.text = String.format("%.2f", data.distanceKm)
                binding.stepsCaloriesText.text = String.format("%.2f", data.caloriesBurned)
            }
        }




        // Observe data and update UI
        lifecycleScope.launch {
            dashboardViewModel.waterIntakeData.collect { data ->
                binding.currentWaterIntakeText.text = "${data.currentIntake} ml"
                binding.currentWaterIntakePercentage.text = "${data.percentage}%"

                if (data.goal > 0) {
                    setupWaterPieChart(binding.waterPieChart, data.currentIntake, data.goal)
                }
            }
        }


        lifecycleScope.launch {
            dashboardViewModel.totalSleepTime.collect { sleepTime ->
                binding.totalSleepTimeText.text = sleepTime
            }
        }



        // Observe selected mood and update UI
        lifecycleScope.launchWhenStarted {
            dashboardViewModel.selectedMood.collect { selectedMood ->
                updateMoodDisplay(selectedMood)
            }
        }



        // Observe the last 6 months' weights and update the bar chart
        lifecycleScope.launchWhenStarted {
            dashboardViewModel.lastSixMonthsWeights.collect { lastSixMonthsData ->
                setupWeightBarChart(binding.weightBarChart, lastSixMonthsData, requireContext())
            }
        }


    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root

    }

    private fun setupStepPieChart(chart: PieChart, currentSteps: Int, totalSteps: Int) {
        val percentage = (currentSteps.toFloat() / totalSteps) * 100f

        val entries = listOf(
            PieEntry(currentSteps.toFloat()),
            PieEntry((totalSteps - currentSteps).toFloat())
        )

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(Color.parseColor("#4CAF50"), Color.LTGRAY) // Green + Gray
            setDrawValues(false) // Hide percentages
            sliceSpace = 2f
        }

        chart.data = PieData(dataSet)



        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setDrawEntryLabels(false)

            setDrawCenterText(true)
            centerText = "${currentSteps.toInt()}\nSteps"


            val typedValue = TypedValue()
            requireContext().theme.resolveAttribute(
                com.google.android.material.R.attr.colorOnSurface,
                typedValue,
                true
            )
            setCenterTextColor(typedValue.data)

            isRotationEnabled = false
            setUsePercentValues(false)
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 70f
            transparentCircleRadius = 0f
            animateY(1000, Easing.EaseInOutQuad)
            invalidate()
        }
    }


    private fun setupWaterPieChart(chart: PieChart, currentMl: Int, goalMl: Int) {
        val entries = listOf(
            PieEntry(currentMl.toFloat()),
            PieEntry((goalMl - currentMl).toFloat())
        )

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(Color.parseColor("#2196F3"), Color.LTGRAY) // Blue + gray
            setDrawValues(false)
            sliceSpace = 2f
        }

        chart.data = PieData(dataSet)

        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setDrawEntryLabels(false)
            setDrawCenterText(false) // we use external overlay instead
            isRotationEnabled = false
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 70f
            transparentCircleRadius = 0f
            animateY(1000, Easing.EaseInOutQuad)
            invalidate()
        }
    }


    private fun updateMoodDisplay(selectedMood: String?) {
        val container = binding.moodEmojiContainer // Reference to the container holding mood items
        for (i in 0 until container.childCount) {
            val moodItem = container.getChildAt(i) as LinearLayout
            val emojiView = moodItem.getChildAt(0) as TextView // First child is the emoji
            val moodLabel = moodItem.getChildAt(1) as TextView // Second child is the mood label

            if (moodLabel.text.toString() == selectedMood) {
                moodItem.alpha = 1f // Highlight the selected mood
                moodLabel.setTypeface(null, android.graphics.Typeface.BOLD) // Make text bold
                emojiView.textSize = 40f // Increase emoji size
            } else {
                moodItem.alpha = 0.3f // Fade out the others
                moodLabel.setTypeface(null, android.graphics.Typeface.NORMAL) // Reset text style
                emojiView.textSize = 32f // Reset emoji size
            }
        }
    }


    fun setupWeightBarChart(chart: BarChart, weights: List<Pair<Float, String>>, context: Context) {
        val entries = weights.mapIndexed { index, pair ->
            BarEntry(index.toFloat(), pair.first)
        }

        val isDarkMode = isNightMode()
        val barColor = if (isDarkMode) Color.WHITE else ContextCompat.getColor(context, R.color.light_primary)
        val baseColors = List(weights.size) { barColor }

        val dataSet = BarDataSet(entries, "Weight over Months").apply {
            colors = baseColors
            valueTextSize = 10f
            setDrawValues(false)
        }

        chart.data = BarData(dataSet)

        val textColor = if (isDarkMode) Color.WHITE else Color.BLACK
        dataSet.valueTextColor = textColor

        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            valueFormatter = IndexAxisValueFormatter(weights.map { it.second })
            setDrawGridLines(false)
            setTextColor(textColor)
        }

        chart.axisLeft.isEnabled = false
        chart.axisRight.isEnabled = false
        chart.legend.isEnabled = false
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setPinchZoom(false)

        chart.marker = WeightMarkerView(context)

        chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                val highlightIndex = h?.x?.toInt() ?: return

                dataSet.colors = baseColors.mapIndexed { index, color ->
                    if (index == highlightIndex) color else adjustAlpha(color, 0.5f)
                }
                dataSet.setDrawValues(false)
                chart.invalidate()

                chart.postDelayed({
                    dataSet.colors = baseColors
                    chart.highlightValues(null)
                    chart.invalidate()
                }, 5000)
            }

            override fun onNothingSelected() {
                // No action needed
            }
        })

        chart.animateY(1000)
        chart.invalidate()
    }



    // Helper function to adjust alpha of a color
    fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).toInt()
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }


    fun getRandomColors(size: Int): List<Int> {
        val random = java.util.Random()
        return List(size) {
            Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
        }
    }


    private fun isNightMode(): Boolean {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }





}
