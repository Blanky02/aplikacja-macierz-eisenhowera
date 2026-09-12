package pl.fokus.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Calendar
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToLong

/** Financial meaning of the four Eisenhower quadrants. */
enum class FinanceQuadrant(
    val index: Int,
    val title: String,
    val subtitle: String,
) {
    IMPORTANT_URGENT(0, "Ważne i pilne", "Rachunki i obowiązki"),
    IMPORTANT_NOT_URGENT(1, "Ważne i niepilne", "Oszczędności i cele"),
    NOT_IMPORTANT_URGENT(2, "Mniej ważne i pilne", "Bieżące potrzeby"),
    NOT_IMPORTANT_NOT_URGENT(3, "Mniej ważne i niepilne", "Przyjemności i zachcianki"),
    ;

    companion object {
        fun fromIndex(index: Int): FinanceQuadrant =
            entries.firstOrNull { it.index == index } ?: IMPORTANT_URGENT
    }
}

val supportedCurrencies = listOf("PLN", "EUR", "USD", "GBP", "CHF")

@Entity(tableName = "incomes", indices = [Index("receivedAt")])
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: String,
    /** Amount in the original transaction currency, stored in minor units. */
    val amountMinor: Long,
    val currency: String,
    /** Converted amount in the selected base currency. */
    val baseAmountMinor: Long?,
    val exchangeRate: Double?,
    val receivedAt: Long,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "expenses", indices = [Index("occurredAt"), Index("quadrantIndex")])
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountMinor: Long,
    val currency: String,
    val baseAmountMinor: Long?,
    val exchangeRate: Double?,
    val quadrantIndex: Int,
    val occurredAt: Long,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "allocations",
    primaryKeys = ["periodKey", "quadrantIndex"],
)
data class AllocationEntity(
    val periodKey: String,
    val quadrantIndex: Int,
    val plannedMinor: Long,
    val currency: String,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "transfers", indices = [Index("periodKey")])
data class TransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val periodKey: String,
    val fromQuadrant: Int,
    val toQuadrant: Int,
    val amountMinor: Long,
    val currency: String,
    val createdAt: Long = System.currentTimeMillis(),
)

data class FinanceSettings(
    val baseCurrency: String = "PLN",
    val budgetStartDay: Int = 1,
    val setupCompleted: Boolean = false,
)

data class BudgetPeriod(
    val key: String,
    val startAt: Long,
    val endAt: Long,
)

data class BudgetSnapshot(
    val period: BudgetPeriod,
    val baseCurrency: String,
    val incomes: List<IncomeEntity>,
    val expenses: List<ExpenseEntity>,
    val allocations: List<AllocationEntity>,
) {
    val totalIncomeMinor: Long get() = incomes.sumOf { it.baseAmountMinor ?: 0L }
    val totalSpentMinor: Long get() = expenses.sumOf { it.baseAmountMinor ?: 0L }
    val allocatedMinor: Long get() = allocations.sumOf { it.plannedMinor }
    val unallocatedMinor: Long get() = totalIncomeMinor - allocatedMinor
    val remainingMinor: Long get() = totalIncomeMinor - totalSpentMinor

    fun planned(quadrant: FinanceQuadrant): Long =
        allocations.firstOrNull { it.quadrantIndex == quadrant.index }?.plannedMinor ?: 0L

    fun spent(quadrant: FinanceQuadrant): Long =
        expenses.filter { it.quadrantIndex == quadrant.index }.sumOf { it.baseAmountMinor ?: 0L }

    fun remaining(quadrant: FinanceQuadrant): Long = planned(quadrant) - spent(quadrant)
}

fun currentBudgetPeriod(startDay: Int, now: Long = System.currentTimeMillis()): BudgetPeriod {
    val actualStartDay = startDay.coerceIn(1, 28)
    val start = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (get(Calendar.DAY_OF_MONTH) < actualStartDay) add(Calendar.MONTH, -1)
        set(Calendar.DAY_OF_MONTH, actualStartDay)
    }
    val end = Calendar.getInstance().apply {
        timeInMillis = start.timeInMillis
        add(Calendar.MONTH, 1)
        add(Calendar.MILLISECOND, -1)
    }
    val key = "${start.get(Calendar.YEAR)}-%02d-%02d".format(
        Locale.US,
        start.get(Calendar.MONTH) + 1,
        start.get(Calendar.DAY_OF_MONTH),
    )
    return BudgetPeriod(key, start.timeInMillis, end.timeInMillis)
}

fun parseMinorUnits(value: String): Long? = runCatching {
    BigDecimal(value.trim().replace(',', '.'))
        .movePointRight(2)
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact()
}.getOrNull()?.takeIf { it > 0L }

fun formatMoney(minor: Long, currencyCode: String, signed: Boolean = false): String {
    val currency = runCatching { Currency.getInstance(currencyCode) }.getOrNull()
    val decimals = currency?.defaultFractionDigits?.coerceAtLeast(0) ?: 2
    val amount = BigDecimal(minor).movePointLeft(decimals)
    val formatter = NumberFormat.getCurrencyInstance(Locale("pl", "PL")).apply {
        currency?.let { setCurrency(it) }
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
    }
    val formatted = formatter.format(amount)
    return if (signed && minor > 0) "+$formatted" else formatted
}

fun convertToBase(amountMinor: Long, rate: Double?): Long? =
    rate?.let { (amountMinor.toDouble() * it).roundToLong() }

fun suggestQuadrant(expenseName: String): FinanceQuadrant {
    val value = expenseName.lowercase(Locale("pl", "PL"))
    return when {
        listOf("czynsz", "prąd", "gaz", "woda", "rat", "kredyt", "leki", "dent", "ubezpiec", "podatek", "mandat")
            .any(value::contains) -> FinanceQuadrant.IMPORTANT_URGENT
        listOf("oszczęd", "inwest", "emeryt", "podusz", "kurs", "szkol", "książ", "cel")
            .any(value::contains) -> FinanceQuadrant.IMPORTANT_NOT_URGENT
        listOf("paliw", "bilet", "transport", "napraw", "telefon", "internet", "zakup")
            .any(value::contains) -> FinanceQuadrant.NOT_IMPORTANT_URGENT
        else -> FinanceQuadrant.NOT_IMPORTANT_NOT_URGENT
    }
}

/** A transparent proposal based on observed expenses; it never saves anything by itself. */
fun suggestedPlan(totalIncomeMinor: Long, history: List<ExpenseEntity>): Map<FinanceQuadrant, Long> {
    if (totalIncomeMinor <= 0L) return FinanceQuadrant.entries.associateWith { 0L }
    val byQuadrant = FinanceQuadrant.entries.associateWith { quadrant ->
        history.filter { it.quadrantIndex == quadrant.index }.sumOf { it.baseAmountMinor ?: 0L }
    }
    val historicalTotal = byQuadrant.values.sum()
    if (historicalTotal <= 0L) {
        return FinanceQuadrant.entries.associateWith { totalIncomeMinor / FinanceQuadrant.entries.size }
    }
    val result = byQuadrant.mapValues { (_, spent) ->
        (totalIncomeMinor.toDouble() * spent / historicalTotal).roundToLong()
    }.toMutableMap()
    val difference = totalIncomeMinor - result.values.sum()
    result[FinanceQuadrant.IMPORTANT_URGENT] =
        (result[FinanceQuadrant.IMPORTANT_URGENT] ?: 0L) + difference
    return result
}
