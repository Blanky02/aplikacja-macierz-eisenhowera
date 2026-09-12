package pl.fokus.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pl.fokus.app.MainActivity
import pl.fokus.app.R
import pl.fokus.app.data.FocusDatabase
import pl.fokus.app.data.Quadrant
import pl.fokus.app.data.TaskEntity
import pl.fokus.app.data.quadrantFor

object WidgetUpdater {
    fun update(context: Context, tasks: List<TaskEntity>) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, FokusWidgetReceiver::class.java)
        val ids = manager.getAppWidgetIds(component)
        if (ids.isEmpty()) return

        val counts = Quadrant.entries.associateWith { quadrant ->
            tasks.count { quadrantFor(it) == quadrant }
        }
        val views = RemoteViews(context.packageName, R.layout.widget_fokus).apply {
            setTextViewText(R.id.widget_subtitle, context.getString(R.string.widget_active_count, tasks.size))
            setTextViewText(R.id.widget_q0, context.getString(R.string.widget_quadrant_count, counts[Quadrant.DO_NOW] ?: 0))
            setTextViewText(R.id.widget_q1, context.getString(R.string.widget_quadrant_count, counts[Quadrant.SCHEDULE] ?: 0))
            setTextViewText(R.id.widget_q2, context.getString(R.string.widget_quadrant_count, counts[Quadrant.DELEGATE] ?: 0))
            setTextViewText(R.id.widget_q3, context.getString(R.string.widget_quadrant_count, counts[Quadrant.ELIMINATE] ?: 0))
            val openIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                500,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            setOnClickPendingIntent(R.id.widget_open, pendingIntent)
            setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        }
        ids.forEach { id -> manager.updateAppWidget(id, views) }
    }

    fun update(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val tasks = FocusDatabase.get(context).taskDao().observeActive().first()
            update(context, tasks)
        }
    }
}
