package com.edalxgoam.nrxgoam.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Modelo de ejemplo para un usuario
 * Las anotaciones @DocumentId y @ServerTimestamp son específicas de Firebase
 */
data class User(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null
) {
    // Constructor sin argumentos requerido para Firebase
    constructor() : this("", "", "", "", null, null)
}

/**
 * Modelo de ejemplo para una nota o documento
 */
data class Note(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val userId: String = "",
    val tags: List<String> = emptyList(),
    val attachmentUrls: List<String> = emptyList(),
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null,
    val isArchived: Boolean = false
) {
    constructor() : this("", "", "", "", emptyList(), emptyList(), null, null, false)
}

/**
 * Modelo de ejemplo para archivos subidos a Storage
 */
data class FileUpload(
    @DocumentId
    val id: String = "",
    val fileName: String = "",
    val originalName: String = "",
    val mimeType: String = "",
    val size: Long = 0,
    val storageUrl: String = "",
    val downloadUrl: String = "",
    val uploadedBy: String = "",
    @ServerTimestamp
    val uploadedAt: Date? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    constructor() : this("", "", "", "", 0, "", "", "", null, emptyMap())
}

/**
 * Modelo de ejemplo para configuración de la app
 */
data class AppConfig(
    @DocumentId
    val id: String = "",
    val version: String = "",
    val maintenanceMode: Boolean = false,
    val features: Map<String, Boolean> = emptyMap(),
    val messages: Map<String, String> = emptyMap(),
    @ServerTimestamp
    val lastUpdated: Date? = null
) {
    constructor() : this("", "", false, emptyMap(), emptyMap(), null)
}

/**
 * Clase utilitaria para operaciones comunes de Firebase
 */
object FirebaseUtils {
    
    /**
     * Genera un timestamp del servidor para usar en updates
     */
    fun serverTimestamp(): Any = FieldValue.serverTimestamp()
    
    /**
     * Genera un ID único para documentos
     */
    fun generateId(): String = java.util.UUID.randomUUID().toString()
    
    /**
     * Formatea la ruta de Storage para archivos de usuario (con prefijo ASE_)
     */
    fun getUserStoragePath(userId: String, fileName: String): String {
        return "ASE_users/$userId/files/$fileName"
    }
    
    /**
     * Formatea la ruta de Storage para imágenes de perfil (con prefijo ASE_)
     */
    fun getProfileImagePath(userId: String, fileName: String): String {
        return "ASE_users/$userId/profile/$fileName"
    }
    
    /**
     * Formatea la ruta de Storage para archivos públicos (con prefijo ASE_)
     */
    fun getPublicStoragePath(fileName: String): String {
        return "ASE_public/$fileName"
    }
    
    /**
     * Formatea la ruta de Storage para archivos temporales de ejemplo
     */
    fun getTempExamplePath(fileName: String): String {
        return "ASE_temp/$fileName"
    }
}

/**
 * Constantes para colecciones de Firestore
 * Prefijo ASE_ (AndroidStudioExample) para diferenciar de tablas reales
 */
object FirebaseCollections {
    const val USERS = "ASE_users"
    const val NOTES = "ASE_notes"
    const val FILES = "ASE_files"
    const val APP_CONFIG = "ASE_app_config"
    const val ANALYTICS = "ASE_analytics"
} 