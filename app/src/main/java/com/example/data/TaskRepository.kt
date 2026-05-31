package com.example.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val allGoals: Flow<List<LongTermGoal>> = taskDao.getAllGoals()

    fun getTasksByGoalId(goalId: Int): Flow<List<Task>> = taskDao.getTasksByGoalId(goalId)

    suspend fun getTaskById(taskId: Int): Task? = taskDao.getTaskById(taskId)

    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    suspend fun deleteTaskById(id: Int) = taskDao.deleteTaskById(id)

    suspend fun reorderTasks(tasks: List<Task>) = taskDao.updateTasksOrder(tasks)

    // Subtasks CRUD
    fun getSubtasksForTask(taskId: Int): Flow<List<Subtask>> = taskDao.getSubtasksForTask(taskId)

    suspend fun getSubtasksForTaskSync(taskId: Int): List<Subtask> = taskDao.getSubtasksForTaskSync(taskId)

    suspend fun insertSubtask(subtask: Subtask): Long = taskDao.insertSubtask(subtask)

    suspend fun updateSubtask(subtask: Subtask) = taskDao.updateSubtask(subtask)

    suspend fun deleteSubtask(subtask: Subtask) = taskDao.deleteSubtask(subtask)

    // Goals CRUD
    suspend fun getGoalById(goalId: Int): LongTermGoal? = taskDao.getGoalById(goalId)

    suspend fun insertGoal(goal: LongTermGoal): Long = taskDao.insertGoal(goal)

    suspend fun updateGoal(goal: LongTermGoal) = taskDao.updateGoal(goal)

    suspend fun deleteGoal(goal: LongTermGoal) = taskDao.deleteGoal(goal)
}
