package pl.fokus.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import pl.fokus.app.notifications.ReminderScheduler
import pl.fokus.app.widget.WidgetUpdater

class TaskRepository(
    private val context: Context,
    private val dao: TaskDao,
) {
    private val reminderScheduler = ReminderScheduler(context)

    val active: Flow<List<TaskEntity>> = dao.observeActive()
    val archived: Flow<List<TaskEntity>> = dao.observeArchived()

    suspend fun save(task: TaskEntity) {
        val saved = task.copy(updatedAt = System.currentTimeMillis())
        val generatedId = dao.upsert(saved)
        val taskWithId = if (saved.id == 0L) saved.copy(id = generatedId) else saved.copy(id = saved.id)
        scheduleReminder(taskWithId)
        refreshWidget()
    }

    suspend fun move(task: TaskEntity, quadrant: Int?) {
        save(task.copy(manualQuadrant = quadrant))
    }

    suspend fun archive(task: TaskEntity) {
        val archivedTask = task.copy(
            isArchived = true,
            updatedAt = System.currentTimeMillis(),
        )
        dao.upsert(archivedTask)
        reminderScheduler.cancel(task.id)
        refreshWidget()
    }

    suspend fun toggleComplete(task: TaskEntity) {
        val now = System.currentTimeMillis()
        if (task.completedAt == null) {
            dao.upsert(
                task.copy(
                    completedAt = now,
                    isArchived = true,
                    updatedAt = now,
                ),
            )
            reminderScheduler.cancel(task.id)

            // Completing a recurring task creates its next occurrence and keeps
            // the completed one in the archive for history and statistics.
            val nextDueAt = Recurrence.nextDate(
                dueAt = task.dueAt,
                type = task.recurrenceType,
                interval = task.recurrenceInterval,
            )
            if (nextDueAt != null) {
                val next = task.copy(
                    id = 0,
                    dueAt = nextDueAt,
                    completedAt = null,
                    isArchived = false,
                    createdAt = now,
                    updatedAt = now,
                )
                val id = dao.upsert(next)
                scheduleReminder(next.copy(id = id))
            }
        } else {
            restore(task)
            return
        }
        refreshWidget()
    }

    suspend fun restore(task: TaskEntity) {
        val restored = task.copy(
            completedAt = null,
            isArchived = false,
            updatedAt = System.currentTimeMillis(),
        )
        dao.upsert(restored)
        scheduleReminder(restored)
        refreshWidget()
    }

    suspend fun delete(task: TaskEntity) {
        dao.deleteById(task.id)
        reminderScheduler.cancel(task.id)
        refreshWidget()
    }

    private fun scheduleReminder(task: TaskEntity) {
        reminderScheduler.schedule(task)
    }

    private suspend fun refreshWidget() {
        WidgetUpdater.update(context, active.first())
    }
}
