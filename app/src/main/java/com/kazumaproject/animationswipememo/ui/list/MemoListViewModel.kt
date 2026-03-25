package com.kazumaproject.animationswipememo.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kazumaproject.animationswipememo.di.AppContainer
import com.kazumaproject.animationswipememo.domain.model.MemoDraft
import com.kazumaproject.animationswipememo.domain.repository.MemoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MemoListUiState(
    val memos: List<MemoDraft> = emptyList(),
    val searchQuery: String = "",
    val hasAnyMemos: Boolean = false
)

class MemoListViewModel(
    private val memoRepository: MemoRepository
) : ViewModel() {
    private val searchQueryState = MutableStateFlow("")

    val uiState: StateFlow<MemoListUiState> = combine(
        memoRepository.observeMemos(),
        searchQueryState
    ) { memos, query ->
        val normalizedQuery = query.trim()
        val filtered = if (normalizedQuery.isBlank()) {
            memos
        } else {
            memos.filter { memo ->
                memo.searchableText.contains(normalizedQuery, ignoreCase = true)
            }
        }
        MemoListUiState(
            memos = filtered,
            searchQuery = query,
            hasAnyMemos = memos.isNotEmpty()
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MemoListUiState()
        )

    fun updateSearchQuery(query: String) {
        searchQueryState.value = query.take(80)
    }

    fun deleteMemo(id: String) {
        viewModelScope.launch {
            memoRepository.deleteMemo(id)
        }
    }

    fun deleteAllMemos() {
        viewModelScope.launch {
            memoRepository.deleteAllMemos()
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MemoListViewModel(container.memoRepository)
            }
        }
    }
}
