package com.edalxgoam.nrxgoam.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.edalxgoam.nrxgoam.ui.theme.NRXGoAmTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryScreen(
    modifier: Modifier = Modifier,
    onAddProduct: () -> Unit,
    onRegisterPurchase: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TopAppBar(
            title = { Text("Despensa") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver"
                    )
                }
            }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "¿Qué quieres hacer?",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        PantryOptionButton(
            title = "Agregar Producto",
            description = "Añade un nuevo producto a tu despensa",
            onClick = onAddProduct
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        PantryOptionButton(
            title = "Registrar Compra",
            description = "Registra una nueva compra de productos",
            onClick = onRegisterPurchase
        )
    }
}

@Composable
fun PantryOptionButton(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PantryScreenPreview() {
    NRXGoAmTheme {
        PantryScreen(
            onAddProduct = {},
            onRegisterPurchase = {},
            onNavigateBack = {}
        )
    }
} 