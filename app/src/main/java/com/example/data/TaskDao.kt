package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // --- Tasks ---
    @Query("SELECT * FROM tasks ORDER BY orderIndex ASC, id ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Int): Task?

    @Query("SELECT * FROM tasks WHERE longTermGoalId = :goalId ORDER BY orderIndex ASC")
    fun getTasksByGoalId(goalId: Int): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Int)

    @Transaction
    suspend fun updateTasksOrder(tasks: List<Task>) {
        tasks.forEach { task ->
            updateTask(task)
        }
    }

    // --- Subtasks ---
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId")
    fun getSubtasksForTask(taskId: Int): Flow<List<Subtask>>

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId")
    suspend fun getSubtasksForTaskSync(taskId: Int): List<Subtask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: Subtask): Long

    @Update
    suspend fun updateSubtask(subtask: Subtask)

    @Delete
    suspend fun deleteSubtask(subtask: Subtask)

    // --- Long Term Goals ---
    @Query("SELECT * FROM long_term_goals")
    fun getAllGoals(): Flow<List<LongTermGoal>>

    @Query("SELECT * FROM long_term_goals WHERE id = :id LIMIT 1")
    suspend fun getGoalById(id: Int): LongTermGoal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: LongTermGoal): Long

    @Update
    suspend fun updateGoal(goal: LongTermGoal)

    @Delete
    suspend fun deleteGoal(goal: LongTermGoal)
}
