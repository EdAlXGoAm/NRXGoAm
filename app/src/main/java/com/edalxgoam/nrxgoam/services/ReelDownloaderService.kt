package com.edalxgoam.nrxgoam.services

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Servicio para descargar videos de redes sociales
 */
object ReelDownloaderService {
    
    private const val BASE_URL = "https://functions.zonayummy.com/api"
    
    // Endpoints para cada plataforma
    private const val ENDPOINT_INSTAGRAM = "/reel-downloader-ig"
    private const val ENDPOINT_FACEBOOK = "/reel-downloader-fb"
    private const val ENDPOINT_YOUTUBE = "/reel-downloader-yt"
    private const val ENDPOINT_TIKTOK = "/reel-downloader-tiktok"
    
    enum class Platform {
        INSTAGRAM,
        FACEBOOK,
        YOUTUBE,
        TIKTOK
    }
    
    sealed class DownloadResult {
        data class Success(val filePath: String, val fileName: String) : DownloadResult()
        data class Error(val message: String) : DownloadResult()
        data class Progress(val percentage: Int) : DownloadResult()
    }

    data class CacheDownloadResult(
        val file: File,
        val fileName: String,
        val contentType: String,
        val sizeBytes: Long,
    )
    
    /**
     * Descarga un video de cualquier plataforma soportada
     */
    suspend fun downloadVideo(
        context: Context,
        videoUrl: String,
        platform: Platform,
        onProgress: (Int) -> Unit = {}
    ): DownloadResult = withContext(Dispatchers.IO) {
        try {
            // Validar URL según la plataforma
            val validationResult = validateUrl(videoUrl, platform)
            if (validationResult != null) {
                return@withContext DownloadResult.Error(validationResult)
            }
            
            // Obtener endpoint según la plataforma
            val endpoint = when (platform) {
                Platform.INSTAGRAM -> ENDPOINT_INSTAGRAM
                Platform.FACEBOOK -> ENDPOINT_FACEBOOK
                Platform.YOUTUBE -> ENDPOINT_YOUTUBE
                Platform.TIKTOK -> ENDPOINT_TIKTOK
            }
            
            // Preparar la petición POST
            val url = URL("$BASE_URL$endpoint")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "video/mp4,application/json")
                connectTimeout = 30000
                readTimeout = 180000 // 3 minutos para descargas grandes
            }
            
            // Enviar el body JSON
            val jsonBody = JSONObject().apply {
                put("url", videoUrl)
            }
            
            connection.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
                os.flush()
            }
            
            val responseCode = connection.responseCode
            
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorStream = connection.errorStream
                val errorBody = errorStream?.bufferedReader()?.readText() ?: "Error desconocido"
                
                val errorMessage = try {
                    val errorJson = JSONObject(errorBody)
                    errorJson.optString("error", "Error al descargar el video")
                } catch (e: Exception) {
                    "Error al descargar el video (código: $responseCode)"
                }
                
                return@withContext DownloadResult.Error(errorMessage)
            }
            
            // Obtener información del archivo
            val contentDisposition = connection.getHeaderField("Content-Disposition")
            val defaultFileName = "${platform.name.lowercase()}_video_${System.currentTimeMillis()}.mp4"
            val fileName = extractFileName(contentDisposition) ?: defaultFileName
            val contentLength = connection.contentLength
            
            // Guardar el video
            val savedPath = saveVideoToDevice(
                context = context,
                inputStream = connection.inputStream,
                fileName = fileName,
                contentLength = contentLength,
                onProgress = onProgress
            )
            
            connection.disconnect()
            
            if (savedPath != null) {
                DownloadResult.Success(savedPath, fileName)
            } else {
                DownloadResult.Error("Error al guardar el video")
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            DownloadResult.Error("Error: ${e.message ?: "Error desconocido"}")
        }
    }

    /**
     * Descarga el video pero lo guarda como archivo temporal en cache (para "Guardar en la nube").
     * Nota: evita cargar el video completo en memoria.
     */
    suspend fun downloadVideoToCacheFile(
        context: Context,
        videoUrl: String,
        platform: Platform,
        onProgress: (Int) -> Unit = {},
    ): Result<CacheDownloadResult> = withContext(Dispatchers.IO) {
        try {
            val validationResult = validateUrl(videoUrl, platform)
            if (validationResult != null) return@withContext Result.failure(IllegalArgumentException(validationResult))

            val endpoint = when (platform) {
                Platform.INSTAGRAM -> ENDPOINT_INSTAGRAM
                Platform.FACEBOOK -> ENDPOINT_FACEBOOK
                Platform.YOUTUBE -> ENDPOINT_YOUTUBE
                Platform.TIKTOK -> ENDPOINT_TIKTOK
            }

            val url = URL("$BASE_URL$endpoint")
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "video/mp4,application/json")
                connectTimeout = 30000
                readTimeout = 180000
            }

            val jsonBody = JSONObject().apply { put("url", videoUrl) }
            connection.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "Error desconocido"
                val errorMessage = try {
                    JSONObject(errorBody).optString("error", "Error al descargar el video")
                } catch (_: Exception) {
                    "Error al descargar el video (código: $responseCode)"
                }
                return@withContext Result.failure(IllegalStateException(errorMessage))
            }

            val contentDisposition = connection.getHeaderField("Content-Disposition")
            val defaultFileName = "${platform.name.lowercase()}_video_${System.currentTimeMillis()}.mp4"
            val fileName = extractFileName(contentDisposition) ?: defaultFileName
            val contentType = connection.contentType?.takeIf { it.isNotBlank() } ?: "video/mp4"
            val contentLength = connection.contentLength

            val outFile = File(context.cacheDir, "zy_${System.currentTimeMillis()}_$fileName")
            var totalRead = 0L

            connection.inputStream.use { input ->
                FileOutputStream(outFile).use { output ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        totalRead += read
                        if (contentLength > 0) {
                            val p = ((totalRead * 100) / contentLength).toInt().coerceIn(0, 100)
                            onProgress(p)
                        }
                    }
                    output.flush()
                }
            }

            onProgress(100)
            connection.disconnect()

            Result.success(
                CacheDownloadResult(
                    file = outFile,
                    fileName = fileName,
                    contentType = contentType,
                    sizeBytes = totalRead
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Valida la URL según la plataforma
     * @return null si es válida, mensaje de error si no lo es
     */
    private fun validateUrl(url: String, platform: Platform): String? {
        val pattern = when (platform) {
            Platform.INSTAGRAM -> Regex(
                "^https?://(www\\.)?instagram\\.com/(reel|reels|p)/[\\w-]+", 
                RegexOption.IGNORE_CASE
            )
            Platform.FACEBOOK -> Regex(
                "^https?://(www\\.|m\\.|web\\.)?(facebook\\.com|fb\\.watch)/(reel|watch|video|share/[rv]|.*/videos)/[\\w\\d\\-/?=&]+", 
                RegexOption.IGNORE_CASE
            )
            Platform.YOUTUBE -> Regex(
                "^https?://(www\\.|m\\.)?(youtube\\.com/(shorts/|watch\\?v=)|youtu\\.be/)", 
                RegexOption.IGNORE_CASE
            )
            Platform.TIKTOK -> Regex(
                "^https?://([a-z]+\\.)?(tiktok\\.com|tiktokcdn\\.com)/", 
                RegexOption.IGNORE_CASE
            )
        }
        
        if (!pattern.containsMatchIn(url)) {
            val example = when (platform) {
                Platform.INSTAGRAM -> "https://www.instagram.com/reel/ABC123"
                Platform.FACEBOOK -> "https://www.facebook.com/reel/123456 o share/r/..."
                Platform.YOUTUBE -> "https://www.youtube.com/shorts/ABC123"
                Platform.TIKTOK -> "https://vt.tiktok.com/ABC123 o @user/video/123"
            }
            return "URL inválida para ${platform.name}. Ejemplo: $example"
        }
        
        return null
    }
    
    // Métodos de conveniencia para cada plataforma
    
    suspend fun downloadInstagramReel(
        context: Context,
        reelUrl: String,
        onProgress: (Int) -> Unit = {}
    ): DownloadResult = downloadVideo(context, reelUrl, Platform.INSTAGRAM, onProgress)
    
    suspend fun downloadFacebookReel(
        context: Context,
        reelUrl: String,
        onProgress: (Int) -> Unit = {}
    ): DownloadResult = downloadVideo(context, reelUrl, Platform.FACEBOOK, onProgress)
    
    suspend fun downloadYouTubeShort(
        context: Context,
        shortUrl: String,
        onProgress: (Int) -> Unit = {}
    ): DownloadResult = downloadVideo(context, shortUrl, Platform.YOUTUBE, onProgress)
    
    suspend fun downloadTikTokVideo(
        context: Context,
        videoUrl: String,
        onProgress: (Int) -> Unit = {}
    ): DownloadResult = downloadVideo(context, videoUrl, Platform.TIKTOK, onProgress)
    
    /**
     * Extrae el nombre del archivo del header Content-Disposition
     */
    private fun extractFileName(contentDisposition: String?): String? {
        if (contentDisposition == null) return null
        
        val pattern = Regex("filename=\"?([^\"]+)\"?")
        val match = pattern.find(contentDisposition)
        return match?.groupValues?.get(1)
    }
    
    /**
     * Guarda el video en el almacenamiento del dispositivo
     */
    private fun saveVideoToDevice(
        context: Context,
        inputStream: InputStream,
        fileName: String,
        contentLength: Int,
        onProgress: (Int) -> Unit
    ): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveWithMediaStore(context, inputStream, fileName, contentLength, onProgress)
            } else {
                saveToExternalStorage(context, inputStream, fileName, contentLength, onProgress)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Guarda usando MediaStore (Android 10+)
     */
    private fun saveWithMediaStore(
        context: Context,
        inputStream: InputStream,
        fileName: String,
        contentLength: Int,
        onProgress: (Int) -> Unit
    ): String? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/NRXGoAm")
        }
        
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return null
        
        resolver.openOutputStream(uri)?.use { outputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                
                if (contentLength > 0) {
                    val progress = ((totalBytesRead * 100) / contentLength).toInt()
                    onProgress(progress.coerceIn(0, 100))
                }
            }
            outputStream.flush()
        }
        
        onProgress(100)
        return uri.toString()
    }
    
    /**
     * Guarda en almacenamiento externo (Android 9 y anterior)
     */
    private fun saveToExternalStorage(
        context: Context,
        inputStream: InputStream,
        fileName: String,
        contentLength: Int,
        onProgress: (Int) -> Unit
    ): String? {
        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val appDir = File(moviesDir, "NRXGoAm")
        
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        
        val file = File(appDir, fileName)
        
        FileOutputStream(file).use { outputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                
                if (contentLength > 0) {
                    val progress = ((totalBytesRead * 100) / contentLength).toInt()
                    onProgress(progress.coerceIn(0, 100))
                }
            }
            outputStream.flush()
        }
        
        onProgress(100)
        return file.absolutePath
    }
}
