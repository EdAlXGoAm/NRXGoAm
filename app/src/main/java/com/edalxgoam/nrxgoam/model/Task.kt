package com.edalxgoam.nrxgoam.model

data class Task(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val emoji: String = "",
    val description: String = "",
    val start: String = "",
    val end: String = "",
    val environment: String = "",
    val project: String = "",
    val priority: String = "medium",
    val duration: Long = 0, // en minutos
    val deadline: String? = null,
    val reminders: List<String> = emptyList(),
    val fragments: List<TaskFragment> = emptyList(),
    val createdAt: String = "",
    val updatedAt: String = "",
    val completed: Boolean = false,
    val completedAt: String? = null,
    val hidden: Boolean = false,
    val status: String = "pending"
)

data class TaskFragment(
    val start: String = "",
    val end: String = ""
)

enum class TaskPriority(val value: String) {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
    CRITICAL("critical");
    
    companion object {
        fun fromString(value: String): TaskPriority {
            return entries.find { it.value == value } ?: MEDIUM
        }
    }
}

enum class TaskStatus(val value: String) {
    PENDING("pending"),
    IN_PROGRESS("in-progress"),
    COMPLETED("completed");
    
    companion object {
        fun fromString(value: String): TaskStatus {
            return entries.find { it.value == value } ?: PENDING
        }
    }
}

// Utilidades para el modelo Task
object TaskUtils {
    const val COLLECTION_NAME = "task-tracker_tasks"
    
    fun Task.toPriorityColor(): androidx.compose.ui.graphics.Color {
        val priorityEnum = TaskPriority.fromString(priority)
        return when (priorityEnum) {
            TaskPriority.LOW -> androidx.compose.ui.graphics.Color.Green
            TaskPriority.MEDIUM -> androidx.compose.ui.graphics.Color.Blue
            TaskPriority.HIGH -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange
            TaskPriority.CRITICAL -> androidx.compose.ui.graphics.Color.Red
        }
    }
    
    fun Task.getStatusDisplayName(): String {
        val statusEnum = TaskStatus.fromString(status)
        return when (statusEnum) {
            TaskStatus.PENDING -> "Pendiente"
            TaskStatus.IN_PROGRESS -> "En Progreso" 
            TaskStatus.COMPLETED -> "Completada"
        }
    }
    
    fun Task.getPriorityDisplayName(): String {
        val priorityEnum = TaskPriority.fromString(priority)
        return when (priorityEnum) {
            TaskPriority.LOW -> "Baja"
            TaskPriority.MEDIUM -> "Media"
            TaskPriority.HIGH -> "Alta"
            TaskPriority.CRITICAL -> "Crítica"
        }
    }
} 