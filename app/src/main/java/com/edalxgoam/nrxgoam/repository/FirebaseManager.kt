package com.edalxgoam.nrxgoam.repository

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.storage.FirebaseStorage

object FirebaseManager {
    
    private var isInitialized = false
    private var iconCacheRepository: IconCacheRepository? = null
    private var authRepository: AuthRepository? = null
    private var taskRepository: TaskRepository? = null
    
    // Repositorios
    val firestoreRepository by lazy { FirestoreRepository() }
    val storageRepository by lazy { FirebaseStorageRepository() }
    
    /**
     * Obtiene la instancia del repositorio de cache de iconos
     */
    fun getIconCacheRepository(context: Context): IconCacheRepository {
        if (iconCacheRepository == null) {
            iconCacheRepository = IconCacheRepository(context)
        }
        return iconCacheRepository!!
    }
    
    /**
     * Obtiene la instancia del repositorio de autenticación
     */
    fun getAuthRepository(context: Context): AuthRepository {
        if (authRepository == null) {
            authRepository = AuthRepository(context)
        }
        return authRepository!!
    }
    
    /**
     * Obtiene la instancia del repositorio de tareas
     */
    fun getTaskRepository(): TaskRepository {
        if (taskRepository == null) {
            taskRepository = TaskRepository()
        }
        return taskRepository!!
    }
    
    /**
     * Inicializa Firebase con configuración optimizada
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        try {
            // Inicializar Firebase si no está inicializado
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            
            // Configurar Firestore
            configureFirestore()
            
            // Configurar Storage
            configureStorage()
            
            // Inicializar cache de iconos
            iconCacheRepository = IconCacheRepository(context)
            
            isInitialized = true
            
        } catch (e: Exception) {
            throw Exception("Error al inicializar Firebase: ${e.message}", e)
        }
    }
    
    /**
     * Configura Firestore con configuraciones optimizadas para offline y sincronización
     */
    private fun configureFirestore() {
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true) // Habilita cache offline
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED) // Cache ilimitado
            .build()
            
        FirebaseFirestore.getInstance().firestoreSettings = settings
        
        // Habilitar logs para debug (opcional, remove en producción)
        FirebaseFirestore.setLoggingEnabled(true)
    }
    
    /**
     * Configura Storage con configuraciones optimizadas
     */
    private fun configureStorage() {
        val storage = FirebaseStorage.getInstance()
        // Configurar timeout si es necesario
        storage.maxUploadRetryTimeMillis = 60000 // 60 segundos
        storage.maxDownloadRetryTimeMillis = 60000 // 60 segundos
    }
    
    /**
     * Verifica si Firebase está inicializado
     */
    fun isFirebaseInitialized(): Boolean {
        return isInitialized
    }
    
    /**
     * Obtiene una instancia de Firestore configurada
     */
    fun getFirestore(): FirebaseFirestore {
        checkInitialization()
        return FirebaseFirestore.getInstance()
    }
    
    /**
     * Obtiene una instancia de Storage configurada
     */
    fun getStorage(): FirebaseStorage {
        checkInitialization()
        return FirebaseStorage.getInstance()
    }
    
    /**
     * Verifica que Firebase esté inicializado antes de usar
     */
    private fun checkInitialization() {
        if (!isInitialized) {
            throw IllegalStateException("Firebase no ha sido inicializado. Llama a FirebaseManager.initialize(context) primero.")
        }
    }
    
    /**
     * Funciones utilitarias para manejar estado offline/online
     */
    
    /**
     * Habilita la red de Firestore (útil para testing o control manual)
     */
    fun enableNetwork(): com.google.android.gms.tasks.Task<Void> {
        return FirebaseFirestore.getInstance().enableNetwork()
    }
    
    /**
     * Deshabilita la red de Firestore (fuerza modo offline)
     */
    fun disableNetwork(): com.google.android.gms.tasks.Task<Void> {
        return FirebaseFirestore.getInstance().disableNetwork()
    }
    
    /**
     * Espera a que todas las escrituras pendientes se sincronicen
     */
    fun waitForPendingWrites(): com.google.android.gms.tasks.Task<Void> {
        return FirebaseFirestore.getInstance().waitForPendingWrites()
    }
    
    /**
     * Limpia toda la cache offline (úsalo con cuidado)
     */
    fun clearPersistence(): com.google.android.gms.tasks.Task<Void> {
        return FirebaseFirestore.getInstance().clearPersistence()
    }
    
    /**
     * Precarga todos los iconos desde Firebase Storage
     */
    suspend fun preloadIcons(context: Context) {
        try {
            getIconCacheRepository(context).preloadAllIcons()
        } catch (e: Exception) {
            println("Error al precargar iconos: ${e.message}")
        }
    }
} 