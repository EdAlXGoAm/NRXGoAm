package com.edalxgoam.nrxgoam.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import com.edalxgoam.nrxgoam.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class IconCacheRepository(private val context: Context) {
    
    private val storageRepo = FirebaseManager.storageRepository
    private val cacheDir = File(context.cacheDir, "firebase_icons")
    
    init {
        // Crear directorio de cache si no existe
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }
    
    /**
     * Rutas de los iconos en Firebase Storage
     */
    object IconPaths {
        const val ALARM_ICON = "NRXGoAm/icons/icon_alarm.png"
        const val PANTRY_ICON = "NRXGoAm/icons/icon_despensa.png"
    }
    
    /**
     * Nombres de archivos en cache local
     */
    object CacheFileNames {
        const val ALARM_ICON = "icon_alarm_cached.png"
        const val PANTRY_ICON = "icon_despensa_cached.png"
    }
    
    /**
     * Descarga un icono desde Firebase Storage y lo cachea localmente
     */
    suspend fun downloadAndCacheIcon(
        firebasePath: String,
        cacheFileName: String
    ): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val cacheFile = File(cacheDir, cacheFileName)
                
                // Si ya existe en cache y es reciente (menos de 24 horas), usar el cache
                if (cacheFile.exists() && isFileFresh(cacheFile)) {
                    return@withContext Result.success(cacheFile)
                }
                
                // Descargar desde Firebase Storage
                val downloadResult = storageRepo.downloadFile(firebasePath)
                
                if (downloadResult.isFailure) {
                    return@withContext Result.failure(
                        downloadResult.exceptionOrNull() ?: Exception("Error al descargar icono")
                    )
                }
                
                val imageBytes = downloadResult.getOrNull()!!
                
                // Guardar en cache
                FileOutputStream(cacheFile).use { output ->
                    output.write(imageBytes)
                }
                
                Result.success(cacheFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Obtiene un icono, primero del cache y si no existe lo descarga
     */
    suspend fun getIcon(
        firebasePath: String,
        cacheFileName: String,
        fallbackResourceId: Int
    ): Drawable {
        return withContext(Dispatchers.IO) {
            try {
                val cacheFile = File(cacheDir, cacheFileName)
                
                // Si existe en cache y es válido, usar el cache
                if (cacheFile.exists() && cacheFile.length() > 0) {
                    val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
                    if (bitmap != null) {
                        return@withContext BitmapDrawable(context.resources, bitmap)
                    }
                }
                
                // Si no existe en cache, intentar descargar
                val downloadResult = downloadAndCacheIcon(firebasePath, cacheFileName)
                
                if (downloadResult.isSuccess) {
                    val bitmap = BitmapFactory.decodeFile(downloadResult.getOrNull()!!.absolutePath)
                    if (bitmap != null) {
                        return@withContext BitmapDrawable(context.resources, bitmap)
                    }
                }
                
                // Fallback a recurso local si todo falla
                ContextCompat.getDrawable(context, fallbackResourceId)!!
                
            } catch (e: Exception) {
                // Fallback a recurso local en caso de error
                ContextCompat.getDrawable(context, fallbackResourceId)!!
            }
        }
    }
    
    /**
     * Obtiene el icono de alarma (Firebase + Cache + Fallback)
     */
    suspend fun getAlarmIcon(): Drawable {
        return getIcon(
            firebasePath = IconPaths.ALARM_ICON,
            cacheFileName = CacheFileNames.ALARM_ICON,
            fallbackResourceId = R.drawable.icon_alarm_placeholder
        )
    }
    
    /**
     * Obtiene el icono de despensa (Firebase + Cache + Fallback)
     */
    suspend fun getPantryIcon(): Drawable {
        return getIcon(
            firebasePath = IconPaths.PANTRY_ICON,
            cacheFileName = CacheFileNames.PANTRY_ICON,
            fallbackResourceId = R.drawable.icon_despensa_placeholder
        )
    }
    
    /**
     * Precarga todos los iconos en background
     */
    suspend fun preloadAllIcons() {
        try {
            // Descargar iconos en paralelo
            val alarmJob = downloadAndCacheIcon(IconPaths.ALARM_ICON, CacheFileNames.ALARM_ICON)
            val pantryJob = downloadAndCacheIcon(IconPaths.PANTRY_ICON, CacheFileNames.PANTRY_ICON)
            
            println("Precarga completada - Alarma: ${alarmJob.isSuccess}, Despensa: ${pantryJob.isSuccess}")
        } catch (e: Exception) {
            println("Error en precarga de iconos: ${e.message}")
        }
    }
    
    /**
     * Limpia el cache de iconos
     */
    fun clearIconCache(): Boolean {
        return try {
            cacheDir.listFiles()?.forEach { file ->
                file.delete()
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Verifica si un archivo en cache es reciente (menos de 24 horas)
     */
    private fun isFileFresh(file: File): Boolean {
        val maxAge = 24 * 60 * 60 * 1000 // 24 horas en milisegundos
        return (System.currentTimeMillis() - file.lastModified()) < maxAge
    }
    
    /**
     * Obtiene el tamaño del cache en bytes
     */
    fun getCacheSize(): Long {
        return try {
            cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}

/**
 * Composable para usar iconos con cache de Firebase
 */
@Composable
fun rememberFirebaseIcon(
    firebasePath: String,
    cacheFileName: String,
    fallbackResourceId: Int
): Painter {
    val context = LocalContext.current
    var iconDrawable by remember { mutableStateOf<Drawable?>(null) }
    val iconRepo = remember { IconCacheRepository(context) }
    
    LaunchedEffect(firebasePath) {
        iconDrawable = iconRepo.getIcon(firebasePath, cacheFileName, fallbackResourceId)
    }
    
    // Si el icono aún no está cargado, usar el fallback
    return if (iconDrawable != null) {
        rememberDrawablePainter(iconDrawable!!)
    } else {
        painterResource(id = fallbackResourceId)
    }
}

/**
 * Convierte un Drawable a Painter (función helper)
 */
@Composable
fun rememberDrawablePainter(drawable: Drawable): Painter {
    return remember(drawable) {
        BitmapPainter(
            (drawable as BitmapDrawable).bitmap.asImageBitmap()
        )
    }
} 