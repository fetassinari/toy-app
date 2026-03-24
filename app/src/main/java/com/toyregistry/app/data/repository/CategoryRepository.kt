package com.toyregistry.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.toyregistry.app.data.model.Category
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun categoriesCollection(userId: String) =
        firestore.collection("users").document(userId).collection("categories")

    fun getCategories(userId: String): Flow<List<Category>> = callbackFlow {
        val listener = categoriesCollection(userId)
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val categories = snapshot?.toObjects(Category::class.java) ?: emptyList()
                trySend(categories)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addCategory(category: Category): Result<String> {
        return try {
            val docRef = categoriesCollection(category.userId).add(category).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCategory(category: Category): Result<Unit> {
        return try {
            categoriesCollection(category.userId).document(category.id).set(category).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(userId: String, categoryId: String): Result<Unit> {
        return try {
            categoriesCollection(userId).document(categoryId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
