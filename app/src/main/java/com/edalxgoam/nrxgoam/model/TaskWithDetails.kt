package com.edalxgoam.nrxgoam.model

/**
 * Clase que combina una tarea con sus detalles de proyecto y ambiente
 */
data class TaskWithDetails(
    val task: Task,
    val project: Project? = null,
    val environment: Environment? = null
) {
    
    /**
     * Obtiene el nombre del proyecto o una cadena por defecto
     */
    fun getProjectName(): String {
        return project?.name?.takeIf { it.isNotEmpty() } ?: "Sin proyecto"
    }
    
    /**
     * Obtiene el nombre del ambiente o una cadena por defecto
     */
    fun getEnvironmentName(): String {
        return environment?.name?.takeIf { it.isNotEmpty() } ?: "Sin ambiente"
    }
    
    /**
     * Obtiene el color del proyecto como Color de Compose
     */
    fun getProjectColor(): androidx.compose.ui.graphics.Color {
        return project?.let { ProjectUtils.run { it.toComposeColor() } } 
            ?: androidx.compose.ui.graphics.Color(0xFF6200EE)
    }
    
    /**
     * Obtiene el color del ambiente como Color de Compose
     */
    fun getEnvironmentColor(): androidx.compose.ui.graphics.Color {
        return environment?.let { EnvironmentUtils.run { it.toComposeColor() } }
            ?: androidx.compose.ui.graphics.Color(0xFF03DAC5)
    }
    
    /**
     * Indica si la tarea tiene información completa de proyecto y ambiente
     */
    fun hasCompleteHierarchy(): Boolean {
        return project != null && environment != null
    }
    
    /**
     * Obtiene una descripción legible de la jerarquía ambiente > proyecto
     */
    fun getHierarchyDescription(): String {
        return when {
            hasCompleteHierarchy() -> "${getEnvironmentName()} > ${getProjectName()}"
            project != null -> getProjectName()
            environment != null -> getEnvironmentName()
            else -> "Sin categorizar"
        }
    }
} 