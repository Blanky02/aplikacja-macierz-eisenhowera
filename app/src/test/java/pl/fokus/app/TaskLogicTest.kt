package pl.fokus.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.fokus.app.data.Quadrant
import pl.fokus.app.data.Recurrence
import pl.fokus.app.data.TaskEntity
import pl.fokus.app.data.quadrantFor
import java.util.Calendar

class TaskLogicTest {
    @Test
    fun importantTaskDueSoonGoesToDoNow() {
        val task = TaskEntity(
            title = "Przygotować prezentację",
            isImportant = true,
            dueAt = System.currentTimeMillis() + 60 * 60 * 1000,
        )
        assertEquals(Quadrant.DO_NOW, quadrantFor(task))
    }

    @Test
    fun importantTaskWithoutNearDeadlineIsScheduled() {
        val task = TaskEntity(
            title = "Plan kwartalny",
            isImportant = true,
            dueAt = System.currentTimeMillis() + 10 * 24 * 60 * 60 * 1000,
        )
        assertEquals(Quadrant.SCHEDULE, quadrantFor(task))
    }

    @Test
    fun recurringTaskMovesToNextDay() {
        val start = Calendar.getInstance().apply { set(2026, 0, 10, 9, 0, 0) }.timeInMillis
        val next = Recurrence.nextDate(start, Recurrence.DAILY)
        assertTrue(next!! > start)
        val result = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(11, result.get(Calendar.DAY_OF_MONTH))
    }
}
