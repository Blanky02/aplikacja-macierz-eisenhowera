package pl.fokus.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.fokus.app.MainViewModel
import pl.fokus.app.data.Quadrant
import pl.fokus.app.data.Recurrence
import pl.fokus.app.data.TaskEntity
import pl.fokus.app.data.isUrgent
import pl.fokus.app.data.quadrantFor
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class AppTab(val label: String) {
    MATRIX("Macierz"),
    TODAY("Dziś"),
    ARCHIVE("Archiwum"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FokusApp(viewModel: MainViewModel) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.MATRIX) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var filterOpen by rememberSaveable { mutableStateOf(false) }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }

    val tasks by viewModel.activeTasks.collectAsStateWithLifecycle()
    val archivedTasks by viewModel.archivedTasks.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            FokusTopBar(
                tab = selectedTab,
                searchOpen = searchOpen,
                query = searchQuery,
                activeFilter = selectedFilter,
                onSearchClick = { searchOpen = !searchOpen },
                onQueryChange = viewModel::setSearchQuery,
                onFilterClick = { filterOpen = true },
                onCloseSearch = {
                    searchOpen = false
                    viewModel.setSearchQuery("")
                },
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
                                imageVector = when (tab) {
                                    AppTab.MATRIX -> Icons.Outlined.GridView
                                    AppTab.TODAY -> Icons.Outlined.Today
                                    AppTab.ARCHIVE -> Icons.Outlined.Archive
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
            if (selectedTab != AppTab.ARCHIVE) {
                FloatingActionButton(
                    onClick = {
                        editingTask = null
                        editorOpen = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Dodaj zadanie")
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (selectedTab) {
                AppTab.MATRIX -> MatrixScreen(
                    tasks = tasks,
                    onOpenTask = { editingTask = it; editorOpen = true },
                    onComplete = viewModel::toggleComplete,
                    onArchive = viewModel::archive,
                    onDelete = viewModel::delete,
                    onMove = viewModel::move,
                )

                AppTab.TODAY -> TodayScreen(
                    tasks = tasks,
                    onOpenTask = { editingTask = it; editorOpen = true },
                    onComplete = viewModel::toggleComplete,
                    onArchive = viewModel::archive,
                    onDelete = viewModel::delete,
                    onMove = viewModel::move,
                )

                AppTab.ARCHIVE -> ArchiveScreen(
                    tasks = archivedTasks,
                    onRestore = viewModel::restore,
                    onDelete = viewModel::delete,
                )
            }
        }
    }

    if (editorOpen) {
        TaskEditorSheet(
            initial = editingTask,
            onDismiss = { editorOpen = false },
            onSave = {
                viewModel.save(it)
                editorOpen = false
            },
        )
    }

    if (filterOpen) {
        FilterSheet(
            selected = selectedFilter,
            onSelect = {
                viewModel.setFilter(it)
                filterOpen = false
            },
            onDismiss = { filterOpen = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FokusTopBar(
    tab: AppTab,
    searchOpen: Boolean,
    query: String,
    activeFilter: MainViewModel.Filter,
    onSearchClick: () -> Unit,
    onQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    onCloseSearch: () -> Unit,
) {
    Column {
        androidx.compose.material3.CenterAlignedTopAppBar(
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = when (tab) {
                            AppTab.MATRIX -> "Dzień dobry 👋"
                            AppTab.TODAY -> "Plan na dziś"
                            AppTab.ARCHIVE -> "Twoja historia"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    if (tab == AppTab.MATRIX) {
                        Text(
                            "Małe kroki, duży spokój",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            actions = {
                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Outlined.Search, contentDescription = "Szukaj")
                }
                IconButton(onClick = onFilterClick) {
                    Icon(
                        Icons.Outlined.Tune,
                        contentDescription = "Filtruj",
                        tint = if (activeFilter != MainViewModel.Filter.ALL) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )
        if (searchOpen) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Szukaj zadań, tagów, projektów") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                )
                IconButton(onClick = onCloseSearch) {
                    Icon(Icons.Outlined.Close, contentDescription = "Zamknij wyszukiwanie")
                }
            }
        }
    }
}

@Composable
private fun MatrixScreen(
    tasks: List<TaskEntity>,
    onOpenTask: (TaskEntity) -> Unit,
    onComplete: (TaskEntity) -> Unit,
    onArchive: (TaskEntity) -> Unit,
    onDelete: (TaskEntity) -> Unit,
    onMove: (TaskEntity, Int?) -> Unit,
) {
    val now = System.currentTimeMillis()
    val urgentCount = tasks.count { isUrgent(it, now) }
    val datedCount = tasks.count { it.dueAt != null }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        OverviewCard(
            total = tasks.size,
            urgent = urgentCount,
            dated = datedCount,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Twoje priorytety",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Termin podpowiada pilność. Ważność ustawiasz Ty.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuadrantCard(
                quadrant = Quadrant.DO_NOW,
                tasks = tasks.filter { quadrantFor(it, now) == Quadrant.DO_NOW },
                modifier = Modifier.weight(1f),
                onOpenTask = onOpenTask,
                onComplete = onComplete,
                onArchive = onArchive,
                onDelete = onDelete,
                onMove = onMove,
            )
            QuadrantCard(
                quadrant = Quadrant.SCHEDULE,
                tasks = tasks.filter { quadrantFor(it, now) == Quadrant.SCHEDULE },
                modifier = Modifier.weight(1f),
                onOpenTask = onOpenTask,
                onComplete = onComplete,
                onArchive = onArchive,
                onDelete = onDelete,
                onMove = onMove,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuadrantCard(
                quadrant = Quadrant.DELEGATE,
                tasks = tasks.filter { quadrantFor(it, now) == Quadrant.DELEGATE },
                modifier = Modifier.weight(1f),
                onOpenTask = onOpenTask,
                onComplete = onComplete,
                onArchive = onArchive,
                onDelete = onDelete,
                onMove = onMove,
            )
            QuadrantCard(
                quadrant = Quadrant.ELIMINATE,
                tasks = tasks.filter { quadrantFor(it, now) == Quadrant.ELIMINATE },
                modifier = Modifier.weight(1f),
                onOpenTask = onOpenTask,
                onComplete = onComplete,
                onArchive = onArchive,
                onDelete = onDelete,
                onMove = onMove,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun OverviewCard(total: Int, urgent: Int, dated: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Macierz jest Twoją mapą.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (total == 0) "Dodaj pierwsze zadanie i odzyskaj przestrzeń w głowie."
                        else "Wiesz, co jest ważne — teraz wybierz jeden następny krok.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Text(
                        total.toString(),
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryPill("pilnych", urgent)
                SummaryPill("z terminem", dated)
            }
        }
    }
}

@Composable
private fun SummaryPill(label: String, count: Int) {
    Surface(
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.09f),
        shape = RoundedCornerShape(50),
    ) {
        Text(
            "$count $label",
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

private data class QuadrantPalette(val background: Color, val accent: Color, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
private fun paletteFor(quadrant: Quadrant): QuadrantPalette {
    val scheme = MaterialTheme.colorScheme
    return when (quadrant) {
        Quadrant.DO_NOW -> QuadrantPalette(
            background = scheme.errorContainer.copy(alpha = 0.72f),
            accent = scheme.error,
            icon = Icons.Outlined.WarningAmber,
        )
        Quadrant.SCHEDULE -> QuadrantPalette(
            background = scheme.tertiaryContainer.copy(alpha = 0.72f),
            accent = scheme.onTertiaryContainer,
            icon = Icons.Outlined.CalendarMonth,
        )
        Quadrant.DELEGATE -> QuadrantPalette(
            background = Color(0xFFDCEFF4).copy(alpha = if (scheme.surface == Color(0xFFFFF8F6)) 1f else 0.25f),
            accent = Color(0xFF27657A),
            icon = Icons.Outlined.ArrowForward,
        )
        Quadrant.ELIMINATE -> QuadrantPalette(
            background = scheme.surfaceVariant.copy(alpha = 0.7f),
            accent = scheme.onSurfaceVariant,
            icon = Icons.Outlined.DeleteOutline,
        )
    }
}

@Composable
private fun QuadrantCard(
    quadrant: Quadrant,
    tasks: List<TaskEntity>,
    modifier: Modifier,
    onOpenTask: (TaskEntity) -> Unit,
    onComplete: (TaskEntity) -> Unit,
    onArchive: (TaskEntity) -> Unit,
    onDelete: (TaskEntity) -> Unit,
    onMove: (TaskEntity, Int?) -> Unit,
) {
    val palette = paletteFor(quadrant)
    Card(
        modifier = modifier.heightIn(min = 214.dp),
        colors = CardDefaults.cardColors(containerColor = palette.background),
        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.16f)),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = palette.accent.copy(alpha = 0.13f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(
                        palette.icon,
                        contentDescription = null,
                        modifier = Modifier.padding(7.dp).size(17.dp),
                        tint = palette.accent,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        quadrant.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = palette.accent,
                    )
                    Text(
                        quadrant.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.accent.copy(alpha = 0.78f),
                    )
                }
                Text(
                    tasks.size.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.accent,
                )
            }
            Spacer(Modifier.height(9.dp))
            if (tasks.isEmpty()) {
                Text(
                    "Pusto. Dobrze Ci idzie!",
                    modifier = Modifier.padding(vertical = 18.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.accent.copy(alpha = 0.75f),
                )
            } else {
                tasks.take(3).forEach { task ->
                    CompactTaskRow(
                        task = task,
                        onOpen = { onOpenTask(task) },
                        onComplete = { onComplete(task) },
                        onArchive = { onArchive(task) },
                        onDelete = { onDelete(task) },
                        onMove = { quadrantIndex -> onMove(task, quadrantIndex) },
                        accent = palette.accent,
                    )
                }
                if (tasks.size > 3) {
                    Text(
                        "+${tasks.size - 3} więcej — otwórz listę Dziś",
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.accent,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactTaskRow(
    task: TaskEntity,
    onOpen: () -> Unit,
    onComplete: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onMove: (Int?) -> Unit,
    accent: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onOpen).padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        IconButton(onClick = onComplete, modifier = Modifier.size(25.dp)) {
            Icon(
                Icons.Outlined.RadioButtonUnchecked,
                contentDescription = "Oznacz jako wykonane",
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(5.dp))
        Column(Modifier.weight(1f).padding(top = 2.dp)) {
            Text(
                task.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            TaskMeta(task, compact = true)
        }
        TaskMenu(
            task = task,
            onMove = onMove,
            onArchive = onArchive,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun TaskMeta(task: TaskEntity, compact: Boolean = false) {
    val due = task.dueAt
    if (due != null || task.project.isNotBlank() || task.tags.isNotBlank()) {
        Row(
            modifier = Modifier.padding(top = if (compact) 2.dp else 5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (due != null) {
                val urgent = isUrgent(task)
                Icon(
                    if (urgent) Icons.Outlined.WarningAmber else Icons.Outlined.Event,
                    contentDescription = null,
                    modifier = Modifier.size(if (compact) 13.dp else 15.dp),
                    tint = if (urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    formatDue(due),
                    style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
                    color = if (urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (task.project.isNotBlank()) {
                Text(
                    "· ${task.project}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (task.reminderOffsetMinutes != null) {
                Icon(
                    Icons.Outlined.NotificationsNone,
                    contentDescription = "Ma przypomnienie",
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TaskMenu(
    task: TaskEntity,
    onMove: (Int?) -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onRestore: (() -> Unit)? = null,
    allowMove: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Outlined.MoreVert, contentDescription = "Więcej opcji", modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (allowMove) {
                DropdownMenuItem(
                    text = { Text("Automatycznie") },
                    onClick = { expanded = false; onMove(null) },
                )
                Quadrant.entries.forEach { quadrant ->
                    DropdownMenuItem(
                        text = { Text(quadrant.title) },
                        onClick = { expanded = false; onMove(quadrant.index) },
                    )
                }
                HorizontalDivider()
            }
            if (onRestore != null) {
                DropdownMenuItem(
                    text = { Text("Przywróć") },
                    leadingIcon = { Icon(Icons.Outlined.Restore, contentDescription = null) },
                    onClick = { expanded = false; onRestore() },
                )
            } else {
                DropdownMenuItem(
                    text = { Text("Archiwizuj") },
                    leadingIcon = { Icon(Icons.Outlined.Archive, contentDescription = null) },
                    onClick = { expanded = false; onArchive() },
                )
            }
            DropdownMenuItem(
                text = { Text("Usuń") },
                leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
                onClick = { expanded = false; onDelete() },
            )
        }
    }
}

@Composable
private fun TodayScreen(
    tasks: List<TaskEntity>,
    onOpenTask: (TaskEntity) -> Unit,
    onComplete: (TaskEntity) -> Unit,
    onArchive: (TaskEntity) -> Unit,
    onDelete: (TaskEntity) -> Unit,
    onMove: (TaskEntity, Int?) -> Unit,
) {
    val todayTasks = tasks.filter { task ->
        val due = task.dueAt ?: return@filter false
        isToday(due) || due < startOfToday()
    }
    if (todayTasks.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.CheckCircle,
            title = "Dziś masz czystą kartę",
            body = "Zadania z terminem na dziś pojawią się tutaj. Możesz też dodać coś z macierzy.",
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Today, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Najpierw to, co ma znaczenie.", fontWeight = FontWeight.Bold)
                        Text(
                            "${todayTasks.size} ${polishTaskWord(todayTasks.size)} do przejrzenia",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        items(todayTasks, key = { it.id }) { task ->
            FullTaskRow(
                task = task,
                onOpen = { onOpenTask(task) },
                onComplete = { onComplete(task) },
                onArchive = { onArchive(task) },
                onDelete = { onDelete(task) },
                onMove = { onMove(task, it) },
            )
        }
    }
}

@Composable
private fun FullTaskRow(
    task: TaskEntity,
    onOpen: () -> Unit,
    onComplete: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onMove: (Int?) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Row(Modifier.padding(start = 5.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.Top) {
            Checkbox(checked = false, onCheckedChange = { onComplete() })
            Column(Modifier.weight(1f).padding(top = 8.dp)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                TaskMeta(task)
                if (task.notes.isNotBlank()) {
                    Text(
                        task.notes,
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            TaskMenu(task, onMove = onMove, onArchive = onArchive, onDelete = onDelete)
        }
    }
}

@Composable
private fun ArchiveScreen(
    tasks: List<TaskEntity>,
    onRestore: (TaskEntity) -> Unit,
    onDelete: (TaskEntity) -> Unit,
) {
    if (tasks.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.Archive,
            title = "Archiwum jest puste",
            body = "Wykonane zadania zostaną tutaj jako historia Twojego postępu.",
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                "${tasks.size} ukończonych ${polishTaskWord(tasks.size)}",
                modifier = Modifier.padding(bottom = 5.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        items(tasks, key = { it.id }) { task ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            ) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = "Wykonane",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(7.dp).size(21.dp),
                    )
                    Column(Modifier.weight(1f).padding(top = 5.dp)) {
                        Text(
                            task.title,
                            style = MaterialTheme.typography.bodyLarge,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (task.completedAt != null) {
                            Text(
                                "Ukończone ${formatDate(task.completedAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    TaskMenu(
                        task = task,
                        onMove = { },
                        onArchive = { },
                        onDelete = { onDelete(task) },
                        onRestore = { onRestore(task) },
                        allowMove = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 35.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(18.dp).size(34.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(7.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    selected: MainViewModel.Filter,
    onSelect: (MainViewModel.Filter) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 6.dp)) {
            Text("Pokaż zadania", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            MainViewModel.Filter.entries.forEach { filter ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(filter) }.padding(vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = filter == selected, onClick = { onSelect(filter) })
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(filterLabel(filter), fontWeight = FontWeight.Medium)
                        Text(filterDescription(filter), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

private fun filterLabel(filter: MainViewModel.Filter): String = when (filter) {
    MainViewModel.Filter.ALL -> "Wszystkie aktywne"
    MainViewModel.Filter.TODAY -> "Na dziś"
    MainViewModel.Filter.OVERDUE -> "Zaległe"
    MainViewModel.Filter.NO_DATE -> "Bez terminu"
}

private fun filterDescription(filter: MainViewModel.Filter): String = when (filter) {
    MainViewModel.Filter.ALL -> "Pełna macierz zadań"
    MainViewModel.Filter.TODAY -> "Termin przypada dzisiaj"
    MainViewModel.Filter.OVERDUE -> "Termin już minął"
    MainViewModel.Filter.NO_DATE -> "Do zaplanowania"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditorSheet(
    initial: TaskEntity?,
    onDismiss: () -> Unit,
    onSave: (TaskEntity) -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var notes by remember(initial?.id) { mutableStateOf(initial?.notes.orEmpty()) }
    var link by remember(initial?.id) { mutableStateOf(initial?.link.orEmpty()) }
    var project by remember(initial?.id) { mutableStateOf(initial?.project.orEmpty()) }
    var tags by remember(initial?.id) { mutableStateOf(initial?.tags.orEmpty()) }
    var important by remember(initial?.id) { mutableStateOf(initial?.isImportant ?: false) }
    var dueAt by remember(initial?.id) { mutableStateOf(initial?.dueAt) }
    var reminder by remember(initial?.id) { mutableStateOf(initial?.reminderOffsetMinutes) }
    var recurrence by remember(initial?.id) { mutableStateOf(initial?.recurrenceType ?: Recurrence.NONE) }
    var recurrenceInterval by remember(initial?.id) { mutableStateOf(initial?.recurrenceInterval ?: 1) }
    var manualQuadrant by remember(initial?.id) { mutableStateOf(initial?.manualQuadrant) }
    var attemptedSave by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(horizontal = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (initial == null) "Nowe zadanie" else "Edytuj zadanie",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Zamień intencję w konkretny następny krok.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = "Zamknij") }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tytuł zadania") },
                placeholder = { Text("Np. wysłać ofertę klientowi") },
                singleLine = true,
                isError = attemptedSave && title.isBlank(),
                supportingText = if (attemptedSave && title.isBlank()) {
                    { Text("Dodaj tytuł, żeby zapisać zadanie") }
                } else null,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Notatka (opcjonalnie)") },
                placeholder = { Text("Kontekst, pierwszy krok, ważne szczegóły…") },
                minLines = 3,
                maxLines = 5,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("To jest ważne", fontWeight = FontWeight.Medium)
                    Text("Pilność wynika z terminu", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = important, onCheckedChange = { important = it })
            }
            Spacer(Modifier.height(12.dp))
            ChoiceField(
                label = "Ćwiartka",
                value = manualQuadrant?.let { Quadrant.fromIndex(it).title } ?: "Automatycznie z terminu",
                options = listOf("Automatycznie z terminu" to null) + Quadrant.entries.map { it.title to it.index },
                onSelect = { selected -> manualQuadrant = selected as? Int },
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { showDatePicker(context, dueAt) { dueAt = it } }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(if (dueAt == null) "Termin" else formatDateTime(dueAt!!), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (dueAt != null) {
                    IconButton(onClick = { dueAt = null; reminder = null }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Usuń termin")
                    }
                }
            }
            if (dueAt != null) {
                Spacer(Modifier.height(8.dp))
                ChoiceField(
                    label = "Przypomnienie",
                    value = reminderLabel(reminder),
                    options = listOf(
                        "Bez przypomnienia" to null,
                        "W chwili terminu" to 0,
                        "15 minut wcześniej" to 15,
                        "Godzinę wcześniej" to 60,
                        "Dzień wcześniej" to 24 * 60,
                    ),
                    onSelect = { reminder = it as? Int },
                )
            }
            Spacer(Modifier.height(10.dp))
            ChoiceField(
                label = "Powtarzanie",
                value = Recurrence.label(recurrence),
                options = listOf(
                    "Nie powtarza się" to Recurrence.NONE,
                    "Codziennie" to Recurrence.DAILY,
                    "Co tydzień" to Recurrence.WEEKLY,
                    "Co miesiąc" to Recurrence.MONTHLY,
                    "Co kilka dni" to Recurrence.CUSTOM,
                ),
                onSelect = { recurrence = it as String },
            )
            if (recurrence == Recurrence.CUSTOM) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = recurrenceInterval.toString(),
                    onValueChange = { recurrenceInterval = it.toIntOrNull()?.coerceAtLeast(1) ?: 1 },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Powtarzaj co ile dni") },
                    singleLine = true,
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = project,
                onValueChange = { project = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Projekt (opcjonalnie)") },
                leadingIcon = { Icon(Icons.Outlined.GridView, contentDescription = null) },
                singleLine = true,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tagi") },
                placeholder = { Text("np. praca, telefon, 15 min") },
                singleLine = true,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = link,
                onValueChange = { link = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Link (opcjonalnie)") },
                leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
                singleLine = true,
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    attemptedSave = true
                    if (title.isNotBlank()) {
                        val now = System.currentTimeMillis()
                        onSave(
                            TaskEntity(
                                id = initial?.id ?: 0,
                                title = title.trim(),
                                notes = notes.trim(),
                                link = link.trim(),
                                isImportant = important,
                                manualQuadrant = manualQuadrant,
                                dueAt = dueAt,
                                reminderOffsetMinutes = reminder,
                                recurrenceType = recurrence,
                                recurrenceInterval = recurrenceInterval,
                                project = project.trim(),
                                tags = tags.trim(),
                                completedAt = initial?.completedAt,
                                isArchived = initial?.isArchived ?: false,
                                createdAt = initial?.createdAt ?: now,
                                updatedAt = now,
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(15.dp),
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (initial == null) "Dodaj zadanie" else "Zapisz zmiany")
            }
            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
private fun <T> ChoiceField(
    label: String,
    value: String,
    options: List<Pair<String, T>>,
    onSelect: (T) -> Unit,
) {
    var expanded by remember(label, value) { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(13.dp),
            contentPadding = PaddingValues(horizontal = 15.dp, vertical = 12.dp),
        ) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Outlined.ArrowForward, contentDescription = "Wybierz", modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (optionLabel, optionValue) ->
                DropdownMenuItem(
                    text = { Text(optionLabel) },
                    onClick = {
                        expanded = false
                        onSelect(optionValue)
                    },
                )
            }
        }
    }
}

private fun showDatePicker(context: Context, current: Long?, onDateSelected: (Long) -> Unit) {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = current ?: System.currentTimeMillis()
    }
    DatePickerDialog(
        context,
        { _, year, month, day ->
            val selected = Calendar.getInstance().apply {
                timeInMillis = current ?: System.currentTimeMillis()
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
            }
            showTimePicker(context, selected.timeInMillis, onDateSelected)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH),
    ).show()
}

private fun showTimePicker(context: Context, current: Long, onTimeSelected: (Long) -> Unit) {
    val calendar = Calendar.getInstance().apply { timeInMillis = current }
    TimePickerDialog(
        context,
        { _, hour, minute ->
            val selected = Calendar.getInstance().apply {
                timeInMillis = current
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onTimeSelected(selected.timeInMillis)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true,
    ).show()
}

private fun reminderLabel(offset: Int?): String = when (offset) {
    null -> "Bez przypomnienia"
    0 -> "W chwili terminu"
    15 -> "15 minut wcześniej"
    60 -> "Godzinę wcześniej"
    24 * 60 -> "Dzień wcześniej"
    else -> "$offset minut wcześniej"
}

private fun formatDue(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val start = startOfToday()
    val tomorrow = start + 24L * 60L * 60L * 1000L
    return when {
        timestamp < start -> "zaległe"
        timestamp < tomorrow -> "dziś, ${SimpleDateFormat("HH:mm", Locale("pl", "PL")).format(Date(timestamp))}"
        timestamp < tomorrow + 24L * 60L * 60L * 1000L -> "jutro"
        else -> SimpleDateFormat("d MMM", Locale("pl", "PL")).format(Date(timestamp))
    }
}

private fun formatDate(timestamp: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale("pl", "PL")).format(Date(timestamp))

private fun formatDateTime(timestamp: Long): String = SimpleDateFormat("d MMM, HH:mm", Locale("pl", "PL")).format(Date(timestamp))

private fun startOfToday(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun isToday(timestamp: Long): Boolean {
    val today = Calendar.getInstance()
    val date = Calendar.getInstance().apply { timeInMillis = timestamp }
    return today.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
        today.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
}

private fun polishTaskWord(count: Int): String = when {
    count == 1 -> "zadanie"
    count in 2..4 -> "zadania"
    else -> "zadań"
}
