package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TaskWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val db = AppDatabase.getDatabase(context)
        val dao = db.taskDao()

        // Update each widget instance on a background coroutine
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch topmost incomplete tasks ordered by index
                val tasks = dao.getAllTasks().first()
                val pendingTasks = tasks.filter { !it.isCompleted }

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.task_widget_layout)
                    
                    // Count indicator text
                    views.setTextViewText(R.id.widget_count, "${pendingTasks.size} Tasks")

                    // Update task text views
                    if (pendingTasks.isEmpty()) {
                        views.setTextViewText(R.id.task_item_1, "• No pending tasks")
                        views.setViewVisibility(R.id.task_item_1, View.VISIBLE)
                        views.setViewVisibility(R.id.task_item_2, View.GONE)
                        views.setViewVisibility(R.id.task_item_3, View.GONE)
                    } else {
                        // Task 1
                        views.setTextViewText(R.id.task_item_1, "• ${pendingTasks[0].title}")
                        views.setViewVisibility(R.id.task_item_1, View.VISIBLE)

                        // Task 2
                        if (pendingTasks.size > 1) {
                            views.setTextViewText(R.id.task_item_2, "• ${pendingTasks[1].title}")
                            views.setViewVisibility(R.id.task_item_2, View.VISIBLE)
                        } else {
                            views.setViewVisibility(R.id.task_item_2, View.GONE)
                        }

                        // Task 3
                        if (pendingTasks.size > 2) {
                            views.setTextViewText(R.id.task_item_3, "• ${pendingTasks[2].title}")
                            views.setViewVisibility(R.id.task_item_3, View.VISIBLE)
                        } else {
                            views.setViewVisibility(R.id.task_item_3, View.GONE)
                        }
                    }

                    // On Click Intent to open MainActivity
                    val intent = Intent(context, MainActivity::class.java)
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // If system database changes, trigger a widget update manually
        if (intent.action == ACTION_UPDATE_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisAppWidget = ComponentName(context.packageName, TaskWidgetProvider::class.java.name)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.example.widget.ACTION_UPDATE_WIDGET"

        fun triggerUpdate(context: Context) {
            val intent = Intent(context, TaskWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            context.sendBroadcast(intent)
        }
    }
}
