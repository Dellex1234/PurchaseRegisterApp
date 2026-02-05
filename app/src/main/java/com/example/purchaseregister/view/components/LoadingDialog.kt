package com.example.purchaseregister.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.DialogProperties

@Composable
fun FacturaLoadingDialog(
    isLoading: Boolean,
    statusMessage: String = "Obteniendo detalle de factura...",
    debugInfo: String? = null,
    onDismiss: () -> Unit = {},
    title: String? = null,
    showSubMessage: Boolean = true
) {
    if (!isLoading) return

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    // Calculamos el ancho como porcentaje de la pantalla (máximo 400dp)
    val dialogWidth = minOf(screenWidth * 0.85f, 400.dp)
    // Calculamos el ancho mínimo necesario para el texto
    val minWidthNeeded = 300.dp // Ancho mínimo para que el texto quepa en una línea

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .widthIn(min = minWidthNeeded, max = dialogWidth)
                .heightIn(min = 220.dp, max = screenHeight * 0.4f) // Máximo 40% de altura
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Círculo de carga
                CircularProgressIndicator(
                    color = Color(0xFF1FB8B9),
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Título opcional
                    title?.let {
                        Text(
                            text = it,
                            fontSize = 16.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(if (title != null) 8.dp else 0.dp))

                    // Mensaje principal
                    Text(
                        text = statusMessage,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Mensaje secundario (opcional)
                    if (showSubMessage) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Este proceso puede tardar 1 minuto",
                            fontSize = 12.sp,
                            color = Color(0xFF666666),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }

                // Información de debug (opcional)
                debugInfo?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = it,
                        fontSize = 10.sp,
                        color = Color(0xFF888888),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}