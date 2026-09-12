package pl.fokus.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.createChannel(context)
        val taskId = intent.getLongExtra("task_id", 0L)
        val title = intent.getStringExtra("title")
            ?: context.getString(pl.fokus.app.R.string.notification_fallback)
        NotificationHelper.showReminder(context, taskId, title)
    }
}
