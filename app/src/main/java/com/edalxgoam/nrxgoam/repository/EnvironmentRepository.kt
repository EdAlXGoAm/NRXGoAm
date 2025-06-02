package com.edalxgoam.nrxgoam.repository

import com.edalxgoam.nrxgoam.model.Environment
import com.edalxgoam.nrxgoam.model.EnvironmentUtils
import kotlinx.coroutines.flow.Flow

class EnvironmentRepository {
    
    private val firestoreRepo = FirebaseManager.firestoreRepository
    
    /**
     * Obtiene todos los ambientes del usuario
     */
    suspend fun getUserEnvironments(userId: String): Result<List<Environment>> {
        return firestoreRepo.getCollectionWithQuery(
            collection = EnvironmentUtils.COLLECTION_NAME,
            clazz = Environment::class.java
        ) { query ->
            query.whereEqualTo("userId", userId)
                // Temporalmente quitar orderBy para debuggear
                // .orderBy("name")
        }
    }
    
    /**
     * Obtiene un ambiente específico por ID
     */
    suspend fun getEnvironment(environmentId: String): Result<Environment?> {
        return firestoreRepo.getDocument(
            collection = EnvironmentUtils.COLLECTION_NAME,
            documentId = environmentId,
            clazz = Environment::class.java
        )
    }
    
    /**
     * Crea un nuevo ambiente
     */
    suspend fun createEnvironment(environment: Environment): Result<String> {
        val environmentWithTimestamp = environment.copy(
            createdAt = getCurrentTimestamp(),
            updatedAt = getCurrentTimestamp()
        )
        
        return firestoreRepo.createDocument(
            collection = EnvironmentUtils.COLLECTION_NAME,
            data = environmentWithTimestamp
        )
    }
    
    /**
     * Actualiza un ambiente existente
     */
    suspend fun updateEnvironment(environmentId: String, updates: Map<String, Any?>): Result<Unit> {
        val updatesWithTimestamp = updates.toMutableMap()
        updatesWithTimestamp["updatedAt"] = getCurrentTimestamp()
        
        return firestoreRepo.updateDocument(
            collection = EnvironmentUtils.COLLECTION_NAME,
            documentId = environmentId,
            updates = updatesWithTimestamp
        )
    }
    
    /**
     * Elimina un ambiente
     */
    suspend fun deleteEnvironment(environmentId: String): Result<Unit> {
        return firestoreRepo.deleteDocument(
            collection = EnvironmentUtils.COLLECTION_NAME,
            documentId = environmentId
        )
    }
    
    /**
     * Escucha cambios en tiempo real de los ambientes del usuario
     */
    fun listenToUserEnvironments(userId: String): Flow<List<Environment>> {
        return firestoreRepo.listenToCollectionWithQuery(
            collection = EnvironmentUtils.COLLECTION_NAME,
            clazz = Environment::class.java
        ) { query ->
            query.whereEqualTo("userId", userId)
                .orderBy("name")
        }
    }
    
    /**
     * Obtiene un timestamp actual como string ISO
     */
    private fun getCurrentTimestamp(): String {
        return java.time.Instant.now().toString()
    }
} 