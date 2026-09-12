package pl.fokus.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.fokus.app.data.TaskEntity
import pl.fokus.app.data.TaskRepository

class MainViewModel(
    private val repository: TaskRepository,
) : ViewModel() {
    enum class Filter { ALL, TODAY, OVERDUE, NO_DATE }

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(Filter.ALL)

    val searchQuery: StateFlow<String> = query
    val selectedFilter: StateFlow<Filter> = filter

    val activeTasks: StateFlow<List<TaskEntity>> = combine(
        repository.active,
        query,
        filter,
    ) { tasks, text, selectedFilter ->
        val normalized = text.trim().lowercase()
        tasks.filter { task ->
            val matchesText = normalized.isBlank() || listOf(
                task.title,
                task.notes,
                task.project,
                task.tags,
            ).joinToString(" ").lowercase().contains(normalized)
            val matchesFilter = when (selectedFilter) {
                Filter.ALL -> true
                Filter.TODAY -> task.dueAt?.let(::isToday) == true
                Filter.OVERDUE -> task.dueAt?.let { it < System.currentTimeMillis() } == true
                Filter.NO_DATE -> task.dueAt == null
            }
            matchesText && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val archivedTasks: StateFlow<List<TaskEntity>> = repository.archived.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    fun setSearchQuery(value: String) {
        query.value = value
    }

    fun setFilter(value: Filter) {
        filter.value = value
    }

    fun save(task: TaskEntity) = viewModelScope.launch {
        repository.save(task)
    }

    fun move(task: TaskEntity, quadrant: Int?) = viewModelScope.launch {
        repository.move(task, quadrant)
    }

    fun archive(task: TaskEntity) = viewModelScope.launch {
        repository.archive(task)
    }

    fun toggleComplete(task: TaskEntity) = viewModelScope.launch {
        repository.toggleComplete(task)
    }

    fun restore(task: TaskEntity) = viewModelScope.launch {
        repository.restore(task)
    }

    fun delete(task: TaskEntity) = viewModelScope.launch {
        repository.delete(task)
    }

    private fun isToday(timestamp: Long): Boolean {
        val today = java.util.Calendar.getInstance()
        val date = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
        return today.get(java.util.Calendar.YEAR) == date.get(java.util.Calendar.YEAR) &&
            today.get(java.util.Calendar.DAY_OF_YEAR) == date.get(java.util.Calendar.DAY_OF_YEAR)
    }

    class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
