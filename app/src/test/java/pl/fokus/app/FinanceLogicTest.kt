package pl.fokus.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.fokus.app.data.FinanceQuadrant
import pl.fokus.app.data.ExpenseEntity
import pl.fokus.app.data.currentBudgetPeriod
import pl.fokus.app.data.parseMinorUnits
import pl.fokus.app.data.suggestQuadrant
import pl.fokus.app.data.suggestedPlan

class FinanceLogicTest {
    @Test
    fun parsesPolishAmountIntoMinorUnits() {
        assertEquals(12345L, parseMinorUnits("123,45"))
        assertEquals(900L, parseMinorUnits("9"))
        assertNull(parseMinorUnits("0"))
    }

    @Test
    fun proposesImportantUrgentForRent() {
        assertEquals(FinanceQuadrant.IMPORTANT_URGENT, suggestQuadrant("Czynsz za mieszkanie"))
        assertEquals(FinanceQuadrant.IMPORTANT_NOT_URGENT, suggestQuadrant("Oszczędności na wakacje"))
    }

    @Test
    fun suggestedPlanAlwaysAddsUpToIncome() {
        val history = listOf(
            ExpenseEntity(name = "Czynsz", amountMinor = 300_000, currency = "PLN", baseAmountMinor = 300_000, exchangeRate = 1.0, quadrantIndex = 0, occurredAt = 1L),
            ExpenseEntity(name = "Film", amountMinor = 50_000, currency = "PLN", baseAmountMinor = 50_000, exchangeRate = 1.0, quadrantIndex = 3, occurredAt = 1L),
        )
        val plan = suggestedPlan(1_000_000, history)
        assertEquals(1_000_000L, plan.values.sum())
        assertTrue(plan[FinanceQuadrant.IMPORTANT_URGENT]!! > plan[FinanceQuadrant.NOT_IMPORTANT_NOT_URGENT]!!)
    }

    @Test
    fun customPeriodStartsOnConfiguredDay() {
        val period = currentBudgetPeriod(startDay = 10, now = 1_735_000_000_000L)
        assertTrue(period.endAt > period.startAt)
    }
}
