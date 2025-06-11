package com.sherazsadiq.healthify.data



import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sherazsadiq.healthify.utils.ResultState
import kotlinx.coroutines.tasks.await
import com.sherazsadiq.healthify.models.DailyEntry
import com.sherazsadiq.healthify.models.HealthGoals
import com.sherazsadiq.healthify.models.SleepEntry
import com.sherazsadiq.healthify.models.WaterEntry
import com.sherazsadiq.healthify.models.MoodEntry
import com.sherazsadiq.healthify.models.ReminderTime
import com.sherazsadiq.healthify.models.StepEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class FirebaseRepository {


    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()


    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun login(email: String, password: String): ResultState<Boolean> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            ResultState.Success(true)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Login failed")
        }
    }

    suspend fun signup(email: String, password: String): ResultState<Boolean> {
        return try {
            auth.createUserWithEmailAndPassword(email, password).await()
            ResultState.Success(true)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Signup failed")
        }
    }


    fun logout() {
        auth.signOut()
    }


    suspend fun saveStepIntake(steps: Int): ResultState<Boolean> {
        val userId = getCurrentUserId() ?: return ResultState.Error("User not logged in")
        val db = firestore.collection("users").document(userId).collection("data")

        val date = getTodayDate() // Use date-based doc id like "2024-05-26"
        val docRef = db.document(date)

        return try {
            val snapshot = docRef.get().await()

            val newEntry = StepEntry(steps = steps, timestamp = System.currentTimeMillis())

            if (snapshot.exists()) {
                // Update existing list
                val current = snapshot.toObject(DailyEntry::class.java)
                val updatedList = current?.stepsIntake?.toMutableList() ?: mutableListOf()
                updatedList.add(newEntry)

                docRef.update("stepsIntake", updatedList).await()
            } else {
                // Create new document
                val dailyEntry = DailyEntry(
                    stepsIntake = listOf(newEntry)
                )
                docRef.set(dailyEntry).await()
            }

            ResultState.Success(true)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Failed to save Steps")
        }
    }


    suspend fun saveWaterIntake(cups: Int): ResultState<Boolean> {
        val userId = getCurrentUserId() ?: return ResultState.Error("User not logged in")
        val db = firestore.collection("users").document(userId).collection("data")

        val date = getTodayDate() // Use date-based doc id like "2024-05-26"
        val docRef = db.document(date)

        return try {
            val snapshot = docRef.get().await()

            val newEntry = WaterEntry(cups = cups, time = getCurrentTime())

            if (snapshot.exists()) {
                // Update existing list
                val current = snapshot.toObject(DailyEntry::class.java)
                val updatedList = current?.waterIntake?.toMutableList() ?: mutableListOf()
                updatedList.add(newEntry)

                docRef.update("waterIntake", updatedList).await()
            } else {
                // Create new document
                val dailyEntry = DailyEntry(
                    waterIntake = listOf(newEntry)
                )
                docRef.set(dailyEntry).await()
            }

            ResultState.Success(true)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Failed to save water intake")
        }
    }

    suspend fun saveSleepEntry(bedTime: String, wakeTime: String): ResultState<Boolean> {
        val userId = getCurrentUserId() ?: return ResultState.Error("User not logged in")
        val db = firestore.collection("users").document(userId).collection("data")

        val date = getTodayDate()
        val docRef = db.document(date)

        return try {
            val snapshot = docRef.get().await()

            val newEntry = SleepEntry(
                getInBedTime = bedTime,
                wakeUpTime = wakeTime
            )

            if (snapshot.exists()) {
                val current = snapshot.toObject(DailyEntry::class.java)
                val updatedList = current?.sleepEntries?.toMutableList() ?: mutableListOf()
                updatedList.add(newEntry)

                docRef.update("sleepEntries", updatedList).await()
            } else {
                val dailyEntry = DailyEntry(
                    sleepEntries = listOf(newEntry)
                )
                docRef.set(dailyEntry).await()
            }

            ResultState.Success(true)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Failed to save sleep entry")
        }
    }


    suspend fun saveMoodEntry(mood: String): ResultState<Boolean> {
        val userId = getCurrentUserId() ?: return ResultState.Error("User not logged in")
        val db = firestore.collection("users").document(userId).collection("data")

        val date = getTodayDate()
        val docRef = db.document(date)

        return try {
            val snapshot = docRef.get().await()

            val newEntry = MoodEntry(
                mood = mood,
                timestamp = System.currentTimeMillis()
            )

            if (snapshot.exists()) {
                val current = snapshot.toObject(DailyEntry::class.java)
                val updatedList = current?.moodEntries?.toMutableList() ?: mutableListOf()
                updatedList.add(newEntry)

                docRef.update("moodEntries", updatedList).await()
            } else {
                val dailyEntry = DailyEntry(
                    moodEntries = listOf(newEntry)
                )
                docRef.set(dailyEntry).await()
            }

            ResultState.Success(true)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Failed to save mood entry")
        }
    }

    suspend fun saveWeight(weight: Float): ResultState<Boolean> {
        val userId = getCurrentUserId() ?: return ResultState.Error("User not logged in")
        val db = firestore.collection("users").document(userId).collection("data")

        val date = getTodayDate()
        val docRef = db.document(date)

        return try {
            val snapshot = docRef.get().await()

            if (snapshot.exists()) {
                // If document exists, update the weight
                docRef.update("weight", weight.toString()).await()
            } else {
                // If document does not exist, create a new entry with weight
                val dailyEntry = DailyEntry(
                    weight = weight.toString() // Store weight as String as per your model
                )
                docRef.set(dailyEntry).await()
            }

            ResultState.Success(true)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Failed to save weight")
        }
    }



    private fun getTodayDate(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    }

    private fun getCurrentTime(): String {
        return java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
    }

    suspend fun saveReminderTime(hour: Int, minute: Int, amPm: String): ResultState<Boolean> {
        val userId = getCurrentUserId() ?: return ResultState.Error("User not logged in")

        val reminderDocRef = firestore.collection("users")
            .document(userId)
            .collection("reminder")
            .document("main")

        return try {
            val snapshot = reminderDocRef.get().await()

            if (snapshot.exists()) {
                // If document exists, update the reminder time
                reminderDocRef.update(
                    mapOf(
                        "hour" to hour,
                        "minute" to minute,
                        "amPm" to amPm
                    )
                ).await()
            } else {
                // If document does not exist, create a new reminder entry
                val reminder = ReminderTime(hour, minute, amPm)
                reminderDocRef.set(reminder).await()
            }

            ResultState.Success(true)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Failed to save reminder time")
        }
    }

    suspend fun getReminderTime(): ResultState<ReminderTime> {
        val userId = getCurrentUserId() ?: return ResultState.Error("User not logged in")

        val reminderDocRef = firestore.collection("users")
            .document(userId)
            .collection("reminder")
            .document("main")

        return try {
            val snapshot = reminderDocRef.get().await()
            if (snapshot.exists()) {
                val reminder = snapshot.toObject(ReminderTime::class.java)
                if (reminder != null) {
                    ResultState.Success(reminder)
                } else {
                    ResultState.Error("Failed to parse reminder time")
                }
            } else {
                ResultState.Error("No reminder time found")
            }
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Failed to get reminder time")
        }
    }




    suspend fun saveStepGoal(newStepGoal: Int): ResultState<Boolean> {
        val userId = getCurrentUserId() ?: return ResultState.Error("User not logged in")

        // Final path: users/{userId}/goals/main
        val goalDocRef = firestore.collection("users")
            .document(userId)
            .collection("goals")
            .document("main")

        return try {
            val snapshot = goalDocRef.get().await()

            if (snapshot.exists()) {
                // Update only the stepGoal field
                goalDocRef.update("stepGoal", newStepGoal).await()
            } else {
                // Create new document with stepGoal
                val goal = HealthGoals(stepGoal = newStepGoal)
                goalDocRef.set(goal).await()
            }

            ResultState.Success(true)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Failed to save step goal")
        }
    }

    suspend fun saveWaterIntakeGoal(newWaterGoal: Int): ResultState<Boolean> {
        val userId = getCurrentUserId() ?: return ResultState.Error("User not logged in")

        val goalDocRef = firestore.collection("users")
            .document(userId)
            .collection("goals")
            .document("main")

        return try {
            val snapshot = goalDocRef.get().await()

            if (snapshot.exists()) {
                // Update only the waterIntakeGoal field
                goalDocRef.update("waterIntakeGoal", newWaterGoal).await()
            } else {
                // Create new document with waterIntakeGoal
                val goal = HealthGoals(waterIntakeGoal = newWaterGoal)
                goalDocRef.set(goal).await()
            }

            ResultState.Success(true)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Failed to save water intake goal")
        }
    }


    suspend fun getTodayDailyEntry(): DailyEntry? {
        val userId = auth.currentUser?.uid ?: return null

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayDate = dateFormat.format(Date()) // e.g. "2025-05-26"

        return try {
            val snapshot = firestore
                .collection("users")
                .document(userId)
                .collection("data")
                .document(todayDate)
                .get()
                .await()

            snapshot.toObject(DailyEntry::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    suspend fun getHealthGoals(): ResultState<HealthGoals> {
        val userId = getCurrentUserId() ?: return ResultState.Error("User not logged in")

        return try {
            val snapshot = firestore
                .collection("users")
                .document(userId)
                .collection("goals")
                .document("main")
                .get()
                .await()

            val goals = snapshot.toObject(HealthGoals::class.java)
            if (goals != null) {
                ResultState.Success(goals)
            } else {
                ResultState.Error("Health goals not found")
            }
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Failed to fetch health goals")
        }
    }


    suspend fun getAllWeights(): List<Pair<Float, String>> {
        val userId = getCurrentUserId() ?: return emptyList()
        val db = firestore.collection("users").document(userId).collection("data")

        return try {
            val snapshots = db.get().await()
            val weights = snapshots.documents.mapNotNull { document ->
                val weight = document.getString("weight")?.toFloatOrNull()
                val date = document.id // Document ID is in "yyyy-MM-dd" format
                if (weight != null) {
                    val monthName = try {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val parsedDate = dateFormat.parse(date)
                        val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
                        monthFormat.format(parsedDate ?: return@mapNotNull null)
                    } catch (e: Exception) {
                        null
                    }
                    if (monthName != null) Pair(weight, monthName) else null
                } else null
            }.sortedBy { document ->
                try {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    dateFormat.parse(document.second)?.time ?: Long.MAX_VALUE
                } catch (e: Exception) {
                    Long.MAX_VALUE
                }
            } // Sort by date in ascending order
            weights
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }






}
