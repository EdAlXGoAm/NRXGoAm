package com.edalxgoam.nrxgoam.ui.screens

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.LruCache
import android.widget.Toast
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import com.edalxgoam.nrxgoam.R
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edalxgoam.nrxgoam.services.ReelDownloaderService
import com.edalxgoam.nrxgoam.services.ZonayummyAuthApi
import com.edalxgoam.nrxgoam.services.ZonayummyReelHistoryApi
import com.edalxgoam.nrxgoam.services.ZonayummySession
import com.edalxgoam.nrxgoam.services.ZonayummySessionStore
import com.edalxgoam.nrxgoam.services.ZyFolder
import com.edalxgoam.nrxgoam.services.ZyHistoryItem
import com.edalxgoam.nrxgoam.services.ZyLabel
import com.edalxgoam.nrxgoam.ui.theme.NRXGoAmTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DownloadReelsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            NRXGoAmTheme {
                DownloadReelsRoot(onBackClick = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadReelsTopBar(
    onBackClick: () -> Unit,
    zySession: ZonayummySession?,
    onOpenHistory: () -> Unit,
    onOpenLogin: () -> Unit,
    onLogout: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = "Download Reels",
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver"
                )
            }
        },
        actions = {
            IconButton(onClick = onOpenHistory) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_history),
                    contentDescription = "Historial",
                    tint = CeoStyle.Muted
                )
            }
            if (zySession != null) {
                IconButton(onClick = onLogout) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Cerrar sesión", tint = CeoStyle.Muted)
                }
            } else {
                IconButton(onClick = onOpenLogin) {
                    Icon(Icons.Default.AccountCircle, contentDescription = "Iniciar sesión", tint = CeoStyle.Muted)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CeoStyle.Surface,
            titleContentColor = CeoStyle.Text,
            navigationIconContentColor = CeoStyle.Muted,
        )
    )
}

// Colores para cada plataforma
data class PlatformInfo(
    val name: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val placeholder: String,
    val isAvailable: Boolean = false
)

val platforms = listOf(
    PlatformInfo(
        name = "Instagram",
        primaryColor = Color(0xFFE4405F),
        secondaryColor = Color(0xFFC13584),
        placeholder = "https://www.instagram.com/reel/ABC123...",
        isAvailable = true
    ),
    PlatformInfo(
        name = "Facebook",
        primaryColor = Color(0xFF1877F2),
        secondaryColor = Color(0xFF4267B2),
        placeholder = "https://facebook.com/reel/123 o share/r/...",
        isAvailable = true
    ),
    PlatformInfo(
        name = "YouTube",
        primaryColor = Color(0xFFFF0000),
        secondaryColor = Color(0xFFCC0000),
        placeholder = "https://youtube.com/shorts/ABC123...",
        isAvailable = true
    ),
    PlatformInfo(
        name = "TikTok",
        primaryColor = Color(0xFF000000),
        secondaryColor = Color(0xFF25F4EE),
        placeholder = "https://vt.tiktok.com/ABC o @user/video/...",
        isAvailable = true
    )
)

// Estados de descarga
sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Int) : DownloadState()
    data class Success(val fileName: String) : DownloadState()
    data class Error(val message: String) : DownloadState()
    data class CloudSaving(val progress: Int, val message: String) : DownloadState()
    data class CloudSuccess(val message: String) : DownloadState()
}

private fun saveCacheVideoToDevice(
    context: Context,
    cacheFile: File,
    fileName: String,
    contentType: String,
): Result<String> {
    return try {
        if (!cacheFile.exists()) {
            return Result.failure(IllegalStateException("Archivo temporal no encontrado"))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, contentType.ifBlank { "video/mp4" })
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/NRXGoAm")
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: return Result.failure(IllegalStateException("No se pudo crear el archivo en MediaStore"))

            try {
                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(cacheFile).use { input ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            val r = input.read(buffer)
                            if (r <= 0) break
                            out.write(buffer, 0, r)
                        }
                        out.flush()
                    }
                } ?: run {
                    return Result.failure(IllegalStateException("No se pudo abrir el OutputStream de MediaStore"))
                }
            } catch (e: Exception) {
                try { resolver.delete(uri, null, null) } catch (_: Exception) {}
                return Result.failure(e)
            }

            Result.success(uri.toString())
        } else {
            val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            val appDir = File(moviesDir, "NRXGoAm")
            if (!appDir.exists()) appDir.mkdirs()

            val outFile = File(appDir, fileName)
            FileInputStream(cacheFile).use { input ->
                FileOutputStream(outFile).use { output ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val r = input.read(buffer)
                        if (r <= 0) break
                        output.write(buffer, 0, r)
                    }
                    output.flush()
                }
            }
            Result.success(outFile.absolutePath)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Estilo CEO (basado en zy_zonayummy/src/pages/CEO_Usuarios/ceo-common.css)
// ─────────────────────────────────────────────────────────────────────────────

private object CeoStyle {
    val Bg = Color(0xFFF8F9FA)          // #f8f9fa
    val Surface = Color(0xFFFFFFFF)     // #ffffff
    val Border = Color(0xFFE5E5E5)      // #e5e5e5
    val Border2 = Color(0xFFDDDDDD)     // #ddd
    val Text = Color(0xFF1A1A1A)        // #1a1a1a
    val Muted = Color(0xFF666666)       // #666
    val Placeholder = Color(0xFF999999) // #999
    val Cyan = Color(0xFF00BCD4)        // #00bcd4
    val CyanBg = Color(0xFFE0F7FA)      // #e0f7fa
    val Danger = Color(0xFFDC3545)      // #dc3545
    val DangerBg = Color(0xFFFFEEEE)    // #fee (aprox)
    val SuccessBg = Color(0xFFD4EDDA)   // #d4edda
    val SuccessText = Color(0xFF155724) // #155724
}

private sealed interface ZyFolderFilter {
    data object Unassigned : ZyFolderFilter // folderId == null
    data object All : ZyFolderFilter
    data class Folder(val id: String) : ZyFolderFilter
}

@Composable
fun DownloadReelsRoot(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var zySession by remember { mutableStateOf(ZonayummySessionStore.getSession(context)) }
    var showLogin by remember { mutableStateOf(false) }

    // Drawer / historial
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var zyHistoryLoading by remember { mutableStateOf(false) }
    var zyHistoryError by remember { mutableStateOf("") }
    var zyItems by remember { mutableStateOf<List<ZyHistoryItem>>(emptyList()) }
    var zyFolders by remember { mutableStateOf<List<ZyFolder>>(emptyList()) }
    var zyLabels by remember { mutableStateOf<List<ZyLabel>>(emptyList()) }
    var folderFilter by remember { mutableStateOf<ZyFolderFilter>(ZyFolderFilter.Unassigned) }
    var showCreateFolder by remember { mutableStateOf(false) }

    // selección para mover
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteLoading by remember { mutableStateOf(false) }

    // edición
    var editingItem by remember { mutableStateOf<ZyHistoryItem?>(null) }

    // acción pendiente (si el usuario toca "Guardar en la nube" sin sesión)
    var pendingCloudSave by remember { mutableStateOf<Pair<String, ReelDownloaderService.Platform>?>(null) }

    fun bumpLabelUsage(labelId: String, delta: Int) {
        val id = labelId.trim()
        if (id.isBlank()) return
        val idx = zyLabels.indexOfFirst { it.id == id }
        if (idx < 0) return
        val current = zyLabels[idx].usageCount
        val next = (current + delta).coerceAtLeast(0)
        val updated = zyLabels.toMutableList()
        updated[idx] = updated[idx].copy(usageCount = next)
        zyLabels = updated
    }

    fun applyLabelUsageDiff(prevLabelIds: List<String>?, nextLabelIds: List<String>?) {
        val prev = (prevLabelIds ?: emptyList()).map { it.trim() }.filter { it.isNotBlank() }.toSet()
        val next = (nextLabelIds ?: emptyList()).map { it.trim() }.filter { it.isNotBlank() }.toSet()
        // Added
        for (id in next) if (!prev.contains(id)) bumpLabelUsage(id, +1)
        // Removed
        for (id in prev) if (!next.contains(id)) bumpLabelUsage(id, -1)
    }

    fun refreshHistory() {
        val token = zySession?.token ?: return
        coroutineScope.launch {
            zyHistoryLoading = true
            zyHistoryError = ""
            val result = withContext(Dispatchers.IO) {
                val links = ZonayummyReelHistoryApi.listUserLinks(token)
                val folders = ZonayummyReelHistoryApi.listFolders(token)
                val labels = ZonayummyReelHistoryApi.listLabels(token)
                Triple(links, folders, labels)
            }
            when (val linksRes = result.first) {
                is ZonayummyReelHistoryApi.ApiResult.Ok -> zyItems = linksRes.value.items
                is ZonayummyReelHistoryApi.ApiResult.Err -> zyHistoryError = linksRes.message
            }
            when (val foldersRes = result.second) {
                is ZonayummyReelHistoryApi.ApiResult.Ok -> zyFolders = foldersRes.value
                is ZonayummyReelHistoryApi.ApiResult.Err -> if (zyHistoryError.isBlank()) zyHistoryError = foldersRes.message
            }
            when (val labelsRes = result.third) {
                is ZonayummyReelHistoryApi.ApiResult.Ok -> zyLabels = labelsRes.value
                is ZonayummyReelHistoryApi.ApiResult.Err -> if (zyHistoryError.isBlank()) zyHistoryError = labelsRes.message
            }
            zyHistoryLoading = false
        }
    }

    fun descendantFolderIds(rootId: String, folders: List<ZyFolder>): Set<String> {
        val byParent = folders.groupBy { it.parentId }
        val out = mutableSetOf<String>()
        val stack = ArrayDeque<String>()
        stack.add(rootId)
        while (stack.isNotEmpty()) {
            val cur = stack.removeLast()
            if (!out.add(cur)) continue
            val children = byParent[cur].orEmpty()
            children.forEach { stack.add(it.id) }
        }
        return out
    }

    fun canDeleteFolder(folderId: String): Boolean {
        val ids = descendantFolderIds(folderId, zyFolders)
        return zyItems.none { it.folderId != null && ids.contains(it.folderId) }
    }

    suspend fun uploadFileToBlob(
        uploadUrlWithSas: String,
        file: File,
        contentType: String,
        onProgress: (Int) -> Unit,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(uploadUrlWithSas).openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                doOutput = true
                doInput = true
                setRequestProperty("x-ms-blob-type", "BlockBlob")
                setRequestProperty("Content-Type", contentType)
                connectTimeout = 60000
                readTimeout = 60000
                setFixedLengthStreamingMode(file.length())
            }

            var sent = 0L
            val total = file.length().coerceAtLeast(1L)

            file.inputStream().use { input ->
                BufferedOutputStream(conn.outputStream).use { out ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val r = input.read(buffer)
                        if (r <= 0) break
                        out.write(buffer, 0, r)
                        sent += r
                        val p = ((sent * 100) / total).toInt().coerceIn(0, 100)
                        onProgress(p)
                    }
                    out.flush()
                }
            }

            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.readText()?.take(300) ?: ""
                return@withContext Result.failure(IllegalStateException("Error subiendo a Blob ($code) $err".trim()))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DownloadReelsHistoryDrawer(
                zySession = zySession,
                loading = zyHistoryLoading,
                error = zyHistoryError,
                folders = zyFolders,
                labels = zyLabels,
                items = zyItems,
                folderFilter = folderFilter,
                selectedIds = selectedIds,
                onSelectFolder = { folderFilter = it },
                onRefresh = { refreshHistory() },
                onCreateFolder = { showCreateFolder = true },
                onDeleteSelectedFolder = {
                    val fid = (folderFilter as? ZyFolderFilter.Folder)?.id
                    val session = zySession
                    if (fid == null || session == null) return@DownloadReelsHistoryDrawer
                    if (!canDeleteFolder(fid)) {
                        Toast.makeText(context, "No se puede eliminar: hay links en esta carpeta", Toast.LENGTH_LONG).show()
                        return@DownloadReelsHistoryDrawer
                    }
                    coroutineScope.launch {
                        val res = withContext(Dispatchers.IO) { ZonayummyReelHistoryApi.deleteFolder(session.token, fid) }
                        when (res) {
                            is ZonayummyReelHistoryApi.ApiResult.Ok -> {
                                Toast.makeText(context, "Carpeta eliminada", Toast.LENGTH_SHORT).show()
                                folderFilter = ZyFolderFilter.Unassigned
                                refreshHistory()
                            }
                            is ZonayummyReelHistoryApi.ApiResult.Err -> {
                                Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                onToggleSelect = { id ->
                    selectedIds = if (selectedIds.contains(id)) selectedIds - id else selectedIds + id
                },
                onClearSelection = { selectedIds = emptySet() },
                onMoveSelected = { showMoveDialog = true },
                onDeleteSelected = { showDeleteDialog = true },
                onCopyUrl = { url ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("url", url))
                    Toast.makeText(context, "Link copiado", Toast.LENGTH_SHORT).show()
                },
                onOpenUrl = { url ->
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(context, "No hay app para abrir este link", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "No se pudo abrir: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                },
                onDownloadItem = { item ->
                    val u = item.downloadUrlWithSas
                    if (u.isNullOrBlank()) {
                        Toast.makeText(context, "Aún no hay URL de descarga (SAS)", Toast.LENGTH_SHORT).show()
                        return@DownloadReelsHistoryDrawer
                    }
                    try {
                        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        val req = DownloadManager.Request(Uri.parse(u)).apply {
                            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            setAllowedOverMetered(true)
                            setAllowedOverRoaming(true)
                            val filename = "${item.platform}_${item.id.takeLast(10)}.mp4"
                            setTitle(filename)
                            setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_MOVIES,
                                "NRXGoAm/$filename"
                            )
                        }
                        dm.enqueue(req)
                        Toast.makeText(context, "Descargando… revisa notificaciones", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "No se pudo iniciar descarga: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                },
                onEditItem = { editingItem = it }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                DownloadReelsTopBar(
                    onBackClick = onBackClick,
                    zySession = zySession,
                    onOpenHistory = {
                        coroutineScope.launch {
                            drawerState.open()
                            if (zySession != null && zyItems.isEmpty()) refreshHistory()
                        }
                    },
                    onOpenLogin = { showLogin = true },
                    onLogout = {
                        ZonayummySessionStore.clearSession(context)
                        zySession = null
                        zyItems = emptyList()
                        zyFolders = emptyList()
                        zyLabels = emptyList()
                    folderFilter = ZyFolderFilter.Unassigned
                        selectedIds = emptySet()
                        Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        ) { innerPadding ->
            DownloadReelsScreen(
                modifier = Modifier.padding(innerPadding),
                zySession = zySession,
                onRequireLogin = { showLogin = true },
                onCloudSaveRequested = { url, platform ->
                    if (zySession == null) {
                        pendingCloudSave = url to platform
                        showLogin = true
                    }
                },
                onCloudSave = { url, platform, setState ->
                    val session = zySession ?: run {
                        setState(DownloadState.Error("Necesitas iniciar sesión para guardar en la nube"))
                        return@DownloadReelsScreen
                    }
                    coroutineScope.launch {
                        setState(DownloadState.CloudSaving(0, "Descargando video…"))
                        val temp = ReelDownloaderService.downloadVideoToCacheFile(
                            context = context,
                            videoUrl = url,
                            platform = platform,
                            onProgress = { p -> setState(DownloadState.CloudSaving((p * 0.4).toInt(), "Descargando video… $p%")) }
                        )
                        if (temp.isFailure) {
                            setState(DownloadState.Error(temp.exceptionOrNull()?.message ?: "Error descargando"))
                            return@launch
                        }
                        val cache = temp.getOrThrow()

                        try {
                            setState(DownloadState.CloudSaving(45, "Generando URL de subida…"))
                            val uploadUrlRes = withContext(Dispatchers.IO) {
                                ZonayummyReelHistoryApi.getUploadUrl(
                                    token = session.token,
                                    platform = platform.name.lowercase(),
                                    url = url,
                                    contentType = cache.contentType
                                )
                            }
                            val upload = when (uploadUrlRes) {
                                is ZonayummyReelHistoryApi.ApiResult.Ok -> uploadUrlRes.value
                                is ZonayummyReelHistoryApi.ApiResult.Err -> {
                                    setState(DownloadState.Error(uploadUrlRes.message))
                                    return@launch
                                }
                            }

                            setState(DownloadState.CloudSaving(55, "Subiendo a la nube…"))
                            val up = uploadFileToBlob(
                                uploadUrlWithSas = upload.uploadUrlWithSas,
                                file = cache.file,
                                contentType = cache.contentType,
                                onProgress = { p -> setState(DownloadState.CloudSaving(55 + (p * 0.35).toInt(), "Subiendo… $p%")) }
                            )
                            if (up.isFailure) {
                                setState(DownloadState.Error(up.exceptionOrNull()?.message ?: "Error subiendo a Blob"))
                                return@launch
                            }

                            setState(DownloadState.CloudSaving(92, "Guardando registro…"))
                            val created = withContext(Dispatchers.IO) {
                                ZonayummyReelHistoryApi.createRecord(
                                    token = session.token,
                                    downloadId = upload.downloadId,
                                    platform = upload.platform,
                                    url = upload.url,
                                    blobPath = upload.blobPath,
                                    contentType = upload.contentType,
                                    size = cache.sizeBytes
                                )
                            }
                            when (created) {
                                is ZonayummyReelHistoryApi.ApiResult.Err -> {
                                    setState(DownloadState.Error(created.message))
                                    return@launch
                                }
                                else -> {}
                            }

                            // thumbnail en background (no bloquea el éxito)
                            withContext(Dispatchers.IO) {
                                ZonayummyReelHistoryApi.generateThumbnail(session.token, upload.downloadId)
                            }

                            setState(DownloadState.CloudSuccess("✅ Guardado en la nube"))
                            refreshHistory()
                        } finally {
                            try { cache.file.delete() } catch (_: Exception) {}
                        }
                    }
                },
                onRefreshHistory = { refreshHistory() },
                onUploadFileToBlob = { u, f, c, p -> uploadFileToBlob(u, f, c, p) }
            )
        }
    }

    if (showLogin) {
        ZyLoginDialog(
            onDismiss = { showLogin = false },
            onLoggedIn = { token, username ->
                val session = ZonayummySession(token = token, username = username)
                ZonayummySessionStore.saveSession(context, session)
                zySession = session
                showLogin = false
                Toast.makeText(context, "Sesión iniciada: $username", Toast.LENGTH_SHORT).show()

                // ejecutar acción pendiente
                val pending = pendingCloudSave
                pendingCloudSave = null
                if (pending != null) {
                    Toast.makeText(context, "Listo. Ahora puedes guardar en la nube.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showMoveDialog && zySession != null) {
        ZyMoveDialog(
            folders = zyFolders,
            onDismiss = { showMoveDialog = false },
            onMove = { folderId ->
                val ids = selectedIds.toList()
                val token = zySession!!.token
                coroutineScope.launch {
                    val res = withContext(Dispatchers.IO) { ZonayummyReelHistoryApi.moveItems(token, ids, folderId) }
                    when (res) {
                        is ZonayummyReelHistoryApi.ApiResult.Ok -> {
                            Toast.makeText(context, "Movidos: ${res.value}", Toast.LENGTH_SHORT).show()
                            selectedIds = emptySet()
                            showMoveDialog = false
                            refreshHistory()
                        }
                        is ZonayummyReelHistoryApi.ApiResult.Err -> Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    if (editingItem != null && zySession != null) {
        ZyEditItemDialog(
            item = editingItem!!,
            folders = zyFolders,
            labels = zyLabels,
            onDismiss = { editingItem = null },
            onSave = { updates ->
                val token = zySession!!.token
                val prevLabelIds = editingItem?.labelIds ?: emptyList()
                coroutineScope.launch {
                    val newLabelNames = updates.newLabelNames
                    val createdLabels = withContext(Dispatchers.IO) {
                        ZonayummyReelHistoryApi.createLabels(token, newLabelNames)
                    }
                    val created = when (createdLabels) {
                        is ZonayummyReelHistoryApi.ApiResult.Ok -> createdLabels.value
                        is ZonayummyReelHistoryApi.ApiResult.Err -> {
                            Toast.makeText(context, createdLabels.message, Toast.LENGTH_LONG).show()
                            emptyList()
                        }
                    }

                    if (created.isNotEmpty()) {
                        // Merge labels creadas (o existentes) a la lista local para que el counter aplique
                        val byId = zyLabels.associateBy { it.id }.toMutableMap()
                        created.forEach { byId[it.id] = it }
                        zyLabels = byId.values.toList()
                    }

                    val finalLabelIds = (updates.labelIds + created.map { it.id }).distinct()

                    val res = withContext(Dispatchers.IO) {
                        ZonayummyReelHistoryApi.updateItem(
                            token = token,
                            downloadId = updates.downloadId,
                            folderId = updates.folderId,
                            labelIds = finalLabelIds,
                            description = updates.description,
                            title = updates.title,
                        )
                    }
                    when (res) {
                        is ZonayummyReelHistoryApi.ApiResult.Ok -> {
                            // Ajuste local de usageCount (igual que Angular / backend diff)
                            applyLabelUsageDiff(prevLabelIds, finalLabelIds)

                            Toast.makeText(context, "Actualizado", Toast.LENGTH_SHORT).show()
                            // Actualizar item local rápido
                            zyItems = zyItems.map { if (it.id == res.value.id) res.value else it }
                            editingItem = null
                            refreshHistory()
                        }
                        is ZonayummyReelHistoryApi.ApiResult.Err -> Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    if (showCreateFolder && zySession != null) {
        ZyCreateFolderDialog(
            onDismiss = { showCreateFolder = false },
            onCreate = { name, icon ->
                val token = zySession!!.token
                coroutineScope.launch {
                    val res = withContext(Dispatchers.IO) { ZonayummyReelHistoryApi.createFolder(token, name = name, icon = icon) }
                    when (res) {
                        is ZonayummyReelHistoryApi.ApiResult.Ok -> {
                            Toast.makeText(context, "Carpeta creada", Toast.LENGTH_SHORT).show()
                            showCreateFolder = false
                            refreshHistory()
                        }
                        is ZonayummyReelHistoryApi.ApiResult.Err -> Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    if (showDeleteDialog && zySession != null) {
        val count = selectedIds.size
        AlertDialog(
            onDismissRequest = { if (!deleteLoading) showDeleteDialog = false },
            title = { Text("Eliminar $count link(s)?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Esto eliminará el/los registro(s) del historial y sus blobs asociados (si existen).",
                        fontSize = 12.sp,
                        color = CeoStyle.Muted
                    )
                    if (deleteLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Eliminando…", fontSize = 12.sp, color = CeoStyle.Muted)
                        }
                    }
                }
            },
            containerColor = CeoStyle.Surface,
            titleContentColor = CeoStyle.Text,
            textContentColor = CeoStyle.Text,
            confirmButton = {
                Button(
                    enabled = !deleteLoading,
                    onClick = {
                        val token = zySession!!.token
                        val ids = selectedIds.toList()
                        val itemsToDelete = zyItems.filter { selectedIds.contains(it.id) }
                        deleteLoading = true
                        coroutineScope.launch {
                            var deletedCount = 0
                            var firstErr: String? = null
                            val res = withContext(Dispatchers.IO) {
                                ids.map { id ->
                                    id to ZonayummyReelHistoryApi.deleteUserLink(token, id, deleteBlob = true)
                                }
                            }
                            res.forEach { (_, r) ->
                                when (r) {
                                    is ZonayummyReelHistoryApi.ApiResult.Ok -> if (r.value) deletedCount++
                                    is ZonayummyReelHistoryApi.ApiResult.Err -> if (firstErr == null) firstErr = r.message
                                }
                            }
                            deleteLoading = false
                            showDeleteDialog = false

                            // Ajuste local de usageCount (similar a Angular + backend)
                            itemsToDelete.forEach { it ->
                                it.labelIds.distinct().forEach { lid ->
                                    bumpLabelUsage(lid, -1)
                                }
                            }

                            // Actualizar lista local rápido (antes de refrescar)
                            zyItems = zyItems.filterNot { selectedIds.contains(it.id) }
                            selectedIds = emptySet()
                            if (firstErr != null) {
                                Toast.makeText(context, "Error eliminando: $firstErr", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Eliminados: $deletedCount", Toast.LENGTH_SHORT).show()
                            }
                            refreshHistory()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CeoStyle.Danger, contentColor = CeoStyle.Surface)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(enabled = !deleteLoading, onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun DownloadReelsScreen(
    modifier: Modifier = Modifier,
    zySession: ZonayummySession?,
    onRequireLogin: () -> Unit,
    onCloudSaveRequested: (String, ReelDownloaderService.Platform) -> Unit,
    onCloudSave: (String, ReelDownloaderService.Platform, (DownloadState) -> Unit) -> Unit,
    onRefreshHistory: () -> Unit,
    onUploadFileToBlob: suspend (String, File, String, (Int) -> Unit) -> Result<Unit>,
) {
    var selectedTabIndex by remember { mutableStateOf(0) } // Instagram primero
    var linkText by remember { mutableStateOf("") }
    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val currentPlatform = platforms[selectedTabIndex]
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CeoStyle.Bg)
    ) {
        // Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = CeoStyle.Surface,
            contentColor = CeoStyle.Text,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    height = 3.dp,
                    color = currentPlatform.primaryColor
                )
            }
        ) {
            platforms.forEachIndexed { index, platform ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { 
                        selectedTabIndex = index
                        linkText = ""
                        downloadState = DownloadState.Idle
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = platform.name,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) CeoStyle.Cyan else CeoStyle.Muted
                            )
                            if (!platform.isAvailable) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "🔒",
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Contenido principal
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Login hint (ZonaYummy)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CeoStyle.Surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, CeoStyle.Border)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (zySession == null) {
                        Text(
                            text = "Modo invitado: puedes descargar, pero no se guardará en tu cuenta.",
                            color = CeoStyle.Muted,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onRequireLogin,
                            colors = ButtonDefaults.buttonColors(containerColor = CeoStyle.Surface, contentColor = CeoStyle.Text),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CeoStyle.Border2)
                        ) { Text("Iniciar sesión") }
                    } else {
                        Text(
                            text = "Sesión ZonaYummy: ${zySession.username}",
                            color = CeoStyle.Text,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Historial disponible",
                            color = CeoStyle.Muted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Título de la plataforma seleccionada
            Text(
                text = "Descargar de ${currentPlatform.name}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = CeoStyle.Text,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Mensaje si no está disponible
            if (!currentPlatform.isAvailable) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CeoStyle.Surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CeoStyle.Border)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = CeoStyle.Cyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Próximamente disponible para ${currentPlatform.name}",
                            color = CeoStyle.Text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                Text(
                    text = "Pega el enlace del video que deseas descargar",
                    fontSize = 14.sp,
                    color = CeoStyle.Muted,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Campo de texto con botón de pegar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CeoStyle.Surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = linkText,
                        onValueChange = { 
                            linkText = it
                            if (downloadState is DownloadState.Error || downloadState is DownloadState.Success) {
                                downloadState = DownloadState.Idle
                            }
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                text = currentPlatform.placeholder,
                                color = CeoStyle.Placeholder,
                                fontSize = 12.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CeoStyle.Text,
                            unfocusedTextColor = CeoStyle.Text,
                            cursorColor = CeoStyle.Cyan,
                            focusedBorderColor = CeoStyle.Cyan,
                            unfocusedBorderColor = CeoStyle.Border2,
                            focusedContainerColor = CeoStyle.Bg,
                            unfocusedContainerColor = CeoStyle.Bg
                        ),
                        singleLine = true,
                        enabled = currentPlatform.isAvailable && downloadState !is DownloadState.Downloading
                    )
                    
                    // Botón de pegar
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipData = clipboard.primaryClip
                            if (clipData != null && clipData.itemCount > 0) {
                                val pastedText = clipData.getItemAt(0).text?.toString() ?: ""
                                linkText = pastedText
                                downloadState = DownloadState.Idle
                                Toast.makeText(context, "Enlace pegado", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Portapapeles vacío", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (currentPlatform.isAvailable) CeoStyle.CyanBg else CeoStyle.Bg
                            )
                            .size(48.dp),
                        enabled = currentPlatform.isAvailable && downloadState !is DownloadState.Downloading
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_paste),
                            contentDescription = "Pegar",
                            tint = CeoStyle.Cyan
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Estado de descarga
            AnimatedVisibility(
                visible = downloadState !is DownloadState.Idle,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                when (val state = downloadState) {
                    is DownloadState.Downloading -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = CeoStyle.Surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CeoStyle.Border)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Descargando...",
                                    color = CeoStyle.Text,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { state.progress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = CeoStyle.Cyan,
                                    trackColor = CeoStyle.Border
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${state.progress}%",
                                    color = CeoStyle.Muted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    is DownloadState.CloudSaving -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CeoStyle.Surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CeoStyle.Border)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = state.message, color = CeoStyle.Text, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { state.progress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = CeoStyle.Cyan,
                                    trackColor = CeoStyle.Border
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${state.progress}%",
                                    color = CeoStyle.Muted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    is DownloadState.CloudSuccess -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CeoStyle.SuccessBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CeoStyle.Border)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = CeoStyle.SuccessText,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(state.message, color = CeoStyle.SuccessText, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    is DownloadState.Success -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = CeoStyle.SuccessBg
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CeoStyle.Border)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = CeoStyle.SuccessText,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "¡Descarga completada!",
                                        color = CeoStyle.SuccessText,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Guardado en: Movies/NRXGoAm",
                                        color = CeoStyle.Muted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                    is DownloadState.Error -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = CeoStyle.DangerBg
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CeoStyle.Border)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = CeoStyle.Danger,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Error en la descarga",
                                        color = CeoStyle.Danger,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = state.message,
                                        color = CeoStyle.Muted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
            
            val platformEnum = when (currentPlatform.name) {
                "Instagram" -> ReelDownloaderService.Platform.INSTAGRAM
                "Facebook" -> ReelDownloaderService.Platform.FACEBOOK
                "YouTube" -> ReelDownloaderService.Platform.YOUTUBE
                "TikTok" -> ReelDownloaderService.Platform.TIKTOK
                else -> ReelDownloaderService.Platform.INSTAGRAM
            }

            // Botones: Guardar en la nube + Descargar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val url = linkText.trim()
                        if (url.isBlank()) {
                            Toast.makeText(context, "Por favor, pega un enlace primero", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (!currentPlatform.isAvailable) {
                            Toast.makeText(context, "${currentPlatform.name} no está disponible aún", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (zySession == null) {
                            onCloudSaveRequested(url, platformEnum)
                            return@Button
                        }
                        onCloudSave(url, platformEnum) { st -> downloadState = st }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CeoStyle.Surface,
                        contentColor = CeoStyle.Text
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CeoStyle.Border2),
                    enabled = downloadState !is DownloadState.Downloading && downloadState !is DownloadState.CloudSaving
                ) {
                    Text("Guardar en la nube", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val url = linkText.trim()
                        if (url.isBlank() || !currentPlatform.isAvailable) {
                            Toast.makeText(context, "Por favor, pega un enlace válido", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val session = zySession
                        // Si hay sesión: Descargar + subir a la nube. Si no: solo descarga local.
                        if (session == null) {
                            downloadState = DownloadState.Downloading(0)
                            coroutineScope.launch {
                                val result = ReelDownloaderService.downloadVideo(
                                    context = context,
                                    videoUrl = url,
                                    platform = platformEnum,
                                    onProgress = { progress -> downloadState = DownloadState.Downloading(progress) }
                                )
                                downloadState = when (result) {
                                    is ReelDownloaderService.DownloadResult.Success -> DownloadState.Success(result.fileName)
                                    is ReelDownloaderService.DownloadResult.Error -> DownloadState.Error(result.message)
                                    else -> DownloadState.Idle
                                }
                            }
                            return@Button
                        }

                        coroutineScope.launch {
                            downloadState = DownloadState.CloudSaving(0, "Descargando video…")
                            val temp = ReelDownloaderService.downloadVideoToCacheFile(
                                context = context,
                                videoUrl = url,
                                platform = platformEnum,
                                onProgress = { p ->
                                    downloadState = DownloadState.CloudSaving((p * 0.4).toInt(), "Descargando video… $p%")
                                }
                            )
                            if (temp.isFailure) {
                                downloadState = DownloadState.Error(temp.exceptionOrNull()?.message ?: "Error descargando")
                                return@launch
                            }
                            val cache = temp.getOrThrow()

                            try {
                                // Guardar local desde cache (sin volver a descargar)
                                downloadState = DownloadState.CloudSaving(42, "Guardando en el teléfono…")
                                val saved = withContext(Dispatchers.IO) {
                                    saveCacheVideoToDevice(
                                        context = context,
                                        cacheFile = cache.file,
                                        fileName = cache.fileName,
                                        contentType = cache.contentType
                                    )
                                }
                                if (saved.isFailure) {
                                    // No abortar la nube si falla guardar local, pero avisar.
                                    Toast.makeText(context, saved.exceptionOrNull()?.message ?: "No se pudo guardar en el teléfono", Toast.LENGTH_LONG).show()
                                }

                                downloadState = DownloadState.CloudSaving(45, "Generando URL de subida…")
                                val uploadUrlRes = withContext(Dispatchers.IO) {
                                    ZonayummyReelHistoryApi.getUploadUrl(
                                        token = session.token,
                                        platform = platformEnum.name.lowercase(),
                                        url = url,
                                        contentType = cache.contentType
                                    )
                                }
                                val upload = when (uploadUrlRes) {
                                    is ZonayummyReelHistoryApi.ApiResult.Ok -> uploadUrlRes.value
                                    is ZonayummyReelHistoryApi.ApiResult.Err -> {
                                        downloadState = DownloadState.Error(uploadUrlRes.message)
                                        return@launch
                                    }
                                }

                                downloadState = DownloadState.CloudSaving(55, "Subiendo a la nube…")
                                val up = onUploadFileToBlob(
                                    upload.uploadUrlWithSas,
                                    cache.file,
                                    cache.contentType
                                ) { p ->
                                        downloadState = DownloadState.CloudSaving(55 + (p * 0.35).toInt(), "Subiendo… $p%")
                                    }
                                if (up.isFailure) {
                                    downloadState = DownloadState.Error(up.exceptionOrNull()?.message ?: "Error subiendo a Blob")
                                    return@launch
                                }

                                downloadState = DownloadState.CloudSaving(92, "Guardando registro…")
                                val created = withContext(Dispatchers.IO) {
                                    ZonayummyReelHistoryApi.createRecord(
                                        token = session.token,
                                        downloadId = upload.downloadId,
                                        platform = upload.platform,
                                        url = upload.url,
                                        blobPath = upload.blobPath,
                                        contentType = upload.contentType,
                                        size = cache.sizeBytes
                                    )
                                }
                                when (created) {
                                    is ZonayummyReelHistoryApi.ApiResult.Err -> {
                                        downloadState = DownloadState.Error(created.message)
                                        return@launch
                                    }
                                    else -> {}
                                }

                                withContext(Dispatchers.IO) {
                                    ZonayummyReelHistoryApi.generateThumbnail(session.token, upload.downloadId)
                                }

                                downloadState = DownloadState.CloudSuccess("✅ Descargado y guardado en la nube")
                                onRefreshHistory()
                            } finally {
                                try { cache.file.delete() } catch (_: Exception) {}
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CeoStyle.Cyan,
                        contentColor = CeoStyle.Surface,
                        disabledContainerColor = CeoStyle.Border
                    ),
                    enabled = linkText.isNotBlank() &&
                        currentPlatform.isAvailable &&
                        downloadState !is DownloadState.Downloading &&
                        downloadState !is DownloadState.CloudSaving
                ) {
                    Text("Descargar", fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Información adicional
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CeoStyle.Surface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, CeoStyle.Border)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "💡 Cómo usar:",
                        fontWeight = FontWeight.Bold,
                        color = CeoStyle.Text,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (currentPlatform.isAvailable) {
                            "1. Abre ${currentPlatform.name} y busca el video\n" +
                            "2. Copia el enlace del video\n" +
                            "3. Toca el botón de pegar o pégalo manualmente\n" +
                            "4. Presiona \"Descargar Video\"\n" +
                            "5. El video se guardará en Movies/NRXGoAm"
                        } else {
                            "Esta plataforma aún no está disponible.\n" +
                            "Por ahora solo puedes descargar de Instagram.\n\n" +
                            "¡Pronto añadiremos soporte para ${currentPlatform.name}!"
                        },
                        color = CeoStyle.Muted,
                        fontSize = 12.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Drawer Historial
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DownloadReelsHistoryDrawer(
    zySession: ZonayummySession?,
    loading: Boolean,
    error: String,
    folders: List<ZyFolder>,
    labels: List<ZyLabel>,
    items: List<ZyHistoryItem>,
    folderFilter: ZyFolderFilter,
    selectedIds: Set<String>,
    onSelectFolder: (ZyFolderFilter) -> Unit,
    onRefresh: () -> Unit,
    onCreateFolder: () -> Unit,
    onDeleteSelectedFolder: () -> Unit,
    onToggleSelect: (String) -> Unit,
    onClearSelection: () -> Unit,
    onMoveSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onCopyUrl: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onDownloadItem: (ZyHistoryItem) -> Unit,
    onEditItem: (ZyHistoryItem) -> Unit,
) {
    ModalDrawerSheet(
        modifier = Modifier.widthIn(max = 380.dp),
        drawerContainerColor = CeoStyle.Surface
    ) {
        Column(modifier = Modifier.fillMaxHeight().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Historial",
                    color = CeoStyle.Text,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = CeoStyle.Muted)
                }
            }

            if (zySession == null) {
                Text(
                    text = "Inicia sesión para ver y administrar tu historial.",
                    color = CeoStyle.Muted,
                    fontSize = 12.sp
                )
                return@Column
            }

            if (selectedIds.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Seleccionados: ${selectedIds.size}",
                        color = CeoStyle.Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )

                    // Acciones compactas (estilo CEO)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = onClearSelection,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = CeoStyle.Muted)
                            Spacer(Modifier.width(6.dp))
                            Text("Cancelar", color = CeoStyle.Text, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onMoveSelected,
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = CeoStyle.Surface,
                                contentColor = CeoStyle.Text
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CeoStyle.Border2)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_drive_file_move),
                                contentDescription = null,
                                tint = CeoStyle.Muted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Mover", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onDeleteSelected,
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = CeoStyle.Surface,
                                contentColor = CeoStyle.Danger
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CeoStyle.Danger)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = CeoStyle.Danger,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Eliminar", fontSize = 12.sp)
                        }
                    }
                }
            }

            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }
            if (error.isNotBlank()) {
                Text(text = error, color = CeoStyle.Danger, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
            }

            // Folders (chips)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CARPETAS",
                    color = CeoStyle.Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onCreateFolder) {
                    Icon(Icons.Default.Add, contentDescription = "Crear carpeta", tint = CeoStyle.Muted)
                }
                if (folderFilter is ZyFolderFilter.Folder) {
                    IconButton(onClick = onDeleteSelectedFolder) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar carpeta", tint = CeoStyle.Danger)
                    }
                }
            }

            val scroll = rememberScrollState()
            val countsByFolderId = remember(items) {
                val map = mutableMapOf<String, Int>()
                for (it in items) {
                    val fid = it.folderId ?: continue
                    map[fid] = (map[fid] ?: 0) + 1
                }
                map
            }
            val unassignedCount = remember(items) { items.count { it.folderId == null } }
            val allCount = remember(items) { items.size }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scroll)
            ) {
                FilterChip(
                    selected = folderFilter is ZyFolderFilter.Unassigned,
                    onClick = { onSelectFolder(ZyFolderFilter.Unassigned) },
                    label = { Text("Sin carpeta ($unassignedCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = CeoStyle.Bg,
                        labelColor = CeoStyle.Text,
                        selectedContainerColor = CeoStyle.CyanBg,
                        selectedLabelColor = CeoStyle.Text
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = folderFilter is ZyFolderFilter.Unassigned,
                        borderColor = CeoStyle.Border2,
                        selectedBorderColor = CeoStyle.Cyan
                    )
                )
                FilterChip(
                    selected = folderFilter is ZyFolderFilter.All,
                    onClick = { onSelectFolder(ZyFolderFilter.All) },
                    label = { Text("Todos ($allCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = CeoStyle.Bg,
                        labelColor = CeoStyle.Text,
                        selectedContainerColor = CeoStyle.CyanBg,
                        selectedLabelColor = CeoStyle.Text
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = folderFilter is ZyFolderFilter.All,
                        borderColor = CeoStyle.Border2,
                        selectedBorderColor = CeoStyle.Cyan
                    )
                )
                folders.sortedBy { it.order }.forEach { f ->
                    val c = countsByFolderId[f.id] ?: 0
                    val isSelected = folderFilter is ZyFolderFilter.Folder && (folderFilter as ZyFolderFilter.Folder).id == f.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectFolder(ZyFolderFilter.Folder(f.id)) },
                        label = { Text((f.icon?.takeIf { it.isNotBlank() } ?: "📁") + " " + f.name + " ($c)") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = CeoStyle.Bg,
                            labelColor = CeoStyle.Text,
                            selectedContainerColor = CeoStyle.CyanBg,
                            selectedLabelColor = CeoStyle.Text
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = CeoStyle.Border2,
                            selectedBorderColor = CeoStyle.Cyan
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            val filtered = remember(items, folderFilter) {
                when (val f = folderFilter) {
                    is ZyFolderFilter.All -> items
                    is ZyFolderFilter.Unassigned -> items.filter { it.folderId == null }
                    is ZyFolderFilter.Folder -> items.filter { it.folderId == f.id }
                }
            }

            if (!loading && filtered.isEmpty()) {
                Text(
                    text = if (items.isEmpty()) "Aún no tienes descargas guardadas." else "No hay descargas en esta carpeta.",
                    color = CeoStyle.Muted,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            // Lista scrolleable (vertical)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(
                    items = filtered,
                    key = { item -> item.id },
                    contentType = { "history_item" }
                ) { item ->
                    ZyHistoryItemCard(
                        item = item,
                        folders = folders,
                        labels = labels,
                        selected = selectedIds.contains(item.id),
                        onToggleSelect = { onToggleSelect(item.id) },
                        onCopy = { onCopyUrl(item.url) },
                        onOpen = { onOpenUrl(item.url) },
                        onDownload = { onDownloadItem(item) },
                        onEdit = { onEditItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ZyHistoryItemCard(
    item: ZyHistoryItem,
    folders: List<ZyFolder>,
    labels: List<ZyLabel>,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onCopy: () -> Unit,
    onOpen: () -> Unit,
    onDownload: () -> Unit,
    onEdit: () -> Unit,
) {
    val thumb = rememberRemoteBitmap(item.thumbnailUrlWithSas)
    val title = item.title?.takeIf { it.isNotBlank() }
    val urlText = item.url
    val folder = remember(item.folderId, folders) { folders.firstOrNull { it.id == item.folderId } }
    val folderLabel = remember(folder) {
        if (folder == null) "📂 Sin carpeta"
        else "${folder.icon?.takeIf { it.isNotBlank() } ?: "📁"} ${folder.name}"
    }
    val platformLabel = item.platform.uppercase()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (selected) onToggleSelect() },
                onLongClick = onToggleSelect
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) CeoStyle.CyanBg else CeoStyle.Surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, CeoStyle.Border),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Fila superior: thumbnail (1/6) + texto (5/6)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CeoStyle.Bg),
                        contentAlignment = Alignment.Center
                    ) {
                        if (thumb != null) {
                            androidx.compose.foundation.Image(
                                bitmap = thumb,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = item.platform.take(1).uppercase(),
                                color = CeoStyle.Muted,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = formatDateOnly(item.createdAt),
                        color = CeoStyle.Placeholder,
                        fontSize = 10.sp,
                        lineHeight = 11.sp
                    )
                    Text(
                        text = formatTimeAmPm(item.createdAt),
                        color = CeoStyle.Placeholder,
                        fontSize = 10.sp,
                        lineHeight = 11.sp
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(5f)) {
                    Text(
                        text = title ?: urlText,
                        color = CeoStyle.Text,
                        fontSize = 13.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (title != null) {
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = urlText,
                            color = CeoStyle.Muted,
                            fontSize = 11.sp,
                            lineHeight = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "$folderLabel • $platformLabel",
                        color = CeoStyle.Muted,
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val labelObjs = remember(item.labelIds, labels) {
                        val map = labels.associateBy { it.id }
                        item.labelIds.mapNotNull { map[it] }.take(8)
                    }
                    if (labelObjs.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(end = 8.dp)
                        ) {
                            items(items = labelObjs, key = { it.id }) { l ->
                                ZyLabelPill(label = l)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Fila inferior: botones al final de la tarjeta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onCopy) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_content_copy),
                        contentDescription = "Copiar",
                        tint = CeoStyle.Muted
                    )
                }
                IconButton(onClick = onOpen) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_open_in_new),
                        contentDescription = "Abrir",
                        tint = CeoStyle.Muted
                    )
                }
                IconButton(onClick = onDownload) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_download),
                        contentDescription = "Descargar",
                        tint = CeoStyle.Muted
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = CeoStyle.Muted)
                }
            }
        }
    }
}

@Composable
private fun ZyLabelPill(label: ZyLabel) {
    val bg = remember(label.color) { parseHexColorOrNull(label.color) ?: CeoStyle.Bg }
    val fg = remember(bg) { contrastTextColor(bg) }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label.name,
            fontSize = 10.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Medium,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun parseHexColorOrNull(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    val h = hex.trim()
    return try {
        when {
            h.startsWith("#") -> Color(android.graphics.Color.parseColor(h))
            else -> Color(android.graphics.Color.parseColor("#$h"))
        }
    } catch (_: Exception) {
        null
    }
}

private fun contrastTextColor(bg: Color): Color {
    // luminance aproximada para decidir negro/gris oscuro vs blanco
    val lum = (0.299f * bg.red + 0.587f * bg.green + 0.114f * bg.blue)
    return if (lum > 0.6f) Color(0xFF374151) else Color.White
}

@Composable
private fun rememberRemoteBitmap(url: String?): ImageBitmap? {
    if (url.isNullOrBlank()) return null
    val cached = remember(url) { RemoteBitmapMemoryCache.get(url) }
    var bmp by remember(url) { mutableStateOf<ImageBitmap?>(cached) }

    LaunchedEffect(url) {
        if (bmp != null) return@LaunchedEffect

        val decoded = withContext(Dispatchers.IO) {
            try {
                val bytes = URL(url).openStream().use { it.readBytes() }
                decodeDownsampledImage(bytes, targetPx = 180)
            } catch (_: Exception) {
                null
            }
        }

        if (decoded != null) {
            RemoteBitmapMemoryCache.put(url, decoded)
            bmp = decoded
        }
    }

    return bmp
}

private object RemoteBitmapMemoryCache {
    private val cache = object : LruCache<String, ImageBitmap>(80) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = 1
    }

    fun get(url: String): ImageBitmap? = cache.get(url)
    fun put(url: String, bmp: ImageBitmap) {
        cache.put(url, bmp)
    }
}

private fun decodeDownsampledImage(bytes: ByteArray, targetPx: Int): ImageBitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        val sample = calculateInSampleSize(bounds, targetPx, targetPx)
        val opts = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return null
        bmp.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (h, w) = options.outHeight to options.outWidth
    var inSampleSize = 1
    if (h > reqHeight || w > reqWidth) {
        var halfH = h / 2
        var halfW = w / 2
        while (halfH / inSampleSize >= reqHeight && halfW / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize.coerceAtLeast(1)
}

private fun formatDate(ts: Long): String {
    if (ts <= 0) return ""
    return try {
        SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date(ts))
    } catch (_: Exception) {
        ""
    }
}

private fun formatDateOnly(ts: Long): String {
    if (ts <= 0) return ""
    return try {
        SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(ts))
    } catch (_: Exception) {
        ""
    }
}

private fun formatTimeAmPm(ts: Long): String {
    if (ts <= 0) return ""
    return try {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(ts))
    } catch (_: Exception) {
        ""
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Login / Mover / Editar
// ─────────────────────────────────────────────────────────────────────────────

private enum class ZyLoginMethod { EMAIL, PHONE, USERNAME }

@Composable
private fun ZyLoginDialog(
    onDismiss: () -> Unit,
    onLoggedIn: (token: String, username: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val deviceId = remember { ZonayummySessionStore.getOrCreateDeviceId(context) }

    var method by remember { mutableStateOf(ZyLoginMethod.EMAIL) }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("+52") }
    var password by remember { mutableStateOf("") }
    var rememberSession by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text("Iniciar sesión (ZonaYummy)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Para guardar tus descargas (Blob + historial).",
                    fontSize = 12.sp
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = method == ZyLoginMethod.EMAIL,
                        onClick = { method = ZyLoginMethod.EMAIL; error = "" },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) { Text("Correo") }
                    SegmentedButton(
                        selected = method == ZyLoginMethod.PHONE,
                        onClick = { method = ZyLoginMethod.PHONE; error = "" },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) { Text("Número") }
                    SegmentedButton(
                        selected = method == ZyLoginMethod.USERNAME,
                        onClick = { method = ZyLoginMethod.USERNAME; error = "" },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) { Text("Usuario") }
                }

                when (method) {
                    ZyLoginMethod.EMAIL -> OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; error = "" },
                        label = { Text("Correo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ZyLoginMethod.USERNAME -> OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; error = "" },
                        label = { Text("Usuario") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ZyLoginMethod.PHONE -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = countryCode,
                                onValueChange = { countryCode = it; error = "" },
                                label = { Text("CC") },
                                singleLine = true,
                                modifier = Modifier.width(92.dp)
                            )
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it; error = "" },
                                label = { Text("Número") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = "" },
                    label = { Text("Contraseña") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = rememberSession, onCheckedChange = { rememberSession = it })
                    Text("Recordar sesión", fontSize = 12.sp)
                }

                if (error.isNotBlank()) {
                    Text(text = error, color = CeoStyle.Danger, fontSize = 12.sp)
                }
            }
        },
        containerColor = CeoStyle.Surface,
        titleContentColor = CeoStyle.Text,
        textContentColor = CeoStyle.Text,
        confirmButton = {
            Button(
                enabled = !loading,
                onClick = {
                    if (password.isBlank()) {
                        error = "La contraseña es requerida"
                        return@Button
                    }
                    loading = true
                    error = ""
                    scope.launch {
                        val res = withContext(Dispatchers.IO) {
                            when (method) {
                                ZyLoginMethod.EMAIL -> ZonayummyAuthApi.loginWithEmail(deviceId, email.trim(), password)
                                ZyLoginMethod.USERNAME -> ZonayummyAuthApi.loginWithUsername(deviceId, username.trim().lowercase(), password)
                                ZyLoginMethod.PHONE -> ZonayummyAuthApi.loginWithPhone(deviceId, (countryCode + phone).trim(), password)
                            }
                        }
                        when (res) {
                            is ZonayummyAuthApi.LoginResult.Success -> {
                                // (por ahora) rememberSession no cambia storage (si quieres, lo conectamos a session/local)
                                onLoggedIn(res.token, res.username)
                            }
                            is ZonayummyAuthApi.LoginResult.Error -> {
                                error = res.message
                                loading = false
                            }
                        }
                    }
                }
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Ingresando…")
                } else {
                    Text("Iniciar sesión")
                }
            }
        },
        dismissButton = {
            TextButton(enabled = !loading, onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ZyMoveDialog(
    folders: List<ZyFolder>,
    onDismiss: () -> Unit,
    onMove: (folderId: String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mover a carpeta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = folders.firstOrNull { it.id == selected }?.name ?: "Sin carpeta (raíz)",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        label = { Text("Carpeta") }
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Sin carpeta (raíz)") },
                            onClick = { selected = null; expanded = false }
                        )
                        folders.forEach { f ->
                            DropdownMenuItem(
                                text = { Text((f.icon?.takeIf { it.isNotBlank() } ?: "📁") + " " + f.name) },
                                onClick = { selected = f.id; expanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onMove(selected) }) { Text("Mover") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

private data class ZyEditUpdates(
    val downloadId: String,
    val folderId: String?,
    val labelIds: List<String>,
    val newLabelNames: List<String>,
    val description: String?,
    val title: String?,
)

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
private fun ZyEditItemDialog(
    item: ZyHistoryItem,
    folders: List<ZyFolder>,
    labels: List<ZyLabel>,
    onDismiss: () -> Unit,
    onSave: (ZyEditUpdates) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var folderId by remember { mutableStateOf(item.folderId) }
    var title by remember { mutableStateOf(item.title ?: "") }
    var description by remember { mutableStateOf(item.description ?: "") }
    var labelIds by remember { mutableStateOf(item.labelIds) }
    var newLabelDraft by remember { mutableStateOf("") }
    var newLabelChips by remember { mutableStateOf<List<String>>(emptyList()) }

    val labelMap = remember(labels) { labels.associateBy { it.id } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar descarga") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = item.url, fontSize = 11.sp, color = Color.Gray, maxLines = 2)

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = folders.firstOrNull { it.id == folderId }?.name ?: "Sin carpeta (raíz)",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        label = { Text("Carpeta") }
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Sin carpeta (raíz)") },
                            onClick = { folderId = null; expanded = false }
                        )
                        folders.forEach { f ->
                            DropdownMenuItem(
                                text = { Text((f.icon?.takeIf { it.isNotBlank() } ?: "📁") + " " + f.name) },
                                onClick = { folderId = f.id; expanded = false }
                            )
                        }
                    }
                }

                if (labelIds.isNotEmpty()) {
                    Text("Etiquetas:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        labelIds.take(10).forEach { lid ->
                            val ln = labelMap[lid]?.name ?: lid
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(ln, modifier = Modifier.weight(1f), fontSize = 12.sp)
                                IconButton(onClick = { labelIds = labelIds.filterNot { it == lid } }) {
                                    Icon(Icons.Default.Close, contentDescription = "Quitar")
                                }
                            }
                        }
                    }
                }

                Text("Nuevas etiquetas:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                // Chips creados al escribir coma
                if (newLabelChips.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        newLabelChips.forEach { chip ->
                            AssistChip(
                                onClick = { /* no-op */ },
                                label = { Text(chip, maxLines = 1) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { newLabelChips = newLabelChips.filterNot { it == chip } },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Quitar", modifier = Modifier.size(16.dp))
                                    }
                                }
                            )
                        }
                    }
                }

                fun commitDraftIntoChips() {
                    val toAdd = newLabelDraft
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                    if (toAdd.isNotEmpty()) {
                        newLabelChips = (newLabelChips + toAdd).distinct()
                    }
                    newLabelDraft = ""
                }

                OutlinedTextField(
                    value = newLabelDraft,
                    onValueChange = { v ->
                        // Si el usuario escribió coma, convertir en chip(s)
                        if (v.contains(",")) {
                            val parts = v.split(",")
                            val completed = parts.dropLast(1).map { it.trim() }.filter { it.isNotBlank() }
                            if (completed.isNotEmpty()) {
                                newLabelChips = (newLabelChips + completed).distinct()
                            }
                            newLabelDraft = parts.lastOrNull()?.trimStart() ?: ""
                        } else {
                            newLabelDraft = v
                        }
                    },
                    label = { Text("Escribe y separa por coma") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPreviewKeyEvent { e ->
                            if (e.type == KeyEventType.KeyDown && e.key == Key.Backspace) {
                                if (newLabelDraft.isBlank() && newLabelChips.isNotEmpty()) {
                                    newLabelChips = newLabelChips.dropLast(1)
                                    return@onPreviewKeyEvent true
                                }
                            }
                            false
                        },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { commitDraftIntoChips() })
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val draftNames = newLabelDraft.split(",").map { it.trim() }.filter { it.isNotBlank() }
                val newNames = (newLabelChips + draftNames).distinct()
                onSave(
                    ZyEditUpdates(
                        downloadId = item.id,
                        folderId = folderId,
                        labelIds = labelIds,
                        newLabelNames = newNames,
                        description = description,
                        title = title,
                    )
                )
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun ZyCreateFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, icon: String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("📁") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva carpeta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = "" },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it; error = "" },
                    label = { Text("Icono (emoji opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error.isNotBlank()) {
                    Text(error, color = CeoStyle.Danger, fontSize = 12.sp)
                }
            }
        },
        containerColor = CeoStyle.Surface,
        titleContentColor = CeoStyle.Text,
        textContentColor = CeoStyle.Text,
        confirmButton = {
            Button(onClick = {
                val n = name.trim()
                if (n.isBlank()) {
                    error = "El nombre es requerido"
                    return@Button
                }
                val ic = icon.trim().takeIf { it.isNotBlank() }
                onCreate(n, ic)
            }) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}


// ─────────────────────────────────────────────────────────────────────────────
// Previews (Android Studio)
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, widthDp = 390, heightDp = 820)
@Composable
private fun Preview_DownloadReelsScreen_Guest() {
    NRXGoAmTheme {
        DownloadReelsScreen(
            modifier = Modifier.fillMaxSize(),
            zySession = null,
            onRequireLogin = {},
            onCloudSaveRequested = { _, _ -> },
            onCloudSave = { _, _, setState ->
                setState(DownloadState.CloudSaving(30, "Preview…"))
            },
            onRefreshHistory = {},
            onUploadFileToBlob = { _, _, _, _ -> Result.success(Unit) }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, widthDp = 390, heightDp = 820)
@Composable
private fun Preview_DownloadReelsScreen_LoggedIn() {
    NRXGoAmTheme {
        DownloadReelsScreen(
            modifier = Modifier.fillMaxSize(),
            zySession = ZonayummySession(token = "preview", username = "Usuario"),
            onRequireLogin = {},
            onCloudSaveRequested = { _, _ -> },
            onCloudSave = { _, _, setState ->
                setState(DownloadState.CloudSuccess("✅ Guardado (preview)"))
            },
            onRefreshHistory = {},
            onUploadFileToBlob = { _, _, _, _ -> Result.success(Unit) }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, widthDp = 360, heightDp = 760)
@Composable
private fun Preview_HistoryDrawer_LoggedIn() {
    val sampleItems = listOf(
        ZyHistoryItem(
            id = "20251218_instagram_demo_1234",
            platform = "instagram",
            url = "https://www.instagram.com/reel/ABC123/",
            blobPath = "users/u/reel_downloader/instagram/demo.mp4",
            createdAt = System.currentTimeMillis() - 3600_000,
            title = "Reel de prueba",
            description = "Descripción",
            labelIds = listOf("lbl_1", "lbl_2"),
        ),
        ZyHistoryItem(
            id = "20251218_youtube_demo_5678",
            platform = "youtube",
            url = "https://youtube.com/shorts/XYZ987",
            blobPath = "users/u/reel_downloader/youtube/demo.mp4",
            createdAt = System.currentTimeMillis() - 7200_000,
        )
    )
    val sampleLabels = listOf(
        ZyLabel(id = "lbl_1", name = "trabajo", color = "#BAE1FF", usageCount = 3),
        ZyLabel(id = "lbl_2", name = "ideas", color = "#BAFFC9", usageCount = 2),
    )
    val sampleFolders = listOf(
        ZyFolder(id = "fld_1", name = "Favoritos", icon = "⭐", order = 0),
        ZyFolder(id = "fld_2", name = "Pendientes", icon = "⏳", order = 1),
    )

    NRXGoAmTheme {
        DownloadReelsHistoryDrawer(
            zySession = ZonayummySession(token = "preview", username = "Usuario"),
            loading = false,
            error = "",
            folders = sampleFolders,
            labels = sampleLabels,
            items = sampleItems,
            folderFilter = ZyFolderFilter.Unassigned,
            selectedIds = emptySet(),
            onSelectFolder = {},
            onRefresh = {},
            onCreateFolder = {},
            onDeleteSelectedFolder = {},
            onToggleSelect = {},
            onClearSelection = {},
            onMoveSelected = {},
            onDeleteSelected = {},
            onCopyUrl = {},
            onOpenUrl = {},
            onDownloadItem = {},
            onEditItem = {}
        )
    }
}

