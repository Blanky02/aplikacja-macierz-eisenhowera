package pl.fokus.app.ui

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.fokus.app.MainViewModel
import pl.fokus.app.data.AllocationEntity
import pl.fokus.app.data.BudgetSnapshot
import pl.fokus.app.data.FinanceQuadrant
import pl.fokus.app.data.FinanceSettings
import pl.fokus.app.data.IncomeEntity
import pl.fokus.app.data.ExpenseEntity
import pl.fokus.app.data.formatMoney
import pl.fokus.app.data.parseMinorUnits
import pl.fokus.app.data.suggestQuadrant
import pl.fokus.app.data.supportedCurrencies
import java.math.BigDecimal
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class AppTab(val label: String) {
    BUDGET("Budżet"),
    EXPENSES("Wydatki"),
    HISTORY("Historia"),
}

private enum class BottomSheet {
    NONE, QUICK_ADD, INCOME, EXPENSE, PLAN, TRANSFER, SETTINGS, SETUP,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FokusApp(viewModel: MainViewModel) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.BUDGET) }
    var sheet by rememberSaveable { mutableStateOf(BottomSheet.NONE) }
    var showQuickAdd by rememberSaveable { mutableStateOf(false) }
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    LaunchedEffect(settings.setupCompleted) {
        if (!settings.setupCompleted) sheet = BottomSheet.SETUP
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            when (selectedTab) {
                                AppTab.BUDGET -> "Fokus Budżet"
                                AppTab.EXPENSES -> "Wydatki"
                                AppTab.HISTORY -> "Historia"
                            },
                            fontWeight = FontWeight.Bold,
                        )
                        if (selectedTab == AppTab.BUDGET) {
                            Text(
                                periodLabel(snapshot),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { sheet = BottomSheet.SETTINGS }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Ustawienia")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                when (tab) {
                                    AppTab.BUDGET -> Icons.Outlined.Home
                                    AppTab.EXPENSES -> Icons.Outlined.ReceiptLong
                                    AppTab.HISTORY -> Icons.Outlined.History
                                },
                                contentDescription = tab.label,
                            )
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { sheet = BottomSheet.QUICK_ADD },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Dodaj wpływ lub wydatek")
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (selectedTab) {
                AppTab.BUDGET -> BudgetScreen(
                    snapshot = snapshot,
                    settings = settings,
                    onPlan = { sheet = BottomSheet.PLAN },
                    onIncome = { sheet = BottomSheet.INCOME },
                    onExpense = { sheet = BottomSheet.EXPENSE },
                    onTransfer = { sheet = BottomSheet.TRANSFER },
                    onSetup = { sheet = BottomSheet.SETUP },
                )

                AppTab.EXPENSES -> ExpensesScreen(
                    snapshot = snapshot,
                    onAdd = { sheet = BottomSheet.EXPENSE },
                    onDelete = viewModel::deleteExpense,
                )

                AppTab.HISTORY -> HistoryScreen(
                    snapshot = snapshot,
                    onDeleteIncome = viewModel::deleteIncome,
                    onDeleteExpense = viewModel::deleteExpense,
                )
            }
        }
    }

    when (sheet) {
        BottomSheet.QUICK_ADD -> QuickAddSheet(
            onDismiss = { sheet = BottomSheet.NONE },
            onIncome = { sheet = BottomSheet.INCOME },
            onExpense = { sheet = BottomSheet.EXPENSE },
            onPlan = { sheet = BottomSheet.PLAN },
        )

        BottomSheet.INCOME -> IncomeSheet(
            baseCurrency = settings.baseCurrency,
            onDismiss = { sheet = BottomSheet.NONE },
            onSave = { source, amount, currency, date ->
                viewModel.addIncome(source, amount, currency, date)
                sheet = BottomSheet.NONE
            },
        )

        BottomSheet.EXPENSE -> ExpenseSheet(
            baseCurrency = settings.baseCurrency,
            onDismiss = { sheet = BottomSheet.NONE },
            onSave = { name, amount, currency, quadrant, date ->
                viewModel.addExpense(name, amount, currency, quadrant, date)
                sheet = BottomSheet.NONE
            },
        )

        BottomSheet.PLAN -> PlanSheet(
            snapshot = snapshot,
            suggested = viewModel.suggestedPlan(),
            isSetup = false,
            onDismiss = { sheet = BottomSheet.NONE },
            onSave = { plan ->
                viewModel.savePlan(plan)
                sheet = BottomSheet.NONE
            },
        )

        BottomSheet.TRANSFER -> TransferSheet(
            snapshot = snapshot,
            onDismiss = { sheet = BottomSheet.NONE },
            onTransfer = { from, to, amount ->
                viewModel.transfer(from, to, amount)
                sheet = BottomSheet.NONE
            },
        )

        BottomSheet.SETTINGS -> SettingsSheet(
            settings = settings,
            hasFinancialData = snapshot.incomes.isNotEmpty() || snapshot.expenses.isNotEmpty() || snapshot.allocations.isNotEmpty(),
            onDismiss = { sheet = BottomSheet.NONE },
            onSave = { currency, startDay ->
                viewModel.updateSettings(currency, startDay)
                sheet = BottomSheet.NONE
            },
        )

        BottomSheet.SETUP -> SetupSheet(
            settings = settings,
            onSkip = {
                viewModel.completeSetup()
                sheet = BottomSheet.NONE
            },
            onSave = { currency, startDay, incomeSource, income, plan ->
                viewModel.updateSettings(currency, startDay)
                if (income != null && income > 0L) {
                    viewModel.addIncome(incomeSource.ifBlank { "Dochód" }, income, currency, System.currentTimeMillis())
                }
                viewModel.savePlan(plan)
                sheet = BottomSheet.NONE
            },
        )

        BottomSheet.NONE -> Unit
    }
}

@Composable
private fun BudgetScreen(
    snapshot: BudgetSnapshot,
    settings: FinanceSettings,
    onPlan: () -> Unit,
    onIncome: () -> Unit,
    onExpense: () -> Unit,
    onTransfer: () -> Unit,
    onSetup: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        SummaryCard(snapshot, settings)
        Spacer(Modifier.height(14.dp))
        if (snapshot.allocations.isEmpty()) {
            SetupBanner(onSetup = onSetup, onIncome = onIncome)
            Spacer(Modifier.height(16.dp))
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Macierz wydatków", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Każdy wydatek ma swoje miejsce.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onPlan) {
                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("Plan")
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FinanceQuadrantCard(
                quadrant = FinanceQuadrant.IMPORTANT_URGENT,
                snapshot = snapshot,
                modifier = Modifier.weight(1f),
            )
            FinanceQuadrantCard(
                quadrant = FinanceQuadrant.IMPORTANT_NOT_URGENT,
                snapshot = snapshot,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FinanceQuadrantCard(
                quadrant = FinanceQuadrant.NOT_IMPORTANT_URGENT,
                snapshot = snapshot,
                modifier = Modifier.weight(1f),
            )
            FinanceQuadrantCard(
                quadrant = FinanceQuadrant.NOT_IMPORTANT_NOT_URGENT,
                snapshot = snapshot,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onIncome, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Wpływ")
            }
            OutlinedButton(onClick = onExpense, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Wydatek")
            }
        }
        OutlinedButton(onClick = onTransfer, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Icon(Icons.Outlined.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text("Przenieś środki między ćwiartkami")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SummaryCard(snapshot: BudgetSnapshot, settings: FinanceSettings) {
    val remainingColor = if (snapshot.remainingMinor < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Pozostało w tym okresie",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        formatMoney(snapshot.remainingMinor, snapshot.baseCurrency),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = remainingColor,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, modifier = Modifier.padding(14.dp).size(28.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryPill("wpływy", formatMoney(snapshot.totalIncomeMinor, settings.baseCurrency))
                SummaryPill("wydatki", formatMoney(snapshot.totalSpentMinor, settings.baseCurrency))
            }
            if (snapshot.unallocatedMinor != 0L) {
                Text(
                    if (snapshot.unallocatedMinor > 0) {
                        "Nieprzydzielone: ${formatMoney(snapshot.unallocatedMinor, settings.baseCurrency)}"
                    } else {
                        "Plan przekracza wpływy o ${formatMoney(-snapshot.unallocatedMinor, settings.baseCurrency)}"
                    },
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (snapshot.unallocatedMinor > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun SummaryPill(label: String, value: String) {
    Surface(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.09f), shape = RoundedCornerShape(50)) {
        Text(
            "$label  $value",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun SetupBanner(onSetup: () -> Unit, onIncome: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text("Ustaw swój pierwszy plan", fontWeight = FontWeight.Bold)
            }
            Text(
                "Kreator pomoże rozdzielić wpływ bez narzucania gotowych proporcji.",
                modifier = Modifier.padding(top = 7.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSetup) { Text("Uruchom kreator") }
                OutlinedButton(onClick = onIncome) { Text("Dodaj wpływ") }
            }
        }
    }
}

private data class QuadrantColors(val background: Color, val accent: Color, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
private fun quadrantColors(quadrant: FinanceQuadrant): QuadrantColors {
    val scheme = MaterialTheme.colorScheme
    return when (quadrant) {
        FinanceQuadrant.IMPORTANT_URGENT -> QuadrantColors(scheme.errorContainer.copy(alpha = 0.75f), scheme.error, Icons.Outlined.WarningAmber)
        FinanceQuadrant.IMPORTANT_NOT_URGENT -> QuadrantColors(scheme.tertiaryContainer.copy(alpha = 0.75f), scheme.onTertiaryContainer, Icons.Outlined.Savings)
        FinanceQuadrant.NOT_IMPORTANT_URGENT -> QuadrantColors(Color(0xFFDCEFF4), Color(0xFF27657A), Icons.Outlined.Payments)
        FinanceQuadrant.NOT_IMPORTANT_NOT_URGENT -> QuadrantColors(scheme.surfaceVariant.copy(alpha = 0.75f), scheme.onSurfaceVariant, Icons.Outlined.ReceiptLong)
    }
}

@Composable
private fun FinanceQuadrantCard(
    quadrant: FinanceQuadrant,
    snapshot: BudgetSnapshot,
    modifier: Modifier,
) {
    val colors = quadrantColors(quadrant)
    val planned = snapshot.planned(quadrant)
    val spent = snapshot.spent(quadrant)
    val remaining = planned - spent
    val progress = if (planned > 0) (spent.toFloat() / planned.toFloat()).coerceIn(0f, 1.2f) else 0f
    Card(
        modifier = modifier.heightIn(min = 184.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.background),
        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.14f)),
    ) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = colors.accent.copy(alpha = 0.12f), shape = RoundedCornerShape(9.dp)) {
                    Icon(colors.icon, contentDescription = null, modifier = Modifier.padding(7.dp).size(17.dp), tint = colors.accent)
                }
                Spacer(Modifier.width(7.dp))
                Text(
                    quadrant.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                quadrant.subtitle,
                modifier = Modifier.padding(top = 5.dp),
                style = MaterialTheme.typography.labelSmall,
                color = colors.accent.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(13.dp))
            Text(
                formatMoney(remaining, snapshot.baseCurrency),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (remaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "z ${formatMoney(planned, snapshot.baseCurrency)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(9.dp))
            androidx.compose.material3.LinearProgressIndicator(
                progress = progress.coerceAtMost(1f),
                modifier = Modifier.fillMaxWidth(),
                color = if (spent > planned && planned > 0) MaterialTheme.colorScheme.error else colors.accent,
                trackColor = colors.accent.copy(alpha = 0.15f),
            )
            Text(
                "wydano ${formatMoney(spent, snapshot.baseCurrency)}",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = colors.accent.copy(alpha = 0.82f),
            )
        }
    }
}

@Composable
private fun ExpensesScreen(
    snapshot: BudgetSnapshot,
    onAdd: () -> Unit,
    onDelete: (Long) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("W tym okresie", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatMoney(snapshot.totalSpentMinor, snapshot.baseCurrency), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onAdd) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text("Wydatek")
            }
        }
        if (snapshot.expenses.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.ReceiptLong,
                title = "Brak wydatków",
                body = "Dodaj pierwszy wydatek, a aplikacja zaproponuje mu ćwiartkę.",
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(snapshot.expenses, key = { it.id }) { expense ->
                    ExpenseRow(expense, snapshot.baseCurrency, onDelete = { onDelete(expense.id) })
                }
            }
        }
    }
}

@Composable
private fun ExpenseRow(expense: ExpenseEntity, baseCurrency: String, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val quadrant = FinanceQuadrant.fromIndex(expense.quadrantIndex)
    val colors = quadrantColors(quadrant)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = colors.background, shape = RoundedCornerShape(11.dp)) {
                Icon(colors.icon, contentDescription = null, modifier = Modifier.padding(9.dp).size(19.dp), tint = colors.accent)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(expense.name, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${quadrant.title} · ${formatDate(expense.occurredAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (expense.currency != baseCurrency) {
                    Text(
                        "${formatMoney(expense.amountMinor, expense.currency)} · kurs zapisany lokalnie",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (expense.baseAmountMinor != null) formatMoney(expense.baseAmountMinor, baseCurrency) else "—",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error.takeIf { expense.baseAmountMinor == null } ?: MaterialTheme.colorScheme.onSurface,
                )
                Box {
                    IconButton(onClick = { expanded = true }, modifier = Modifier.size(27.dp)) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Opcje", modifier = Modifier.size(17.dp))
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Usuń") },
                            leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
                            onClick = { expanded = false; onDelete() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(
    snapshot: BudgetSnapshot,
    onDeleteIncome: (Long) -> Unit,
    onDeleteExpense: (Long) -> Unit,
) {
    val events = buildList {
        snapshot.incomes.forEach { add(HistoryEvent.Income(it)) }
        snapshot.expenses.forEach { add(HistoryEvent.Expense(it)) }
    }.sortedByDescending { it.date }
    if (events.isEmpty()) {
        EmptyState(Icons.Outlined.History, "Historia jest pusta", "Wpływy i wydatki z bieżącego okresu pojawią się tutaj.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text("Wpływy i wydatki", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(events, key = { it.key }) { event ->
            when (event) {
                is HistoryEvent.Income -> IncomeRow(event.value, snapshot.baseCurrency) { onDeleteIncome(event.value.id) }
                is HistoryEvent.Expense -> ExpenseRow(event.value, snapshot.baseCurrency) { onDeleteExpense(event.value.id) }
            }
        }
    }
}

private sealed class HistoryEvent {
    abstract val date: Long
    abstract val key: String
    data class Income(val value: IncomeEntity) : HistoryEvent() {
        override val date: Long = value.receivedAt
        override val key: String = "income-${value.id}"
    }
    data class Expense(val value: ExpenseEntity) : HistoryEvent() {
        override val date: Long = value.occurredAt
        override val key: String = "expense-${value.id}"
    }
}

@Composable
private fun IncomeRow(income: IncomeEntity, baseCurrency: String, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(11.dp)) {
                Icon(Icons.Outlined.ArrowUpward, contentDescription = null, modifier = Modifier.padding(9.dp).size(19.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(income.source, fontWeight = FontWeight.Medium)
                Text(formatDate(income.receivedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    income.baseAmountMinor?.let { formatMoney(it, baseCurrency, signed = true) } ?: "—",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Box {
                    IconButton(onClick = { expanded = true }, modifier = Modifier.size(27.dp)) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Opcje", modifier = Modifier.size(17.dp))
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Usuń") },
                            leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
                            onClick = { expanded = false; onDelete() },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddSheet(
    onDismiss: () -> Unit,
    onIncome: () -> Unit,
    onExpense: () -> Unit,
    onPlan: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp)) {
            Text("Co chcesz dodać?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Wszystko zostaje zapisane tylko na tym urządzeniu.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(15.dp))
            QuickActionCard(Icons.Outlined.ArrowDownward, "Wydatek", "Aplikacja zaproponuje ćwiartkę", onExpense)
            QuickActionCard(Icons.Outlined.ArrowUpward, "Wpływ", "Pensja, premia lub inne źródło", onIncome)
            QuickActionCard(Icons.Outlined.Tune, "Plan budżetu", "Zmień kwoty w macierzy", onPlan)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun QuickActionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(25.dp))
            Spacer(Modifier.width(13.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncomeSheet(
    baseCurrency: String,
    onDismiss: () -> Unit,
    onSave: (String, Long, String, Long) -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var source by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(baseCurrency) }
    var date by remember { mutableStateOf(System.currentTimeMillis()) }
    var attempted by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
            SheetHeader("Dodaj wpływ", "Każdy wpływ zwiększa pulę do rozdzielenia.", onDismiss)
            OutlinedTextField(
                value = source,
                onValueChange = { source = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Źródło wpływu") },
                placeholder = { Text("np. pensja, premia, zlecenie") },
                singleLine = true,
                isError = attempted && source.isBlank(),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Kwota") },
                    placeholder = { Text("0,00") },
                    singleLine = true,
                    isError = attempted && parseMinorUnits(amount) == null,
                )
                ChoiceButton("Waluta", currency, supportedCurrencies, modifier = Modifier.weight(0.72f)) { currency = it }
            }
            Spacer(Modifier.height(10.dp))
            DateButton("Data wpływu", date, context) { date = it }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    attempted = true
                    val parsed = parseMinorUnits(amount)
                    if (source.isNotBlank() && parsed != null) onSave(source.trim(), parsed, currency, date)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null)
                Spacer(Modifier.width(7.dp))
                Text("Zapisz wpływ")
            }
            Spacer(Modifier.height(22.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseSheet(
    baseCurrency: String,
    onDismiss: () -> Unit,
    onSave: (String, Long, String, FinanceQuadrant, Long) -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(baseCurrency) }
    var quadrant by remember { mutableStateOf(FinanceQuadrant.NOT_IMPORTANT_NOT_URGENT) }
    var date by remember { mutableStateOf(System.currentTimeMillis()) }
    var attempted by remember { mutableStateOf(false) }
    val suggestion = suggestQuadrant(name)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
            SheetHeader("Dodaj wydatek", "Wybierz ćwiartkę po sprawdzeniu propozycji.", onDismiss)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nazwa wydatku") },
                placeholder = { Text("np. czynsz, zakupy, kino") },
                singleLine = true,
                isError = attempted && name.isBlank(),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Kwota") },
                    placeholder = { Text("0,00") },
                    singleLine = true,
                    isError = attempted && parseMinorUnits(amount) == null,
                )
                ChoiceButton("Waluta", currency, supportedCurrencies, modifier = Modifier.weight(0.72f)) { currency = it }
            }
            Spacer(Modifier.height(11.dp))
            if (name.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Propozycja aplikacji", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(suggestion.title, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { quadrant = suggestion }) { Text("Użyj") }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            ChoiceButton(
                label = "Wybrana ćwiartka",
                value = quadrant.title,
                options = FinanceQuadrant.entries.toList(),
                modifier = Modifier.fillMaxWidth(),
                optionLabel = { it.title },
            ) { quadrant = it }
            Spacer(Modifier.height(10.dp))
            DateButton("Data wydatku", date, context) { date = it }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    attempted = true
                    val parsed = parseMinorUnits(amount)
                    if (name.isNotBlank() && parsed != null) onSave(name.trim(), parsed, currency, quadrant, date)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null)
                Spacer(Modifier.width(7.dp))
                Text("Zapisz wydatek")
            }
            Spacer(Modifier.height(22.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanSheet(
    snapshot: BudgetSnapshot,
    suggested: Map<FinanceQuadrant, Long>,
    isSetup: Boolean,
    onDismiss: () -> Unit,
    onSave: (Map<FinanceQuadrant, Long>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var values by remember(snapshot.period.key, isSetup) {
        mutableStateOf(
            FinanceQuadrant.entries.associateWith { quadrant ->
                snapshot.allocations.firstOrNull { it.quadrantIndex == quadrant.index }?.plannedMinor?.let(::minorToInput) ?: ""
            },
        )
    }
    var error by remember { mutableStateOf<String?>(null) }
    val sum = values.values.mapNotNull(::parseMinorUnits).sum()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
            SheetHeader(
                if (isSetup) "Pierwszy plan" else "Plan budżetu",
                "Wpisz kwoty, które chcesz przeznaczyć na każdy obszar.",
                onDismiss,
            )
            if (!isSetup && snapshot.totalIncomeMinor > 0) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Propozycja z historii", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            Text("To sugestia — zatwierdź ją dopiero, gdy Ci odpowiada.", style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = {
                            values = suggested.mapValues { (_, amount) -> minorToInput(amount) }
                        }) { Text("Wstaw") }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            FinanceQuadrant.entries.forEach { quadrant ->
                val colors = quadrantColors(quadrant)
                OutlinedTextField(
                    value = values[quadrant].orEmpty(),
                    onValueChange = { value -> values = values.toMutableMap().apply { put(quadrant, value) } },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    label = { Text(quadrant.title) },
                    supportingText = { Text(quadrant.subtitle) },
                    leadingIcon = { Icon(colors.icon, contentDescription = null, tint = colors.accent) },
                    singleLine = true,
                )
            }
            Text(
                "Suma planu: ${formatMoney(sum, snapshot.baseCurrency)} · wpływy: ${formatMoney(snapshot.totalIncomeMinor, snapshot.baseCurrency)}",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = if (sum > snapshot.totalIncomeMinor && snapshot.totalIncomeMinor > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            error?.let {
                Text(it, modifier = Modifier.padding(top = 5.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(15.dp))
            Button(
                onClick = {
                    val parsed = values.mapValues { (_, value) -> parseMinorUnits(value) ?: 0L }
                    if (snapshot.totalIncomeMinor > 0 && parsed.values.sum() > snapshot.totalIncomeMinor) {
                        error = "Plan nie może przekraczać wpływów. Zmniejsz jedną z kwot."
                    } else {
                        onSave(parsed)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null)
                Spacer(Modifier.width(7.dp))
                Text("Zapisz plan")
            }
            Spacer(Modifier.height(22.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetupSheet(
    settings: FinanceSettings,
    onSkip: () -> Unit,
    onSave: (String, Int, String, Long?, Map<FinanceQuadrant, Long>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currency by remember { mutableStateOf(settings.baseCurrency) }
    var startDay by remember { mutableStateOf(settings.budgetStartDay.toString()) }
    var incomeSource by remember { mutableStateOf("Pensja") }
    var income by remember { mutableStateOf("") }
    var planValues by remember { mutableStateOf(FinanceQuadrant.entries.associateWith { "" }) }
    var error by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onSkip, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
            SheetHeader("Ustaw swój budżet", "Kilka prostych pytań i masz punkt wyjścia — bez narzuconych proporcji.", onSkip)
            Text("Waluta bazowa", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            ChoiceButton("Waluta", currency, supportedCurrencies, modifier = Modifier.fillMaxWidth()) { currency = it }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = startDay,
                onValueChange = { startDay = it.filter(Char::isDigit).take(2) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Dzień rozpoczęcia okresu") },
                supportingText = { Text("Np. 1 dla miesiąca kalendarzowego albo 10 po wypłacie") },
                singleLine = true,
            )
            Spacer(Modifier.height(10.dp))
            Text("Pierwszy wpływ (opcjonalnie)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = incomeSource, onValueChange = { incomeSource = it }, modifier = Modifier.weight(1f), label = { Text("Źródło") }, singleLine = true)
                OutlinedTextField(value = income, onValueChange = { income = it }, modifier = Modifier.weight(0.72f), label = { Text("Kwota") }, placeholder = { Text("0,00") }, singleLine = true)
            }
            Spacer(Modifier.height(14.dp))
            Text("Jak chcesz rozdysponować wpływ?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("Możesz zostawić puste pola i uzupełnić plan później.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(5.dp))
            FinanceQuadrant.entries.forEach { quadrant ->
                OutlinedTextField(
                    value = planValues[quadrant].orEmpty(),
                    onValueChange = { value -> planValues = planValues.toMutableMap().apply { put(quadrant, value) } },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    label = { Text(quadrant.title) },
                    singleLine = true,
                )
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(15.dp))
            Button(
                onClick = {
                    val parsedIncome = parseMinorUnits(income)
                    val plan = planValues.mapValues { (_, value) -> parseMinorUnits(value) ?: 0L }
                    val parsedDay = startDay.toIntOrNull()?.coerceIn(1, 28) ?: 1
                    if (parsedIncome != null && plan.values.sum() > parsedIncome) {
                        error = "Podział nie może przekraczać pierwszego wpływu."
                    } else {
                        onSave(currency, parsedDay, incomeSource, parsedIncome, plan)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null)
                Spacer(Modifier.width(7.dp))
                Text("Zapisz i zacznij")
            }
            TextButton(onClick = onSkip, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Pomiń na razie") }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransferSheet(
    snapshot: BudgetSnapshot,
    onDismiss: () -> Unit,
    onTransfer: (FinanceQuadrant, FinanceQuadrant, Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var from by remember { mutableStateOf(FinanceQuadrant.IMPORTANT_URGENT) }
    var to by remember { mutableStateOf(FinanceQuadrant.IMPORTANT_NOT_URGENT) }
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp)) {
            SheetHeader("Przenieś środki", "Zmiana zostanie zapisana w historii budżetu.", onDismiss)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(Modifier.width(8.dp))
                    Text("To zmieni limity, ale nie zmieni już zapisanych wydatków.", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(11.dp))
            ChoiceButton("Z ćwiartki", from.title, FinanceQuadrant.entries.toList(), modifier = Modifier.fillMaxWidth(), optionLabel = { it.title }) { from = it }
            Spacer(Modifier.height(9.dp))
            ChoiceButton("Do ćwiartki", to.title, FinanceQuadrant.entries.toList(), modifier = Modifier.fillMaxWidth(), optionLabel = { it.title }) { to = it }
            Spacer(Modifier.height(9.dp))
            OutlinedTextField(value = amount, onValueChange = { amount = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Kwota") }, singleLine = true)
            error?.let { Text(it, modifier = Modifier.padding(top = 5.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(15.dp))
            Button(
                onClick = {
                    val parsed = parseMinorUnits(amount)
                    when {
                        from == to -> error = "Wybierz dwie różne ćwiartki."
                        parsed == null -> error = "Podaj poprawną kwotę."
                        parsed > snapshot.planned(from) -> error = "Nie możesz przenieść więcej niż zaplanowano w tej ćwiartce."
                        else -> onTransfer(from, to, parsed)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Przenieś środki") }
            Spacer(Modifier.height(22.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    settings: FinanceSettings,
    hasFinancialData: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currency by remember { mutableStateOf(settings.baseCurrency) }
    var startDay by remember { mutableStateOf(settings.budgetStartDay.toString()) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp)) {
            SheetHeader("Ustawienia budżetu", "Dane finansowe pozostają na tym urządzeniu.", onDismiss)
            if (hasFinancialData) {
                OutlinedTextField(
                    value = currency,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Waluta bazowa") },
                    supportingText = { Text("Nie zmieniamy jej po zapisaniu danych, żeby nie przeliczyć historii błędnie.") },
                    readOnly = true,
                    singleLine = true,
                )
            } else {
                ChoiceButton("Waluta bazowa", currency, supportedCurrencies, modifier = Modifier.fillMaxWidth()) { currency = it }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = startDay,
                onValueChange = { startDay = it.filter(Char::isDigit).take(2) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Dzień rozpoczęcia okresu") },
                supportingText = { Text("Zakres 1–28, żeby każdy miesiąc był jednoznaczny.") },
                singleLine = true,
            )
            Spacer(Modifier.height(15.dp))
            Button(
                onClick = { onSave(currency, startDay.toIntOrNull()?.coerceIn(1, 28) ?: 1) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Zapisz ustawienia") }
            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
private fun SheetHeader(title: String, subtitle: String, onDismiss: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = "Zamknij") }
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun <T> ChoiceButton(
    label: String,
    value: String,
    options: List<T>,
    modifier: Modifier = Modifier,
    optionLabel: (T) -> String = { it.toString() },
    onSelect: (T) -> Unit,
) {
    var expanded by remember(label, value) { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp),
        ) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Outlined.Tune, contentDescription = "Wybierz", modifier = Modifier.size(17.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = { expanded = false; onSelect(option) },
                )
            }
        }
    }
}

@Composable
private fun DateButton(label: String, timestamp: Long, context: Context, onDateChanged: (Long) -> Unit) {
    OutlinedButton(onClick = { showDatePicker(context, timestamp, onDateChanged) }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(7.dp))
        Text("$label: ${formatDate(timestamp)}")
    }
}

private fun showDatePicker(context: Context, current: Long, onDateSelected: (Long) -> Unit) {
    val calendar = Calendar.getInstance().apply { timeInMillis = current }
    DatePickerDialog(
        context,
        { _, year, month, day ->
            val selected = Calendar.getInstance().apply {
                timeInMillis = current
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
            }
            onDateSelected(selected.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH),
    ).show()
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 35.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(24.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(18.dp).size(34.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(17.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

private fun periodLabel(snapshot: BudgetSnapshot): String =
    "${formatDate(snapshot.period.startAt)} – ${formatDate(snapshot.period.endAt)}"

private fun formatDate(timestamp: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM, Locale("pl", "PL")).format(Date(timestamp))

private fun minorToInput(minor: Long): String =
    BigDecimal(minor).movePointLeft(2).stripTrailingZeros().toPlainString()
