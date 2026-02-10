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
import java.text.SimpleDateFormat
import java.util.*

val PERU_TIME_ZONE = TimeZone.getTimeZone("America/Lima")

fun normalizarFechaAMedianochePeru(millis: Long): Long {
    val calendar = Calendar.getInstance(PERU_TIME_ZONE).apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis
}

fun getHoyMillisPeru(): Long {
    return Calendar.getInstance(PERU_TIME_ZONE).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

fun Long?.toFormattedDatePeru(): String {
    if (this == null) return ""
    val calendar = Calendar.getInstance(PERU_TIME_ZONE).apply {
        timeInMillis = this@toFormattedDatePeru
    }
    val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    format.timeZone = PERU_TIME_ZONE
    return format.format(calendar.time)
}

fun convertirDatePickerUTCaPeru(millisUTC: Long): Long {
    val offset = PERU_TIME_ZONE.getOffset(millisUTC)
    val fechaEnPeru = millisUTC - offset
    return normalizarFechaAMedianochePeru(fechaEnPeru)
}

fun getPrimerDiaMesPeru(millis: Long): Long {
    val calendar = Calendar.getInstance(PERU_TIME_ZONE).apply {
        timeInMillis = millis
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis
}

fun getUltimoDiaMesPeru(millis: Long): Long {
    val calendar = Calendar.getInstance(PERU_TIME_ZONE).apply {
        timeInMillis = millis
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, 1)
        add(Calendar.DAY_OF_MONTH, -1)
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    return calendar.timeInMillis
}

fun getNombreMesPeru(millis: Long): String {
    val calendar = Calendar.getInstance(PERU_TIME_ZONE).apply {
        timeInMillis = millis
    }
    val format = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
    format.timeZone = PERU_TIME_ZONE
    return format.format(calendar.time).replaceFirstChar { it.uppercase() }
}

@Composable
fun DateRangeSelector(
    selectedStartMillis: Long?,
    selectedEndMillis: Long?,
    onDateRangeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hoyMillis = getHoyMillisPeru()
    val selectedDateRangeText = if (selectedStartMillis != null) {
        val startStr = selectedStartMillis.toFormattedDatePeru()
        val endStr = selectedEndMillis?.toFormattedDatePeru() ?: startStr

        val primerDiaMes = getPrimerDiaMesPeru(selectedStartMillis)
        val ultimoDiaMes = getUltimoDiaMesPeru(selectedStartMillis)

        if (selectedStartMillis == primerDiaMes && selectedEndMillis == ultimoDiaMes) {
            getNombreMesPeru(selectedStartMillis)
        } else if (startStr == endStr) {
            startStr
        } else {
            "$startStr - $endStr"
        }
    } else {
        val primerDia = getPrimerDiaMesPeru(hoyMillis)
        getNombreMesPeru(primerDia)
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
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
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