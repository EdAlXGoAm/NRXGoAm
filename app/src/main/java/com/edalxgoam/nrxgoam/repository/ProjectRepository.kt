package com.edalxgoam.nrxgoam.repository

import com.edalxgoam.nrxgoam.model.Project
import com.edalxgoam.nrxgoam.model.ProjectUtils
import kotlinx.coroutines.flow.Flow

class ProjectRepository {
    
    private val firestoreRepo = FirebaseManager.firestoreRepository
    
    /**
     * Obtiene todos los proyectos del usuario
     */
    suspend fun getUserProjects(userId: String): Result<List<Project>> {
        return firestoreRepo.getCollectionWithQuery(
            collection = ProjectUtils.COLLECTION_NAME,
            clazz = Project::class.java
        ) { query ->
            query.whereEqualTo("userId", userId)
                // Temporalmente quitar orderBy para debuggear
                // .orderBy("name")
        }
    }
    
    /**
     * Obtiene todos los proyectos de un ambiente específico
     */
    suspend fun getProjectsByEnvironment(userId: String, environmentId: String): Result<List<Project>> {
        return firestoreRepo.getCollectionWithQuery(
            collection = ProjectUtils.COLLECTION_NAME,
            clazz = Project::class.java
        ) { query ->
            query.whereEqualTo("userId", userId)
                .whereEqualTo("environment", environmentId)
                .orderBy("name")
        }
    }
    
    /**
     * Obtiene un proyecto específico por ID
     */
    suspend fun getProject(projectId: String): Result<Project?> {
        return firestoreRepo.getDocument(
            collection = ProjectUtils.COLLECTION_NAME,
            documentId = projectId,
            clazz = Project::class.java
        )
    }
    
    /**
     * Crea un nuevo proyecto
     */
    suspend fun createProject(project: Project): Result<String> {
        val projectWithTimestamp = project.copy(
            createdAt = getCurrentTimestamp(),
            updatedAt = getCurrentTimestamp()
        )
        
        return firestoreRepo.createDocument(
            collection = ProjectUtils.COLLECTION_NAME,
            data = projectWithTimestamp
        )
    }
    
    /**
     * Actualiza un proyecto existente
     */
    suspend fun updateProject(projectId: String, updates: Map<String, Any?>): Result<Unit> {
        val updatesWithTimestamp = updates.toMutableMap()
        updatesWithTimestamp["updatedAt"] = getCurrentTimestamp()
        
        return firestoreRepo.updateDocument(
            collection = ProjectUtils.COLLECTION_NAME,
            documentId = projectId,
            updates = updatesWithTimestamp
        )
    }
    
    /**
     * Elimina un proyecto
     */
    suspend fun deleteProject(projectId: String): Result<Unit> {
        return firestoreRepo.deleteDocument(
            collection = ProjectUtils.COLLECTION_NAME,
            documentId = projectId
        )
    }
    
    /**
     * Escucha cambios en tiempo real de los proyectos del usuario
     */
    fun listenToUserProjects(userId: String): Flow<List<Project>> {
        return firestoreRepo.listenToCollectionWithQuery(
            collection = ProjectUtils.COLLECTION_NAME,
            clazz = Project::class.java
        ) { query ->
            query.whereEqualTo("userId", userId)
                .orderBy("name")
        }
    }
    
    /**
     * Escucha cambios en tiempo real de los proyectos de un ambiente específico
     */
    fun listenToProjectsByEnvironment(userId: String, environmentId: String): Flow<List<Project>> {
        return firestoreRepo.listenToCollectionWithQuery(
            collection = ProjectUtils.COLLECTION_NAME,
            clazz = Project::class.java
        ) { query ->
            query.whereEqualTo("userId", userId)
                .whereEqualTo("environment", environmentId)
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