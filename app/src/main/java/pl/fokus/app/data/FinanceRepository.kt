package pl.fokus.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pl.fokus.app.widget.WidgetUpdater

class FinanceRepository(
    context: Context,
    private val dao: FinanceDao,
) {
    private val appContext = context.applicationContext
    private val preferences = context.getSharedPreferences("fokus_finance_settings", Context.MODE_PRIVATE)
    private val rateCache = context.getSharedPreferences("fokus_exchange_rates", Context.MODE_PRIVATE)
    private val rateClient = ExchangeRateClient()
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<FinanceSettings> = _settings

    fun incomes(period: BudgetPeriod): Flow<List<IncomeEntity>> = dao.observeIncomes(period.startAt, period.endAt)
    fun expenses(period: BudgetPeriod): Flow<List<ExpenseEntity>> = dao.observeExpenses(period.startAt, period.endAt)
    fun allocations(period: BudgetPeriod): Flow<List<AllocationEntity>> = dao.observeAllocations(period.key)
    val historyExpenses: Flow<List<ExpenseEntity>> = dao.observeAllExpenses()

    suspend fun addIncome(source: String, amountMinor: Long, currency: String, receivedAt: Long) {
        val settings = _settings.value
        val rate = resolveRate(currency, settings.baseCurrency)
        dao.insertIncome(
            IncomeEntity(
                source = source,
                amountMinor = amountMinor,
                currency = currency,
                baseAmountMinor = convertToBase(amountMinor, rate),
                exchangeRate = rate,
                receivedAt = receivedAt,
            ),
        )
        WidgetUpdater.update(appContext)
    }

    suspend fun addExpense(name: String, amountMinor: Long, currency: String, quadrant: FinanceQuadrant, occurredAt: Long) {
        val settings = _settings.value
        val rate = resolveRate(currency, settings.baseCurrency)
        dao.insertExpense(
            ExpenseEntity(
                name = name,
                amountMinor = amountMinor,
                currency = currency,
                baseAmountMinor = convertToBase(amountMinor, rate),
                exchangeRate = rate,
                quadrantIndex = quadrant.index,
                occurredAt = occurredAt,
            ),
        )
        WidgetUpdater.update(appContext)
    }

    suspend fun deleteIncome(id: Long) {
        dao.deleteIncome(id)
        WidgetUpdater.update(appContext)
    }

    suspend fun deleteExpense(id: Long) {
        dao.deleteExpense(id)
        WidgetUpdater.update(appContext)
    }

    suspend fun savePlan(period: BudgetPeriod, plan: Map<FinanceQuadrant, Long>) {
        val settings = _settings.value
        dao.upsertAllocations(
            FinanceQuadrant.entries.map { quadrant ->
                AllocationEntity(
                    periodKey = period.key,
                    quadrantIndex = quadrant.index,
                    plannedMinor = (plan[quadrant] ?: 0L).coerceAtLeast(0L),
                    currency = settings.baseCurrency,
                )
            },
        )
        WidgetUpdater.update(appContext)
    }

    suspend fun transfer(period: BudgetPeriod, from: FinanceQuadrant, to: FinanceQuadrant, amountMinor: Long) {
        if (from == to || amountMinor <= 0L) return
        val current = dao.getAllocations(period.key).associateBy { it.quadrantIndex }.toMutableMap()
        val fromCurrent = current[from.index]?.plannedMinor ?: 0L
        if (fromCurrent < amountMinor) return
        val now = System.currentTimeMillis()
        val currency = _settings.value.baseCurrency
        val updated = listOf(
            AllocationEntity(period.key, from.index, fromCurrent - amountMinor, currency, now),
            AllocationEntity(period.key, to.index, (current[to.index]?.plannedMinor ?: 0L) + amountMinor, currency, now),
        )
        dao.upsertAllocations(updated)
        dao.insertTransfer(
            TransferEntity(
                periodKey = period.key,
                fromQuadrant = from.index,
                toQuadrant = to.index,
                amountMinor = amountMinor,
                currency = currency,
            ),
        )
        WidgetUpdater.update(appContext)
    }

    fun updateSettings(baseCurrency: String, budgetStartDay: Int) {
        val updated = _settings.value.copy(
            baseCurrency = baseCurrency,
            budgetStartDay = budgetStartDay.coerceIn(1, 28),
        )
        saveSettings(updated)
        WidgetUpdater.update(appContext)
    }

    fun markSetupCompleted() {
        saveSettings(_settings.value.copy(setupCompleted = true))
        WidgetUpdater.update(appContext)
    }

    suspend fun resolveRate(from: String, to: String): Double? {
        if (from == to) return 1.0
        val key = "${from}_$to"
        val online = rateClient.fetchRate(from, to)
        if (online != null) {
            rateCache.edit().putLong(key, java.lang.Double.doubleToRawLongBits(online)).apply()
            return online
        }
        return rateCache.getLong(key, Long.MIN_VALUE)
            .takeIf { it != Long.MIN_VALUE }
            ?.let { java.lang.Double.longBitsToDouble(it) }
    }

    private fun loadSettings(): FinanceSettings = FinanceSettings(
        baseCurrency = preferences.getString("base_currency", "PLN") ?: "PLN",
        budgetStartDay = preferences.getInt("budget_start_day", 1),
        setupCompleted = preferences.getBoolean("setup_completed", false),
    )

    private fun saveSettings(value: FinanceSettings) {
        preferences.edit()
            .putString("base_currency", value.baseCurrency)
            .putInt("budget_start_day", value.budgetStartDay)
            .putBoolean("setup_completed", value.setupCompleted)
            .apply()
        _settings.value = value
    }
}
