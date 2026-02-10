package com.example.purchaseregister.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.purchaseregister.utils.*
import java.util.*

enum class DatePickerMode {
    PERIODO,
    RANGO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePickerDialog(
    onDismiss: () -> Unit,
    onPeriodoSelected: (Long, Long) -> Unit,
    onRangoSelected: (Long, Long) -> Unit,
    initialStartMillis: Long? = null,
    initialEndMillis: Long? = null
) {
    val hoyMillis = getHoyMillisPeru()
    var pickerMode by remember { mutableStateOf(DatePickerMode.PERIODO) }

    var selectedMonthMillis by remember {
        mutableStateOf(initialStartMillis ?: getPrimerDiaMesPeru(hoyMillis))
    }

    var startDateMillis by remember {
        mutableStateOf(
            initialStartMillis?.let { normalizarFechaAMedianochePeru(it) } ?: normalizarFechaAMedianochePeru(hoyMillis)
        )
    }

    var endDateMillis by remember {
        mutableStateOf(
            initialEndMillis?.let { normalizarFechaAMedianochePeru(it) } ?: normalizarFechaAMedianochePeru(hoyMillis)
        )
    }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val startDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = startDateMillis
    )

    val endDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = endDateMillis
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Seleccionar Fechas",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { pickerMode = DatePickerMode.PERIODO },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (pickerMode == DatePickerMode.PERIODO)
                                Color(0xFF1FB8B9) else Color.LightGray
                        )
                    ) {
                        Text(
                            text = "Período (mes)",
                            fontSize = 12.sp,
                            color = if (pickerMode == DatePickerMode.PERIODO)
                                Color.White else Color.Black,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = { pickerMode = DatePickerMode.RANGO },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (pickerMode == DatePickerMode.RANGO)
                                Color(0xFF1FB8B9) else Color.LightGray
                        )
                    ) {
                        Text(
                            text = "Rango de fechas",
                            fontSize = 12.sp,
                            color = if (pickerMode == DatePickerMode.RANGO)
                                Color.White else Color.Black,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                when (pickerMode) {
                    DatePickerMode.PERIODO -> {
                        PeriodoSelector(
                            selectedMonthMillis = selectedMonthMillis,
                            onMonthChange = { selectedMonthMillis = it }
                        )
                    }
                    DatePickerMode.RANGO -> {
                        RangoSelector(
                            startDateMillis = startDateMillis,
                            endDateMillis = endDateMillis,
                            onStartDateClick = { showStartDatePicker = true },
                            onEndDateClick = { showEndDatePicker = true }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336)
                        )
                    ) {
                        Text(
                            text = "Cancelar",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = {
                            when (pickerMode) {
                                DatePickerMode.PERIODO -> {
                                    val primerDia = getPrimerDiaMesPeru(selectedMonthMillis)
                                    val ultimoDia = getUltimoDiaMesPeru(selectedMonthMillis)
                                    onPeriodoSelected(primerDia, ultimoDia)
                                }
                                DatePickerMode.RANGO -> {
                                    onRangoSelected(startDateMillis, endDateMillis)
                                }
                            }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1FB8B9)
                        )
                    ) {
                        Text(
                            text = "Aceptar",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDatePickerState.selectedDateMillis?.let {
                        startDateMillis = convertirDatePickerUTCaPeru(it)
                    }
                    showStartDatePicker = false
                }) {
                    Text("Aceptar", color = Color(0xFF1FB8B9))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancelar", color = Color(0xFFFF5A00))
                }
            }
        ) {
            DatePicker(state = startDatePickerState)
        }
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDatePickerState.selectedDateMillis?.let {
                        endDateMillis = convertirDatePickerUTCaPeru(it)
                    }
                    showEndDatePicker = false
                }) {
                    Text("Aceptar", color = Color(0xFF1FB8B9))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancelar", color = Color(0xFFFF5A00))
                }
            }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }
}

@Composable
fun PeriodoSelector(
    selectedMonthMillis: Long,
    onMonthChange: (Long) -> Unit
) {
    val calendar = Calendar.getInstance(PERU_TIME_ZONE).apply {
        timeInMillis = selectedMonthMillis
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Seleccionar Mes:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    calendar.add(Calendar.MONTH, -1)
                    onMonthChange(calendar.timeInMillis)
                },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.LightGray
                )
            ) {
                Text(
                    text = "◀",
                    color = Color.Black,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Text(
                text = getNombreMesPeru(selectedMonthMillis),
                modifier = Modifier.weight(2f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color(0xFF1FB8B9)
            )

            Button(
                onClick = {
                    calendar.timeInMillis = selectedMonthMillis
                    calendar.add(Calendar.MONTH, 1)
                    onMonthChange(calendar.timeInMillis)
                },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.LightGray
                )
            ) {
                Text(
                    text = "▶",
                    color = Color.Black,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        val primerDia = getPrimerDiaMesPeru(selectedMonthMillis)
        val ultimoDia = getUltimoDiaMesPeru(selectedMonthMillis)

        Text(
            text = "Período seleccionado:",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Text(
            text = "${primerDia.toFormattedDatePeru()} - ${ultimoDia.toFormattedDatePeru()}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RangoSelector(
    startDateMillis: Long,
    endDateMillis: Long,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Seleccionar Rango:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Fecha de Inicio:",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
                    .border(1.dp, Color.Gray, MaterialTheme.shapes.medium)
                    .clickable { onStartDateClick() },
                shape = MaterialTheme.shapes.medium,
                color = Color.White
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = startDateMillis.toFormattedDatePeru(),
                        fontSize = 14.sp,
                        color = Color.Black,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Fecha de Fin:",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
                    .border(1.dp, Color.Gray, MaterialTheme.shapes.medium)
                    .clickable { onEndDateClick() },
                shape = MaterialTheme.shapes.medium,
                color = Color.White
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = endDateMillis.toFormattedDatePeru(),
                        fontSize = 14.sp,
                        color = Color.Black,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Text(
            text = "Rango seleccionado:",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Text(
            text = "${startDateMillis.toFormattedDatePeru()} - ${endDateMillis.toFormattedDatePeru()}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}