package com.edalxgoam.nrxgoam.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.edalxgoam.nrxgoam.ui.theme.NRXGoAmTheme

class PantryActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            NRXGoAmTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PantryScreen(
                        modifier = Modifier.padding(innerPadding),
                        onAddProduct = {
                            // Implementar lógica para agregar producto
                        },
                        onRegisterPurchase = {
                            // Implementar lógica para registrar compra
                        },
                        onNavigateBack = {
                            finish()
                        }
                    )
                }
            }
        }
    }
} 