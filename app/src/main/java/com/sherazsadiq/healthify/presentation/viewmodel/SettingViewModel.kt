import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherazsadiq.healthify.data.FirebaseRepository
import com.sherazsadiq.healthify.utils.ResultState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Define UiState for showing Toast messages
data class ToastMessageState(val message: String, val isError: Boolean = false)

data class HealthGoalUiState(
    val stepGoal: Int = 6000,
    val waterGoal: Int = 3000,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ReminderTimeUiState(
    val timeFormatted: String = "Not set",
    val isLoading: Boolean = false,
    val error: String? = null
)

class SettingViewModel(
    private val firebaseRepo: FirebaseRepository = FirebaseRepository()
) : ViewModel() {

    // Health goals state
    private val _healthGoalState = MutableStateFlow(HealthGoalUiState())
    val healthGoalState: StateFlow<HealthGoalUiState> get() = _healthGoalState

    // Reminder time state
    private val _reminderTimeState = MutableStateFlow(ReminderTimeUiState())
    val reminderTimeState: StateFlow<ReminderTimeUiState> get() = _reminderTimeState

    // State for showing Toast messages
    private val _toastMessageState = MutableStateFlow<ToastMessageState?>(null)
    val toastMessageState: StateFlow<ToastMessageState?> get() = _toastMessageState

    fun fetchHealthGoals() {
        viewModelScope.launch {
            _healthGoalState.value = HealthGoalUiState(isLoading = true)
            when (val result = firebaseRepo.getHealthGoals()) {
                is ResultState.Success -> {
                    _healthGoalState.value = HealthGoalUiState(
                        stepGoal = result.data.stepGoal,
                        waterGoal = result.data.waterIntakeGoal
                    )
                }
                is ResultState.Error -> {
                    _healthGoalState.value = HealthGoalUiState(error = result.message)
                    _toastMessageState.value = ToastMessageState("Error fetching health goals", true)
                }
                ResultState.Loading -> {
                    _healthGoalState.value = HealthGoalUiState(isLoading = true)
                }
            }
        }
    }

    fun saveStepGoal(stepGoal: Int) {
        viewModelScope.launch {
            when (val result = firebaseRepo.saveStepGoal(stepGoal)) {
                is ResultState.Success -> {
                    _toastMessageState.value = ToastMessageState("New step goal set: $stepGoal steps")
                }
                is ResultState.Error -> {
                    _toastMessageState.value = ToastMessageState("Error: ${result.message}", true)
                }
                else -> {}
            }
        }
    }

    fun saveWaterGoal(waterGoal: Int) {
        viewModelScope.launch {
            when (val result = firebaseRepo.saveWaterIntakeGoal(waterGoal)) {
                is ResultState.Success -> {
                    _toastMessageState.value = ToastMessageState("New water goal set: $waterGoal ml")
                }
                is ResultState.Error -> {
                    _toastMessageState.value = ToastMessageState("Error: ${result.message}", true)
                }
                else -> {}
            }
        }
    }

    fun fetchReminderTime() {
        viewModelScope.launch {
            _reminderTimeState.value = ReminderTimeUiState(isLoading = true)
            when (val result = firebaseRepo.getReminderTime()) {
                is ResultState.Success -> {
                    val time = result.data
                    val formattedTime = formatTime(time.hour, time.minute, time.amPm)
                    _reminderTimeState.value = ReminderTimeUiState(timeFormatted = formattedTime)
                }
                is ResultState.Error -> {
                    _reminderTimeState.value = ReminderTimeUiState(error = result.message)
                    _toastMessageState.value = ToastMessageState("Error fetching reminder time", true)
                }
                ResultState.Loading -> {
                    _reminderTimeState.value = ReminderTimeUiState(isLoading = true)
                }
            }
        }
    }

    fun saveReminderTime(hour: Int, minute: Int, amPm: String) {
        viewModelScope.launch {
            when (val result = firebaseRepo.saveReminderTime(hour, minute, amPm)) {
                is ResultState.Success -> {
                    _toastMessageState.value = ToastMessageState("Reminder time set successfully")
                }
                is ResultState.Error -> {
                    _toastMessageState.value = ToastMessageState("Error: ${result.message}", true)
                }
                else -> {}
            }
        }
    }

    private fun formatTime(hour: Int, minute: Int, amPm: String): String {
        val hr = if (hour == 0) 12 else hour
        return String.format("%02d:%02d %s", hr, minute, amPm)
    }
}
