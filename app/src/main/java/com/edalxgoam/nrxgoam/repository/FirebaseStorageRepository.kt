package com.edalxgoam.nrxgoam.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import java.io.InputStream

class FirebaseStorageRepository {
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference

    // Subir archivo desde URI
    suspend fun uploadFile(
        filePath: String,
        fileUri: Uri,
        metadata: Map<String, String>? = null
    ): Result<String> {
        return try {
            val fileRef = storageRef.child(filePath)
            
            val uploadTask = if (metadata != null) {
                val storageMetadata = com.google.firebase.storage.StorageMetadata.Builder()
                    .apply {
                        metadata.forEach { (key, value) ->
                            setCustomMetadata(key, value)
                        }
                    }.build()
                fileRef.putFile(fileUri, storageMetadata)
            } else {
                fileRef.putFile(fileUri)
            }
            
            uploadTask.await()
            val downloadUrl = fileRef.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Subir archivo desde bytes
    suspend fun uploadBytes(
        filePath: String,
        data: ByteArray,
        metadata: Map<String, String>? = null
    ): Result<String> {
        return try {
            val fileRef = storageRef.child(filePath)
            
            val uploadTask = if (metadata != null) {
                val storageMetadata = com.google.firebase.storage.StorageMetadata.Builder()
                    .apply {
                        metadata.forEach { (key, value) ->
                            setCustomMetadata(key, value)
                        }
                    }.build()
                fileRef.putBytes(data, storageMetadata)
            } else {
                fileRef.putBytes(data)
            }
            
            uploadTask.await()
            val downloadUrl = fileRef.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Subir archivo desde InputStream
    suspend fun uploadStream(
        filePath: String,
        stream: InputStream,
        metadata: Map<String, String>? = null
    ): Result<String> {
        return try {
            val fileRef = storageRef.child(filePath)
            
            val uploadTask = if (metadata != null) {
                val storageMetadata = com.google.firebase.storage.StorageMetadata.Builder()
                    .apply {
                        metadata.forEach { (key, value) ->
                            setCustomMetadata(key, value)
                        }
                    }.build()
                fileRef.putStream(stream, storageMetadata)
            } else {
                fileRef.putStream(stream)
            }
            
            uploadTask.await()
            val downloadUrl = fileRef.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Monitorear progreso de subida con Flow
    fun uploadFileWithProgress(
        filePath: String,
        fileUri: Uri,
        metadata: Map<String, String>? = null
    ): Flow<UploadProgress> = callbackFlow {
        val fileRef = storageRef.child(filePath)
        
        val uploadTask = if (metadata != null) {
            val storageMetadata = com.google.firebase.storage.StorageMetadata.Builder()
                .apply {
                    metadata.forEach { (key, value) ->
                        setCustomMetadata(key, value)
                    }
                }.build()
            fileRef.putFile(fileUri, storageMetadata)
        } else {
            fileRef.putFile(fileUri)
        }
        
        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
            trySend(UploadProgress.InProgress(progress, taskSnapshot.bytesTransferred, taskSnapshot.totalByteCount))
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                fileRef.downloadUrl.addOnSuccessListener { uri ->
                    trySend(UploadProgress.Success(uri.toString()))
                    close()
                }.addOnFailureListener { exception ->
                    trySend(UploadProgress.Error(exception))
                    close()
                }
            } else {
                trySend(UploadProgress.Error(task.exception ?: Exception("Upload failed")))
                close()
            }
        }
        
        awaitClose { uploadTask.cancel() }
    }

    // Descargar archivo como bytes
    suspend fun downloadFile(filePath: String): Result<ByteArray> {
        return try {
            val fileRef = storageRef.child(filePath)
            val bytes = fileRef.getBytes(Long.MAX_VALUE).await()
            Result.success(bytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Obtener URL de descarga
    suspend fun getDownloadUrl(filePath: String): Result<String> {
        return try {
            val fileRef = storageRef.child(filePath)
            val downloadUrl = fileRef.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Eliminar archivo
    suspend fun deleteFile(filePath: String): Result<Unit> {
        return try {
            val fileRef = storageRef.child(filePath)
            fileRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Obtener metadatos del archivo
    suspend fun getFileMetadata(filePath: String): Result<com.google.firebase.storage.StorageMetadata> {
        return try {
            val fileRef = storageRef.child(filePath)
            val metadata = fileRef.metadata.await()
            Result.success(metadata)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Listar archivos en un directorio
    suspend fun listFiles(directoryPath: String): Result<List<StorageReference>> {
        return try {
            val dirRef = storageRef.child(directoryPath)
            val listResult = dirRef.listAll().await()
            Result.success(listResult.items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Actualizar metadatos
    suspend fun updateMetadata(
        filePath: String,
        metadata: Map<String, String>
    ): Result<com.google.firebase.storage.StorageMetadata> {
        return try {
            val fileRef = storageRef.child(filePath)
            val storageMetadata = com.google.firebase.storage.StorageMetadata.Builder()
                .apply {
                    metadata.forEach { (key, value) ->
                        setCustomMetadata(key, value)
                    }
                }.build()
            
            val updatedMetadata = fileRef.updateMetadata(storageMetadata).await()
            Result.success(updatedMetadata)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Clase sellada para manejar el progreso de subida
sealed class UploadProgress {
    data class InProgress(val progress: Double, val bytesTransferred: Long, val totalBytes: Long) : UploadProgress()
    data class Success(val downloadUrl: String) : UploadProgress()
    data class Error(val exception: Exception) : UploadProgress()
} 