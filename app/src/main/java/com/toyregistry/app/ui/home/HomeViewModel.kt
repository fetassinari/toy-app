package com.toyregistry.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toyregistry.app.data.model.Toy
import com.toyregistry.app.data.repository.AuthRepository
import com.toyregistry.app.data.repository.ToyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val toys: List<Toy> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val toyToDelete: Toy? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val toyRepository: ToyRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _toyToDelete = MutableStateFlow<Toy?>(null)
    val toyToDelete: StateFlow<Toy?> = _toyToDelete.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)

    private val userId: String
        get() = authRepository.currentUser?.uid ?: ""

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = _searchQuery
        .flatMapLatest { query ->
            if (userId.isEmpty()) {
                flowOf(emptyList())
            } else if (query.isBlank()) {
                toyRepository.getToys(userId)
            } else {
                toyRepository.searchToys(userId, query)
            }
        }
        .combine(_error) { toys, error ->
            HomeUiState(
                toys = toys,
                isLoading = false,
                error = error
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun requestDeleteToy(toy: Toy) {
        _toyToDelete.value = toy
    }

    fun cancelDelete() {
        _toyToDelete.value = null
    }

    fun confirmDeleteToy() {
        val toy = _toyToDelete.value ?: return
        _toyToDelete.value = null

        viewModelScope.launch {
            val result = toyRepository.deleteToy(userId, toy.id)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to delete toy"
            }
            // Also delete associated images
            toy.imageUrls.forEach { url ->
                toyRepository.deleteImage(url)
            }
        }
    }
}
