package com.toyregistry.app.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toyregistry.app.data.model.Category
import com.toyregistry.app.data.repository.AuthRepository
import com.toyregistry.app.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val userId: String
        get() = authRepository.currentUser?.uid ?: ""

    private val _newCategoryName = MutableStateFlow("")
    val newCategoryName: StateFlow<String> = _newCategoryName.asStateFlow()

    private val _editingCategory = MutableStateFlow<Category?>(null)
    val editingCategory: StateFlow<Category?> = _editingCategory.asStateFlow()

    private val _categoryToDelete = MutableStateFlow<Category?>(null)
    val categoryToDelete: StateFlow<Category?> = _categoryToDelete.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CategoryUiState> = categoryRepository.getCategories(userId)
        .map { categories ->
            CategoryUiState(
                categories = categories,
                isLoading = false,
                error = _error.value
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CategoryUiState())

    fun onNewCategoryNameChanged(name: String) {
        _newCategoryName.value = name
    }

    fun addCategory() {
        val name = _newCategoryName.value.trim()
        if (name.isBlank()) return

        viewModelScope.launch {
            val category = Category(
                name = name,
                userId = userId
            )
            val result = categoryRepository.addCategory(category)
            if (result.isSuccess) {
                _newCategoryName.value = ""
            } else {
                _error.value = result.exceptionOrNull()?.message
            }
        }
    }

    fun startEditing(category: Category) {
        _editingCategory.value = category
        _newCategoryName.value = category.name
    }

    fun cancelEditing() {
        _editingCategory.value = null
        _newCategoryName.value = ""
    }

    fun updateCategory() {
        val category = _editingCategory.value ?: return
        val name = _newCategoryName.value.trim()
        if (name.isBlank()) return

        viewModelScope.launch {
            val updatedCategory = category.copy(name = name)
            val result = categoryRepository.updateCategory(updatedCategory)
            if (result.isSuccess) {
                _editingCategory.value = null
                _newCategoryName.value = ""
            } else {
                _error.value = result.exceptionOrNull()?.message
            }
        }
    }

    fun requestDeleteCategory(category: Category) {
        _categoryToDelete.value = category
    }

    fun cancelDelete() {
        _categoryToDelete.value = null
    }

    fun confirmDeleteCategory() {
        val category = _categoryToDelete.value ?: return
        _categoryToDelete.value = null

        viewModelScope.launch {
            val result = categoryRepository.deleteCategory(userId, category.id)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message
            }
        }
    }
}
