package com.toyregistry.app.ui.toy

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toyregistry.app.data.model.Category
import com.toyregistry.app.data.model.Toy
import com.toyregistry.app.data.repository.AuthRepository
import com.toyregistry.app.data.repository.CategoryRepository
import com.toyregistry.app.data.repository.ToyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditToyUiState(
    val name: String = "",
    val description: String = "",
    val storageLocation: String = "",
    val selectedCategoryId: String = "",
    val selectedCategoryName: String = "",
    val existingImageUrls: List<String> = emptyList(),
    val newImageUris: List<Uri> = emptyList(),
    val removedImageUrls: List<String> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false,
    val isEditing: Boolean = false
)

@HiltViewModel
class AddEditToyViewModel @Inject constructor(
    private val toyRepository: ToyRepository,
    private val categoryRepository: CategoryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditToyUiState())
    val uiState: StateFlow<AddEditToyUiState> = _uiState.asStateFlow()

    private val userId: String
        get() = authRepository.currentUser?.uid ?: ""

    private var originalToy: Toy? = null

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getCategories(userId).collect { categories ->
                _uiState.value = _uiState.value.copy(categories = categories)
            }
        }
    }

    fun loadToy(toyId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            toyRepository.getToys(userId).collect { toys ->
                val toy = toys.find { it.id == toyId }
                if (toy != null) {
                    originalToy = toy
                    _uiState.value = _uiState.value.copy(
                        name = toy.name,
                        description = toy.description,
                        storageLocation = toy.storageLocation,
                        selectedCategoryId = toy.categoryId,
                        selectedCategoryName = toy.categoryName,
                        existingImageUrls = toy.imageUrls,
                        isLoading = false,
                        isEditing = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Toy not found"
                    )
                }
            }
        }
    }

    fun onNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(name = name, error = null)
    }

    fun onDescriptionChanged(description: String) {
        _uiState.value = _uiState.value.copy(description = description, error = null)
    }

    fun onStorageLocationChanged(location: String) {
        _uiState.value = _uiState.value.copy(storageLocation = location, error = null)
    }

    fun onCategorySelected(category: Category?) {
        _uiState.value = _uiState.value.copy(
            selectedCategoryId = category?.id ?: "",
            selectedCategoryName = category?.name ?: "",
            error = null
        )
    }

    fun addImages(uris: List<Uri>) {
        _uiState.value = _uiState.value.copy(
            newImageUris = _uiState.value.newImageUris + uris,
            error = null
        )
    }

    fun removeNewImage(uri: Uri) {
        _uiState.value = _uiState.value.copy(
            newImageUris = _uiState.value.newImageUris - uri
        )
    }

    fun removeExistingImage(url: String) {
        _uiState.value = _uiState.value.copy(
            existingImageUrls = _uiState.value.existingImageUrls - url,
            removedImageUrls = _uiState.value.removedImageUrls + url
        )
    }

    fun saveToy() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.value = state.copy(error = "Toy name is required")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)

            // Upload new images
            val uploadedUrls = mutableListOf<String>()
            for (uri in state.newImageUris) {
                val result = toyRepository.uploadImage(userId, uri)
                if (result.isSuccess) {
                    uploadedUrls.add(result.getOrThrow())
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = "Failed to upload image: ${result.exceptionOrNull()?.message}"
                    )
                    return@launch
                }
            }

            val allImageUrls = state.existingImageUrls + uploadedUrls

            val toy = Toy(
                id = originalToy?.id ?: "",
                name = state.name,
                description = state.description,
                storageLocation = state.storageLocation,
                categoryId = state.selectedCategoryId,
                categoryName = state.selectedCategoryName,
                imageUrls = allImageUrls,
                userId = userId,
                createdAt = originalToy?.createdAt
            )

            val result = if (state.isEditing) {
                toyRepository.updateToy(toy)
            } else {
                toyRepository.addToy(toy).map { }
            }

            if (result.isSuccess) {
                // Delete removed images from storage
                for (url in state.removedImageUrls) {
                    toyRepository.deleteImage(url)
                }
                _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
            } else {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to save toy"
                )
            }
        }
    }
}
