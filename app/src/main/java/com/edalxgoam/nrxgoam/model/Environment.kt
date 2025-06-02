package com.edalxgoam.nrxgoam.model

import androidx.compose.ui.graphics.Color
import com.google.firebase.firestore.DocumentId

data class Environment(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val color: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

// Utilidades para el modelo Environment
object EnvironmentUtils {
    const val COLLECTION_NAME = "task-tracker_environments"
    
    /**
     * Convierte el color en string a Color de Compose
     */
    fun Environment.toComposeColor(): Color {
        return try {
            if (color.startsWith("#")) {
                Color(android.graphics.Color.parseColor(color))
            } else {
                // Colores por defecto basados en el nombre
                when (color.lowercase()) {
                    "blue" -> Color.Blue
                    "green" -> Color.Green
                    "red" -> Color.Red
                    "orange" -> Color(0xFFFF9800)
                    "purple" -> Color(0xFF9C27B0)
                    "teal" -> Color(0xFF009688)
                    "indigo" -> Color(0xFF3F51B5)
                    "pink" -> Color(0xFFE91E63)
                    "yellow" -> Color(0xFFFFC107)
                    "cyan" -> Color(0xFF00BCD4)
                    else -> Color(0xFF6200EE) // Color por defecto
                }
            }
        } catch (e: Exception) {
            Color(0xFF6200EE) // Color por defecto en caso de error
        }
    }
} 