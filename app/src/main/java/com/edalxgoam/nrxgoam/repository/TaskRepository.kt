package com.edalxgoam.nrxgoam.repository

import com.edalxgoam.nrxgoam.model.Task
import com.edalxgoam.nrxgoam.model.TaskStatus
import com.edalxgoam.nrxgoam.model.TaskPriority
import com.edalxgoam.nrxgoam.model.TaskUtils
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepository {
    
    private val firestoreRepo = FirebaseManager.firestoreRepository
    
    /**
     * Obtiene todas las tareas del usuario actual
     */
    suspend fun getUserTasks(userId: String): Result<List<Task>> {
        return firestoreRepo.getCollectionWithQuery(
            collection = TaskUtils.COLLECTION_NAME,
            clazz = Task::class.java
        ) { query ->
            query.whereEqualTo("userId", userId)
        }.map { tasks ->
            tasks.sortByStartDate()
        }
    }
    
    /**
     * Obtiene todas las tareas del usuario filtradas por estado
     */
    suspend fun getUserTasksByStatus(userId: String, status: String): Result<List<Task>> {
        return firestoreRepo.getCollectionWithQuery(
            collection = TaskUtils.COLLECTION_NAME,
            clazz = Task::class.java
        ) { query ->
            query.whereEqualTo("userId", userId)
                .whereEqualTo("status", status)
        }.map { tasks ->
            tasks.sortByStartDate()
        }
    }
    
    /**
     * Obtiene todas las tareas del usuario filtradas por prioridad
     */
    suspend fun getUserTasksByPriority(userId: String, priority: String): Result<List<Task>> {
        return firestoreRepo.getCollectionWithQuery(
            collection = TaskUtils.COLLECTION_NAME,
            clazz = Task::class.java
        ) { query ->
            query.whereEqualTo("userId", userId)
                .whereEqualTo("priority", priority)
        }.map { tasks ->
            tasks.sortByStartDate()
        }
    }
    
    /**
     * Obtiene las tareas activas (no completadas y no ocultas)
     */
    suspend fun getActiveTasks(userId: String): Result<List<Task>> {
        return getActiveTasks(userId, descending = true)
    }
    
    /**
     * Obtiene las tareas activas (no completadas y no ocultas) con orden especificado
     * @param descending Si es true ordena descendente (más reciente primero), si es false ascendente
     */
    suspend fun getActiveTasks(userId: String, descending: Boolean): Result<List<Task>> {
        return firestoreRepo.getCollectionWithQuery(
            collection = TaskUtils.COLLECTION_NAME,
            clazz = Task::class.java
        ) { query ->
            query.whereEqualTo("userId", userId)
                .whereEqualTo("completed", false)
                .whereEqualTo("hidden", false)
        }.map { tasks ->
            tasks.sortByStartDate(descending)
        }
    }
    
    /**
     * Obtiene una tarea específica por ID
     */
    suspend fun getTask(taskId: String): Result<Task?> {
        return firestoreRepo.getDocument(
            collection = TaskUtils.COLLECTION_NAME,
            documentId = taskId,
            clazz = Task::class.java
        )
    }
    
    /**
     * Crea una nueva tarea
     */
    suspend fun createTask(task: Task): Result<String> {
        val taskWithTimestamp = task.copy(
            createdAt = getCurrentTimestamp(),
            updatedAt = getCurrentTimestamp()
        )
        
        return firestoreRepo.createDocument(
            collection = TaskUtils.COLLECTION_NAME,
            data = taskWithTimestamp
        )
    }
    
    /**
     * Actualiza una tarea existente
     */
    suspend fun updateTask(taskId: String, updates: Map<String, Any?>): Result<Unit> {
        val updatesWithTimestamp = updates.toMutableMap()
        updatesWithTimestamp["updatedAt"] = getCurrentTimestamp()
        
        return firestoreRepo.updateDocument(
            collection = TaskUtils.COLLECTION_NAME,
            documentId = taskId,
            updates = updatesWithTimestamp
        )
    }
    
    /**
     * Marca una tarea como completada
     */
    suspend fun completeTask(taskId: String): Result<Unit> {
        val updates = mapOf(
            "completed" to true,
            "status" to "completed",
            "completedAt" to getCurrentTimestamp(),
            "updatedAt" to getCurrentTimestamp()
        )
        
        return updateTask(taskId, updates)
    }
    
    /**
     * Cambia el estado de una tarea
     */
    suspend fun updateTaskStatus(taskId: String, status: String): Result<Unit> {
        val updates = mutableMapOf<String, Any?>(
            "status" to status,
            "updatedAt" to getCurrentTimestamp()
        )
        
        // Si se marca como completada, agregar información adicional
        if (status == "completed") {
            updates["completed"] = true
            updates["completedAt"] = getCurrentTimestamp()
        } else {
            updates["completed"] = false
            updates["completedAt"] = null
        }
        
        return updateTask(taskId, updates)
    }
    
    /**
     * Oculta/muestra una tarea
     */
    suspend fun toggleTaskVisibility(taskId: String, hidden: Boolean): Result<Unit> {
        val updates = mapOf(
            "hidden" to hidden,
            "updatedAt" to getCurrentTimestamp()
        )
        
        return updateTask(taskId, updates)
    }
    
    /**
     * Elimina una tarea
     */
    suspend fun deleteTask(taskId: String): Result<Unit> {
        return firestoreRepo.deleteDocument(
            collection = TaskUtils.COLLECTION_NAME,
            documentId = taskId
        )
    }
    
    /**
     * Escucha cambios en tiempo real de las tareas del usuario
     */
    fun listenToUserTasks(userId: String): Flow<List<Task>> {
        return firestoreRepo.listenToCollectionWithQuery(
            collection = TaskUtils.COLLECTION_NAME,
            clazz = Task::class.java
        ) { query ->
            query.whereEqualTo("userId", userId)
                 .whereEqualTo("hidden", false)
        }.map { tasks ->
            tasks.sortByStartDate()
        }
    }
    
    /**
     * Escucha cambios en tiempo real de las tareas activas
     */
    fun listenToActiveTasks(userId: String): Flow<List<Task>> {
        return listenToUserTasks(userId).map { tasks ->
            tasks.filter { !it.completed }
        }
    }
    
    /**
     * Escucha cambios en tiempo real de las tareas completadas
     */
    fun listenToCompletedTasks(userId: String): Flow<List<Task>> {
        return listenToUserTasks(userId).map { tasks ->
            tasks.filter { it.completed }
        }
    }
    
    /**
     * Obtiene un timestamp actual como string ISO
     */
    private fun getCurrentTimestamp(): String {
        return java.time.Instant.now().toString()
    }
    
    /**
     * Ordena las tareas por fecha de inicio, luego por fecha de creación
     * @param descending Si es true ordena descendente (más reciente primero), si es false ascendente
     */
    private fun List<Task>.sortByStartDate(descending: Boolean = true): List<Task> {
        val comparator = compareByDescending<Task> { task ->
            // Priorizar tareas con fecha de inicio
            if (task.start.isNotEmpty()) {
                try {
                    // Normalizar formato de fecha para comparación
                    val normalizedStart = when {
                        task.start.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}")) -> "${task.start}:00Z"
                        task.start.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")) -> "${task.start}Z"
                        else -> task.start
                    }
                    java.time.Instant.parse(normalizedStart)
                } catch (e: Exception) {
                    // Si falla el parsing, usar fecha de creación
                    try {
                        java.time.Instant.parse(task.createdAt)
                    } catch (e2: Exception) {
                        java.time.Instant.EPOCH
                    }
                }
            } else {
                // Si no tiene fecha de inicio, usar fecha de creación
                try {
                    java.time.Instant.parse(task.createdAt)
                } catch (e: Exception) {
                    java.time.Instant.EPOCH
                }
            }
        }
        
        return if (descending) {
            this.sortedWith(comparator)
        } else {
            this.sortedWith(comparator.reversed())
        }
    }
    
    /**
     * Busca tareas por texto (nombre o descripción)
     */
    suspend fun searchTasks(userId: String, searchText: String): Result<List<Task>> {
        return getUserTasks(userId).map { tasks ->
            tasks.filter { task ->
                task.name.contains(searchText, ignoreCase = true) ||
                task.description.contains(searchText, ignoreCase = true) ||
                task.project.contains(searchText, ignoreCase = true)
            }
        }
    }
    
    // Métodos de conveniencia con enums (wrappers)
    
    /**
     * Obtiene todas las tareas del usuario filtradas por estado (usando enum)
     */
    suspend fun getUserTasksByStatus(userId: String, status: TaskStatus): Result<List<Task>> {
        return getUserTasksByStatus(userId, status.value)
    }
    
    /**
     * Obtiene todas las tareas del usuario filtradas por prioridad (usando enum)
     */
    suspend fun getUserTasksByPriority(userId: String, priority: TaskPriority): Result<List<Task>> {
        return getUserTasksByPriority(userId, priority.value)
    }
    
    /**
     * Cambia el estado de una tarea (usando enum)
     */
    suspend fun updateTaskStatus(taskId: String, status: TaskStatus): Result<Unit> {
        return updateTaskStatus(taskId, status.value)
    }
} 