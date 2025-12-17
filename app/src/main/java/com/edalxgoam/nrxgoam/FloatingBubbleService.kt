package com.edalxgoam.nrxgoam

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.edalxgoam.nrxgoam.services.ReelDownloaderService
import com.edalxgoam.nrxgoam.ui.screens.AlarmActivity
import com.edalxgoam.nrxgoam.ui.screens.DownloadReelsActivity
import com.edalxgoam.nrxgoam.ui.screens.PantryActivity
import com.edalxgoam.nrxgoam.ui.screens.TaskActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var menuView: View? = null
    private var downloadMiniView: View? = null
    private var downloadMiniParams: WindowManager.LayoutParams? = null
    private var isMenuExpanded = false
    private var isDownloadMiniShown = false
    private var currentPlatform: String = "instagram"

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    
    // Handler para actualizar el color de la burbuja
    private val colorUpdateHandler = Handler(Looper.getMainLooper())
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastMinuteDigit = -1
    
    // Coroutine scope para descargas
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    
    // 10 colores para cada dígito (0-9)
    private val bubbleColors = arrayOf(
        Color.parseColor("#3B82F6"),  // 0 - Azul
        Color.parseColor("#F59E0B"),  // 1 - Amarillo/Ámbar
        Color.parseColor("#14B8A6"),  // 2 - Teal
        Color.parseColor("#22C55E"),  // 3 - Verde
        Color.parseColor("#8B5CF6"),  // 4 - Púrpura
        Color.parseColor("#EC4899"),  // 5 - Rosa
        Color.parseColor("#6366F1"),  // 6 - Índigo
        Color.parseColor("#EF4444"),  // 7 - Rojo
        Color.parseColor("#F97316"),  // 8 - Naranja
        Color.parseColor("#6B7280")   // 9 - Gris
    )
    
    // Colores de plataformas
    private val platformColors = mapOf(
        "instagram" to Color.parseColor("#E4405F"),
        "facebook" to Color.parseColor("#1877F2"),
        "youtube" to Color.parseColor("#FF0000"),
        "tiktok" to Color.parseColor("#000000")
    )

    companion object {
        const val CHANNEL_ID = "floating_bubble_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_BUBBLE = "com.edalxgoam.nrxgoam.STOP_BUBBLE"
        const val COLOR_UPDATE_INTERVAL = 5000L // Verificar cada 5 segundos
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        createBubbleView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_BUBBLE) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Burbuja Flotante",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación para mantener la burbuja flotante activa"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, FloatingBubbleService::class.java).apply {
            action = ACTION_STOP_BUBBLE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NRXGoAm")
            .setContentText("Burbuja flotante activa")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Cerrar", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createBubbleView() {
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }

        bubbleView = LayoutInflater.from(this).inflate(R.layout.floating_bubble, null)
        
        bubbleView?.setOnTouchListener(object : View.OnTouchListener {
            private var lastAction = 0
            private var startClickTime: Long = 0

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startClickTime = System.currentTimeMillis()
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        lastAction = event.action
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val clickDuration = System.currentTimeMillis() - startClickTime
                        if (clickDuration < 200) {
                            // Es un click, mostrar/ocultar menú
                            toggleMenu()
                        }
                        lastAction = event.action
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(bubbleView, layoutParams)
                        lastAction = event.action
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(bubbleView, layoutParams)
        
        // Iniciar actualización de color
        startColorUpdates()
    }
    
    private fun startColorUpdates() {
        colorUpdateHandler.post(colorUpdateRunnable)
    }
    
    private fun stopColorUpdates() {
        colorUpdateHandler.removeCallbacks(colorUpdateRunnable)
    }
    
    private val colorUpdateRunnable = object : Runnable {
        override fun run() {
            updateBubbleColor()
            colorUpdateHandler.postDelayed(this, COLOR_UPDATE_INTERVAL)
        }
    }
    
    private fun updateBubbleColor() {
        val calendar = Calendar.getInstance()
        val minute = calendar.get(Calendar.MINUTE)
        val lastDigit = minute % 10
        
        // Solo actualizar si el dígito cambió
        if (lastDigit != lastMinuteDigit) {
            lastMinuteDigit = lastDigit
            val color = bubbleColors[lastDigit]
            
            bubbleView?.findViewById<ImageView>(R.id.bubble_icon)?.let { imageView ->
                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    setStroke(4, Color.WHITE)
                }
                imageView.background = drawable
            }
            
            println("Burbuja: Color actualizado a índice $lastDigit (minuto: $minute)")
        }
    }

    private fun toggleMenu() {
        if (isDownloadMiniShown) {
            hideDownloadMini()
            return
        }
        if (isMenuExpanded) {
            hideMenu()
        } else {
            showMenu()
        }
    }

    private fun showMenu() {
        if (menuView != null) return

        val bubbleParams = bubbleView?.layoutParams as? WindowManager.LayoutParams ?: return

        val menuParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = bubbleParams.x + 70
            y = bubbleParams.y
        }

        menuView = LayoutInflater.from(this).inflate(R.layout.floating_menu, null)

        // Configurar clicks de los botones del menú
        menuView?.findViewById<LinearLayout>(R.id.btn_abrir_app)?.setOnClickListener {
            openActivity(MainActivity::class.java)
        }

        menuView?.findViewById<LinearLayout>(R.id.btn_alarmas)?.setOnClickListener {
            openActivity(AlarmActivity::class.java)
        }

        menuView?.findViewById<LinearLayout>(R.id.btn_despensa)?.setOnClickListener {
            openActivity(PantryActivity::class.java)
        }

        menuView?.findViewById<LinearLayout>(R.id.btn_tareas)?.setOnClickListener {
            openActivity(TaskActivity::class.java)
        }

        menuView?.findViewById<LinearLayout>(R.id.btn_download_reels)?.setOnClickListener {
            openActivity(DownloadReelsActivity::class.java)
        }

        menuView?.findViewById<LinearLayout>(R.id.btn_cerrar)?.setOnClickListener {
            hideMenu()
        }
        
        // Botones de descarga rápida
        menuView?.findViewById<FrameLayout>(R.id.btn_quick_instagram)?.setOnClickListener {
            showDownloadMini("instagram")
        }
        
        menuView?.findViewById<FrameLayout>(R.id.btn_quick_facebook)?.setOnClickListener {
            showDownloadMini("facebook")
        }
        
        menuView?.findViewById<FrameLayout>(R.id.btn_quick_youtube)?.setOnClickListener {
            showDownloadMini("youtube")
        }
        
        menuView?.findViewById<FrameLayout>(R.id.btn_quick_tiktok)?.setOnClickListener {
            showDownloadMini("tiktok")
        }

        // Click fuera del menú para cerrarlo
        menuView?.setOnClickListener {
            hideMenu()
        }

        windowManager.addView(menuView, menuParams)
        isMenuExpanded = true
    }
    
    private fun showDownloadMini(platform: String) {
        currentPlatform = platform
        
        // Ocultar el menú principal
        menuView?.let {
            windowManager.removeView(it)
            menuView = null
        }
        
        val bubbleParams = bubbleView?.layoutParams as? WindowManager.LayoutParams ?: return
        
        val miniParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = bubbleParams.x + 70
            y = bubbleParams.y
        }
        
        downloadMiniView = LayoutInflater.from(this).inflate(R.layout.floating_download_mini, null)
        
        // Configurar la vista según la plataforma
        val platformColor = platformColors[platform] ?: Color.parseColor("#E4405F")
        val platformName = when (platform) {
            "instagram" -> "Instagram"
            "facebook" -> "Facebook"
            "youtube" -> "YouTube"
            "tiktok" -> "TikTok"
            else -> "Instagram"
        }
        
        downloadMiniView?.findViewById<TextView>(R.id.txt_platform_name)?.text = platformName
        
        // Configurar indicador de plataforma
        downloadMiniView?.findViewById<View>(R.id.platform_indicator)?.background = 
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(platformColor)
            }
        
        // Configurar botón de pegar con el color de la plataforma
        downloadMiniView?.findViewById<ImageView>(R.id.btn_paste)?.background =
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(platformColor)
            }
        
        // Configurar botón de descargar con el color de la plataforma
        downloadMiniView?.findViewById<Button>(R.id.btn_download)?.background =
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(platformColor)
                cornerRadius = 16f
            }
        
        // Botón de regresar
        downloadMiniView?.findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            hideDownloadMini()
            showMenu()
        }
        
        // Botón de pegar
        downloadMiniView?.findViewById<ImageView>(R.id.btn_paste)?.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val pastedText = clipData.getItemAt(0).text?.toString() ?: ""
                downloadMiniView?.findViewById<EditText>(R.id.edit_url)?.setText(pastedText)
                Toast.makeText(this, "Enlace pegado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Portapapeles vacío", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Botón de descargar
        downloadMiniView?.findViewById<Button>(R.id.btn_download)?.setOnClickListener {
            val url = downloadMiniView?.findViewById<EditText>(R.id.edit_url)?.text?.toString() ?: ""
            if (url.isNotBlank()) {
                startDownload(url, platform)
            } else {
                Toast.makeText(this, "Pega un enlace primero", Toast.LENGTH_SHORT).show()
            }
        }
        
        downloadMiniParams = miniParams
        windowManager.addView(downloadMiniView, miniParams)
        isDownloadMiniShown = true
    }
    
    private fun hideDownloadMini() {
        downloadMiniView?.let {
            windowManager.removeView(it)
            downloadMiniView = null
        }
        downloadMiniParams = null
        isDownloadMiniShown = false
    }
    
    private fun startDownload(url: String, platform: String) {
        val editText = downloadMiniView?.findViewById<EditText>(R.id.edit_url)
        
        // Cerrar el teclado
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        editText?.let { 
            imm.hideSoftInputFromWindow(it.windowToken, 0)
            it.clearFocus()
        }
        
        // Ocultar el campo de texto
        downloadMiniView?.findViewById<LinearLayout>(R.id.input_container)?.visibility = View.GONE
        
        // Cambiar los flags de la ventana para que no capture el foco
        downloadMiniParams?.let { params ->
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                          WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            downloadMiniView?.let { view ->
                windowManager.updateViewLayout(view, params)
            }
        }
        
        // Mostrar estado de descarga
        downloadMiniView?.findViewById<LinearLayout>(R.id.download_status_container)?.visibility = View.VISIBLE
        downloadMiniView?.findViewById<Button>(R.id.btn_download)?.visibility = View.GONE
        downloadMiniView?.findViewById<TextView>(R.id.txt_status)?.text = "Iniciando descarga..."
        
        // Seleccionar la plataforma correcta
        val downloadPlatform = when (platform) {
            "instagram" -> ReelDownloaderService.Platform.INSTAGRAM
            "facebook" -> ReelDownloaderService.Platform.FACEBOOK
            "youtube" -> ReelDownloaderService.Platform.YOUTUBE
            "tiktok" -> ReelDownloaderService.Platform.TIKTOK
            else -> ReelDownloaderService.Platform.INSTAGRAM
        }
        
        serviceScope.launch {
            val result = ReelDownloaderService.downloadVideo(
                context = this@FloatingBubbleService,
                videoUrl = url,
                platform = downloadPlatform,
                onProgress = { progress ->
                    mainHandler.post {
                        downloadMiniView?.findViewById<ProgressBar>(R.id.progress_bar)?.progress = progress
                        downloadMiniView?.findViewById<TextView>(R.id.txt_status)?.text = "Descargando... $progress%"
                    }
                }
            )
            
            withContext(Dispatchers.Main) {
                when (result) {
                    is ReelDownloaderService.DownloadResult.Success -> {
                        downloadMiniView?.findViewById<TextView>(R.id.txt_status)?.text = "¡Descarga completada!"
                        downloadMiniView?.findViewById<ProgressBar>(R.id.progress_bar)?.progress = 100
                        Toast.makeText(this@FloatingBubbleService, "Video guardado en Movies/NRXGoAm", Toast.LENGTH_LONG).show()
                        
                        // Cerrar la mini interfaz después de 1.5 segundos
                        mainHandler.postDelayed({
                            hideDownloadMini()
                        }, 1500)
                    }
                    is ReelDownloaderService.DownloadResult.Error -> {
                        downloadMiniView?.findViewById<TextView>(R.id.txt_status)?.text = "Error: ${result.message}"
                        Toast.makeText(this@FloatingBubbleService, result.message, Toast.LENGTH_LONG).show()
                        // Restaurar la UI para permitir reintentar
                        resetDownloadState()
                    }
                    else -> {}
                }
            }
        }
    }
    
    private fun resetDownloadState() {
        // Restaurar los flags de la ventana para permitir el foco nuevamente
        downloadMiniParams?.let { params ->
            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or 
                          WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            downloadMiniView?.let { view ->
                windowManager.updateViewLayout(view, params)
            }
        }
        
        // Restaurar el campo de texto
        downloadMiniView?.findViewById<LinearLayout>(R.id.input_container)?.visibility = View.VISIBLE
        // Ocultar estado de descarga
        downloadMiniView?.findViewById<LinearLayout>(R.id.download_status_container)?.visibility = View.GONE
        downloadMiniView?.findViewById<ProgressBar>(R.id.progress_bar)?.progress = 0
        // Restaurar el botón de descargar
        downloadMiniView?.findViewById<Button>(R.id.btn_download)?.visibility = View.VISIBLE
    }

    private fun hideMenu() {
        menuView?.let {
            windowManager.removeView(it)
            menuView = null
        }
        isMenuExpanded = false
    }
    
    private fun hideAll() {
        hideMenu()
        hideDownloadMini()
    }

    private fun openActivity(activityClass: Class<*>) {
        hideAll()
        val intent = Intent(this, activityClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopColorUpdates()
        bubbleView?.let {
            windowManager.removeView(it)
            bubbleView = null
        }
        hideAll()
    }
}

