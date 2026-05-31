package com.example.receiver

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("task_id", 0)
        val taskTitle = intent.getStringExtra("task_title") ?: "Checklist Milestone"
        val taskDesc = intent.getStringExtra("task_desc") ?: "Active task reminder!"
        val alarmType = intent.getStringExtra("alarm_type") ?: "EXACT_ALARM"

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val task = db.taskDao().getTaskById(taskId)

                // If task no longer exists, exit silently
                if (task == null) {
                    pendingResult.finish()
                    return@launch
                }

                // If the task has already been completed, we don't ring the alarm or notify!
                if (task.isCompleted) {
                    pendingResult.finish()
                    return@launch
                }

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                when (alarmType) {
                    "PRE_REMINDER" -> {
                        val channelId = "taskflow_pre_reminder_channel"
                        val channelName = "Pre-Task Reminders"
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                                description = "Alerts sent 10 minutes prior to task start times."
                            }
                            notificationManager.createNotificationChannel(channel)
                        }

                        val mainIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        val pendingIntent = PendingIntent.getActivity(
                            context,
                            taskId * 10 + 1,
                            mainIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                        )

                        val builder = NotificationCompat.Builder(context, channelId)
                            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                            .setContentTitle("Upcoming Checklist Milestone 🧸")
                            .setContentText("\"$taskTitle\" starts in exactly 10 minutes! Get ready.")
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setContentIntent(pendingIntent)
                            .setOngoing(true)
                            .setAutoCancel(false)

                        NotificationManagerCompat.from(context).notify(taskId * 10 + 1, builder.build())
                    }

                    "EXACT_ALARM" -> {
                        val channelId = "taskflow_exact_alarm_channel"
                        val channelName = "Loud Task Alarms"
                        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                                description = "Ringing task alarms at precise times."
                                setSound(alarmUri, AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build())
                                enableVibration(true)
                                setVibrationPattern(longArrayOf(0, 500, 250, 500, 250, 500))
                            }
                            notificationManager.createNotificationChannel(channel)
                        }

                        val mainIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        val pendingIntent = PendingIntent.getActivity(
                            context,
                            taskId * 10 + 2,
                            mainIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                        )

                        val builder = NotificationCompat.Builder(context, channelId)
                            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                            .setContentTitle("🚨 TASK ALARM ACTIVE!")
                            .setContentText("It is precisely time for: \"$taskTitle\"! Let's do it now. 🧸")
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setCategory(NotificationCompat.CATEGORY_ALARM)
                            .setSound(alarmUri)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)

                        NotificationManagerCompat.from(context).notify(taskId * 10 + 2, builder.build())

                        // Play custom synthesized alarm tune for exactly 5 seconds
                        try {
                            val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)
                            val endTime = System.currentTimeMillis() + 5000
                            val tones = intArrayOf(
                                android.media.ToneGenerator.TONE_DTMF_1,
                                android.media.ToneGenerator.TONE_DTMF_3,
                                android.media.ToneGenerator.TONE_DTMF_5,
                                android.media.ToneGenerator.TONE_DTMF_9
                            )
                            var idx = 0
                            while (System.currentTimeMillis() < endTime) {
                                toneGen.startTone(tones[idx % tones.size], 250)
                                delay(450)
                                idx++
                            }
                            toneGen.release()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    "FORGOT_REMINDER" -> {
                        val channelId = "taskflow_forgot_reminder_channel"
                        val channelName = "Overdue Tasks Checks"
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                                description = "Alerts sent 15 minutes after task time if uncompleted."
                            }
                            notificationManager.createNotificationChannel(channel)
                        }

                        val mainIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        val pendingIntent = PendingIntent.getActivity(
                            context,
                            taskId * 10 + 3,
                            mainIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                        )

                        val builder = NotificationCompat.Builder(context, channelId)
                            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                            .setContentTitle("Forgot your task? 🧸")
                            .setContentText("Your scheduled milestone \"$taskTitle\" has not been checked off as completed yet. Don't let it slip!")
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)

                        NotificationManagerCompat.from(context).notify(taskId * 10 + 3, builder.build())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
