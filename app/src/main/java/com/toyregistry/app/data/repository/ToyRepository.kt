package com.toyregistry.app.data.repository

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.toyregistry.app.data.model.Toy
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToyRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private fun toysCollection(userId: String) =
        firestore.collection("users").document(userId).collection("toys")

    fun getToys(userId: String): Flow<List<Toy>> = callbackFlow {
        val listener = toysCollection(userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val toys = snapshot?.toObjects(Toy::class.java) ?: emptyList()
                trySend(toys)
            }
        awaitClose { listener.remove() }
    }

    fun searchToys(userId: String, query: String): Flow<List<Toy>> = callbackFlow {
        val listener = toysCollection(userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val toys = snapshot?.toObjects(Toy::class.java)
                    ?.filter { toy ->
                        toy.name.contains(query, ignoreCase = true) ||
                        toy.description.contains(query, ignoreCase = true) ||
                        toy.storageLocation.contains(query, ignoreCase = true) ||
                        toy.categoryName.contains(query, ignoreCase = true)
                    } ?: emptyList()
                trySend(toys)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addToy(toy: Toy): Result<String> {
        return try {
            val docRef = toysCollection(toy.userId).add(toy).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateToy(toy: Toy): Result<Unit> {
        return try {
            toysCollection(toy.userId).document(toy.id).set(toy).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteToy(userId: String, toyId: String): Result<Unit> {
        return try {
            toysCollection(userId).document(toyId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadImage(userId: String, imageUri: Uri): Result<String> {
        return try {
            val fileName = "toys/${userId}/${UUID.randomUUID()}.jpg"
            val ref = storage.reference.child(fileName)
            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteImage(imageUrl: String): Result<Unit> {
        return try {
            val ref = storage.getReferenceFromUrl(imageUrl)
            ref.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
