package com.kazumaproject.animationswipememo.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kazumaproject.animationswipememo.di.AppContainer
import com.kazumaproject.animationswipememo.domain.model.MemoDraft
import com.kazumaproject.animationswipememo.domain.repository.MemoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MemoListUiState(
    val memos: List<MemoDraft> = emptyList()
)

class MemoListViewModel(
    memoRepository: MemoRepository
) : ViewModel() {
    val uiState: StateFlow<MemoListUiState> = memoRepository.observeMemos()
        .map { memos -> MemoListUiState(memos) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MemoListUiState()
        )

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MemoListViewModel(container.memoRepository)
            }
        }
    }
}
