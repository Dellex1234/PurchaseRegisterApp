package com.example.purchaseregister.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun FacturaLoadingDialog(
    isLoading: Boolean,
    statusMessage: String = "Obteniendo detalle de factura...",
    debugInfo: String? = null,
    onDismiss: () -> Unit = {}
) {
    if (!isLoading) return

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Círculo de carga
                CircularProgressIndicator(
                    color = Color(0xFF1FB8B9),
                    modifier = Modifier.size(60.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Mensaje principal
                Text(
                    text = statusMessage,
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Mensaje secundario
                Text(
                    text = "Este proceso puede tardar 1 minuto",
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}