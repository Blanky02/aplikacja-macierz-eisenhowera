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
import pl.fokus.app.data.FinanceDatabase
import pl.fokus.app.data.FinanceQuadrant
import pl.fokus.app.data.currentBudgetPeriod
import pl.fokus.app.data.formatMoney

object WidgetUpdater {
    fun update(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val database = FinanceDatabase.get(context)
            val settings = context.getSharedPreferences("fokus_finance_settings", Context.MODE_PRIVATE)
            val baseCurrency = settings.getString("base_currency", "PLN") ?: "PLN"
            val startDay = settings.getInt("budget_start_day", 1)
            val period = currentBudgetPeriod(startDay)
            val incomes = database.financeDao().observeIncomes(period.startAt, period.endAt).first()
            val expenses = database.financeDao().observeExpenses(period.startAt, period.endAt).first()
            val allocations = database.financeDao().observeAllocations(period.key).first().associateBy { it.quadrantIndex }
            val spentByQuadrant = expenses.groupingBy { it.quadrantIndex }
                .fold(0L) { total, expense -> total + (expense.baseAmountMinor ?: 0L) }
            val totalIncome = incomes.sumOf { it.baseAmountMinor ?: 0L }
            val totalSpent = expenses.sumOf { it.baseAmountMinor ?: 0L }
            update(
                context = context,
                baseCurrency = baseCurrency,
                totalIncome = totalIncome,
                totalSpent = totalSpent,
                allocations = allocations,
                spentByQuadrant = spentByQuadrant,
            )
        }
    }

    private fun update(
        context: Context,
        baseCurrency: String,
        totalIncome: Long,
        totalSpent: Long,
        allocations: Map<Int, pl.fokus.app.data.AllocationEntity>,
        spentByQuadrant: Map<Int, Long>,
    ) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, FokusWidgetReceiver::class.java)
        val ids = manager.getAppWidgetIds(component)
        if (ids.isEmpty()) return

        val views = RemoteViews(context.packageName, R.layout.widget_fokus).apply {
            setTextViewText(R.id.widget_subtitle, "Pozostało ${formatMoney(totalIncome - totalSpent, baseCurrency)}")
            FinanceQuadrant.entries.forEach { quadrant ->
                val planned = allocations[quadrant.index]?.plannedMinor ?: 0L
                val spent = spentByQuadrant[quadrant.index] ?: 0L
                val label = "${quadrant.title}: ${formatMoney(planned - spent, baseCurrency)}"
                setTextViewText(
                    when (quadrant) {
                        FinanceQuadrant.IMPORTANT_URGENT -> R.id.widget_q0
                        FinanceQuadrant.IMPORTANT_NOT_URGENT -> R.id.widget_q1
                        FinanceQuadrant.NOT_IMPORTANT_URGENT -> R.id.widget_q2
                        FinanceQuadrant.NOT_IMPORTANT_NOT_URGENT -> R.id.widget_q3
                    },
                    label,
                )
            }
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
}
