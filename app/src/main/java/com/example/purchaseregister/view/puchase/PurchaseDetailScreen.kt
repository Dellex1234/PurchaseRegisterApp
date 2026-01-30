package com.example.purchaseregister.view.puchase

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purchaseregister.model.Invoice
import java.text.SimpleDateFormat
import java.util.*
import com.example.purchaseregister.navigation.DetailRoute
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.purchaseregister.viewmodel.InvoiceViewModel
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import com.example.purchaseregister.utils.*
import com.example.purchaseregister.components.*

val LongSaver = Saver<Long, Any>(
    save = { it },
    restore = { (it as? Long) ?: 0L }
)

enum class Section { COMPRAS, VENTAS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseDetailScreen(
    viewModel: InvoiceViewModel,
    onComprasClick: () -> Unit,
    onVentasClick: () -> Unit,
    onNavigateToRegistrar: () -> Unit,
    onNavigateToDetalle: (DetailRoute) -> Unit
) {
    println("🔄 [PurchaseDetailScreen] INICIO - Pantalla se está COMPONIENDO/RECREANDO")
    val context = LocalContext.current
    var sectionActive by remember { mutableStateOf(Section.COMPRAS) }
    var isListVisible by rememberSaveable { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showSunatLogin by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var hasLoadedSunatData by rememberSaveable {
        mutableStateOf(SunatPrefs.getToken(context) != null)
    }
    var selectedStartMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedEndMillis by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        // Si hay token, asumimos que los datos están cargados
        if (SunatPrefs.getToken(context) != null && !hasLoadedSunatData) {
            hasLoadedSunatData = true
            println("🔑 [PurchaseDetailScreen] Token encontrado, marcando datos como cargados")
        }
    }
    LaunchedEffect(selectedStartMillis) {
        if (selectedStartMillis != null && !isListVisible && hasLoadedSunatData) {
            println("🔄 Mostrando lista automáticamente (fecha seleccionada: ${selectedStartMillis!!.toFormattedDate()})")
            isListVisible = true
        }
    }

    val hoyMillis = remember {
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = selectedStartMillis ?: hoyMillis,
        initialSelectedEndDateMillis = selectedEndMillis ?: hoyMillis
    )

    val facturasCompras by viewModel.facturasCompras.collectAsStateWithLifecycle()
    val facturasVentas by viewModel.facturasVentas.collectAsStateWithLifecycle()

    LaunchedEffect(facturasCompras) {
        println("📊 [PurchaseDetailScreen] facturasCompras actualizadas: ${facturasCompras.size} elementos")
        facturasCompras.take(3).forEach { factura ->  // Mostrar solo las primeras 3
            println("📊 [PurchaseDetailScreen] Factura COMPRA: ID=${factura.id}, Estado=${factura.estado}")
        }
    }

    LaunchedEffect(facturasVentas) {
        println("📊 [PurchaseDetailScreen] facturasVentas actualizadas: ${facturasVentas.size} elementos")
        facturasVentas.take(3).forEach { factura ->  // Mostrar solo las primeras 3
            println("📊 [PurchaseDetailScreen] Factura VENTA: ID=${factura.id}, Estado=${factura.estado}")
        }
    }

    val facturasComprasMutable = remember(facturasCompras) {
        facturasCompras.toMutableStateList()
    }

    val facturasVentasMutable = remember(facturasVentas) {
        facturasVentas.toMutableStateList()
    }

    // --- LÓGICA DE FILTRADO ---
    val listaActualBase = if (sectionActive == Section.COMPRAS) facturasCompras else facturasVentas

    val listaFiltrada by remember(
        sectionActive,
        isListVisible,
        selectedStartMillis,
        selectedEndMillis,
        hasLoadedSunatData,
        facturasCompras,
        facturasVentas
    ) {
        derivedStateOf {
            if (!hasLoadedSunatData) return@derivedStateOf emptyList<Invoice>()
            if (!isListVisible) return@derivedStateOf emptyList<Invoice>()

            // Usar las fechas guardadas, no las del DatePickerState
            val start = selectedStartMillis ?: hoyMillis
            val end = selectedEndMillis ?: start

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            listaActualBase.filter { factura ->
                val fechaFacturaTime = sdf.parse(factura.fechaEmision)?.time ?: 0L
                fechaFacturaTime in start..end
            }
        }
    }

    val isSelectAllChecked by remember(sectionActive, listaFiltrada) {
        derivedStateOf {
            listaFiltrada.isNotEmpty() && listaFiltrada.all { factura ->
                // Obtener el estado actual desde el ViewModel
                if (sectionActive == Section.COMPRAS) {
                    facturasCompras.firstOrNull { it.id == factura.id }?.isSelected ?: false
                } else {
                    facturasVentas.firstOrNull { it.id == factura.id }?.isSelected ?: false
                }
            }
        }
    }

    // --- DIÁLOGO DEL WEBVIEW ---
    if (showSunatLogin) {
        SunatLoginDialog(
            onDismiss = { showSunatLogin = false },
            onLoginSuccess = {
                showSunatLogin = false
                hasLoadedSunatData = true
                isLoading = true

                // Simulamos que la App está bajando las facturas de SUNAT
                Handler(Looper.getMainLooper()).postDelayed({
                    isListVisible = true
                    isLoading = false // Quita el círculo y muestra la tabla
                    Toast.makeText(context, "Sincronización completa", Toast.LENGTH_SHORT).show()
                }, 2000)
            }
        )
    }

    // --- LÓGICA DEL DIÁLOGO DEL CALENDARIO ---
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    // OBTENER FECHAS DEL PICKER
                    val startMillisFromPicker = dateRangePickerState.selectedStartDateMillis
                    val endMillisFromPicker = dateRangePickerState.selectedEndDateMillis

                    println("📅 [DATEPICKER] Fecha seleccionada - Start: $startMillisFromPicker, End: $endMillisFromPicker")

                    if (startMillisFromPicker != null) {
                        // ¡GUARDAR EN LAS VARIABLES!
                        selectedStartMillis = startMillisFromPicker
                        selectedEndMillis = endMillisFromPicker

                        println("📅 [DATEPICKER] Fechas GUARDADAS en variables")
                    }
                    showDatePicker = false
                }) {
                    Text(
                        text = "Aceptar",
                        color = Color(0xFF1FB8B9),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(
                        text = "Cancelar",
                        color = Color(0xFFFF5A00),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = { Text("Selecciona el rango", modifier = Modifier.padding(16.dp)) },
                showModeToggle = false,
                modifier = Modifier.weight(1f)
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Título Provisional
        Text(
            text = "Registro Contable", // Aquí puedes cambiar el nombre
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 15.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), // Espacio entre botones
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón Compras
            Button(
                onClick = {
                    sectionActive = Section.COMPRAS
                    onComprasClick()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(45.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    // Cambiamos el color si está seleccionado o no
                    containerColor = if (sectionActive == Section.COMPRAS) Color(0xFFFF5A00) else Color.Gray
                )
            ) {
                Text(text = "Compras", style = MaterialTheme.typography.titleMedium)
            }

            // Botón Ventas
            Button(
                onClick = {
                    sectionActive = Section.VENTAS
                    onVentasClick()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(45.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (sectionActive == Section.VENTAS) Color(0xFFFF5A00) else Color.Gray
                )
            ) {
                Text(text = "Ventas", style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(modifier = Modifier.height(15.dp))

        // NUEVA FILA: Seleccionar Todos y Rango de Fechas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Grupo: Checkbox + Texto
            Row(
                verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)
            ) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    Checkbox(checked = isSelectAllChecked, onCheckedChange = { checked ->
                        println("🔘 [Seleccionar todos] Checked: $checked, Facturas filtradas: ${listaFiltrada.size}")
                        if (sectionActive == Section.COMPRAS) {
                            listaFiltrada.forEach { factura ->
                                println("🔘 [Seleccionando] ID: ${factura.id}")
                                viewModel.actualizarSeleccionCompras(factura.id, checked)
                            }
                        } else {
                            listaFiltrada.forEach { factura ->
                                println("🔘 [Seleccionando] ID: ${factura.id}")
                                viewModel.actualizarSeleccionVentas(factura.id, checked)
                            }
                        }
                    })
                }

                Text(
                    text = "Seleccionar todos",
                    fontSize = 12.sp,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }

            // NUEVO: Usa el componente DateRangeSelector en lugar del Surface
            DateRangeSelector(
                selectedStartMillis = selectedStartMillis,
                selectedEndMillis = selectedEndMillis,
                onDateRangeClick = { showDatePicker = true },
                modifier = Modifier
            )
        }

        Spacer(modifier = Modifier.height(15.dp))

        // 2. --- TABLA DINÁMICA ---
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .border(1.dp, Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF1FB8B9))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Sincronizando con SUNAT...",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val horizontalScrollState = rememberScrollState()
                val totalWidth = 650.dp

                Column(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                    // CABECERA
                    Row(
                        modifier = Modifier
                            .width(totalWidth)
                            .background(Color.LightGray)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(50.dp))
                        HeaderCell("RUC", 120.dp)
                        HeaderCell("Serie", 70.dp)
                        HeaderCell("Número", 90.dp)
                        HeaderCell("Fecha", 100.dp)
                        HeaderCell("Estado", 100.dp)
                        Box(
                            modifier = Modifier.width(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val todasConDetalle = listaFiltrada.count { factura ->
                                val estaSeleccionada = if (sectionActive == Section.COMPRAS) {
                                    facturasCompras.firstOrNull { it.id == factura.id }?.isSelected
                                        ?: false
                                } else {
                                    facturasVentas.firstOrNull { it.id == factura.id }?.isSelected
                                        ?: false
                                }

                                // Solo verificar las que están seleccionadas
                                estaSeleccionada && factura.estado == "CON DETALLE"
                            }

                            val haySeleccionadas = todasConDetalle > 0

                            Button(
                                onClick = {
                                    Toast.makeText(
                                        context,
                                        "✅ Se ha registrado $todasConDetalle factura(s) exitosamente",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier
                                    .height(36.dp)
                                    .width(150.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (haySeleccionadas) Color(
                                        0xFF1FB8B9
                                    ) else Color.Gray
                                ),
                                enabled = haySeleccionadas
                            ) {
                                Text(
                                    text = "Registrar",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                        // CUERPO (Listado)
                        Column(
                            modifier = Modifier
                                .width(totalWidth)
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (!isListVisible) {
                                Text(
                                    "Presione Consultar para ver registros",
                                    modifier = Modifier
                                        .width(totalWidth)
                                        .padding(20.dp),
                                    textAlign = TextAlign.Center,
                                    color = Color.Gray
                                )
                            } else if (listaFiltrada.isEmpty()) {
                                Text(
                                    "No hay resultados",
                                    modifier = Modifier
                                        .width(totalWidth)
                                        .padding(20.dp),
                                    textAlign = TextAlign.Center,
                                    color = Color.Gray
                                )
                            } else {
                                listaFiltrada.forEach { factura ->
                                    // Agrega este log para ver el estado actual de cada factura
                                    val estadoActual = if (sectionActive == Section.COMPRAS) {
                                        facturasCompras.firstOrNull { it.id == factura.id }?.isSelected
                                            ?: false
                                    } else {
                                        facturasVentas.firstOrNull { it.id == factura.id }?.isSelected
                                            ?: false
                                    }

                                    println("🔘 [Checkbox] ID: ${factura.id}, Estado en listaFiltrada: ${factura.isSelected}, Estado real en ViewModel: $estadoActual")

                                    Row(
                                        modifier = Modifier
                                            .width(totalWidth)
                                            .background(if (estadoActual) Color(0xFFF5F5F5) else Color.Transparent)
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.width(50.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Checkbox(
                                                checked = estadoActual,  // ¡IMPORTANTE! Usar el estado real
                                                onCheckedChange = { checked ->
                                                    println("🔘 [Checkbox click] ID: ${factura.id}, Checked: $checked")
                                                    if (sectionActive == Section.COMPRAS) {
                                                        viewModel.actualizarSeleccionCompras(
                                                            factura.id,
                                                            checked
                                                        )
                                                    } else {
                                                        viewModel.actualizarSeleccionVentas(
                                                            factura.id,
                                                            checked
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                        SimpleTableCell(factura.ruc, 120.dp)
                                        SimpleTableCell(factura.serie, 70.dp)
                                        SimpleTableCell(factura.numero, 90.dp)
                                        SimpleTableCell(factura.fechaEmision, 100.dp)
                                        Box(
                                            modifier = Modifier.width(100.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            InvoiceStatusCell(factura.estado)
                                        }
                                        Box(
                                            modifier = Modifier.width(120.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            TextButton(onClick = {
                                                onNavigateToDetalle(
                                                    DetailRoute(
                                                        id = factura.id,
                                                        rucProveedor = factura.ruc,
                                                        serie = factura.serie,
                                                        numero = factura.numero,
                                                        fecha = factura.fechaEmision,
                                                        razonSocial = factura.razonSocial,
                                                        tipoDocumento = factura.tipoDocumento,
                                                        moneda = factura.moneda,
                                                        costoTotal = factura.costoTotal,
                                                        igv = factura.igv,
                                                        importeTotal = factura.importeTotal,
                                                        anio = factura.anio,
                                                        tipoCambio = factura.tipoCambio,
                                                        esCompra = (sectionActive == Section.COMPRAS)
                                                    )
                                                )
                                            }) {
                                                Text(
                                                    "Detalle",
                                                    fontSize = 12.sp,
                                                    textDecoration = TextDecoration.Underline
                                                )
                                            }
                                        }
                                    }
                                    Divider(
                                        modifier = Modifier.width(totalWidth),
                                        thickness = 0.5.dp,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }
                        // --- FOOTER
                        if (isListVisible) {
                            val seleccionados = listaFiltrada.count { factura ->
                                val estadoReal = if (sectionActive == Section.COMPRAS) {
                                    facturasCompras.firstOrNull { it.id == factura.id }?.isSelected
                                        ?: false
                                } else {
                                    facturasVentas.firstOrNull { it.id == factura.id }?.isSelected
                                        ?: false
                                }
                                estadoReal
                            }
                            Row(
                                modifier = Modifier
                                    .width(totalWidth)
                                    .background(Color.LightGray)
                                    .padding(vertical = 10.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Text(
                                    text = when {
                                        seleccionados == 1 -> "1 factura seleccionada de ${listaFiltrada.size}"
                                        seleccionados > 1 -> "$seleccionados facturas seleccionadas de ${listaFiltrada.size}"
                                        else -> "Facturas registradas: ${listaFiltrada.size}"
                                    },
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }

            // 3. BOTONES INFERIORES: CONSULTAR Y REGISTRAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val token = SunatPrefs.getToken(context)
                        val ruc = SunatPrefs.getRuc(context)
                        val user = SunatPrefs.getUser(context)

                        println("SUNAT DATA → RUC: $ruc | USUARIO: $user | TOKEN: $token")
                        if (token == null) {
                            showSunatLogin = true // Abre el WebView si no hay hash
                        } else {
                            if (!hasLoadedSunatData) {
                                hasLoadedSunatData = true
                            }
                            isLoading = true
                            Handler(Looper.getMainLooper()).postDelayed({
                                isListVisible = true
                                isLoading = false
                            }, 1000)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(45.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1FB8B9))
                ) {
                    Text("Consultar", style = MaterialTheme.typography.titleMedium)
                }
                if (sectionActive == Section.COMPRAS) {
                    Button(
                        onClick = onNavigateToRegistrar,
                        modifier = Modifier
                            .weight(1f)
                            .height(45.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1FB8B9))
                    ) { Text("Subir Factura", style = MaterialTheme.typography.titleMedium) }
                }
            }
        }
    }

    @Preview(showBackground = true, showSystemUi = true)
    @Composable
    fun PurchaseScreenPreview() {
        val viewModel: InvoiceViewModel = viewModel()
        PurchaseDetailScreen(
            viewModel = viewModel,
            onComprasClick = { },
            onVentasClick = { },
            onNavigateToRegistrar = { },
            onNavigateToDetalle = { })
    }