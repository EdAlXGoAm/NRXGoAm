package com.edalxgoam.nrxgoam

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager

/**
 * Clase Application personalizada para configurar WorkManager y otros componentes globales
 */
class NRXGoAmApplication : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        
        println("NRXGoAmApplication: Application inicializada correctamente")
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
} 