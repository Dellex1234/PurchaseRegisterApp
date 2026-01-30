package com.example.purchaseregister.utils

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import java.text.SimpleDateFormat
import java.util.*

// Función de extensión para formatear fechas
fun Long?.toFormattedDate(): String {
    if (this == null) return ""
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    calendar.timeInMillis = this
    val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    format.timeZone = TimeZone.getTimeZone("UTC")
    return format.format(calendar.time)
}

// Función auxiliar para obtener la fecha de hoy
fun getHoyMillis(): Long {
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

@Composable
fun AutoShowListEffect(
    selectedStartMillis: Long?,
    isListVisible: Boolean,
    hasLoadedSunatData: Boolean,
    onShowList: () -> Unit
) {
    LaunchedEffect(selectedStartMillis) {
        if (selectedStartMillis != null && !isListVisible && hasLoadedSunatData) {
            println("🔄 Mostrando lista automáticamente (fecha seleccionada: ${selectedStartMillis.toFormattedDate()})")
            onShowList()
        }
    }
}

// Clase SIMPLE para manejar solo el texto del rango de fechas
@Composable
fun DateRangeSelector(
    selectedStartMillis: Long?,
    selectedEndMillis: Long?,
    onDateRangeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hoyMillis = getHoyMillis()
    val selectedDateRangeText = if (selectedStartMillis != null) {
        val startStr = selectedStartMillis.toFormattedDate()
        val endStr = selectedEndMillis?.toFormattedDate() ?: startStr
        if (startStr == endStr) startStr else "$startStr - $endStr"
    } else {
        hoyMillis.toFormattedDate()
    }

    Surface(
        modifier = modifier
            .width(200.dp)
            .height(45.dp)
            .border(1.dp, Color.Gray, MaterialTheme.shapes.medium)
            .clickable { onDateRangeClick() },
        shape = MaterialTheme.shapes.medium,
        color = Color.White
    ) {
        Box(
            contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = selectedDateRangeText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
    }
}