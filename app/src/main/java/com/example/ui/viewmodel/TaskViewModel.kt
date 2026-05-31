package com.example.ui.viewmodel

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.Web3FormsClient
import com.example.receiver.ReminderReceiver
import com.example.widget.TaskWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TaskViewModel(
    private val repository: TaskRepository,
    private val sharedPrefs: SharedPreferences
) : ViewModel() {

    // --- Loading States ---
    private val _isAppLoading = MutableStateFlow(true)
    val isAppLoading: StateFlow<Boolean> = _isAppLoading.asStateFlow()

    // --- Day/Night Theme Mode State ---
    private val _isDarkTheme = MutableStateFlow(sharedPrefs.getBoolean("dark_theme", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        val current = _isDarkTheme.value
        val newValue = !current
        _isDarkTheme.value = newValue
        sharedPrefs.edit().putBoolean("dark_theme", newValue).apply()
    }

    // --- iCloud Synch Simulation State ---
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(sharedPrefs.getLong("last_sync_time", System.currentTimeMillis()))
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    // --- Developer Form Communication States ---
    private val _contactFormState = MutableStateFlow<FormState>(FormState.Idle)
    val contactFormState: StateFlow<FormState> = _contactFormState.asStateFlow()

    // --- Database Source States ---
    val tasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val goals: StateFlow<List<LongTermGoal>> = repository.allGoals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // App loading delay to showcase beautiful layout animations
        viewModelScope.launch {
            delay(1800)
            _isAppLoading.value = false
        }
    }

    // Trigger iCloud Synch Simulation
    fun triggerCloudSync(context: Context) {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            // Simulate networking
            delay(1500)
            val now = System.currentTimeMillis()
            _lastSyncTime.value = now
            sharedPrefs.edit().putLong("last_sync_time", now).apply()
            _isSyncing.value = false
            // Update Widgets to sync
            TaskWidgetProvider.triggerUpdate(context)
        }
    }

    // Send Form message using Web3Forms Client
    fun submitContactForm(name: String, email: String, message: String) {
        if (name.isBlank() || email.isBlank() || message.isBlank()) {
            _contactFormState.value = FormState.Error("Please fill out all fields.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _contactFormState.value = FormState.Submitting
            try {
                val response = Web3FormsClient.service.submitForm(
                    accessKey = "c402fcce-5e4f-4c13-801e-7f0bf46c2989",
                    name = name,
                    email = email,
                    message = message
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    _contactFormState.value = FormState.Success("Message sent successfully!")
                } else {
                    val errMsg = response.body()?.message ?: "Web3Forms submission failed."
                    _contactFormState.value = FormState.Error(errMsg)
                }
            } catch (e: Exception) {
                _contactFormState.value = FormState.Error("Failed to connect: ${e.localizedMessage}")
            }
        }
    }

    fun resetContactFormState() {
        _contactFormState.value = FormState.Idle
    }

    // --- Tasks CRUD Operations (Room + Widget Updates) ---
    fun addTask(context: Context, title: String, description: String, priority: Int, dueDate: Long?, reminderTime: Long?, longTermGoalId: Int?) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentTasks = tasks.value
            val maxOrderIndex = currentTasks.maxOfOrNull { it.orderIndex } ?: -1
            
            val task = Task(
                title = title,
                description = description,
                priority = priority,
                dueDate = dueDate,
                orderIndex = maxOrderIndex + 1,
                reminderTime = reminderTime,
                longTermGoalId = longTermGoalId
            )
            val createdId = repository.insertTask(task)
            
            // If reminders are set, register with AlarmManager
            if (reminderTime != null && reminderTime > System.currentTimeMillis()) {
                scheduleAlarm(context, createdId.toInt(), title, description, reminderTime)
            }
            TaskWidgetProvider.triggerUpdate(context)
        }
    }

    fun updateTask(context: Context, task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTask(task)
            TaskWidgetProvider.triggerUpdate(context)
        }
    }

    fun toggleTaskCompletion(context: Context, task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = task.copy(isCompleted = !task.isCompleted)
            repository.updateTask(updated)
            TaskWidgetProvider.triggerUpdate(context)
        }
    }

    fun updateTaskReminder(context: Context, task: Task, reminderTime: Long?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (reminderTime == null) {
                cancelAlarm(context, task.id)
            } else if (reminderTime > System.currentTimeMillis()) {
                scheduleAlarm(context, task.id, task.title, task.description, reminderTime)
            }
            val updated = task.copy(reminderTime = reminderTime)
            repository.updateTask(updated)
            TaskWidgetProvider.triggerUpdate(context)
        }
    }

    fun deleteTask(context: Context, task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            cancelAlarm(context, task.id)
            repository.deleteTask(task)
            TaskWidgetProvider.triggerUpdate(context)
        }
    }

    fun moveTask(fromIndex: Int, toIndex: Int, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val taskList = tasks.value.toMutableList()
            if (fromIndex in taskList.indices && toIndex in taskList.indices) {
                val item = taskList.removeAt(fromIndex)
                taskList.add(toIndex, item)
                
                // Reassign indexes
                val updatedList = taskList.mapIndexed { index, task ->
                    task.copy(orderIndex = index)
                }
                repository.reorderTasks(updatedList)
                TaskWidgetProvider.triggerUpdate(context)
            }
        }
    }

    // --- Subtasks CRUD ---
    fun getSubtasks(taskId: Int): Flow<List<Subtask>> {
        return repository.getSubtasksForTask(taskId)
    }

    fun addSubtask(taskId: Int, title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val subtask = Subtask(taskId = taskId, title = title)
            repository.insertSubtask(subtask)
        }
    }

    fun toggleSubtaskCompletion(subtask: Subtask) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = subtask.copy(isCompleted = !subtask.isCompleted)
            repository.updateSubtask(updated)
        }
    }

    fun deleteSubtask(subtask: Subtask) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSubtask(subtask)
        }
    }

    // --- Long Term Goals CRUD ---
    fun addGoal(title: String, description: String, targetDate: Long?) {
        viewModelScope.launch(Dispatchers.IO) {
            val goal = LongTermGoal(
                title = title,
                description = description,
                targetDate = targetDate
            )
            repository.insertGoal(goal)
        }
    }

    fun toggleGoalCompletion(goal: LongTermGoal) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = goal.copy(isCompleted = !goal.isCompleted)
            repository.updateGoal(updated)
        }
    }

    fun deleteGoal(goal: LongTermGoal) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteGoal(goal)
        }
    }

    // --- Alarm manager Scheduling helper ---
    private fun scheduleAlarm(context: Context, taskId: Int, title: String, desc: String, timeInMillis: Long) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("task_id", taskId)
                putExtra("task_title", title)
                putExtra("task_desc", desc)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cancelAlarm(context: Context, taskId: Int) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId,
                intent,
                PendingIntent.FLAG_NO_CREATE or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

sealed class FormState {
    object Idle : FormState()
    object Submitting : FormState()
    data class Success(val message: String) : FormState()
    data class Error(val error: String) : FormState()
}

class TaskViewModelFactory(
    private val repository: TaskRepository,
    private val sharedPrefs: SharedPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository, sharedPrefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
