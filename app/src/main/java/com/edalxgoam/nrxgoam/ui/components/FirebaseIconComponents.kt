package com.edalxgoam.nrxgoam.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.edalxgoam.nrxgoam.R
import com.edalxgoam.nrxgoam.repository.FirebaseManager
import com.edalxgoam.nrxgoam.repository.IconCacheRepository

/**
 * Composable para el icono de alarma que se carga desde Firebase Storage
 */
@Composable
fun AlarmIconFromFirebase(
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    var painter by remember { mutableStateOf<Painter?>(null) }
    val fallbackPainter = painterResource(id = R.drawable.icon_alarm_placeholder)
    
    LaunchedEffect(Unit) {
        try {
            val iconRepo = FirebaseManager.getIconCacheRepository(context)
            val drawable = iconRepo.getAlarmIcon()
            painter = BitmapPainter(
                (drawable as android.graphics.drawable.BitmapDrawable).bitmap.asImageBitmap()
            )
        } catch (e: Exception) {
            // Si hay error, usar el fallback
            painter = fallbackPainter
        }
    }
    
    Image(
        painter = painter ?: fallbackPainter,
        contentDescription = contentDescription,
        modifier = modifier
    )
}

/**
 * Composable para el icono de despensa que se carga desde Firebase Storage
 */
@Composable
fun PantryIconFromFirebase(
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    var painter by remember { mutableStateOf<Painter?>(null) }
    val fallbackPainter = painterResource(id = R.drawable.icon_despensa_placeholder)
    
    LaunchedEffect(Unit) {
        try {
            val iconRepo = FirebaseManager.getIconCacheRepository(context)
            val drawable = iconRepo.getPantryIcon()
            painter = BitmapPainter(
                (drawable as android.graphics.drawable.BitmapDrawable).bitmap.asImageBitmap()
            )
        } catch (e: Exception) {
            // Si hay error, usar el fallback
            painter = fallbackPainter
        }
    }
    
    Image(
        painter = painter ?: fallbackPainter,
        contentDescription = contentDescription,
        modifier = modifier
    )
}

/**
 * Composable genérico para iconos de Firebase con estados de carga
 */
@Composable
fun FirebaseIcon(
    firebasePath: String,
    cacheFileName: String,
    fallbackResourceId: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    showLoadingIndicator: Boolean = false
) {
    val context = LocalContext.current
    var painter by remember { mutableStateOf<Painter?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val fallbackPainter = painterResource(id = fallbackResourceId)
    
    LaunchedEffect(firebasePath) {
        try {
            val iconRepo = FirebaseManager.getIconCacheRepository(context)
            val drawable = iconRepo.getIcon(firebasePath, cacheFileName, fallbackResourceId)
            painter = BitmapPainter(
                (drawable as android.graphics.drawable.BitmapDrawable).bitmap.asImageBitmap()
            )
        } catch (e: Exception) {
            painter = fallbackPainter
        } finally {
            isLoading = false
        }
    }
    
    if (showLoadingIndicator && isLoading) {
        // Aquí podrías mostrar un indicador de carga
        Image(
            painter = fallbackPainter,
            contentDescription = contentDescription,
            modifier = modifier,
            alpha = 0.5f // Mostrar con opacidad reducida mientras carga
        )
    } else {
        Image(
            painter = painter ?: fallbackPainter,
            contentDescription = contentDescription,
            modifier = modifier
        )
    }
} 