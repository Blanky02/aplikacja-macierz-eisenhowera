package pl.fokus.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.fokus.app.data.BudgetPeriod
import pl.fokus.app.data.BudgetSnapshot
import pl.fokus.app.data.FinanceQuadrant
import pl.fokus.app.data.FinanceRepository
import pl.fokus.app.data.FinanceSettings
import pl.fokus.app.data.currentBudgetPeriod
import pl.fokus.app.data.suggestedPlan as buildSuggestedPlan

class MainViewModel(private val repository: FinanceRepository) : ViewModel() {
    val settings: StateFlow<FinanceSettings> = repository.settings

    val period: StateFlow<BudgetPeriod> = settings
        .map { currentBudgetPeriod(it.budgetStartDay) }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            currentBudgetPeriod(settings.value.budgetStartDay),
        )

    private val currentIncomes = period.flatMapLatest(repository::incomes)
    private val currentExpenses = period.flatMapLatest(repository::expenses)
    private val currentAllocations = period.flatMapLatest(repository::allocations)

    val historyExpenses = repository.historyExpenses.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val snapshot: StateFlow<BudgetSnapshot> = combine(
        period,
        settings,
        currentIncomes,
        currentExpenses,
        currentAllocations,
    ) { currentPeriod, currentSettings, incomes, expenses, allocations ->
        BudgetSnapshot(
            period = currentPeriod,
            baseCurrency = currentSettings.baseCurrency,
            incomes = incomes,
            expenses = expenses,
            allocations = allocations,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        BudgetSnapshot(
            period = currentBudgetPeriod(settings.value.budgetStartDay),
            baseCurrency = settings.value.baseCurrency,
            incomes = emptyList(),
            expenses = emptyList(),
            allocations = emptyList(),
        ),
    )

    fun addIncome(source: String, amountMinor: Long, currency: String, receivedAt: Long) = viewModelScope.launch {
        repository.addIncome(source, amountMinor, currency, receivedAt)
    }

    fun addExpense(name: String, amountMinor: Long, currency: String, quadrant: FinanceQuadrant, occurredAt: Long) = viewModelScope.launch {
        repository.addExpense(name, amountMinor, currency, quadrant, occurredAt)
    }

    fun deleteIncome(id: Long) = viewModelScope.launch { repository.deleteIncome(id) }
    fun deleteExpense(id: Long) = viewModelScope.launch { repository.deleteExpense(id) }

    fun savePlan(plan: Map<FinanceQuadrant, Long>) = viewModelScope.launch {
        repository.savePlan(currentBudgetPeriod(settings.value.budgetStartDay), plan)
        repository.markSetupCompleted()
    }

    fun transfer(from: FinanceQuadrant, to: FinanceQuadrant, amountMinor: Long) = viewModelScope.launch {
        repository.transfer(period.value, from, to, amountMinor)
    }

    fun suggestedPlan(): Map<FinanceQuadrant, Long> =
        buildSuggestedPlan(snapshot.value.totalIncomeMinor, historyExpenses.value)

    fun updateSettings(baseCurrency: String, budgetStartDay: Int) {
        repository.updateSettings(baseCurrency, budgetStartDay)
    }

    fun completeSetup() {
        repository.markSetupCompleted()
    }

    class Factory(private val repository: FinanceRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) return MainViewModel(repository) as T
            throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
