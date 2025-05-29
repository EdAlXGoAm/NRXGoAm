package com.edalxgoam.nrxgoam.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose

class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()

    // Crear un documento
    suspend fun <T : Any> createDocument(collection: String, documentId: String? = null, data: T): Result<String> {
        return try {
            val docRef = if (documentId != null) {
                db.collection(collection).document(documentId)
            } else {
                db.collection(collection).document()
            }
            
            docRef.set(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Leer un documento
    suspend fun <T : Any> getDocument(
        collection: String, 
        documentId: String, 
        clazz: Class<T>
    ): Result<T?> {
        return try {
            val document = db.collection(collection).document(documentId).get().await()
            if (document.exists()) {
                val data = document.toObject(clazz)
                Result.success(data)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Obtener todos los documentos de una colección
    suspend fun <T : Any> getCollection(collection: String, clazz: Class<T>): Result<List<T>> {
        return try {
            val snapshot = db.collection(collection).get().await()
            val items = snapshot.documents.mapNotNull { it.toObject(clazz) }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Obtener colección con consulta
    suspend fun <T : Any> getCollectionWithQuery(
        collection: String,
        clazz: Class<T>,
        queryBuilder: (Query) -> Query
    ): Result<List<T>> {
        return try {
            val baseQuery = db.collection(collection)
            val query = queryBuilder(baseQuery)
            val snapshot = query.get().await()
            val items = snapshot.documents.mapNotNull { it.toObject(clazz) }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Actualizar un documento
    suspend fun updateDocument(
        collection: String, 
        documentId: String, 
        updates: Map<String, Any?>
    ): Result<Unit> {
        return try {
            db.collection(collection).document(documentId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Eliminar un documento
    suspend fun deleteDocument(collection: String, documentId: String): Result<Unit> {
        return try {
            db.collection(collection).document(documentId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Escuchar cambios en tiempo real de un documento
    fun <T : Any> listenToDocument(
        collection: String, 
        documentId: String, 
        clazz: Class<T>
    ): Flow<T?> = callbackFlow {
        val listener = db.collection(collection).document(documentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("FirestoreRepository: Error listening to document $documentId: ${error.message}")
                    // En lugar de cerrar, enviar null y continuar
                    trySend(null)
                    return@addSnapshotListener
                }
                
                val data = if (snapshot != null && snapshot.exists()) {
                    snapshot.toObject(clazz)
                } else {
                    null
                }
                trySend(data)
            }
        
        awaitClose { listener.remove() }
    }

    // Escuchar cambios en tiempo real de una colección
    fun <T : Any> listenToCollection(collection: String, clazz: Class<T>): Flow<List<T>> = callbackFlow {
        val listener = db.collection(collection)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("FirestoreRepository: Error listening to collection $collection: ${error.message}")
                    // En lugar de cerrar, enviar lista vacía y continuar
                    trySend(emptyList<T>())
                    return@addSnapshotListener
                }
                
                val items = snapshot?.documents?.mapNotNull { it.toObject(clazz) } ?: emptyList()
                trySend(items)
            }
        
        awaitClose { listener.remove() }
    }

    // Escuchar cambios en tiempo real de una colección con consulta filtrada
    fun <T : Any> listenToCollectionWithQuery(
        collection: String,
        clazz: Class<T>,
        queryBuilder: (Query) -> Query
    ): Flow<List<T>> = callbackFlow {
        val baseQuery = db.collection(collection)
        val query = queryBuilder(baseQuery)
        
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                println("FirestoreRepository: Error listening to collection with query $collection: ${error.message}")
                // En lugar de cerrar, enviar lista vacía y continuar
                trySend(emptyList<T>())
                return@addSnapshotListener
            }
            
            val items = snapshot?.documents?.mapNotNull { it.toObject(clazz) } ?: emptyList()
            trySend(items)
        }
        
        awaitClose { listener.remove() }
    }

    // Transacción para operaciones atomicas
    suspend fun runTransaction(operation: suspend (FirebaseFirestore) -> Unit): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                // Las transacciones requieren una función específica de Firebase
                // Se puede extender según las necesidades específicas
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 