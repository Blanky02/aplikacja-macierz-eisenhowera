package pl.fokus.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Calendar

/**
 * A task is deliberately kept as one local record in v1. Tags are stored as a
 * comma-separated value so the app can stay small and work without a server.
 */
@Entity(
    tableName = "tasks",
    indices = [Index("isArchived"), Index("dueAt")],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val notes: String = "",
    val link: String = "",
    val isImportant: Boolean = false,
    /** Null means that the matrix should calculate the quadrant automatically. */
    val manualQuadrant: Int? = null,
    val dueAt: Long? = null,
    /** Minutes before dueAt. Null means that no reminder is scheduled. */
    val reminderOffsetMinutes: Int? = null,
    val recurrenceType: String = Recurrence.NONE,
    val recurrenceInterval: Int = 1,
    val recurrenceDays: String = "",
    val project: String = "",
    val tags: String = "",
    val completedAt: Long? = null,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

enum class Quadrant(
    val index: Int,
    val title: String,
    val subtitle: String,
) {
    DO_NOW(0, "Zrób teraz", "Pilne i ważne"),
    SCHEDULE(1, "Zaplanuj", "Ważne, niepilne"),
    DELEGATE(2, "Ogranicz", "Pilne, mniej ważne"),
    ELIMINATE(3, "Usuń", "Niepilne i mniej ważne"),
    ;

    companion object {
        fun fromIndex(index: Int): Quadrant = entries.firstOrNull { it.index == index } ?: DO_NOW
    }
}

object Recurrence {
    const val NONE = "none"
    const val DAILY = "daily"
    const val WEEKLY = "weekly"
    const val MONTHLY = "monthly"
    const val CUSTOM = "custom"

    fun label(type: String): String = when (type) {
        DAILY -> "Codziennie"
        WEEKLY -> "Co tydzień"
        MONTHLY -> "Co miesiąc"
        CUSTOM -> "Co kilka dni"
        else -> "Nie powtarza się"
    }

    /** Returns the next occurrence, preserving the selected local time. */
    fun nextDate(
        dueAt: Long?,
        type: String,
        interval: Int = 1,
    ): Long? {
        if (dueAt == null || type == NONE) return null
        val calendar = Calendar.getInstance().apply { timeInMillis = dueAt }
        when (type) {
            DAILY -> calendar.add(Calendar.DAY_OF_YEAR, interval.coerceAtLeast(1))
            WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 7 * interval.coerceAtLeast(1))
            MONTHLY -> calendar.add(Calendar.MONTH, interval.coerceAtLeast(1))
            CUSTOM -> calendar.add(Calendar.DAY_OF_YEAR, interval.coerceAtLeast(1))
            else -> return null
        }
        return calendar.timeInMillis
    }
}

/**
 * Urgency is intentionally transparent instead of being an opaque score:
 * overdue tasks and tasks due within the next 48 hours are considered urgent.
 */
fun isUrgent(task: TaskEntity, now: Long = System.currentTimeMillis()): Boolean {
    val dueAt = task.dueAt ?: return false
    return dueAt <= now + 48L * 60L * 60L * 1000L
}

fun suggestedQuadrant(task: TaskEntity, now: Long = System.currentTimeMillis()): Quadrant {
    val urgent = isUrgent(task, now)
    return when {
        task.isImportant && urgent -> Quadrant.DO_NOW
        task.isImportant && !urgent -> Quadrant.SCHEDULE
        !task.isImportant && urgent -> Quadrant.DELEGATE
        else -> Quadrant.ELIMINATE
    }
}

fun quadrantFor(task: TaskEntity, now: Long = System.currentTimeMillis()): Quadrant =
    task.manualQuadrant?.let(Quadrant::fromIndex) ?: suggestedQuadrant(task, now)
