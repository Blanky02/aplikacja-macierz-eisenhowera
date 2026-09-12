package pl.fokus.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import pl.fokus.app.data.TaskEntity

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(task: TaskEntity) {
        cancel(task.id)
        val dueAt = task.dueAt ?: return
        val offset = task.reminderOffsetMinutes ?: return
        val triggerAt = dueAt - offset * 60_000L
        if (triggerAt <= System.currentTimeMillis()) return

        val pendingIntent = pendingIntent(task)
        alarmManager?.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent,
        )
    }

    fun cancel(taskId: Long) {
        if (taskId == 0L) return
        alarmManager?.cancel(pendingIntent(taskId))
    }

    private fun pendingIntent(task: TaskEntity): PendingIntent {
        // The title is copied into the intent when an alarm is scheduled so the
        // receiver does not need to open the database while Android is waking up.
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("task_id", task.id)
            putExtra("title", task.title)
        }
        return PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun pendingIntent(taskId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("task_id", taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
