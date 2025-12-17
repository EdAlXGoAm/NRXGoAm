package com.edalxgoam.nrxgoam.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edalxgoam.nrxgoam.services.ReelDownloaderService
import com.edalxgoam.nrxgoam.ui.theme.NRXGoAmTheme
import kotlinx.coroutines.launch

class DownloadReelsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            NRXGoAmTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        DownloadReelsTopBar(onBackClick = { finish() })
                    }
                ) { innerPadding ->
                    DownloadReelsScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadReelsTopBar(onBackClick: () -> Unit) {
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
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
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
}

@Composable
fun DownloadReelsScreen(modifier: Modifier = Modifier) {
    var selectedTabIndex by remember { mutableStateOf(0) } // Instagram primero
    var linkText by remember { mutableStateOf("") }
    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val currentPlatform = platforms[selectedTabIndex]
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E293B),
                        Color(0xFF0F172A)
                    )
                )
            )
    ) {
        // Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = Color.White,
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
                                color = if (selectedTabIndex == index) platform.primaryColor else Color.White.copy(alpha = 0.7f)
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
            // Título de la plataforma seleccionada
            Text(
                text = "Descargar de ${currentPlatform.name}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
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
                        containerColor = Color(0xFFFEF3C7)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Próximamente disponible para ${currentPlatform.name}",
                            color = Color(0xFF92400E),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                Text(
                    text = "Pega el enlace del video que deseas descargar",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Campo de texto con botón de pegar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF334155)
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
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 12.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = currentPlatform.primaryColor,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
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
                                if (currentPlatform.isAvailable) currentPlatform.primaryColor 
                                else Color.Gray
                            )
                            .size(48.dp),
                        enabled = currentPlatform.isAvailable && downloadState !is DownloadState.Downloading
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_paste),
                            contentDescription = "Pegar",
                            tint = Color.White
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
                                containerColor = Color(0xFF334155)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Descargando...",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { state.progress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = currentPlatform.primaryColor,
                                    trackColor = Color.White.copy(alpha = 0.2f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${state.progress}%",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
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
                                containerColor = Color(0xFF22C55E).copy(alpha = 0.2f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF22C55E),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "¡Descarga completada!",
                                        color = Color(0xFF22C55E),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Guardado en: Movies/NRXGoAm",
                                        color = Color.White.copy(alpha = 0.7f),
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
                                containerColor = Color(0xFFEF4444).copy(alpha = 0.2f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Error en la descarga",
                                        color = Color(0xFFEF4444),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = state.message,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
            
            // Botón de descargar
            Button(
                onClick = {
                    if (linkText.isNotBlank() && currentPlatform.isAvailable) {
                        downloadState = DownloadState.Downloading(0)
                        
                        coroutineScope.launch {
                            // Seleccionar la plataforma correcta
                            val platform = when (currentPlatform.name) {
                                "Instagram" -> ReelDownloaderService.Platform.INSTAGRAM
                                "Facebook" -> ReelDownloaderService.Platform.FACEBOOK
                                "YouTube" -> ReelDownloaderService.Platform.YOUTUBE
                                "TikTok" -> ReelDownloaderService.Platform.TIKTOK
                                else -> ReelDownloaderService.Platform.INSTAGRAM
                            }
                            
                            val result = ReelDownloaderService.downloadVideo(
                                context = context,
                                videoUrl = linkText,
                                platform = platform,
                                onProgress = { progress ->
                                    downloadState = DownloadState.Downloading(progress)
                                }
                            )
                            
                            downloadState = when (result) {
                                is ReelDownloaderService.DownloadResult.Success -> {
                                    DownloadState.Success(result.fileName)
                                }
                                is ReelDownloaderService.DownloadResult.Error -> {
                                    DownloadState.Error(result.message)
                                }
                                else -> DownloadState.Idle
                            }
                        }
                    } else if (!currentPlatform.isAvailable) {
                        Toast.makeText(
                            context,
                            "${currentPlatform.name} no está disponible aún",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Por favor, pega un enlace primero",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentPlatform.isAvailable) currentPlatform.primaryColor else Color.Gray,
                    disabledContainerColor = Color.Gray
                ),
                enabled = linkText.isNotBlank() && 
                         currentPlatform.isAvailable && 
                         downloadState !is DownloadState.Downloading
            ) {
                if (downloadState is DownloadState.Downloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Descargando...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Descargar Video",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Información adicional
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF334155).copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "💡 Cómo usar:",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
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
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
