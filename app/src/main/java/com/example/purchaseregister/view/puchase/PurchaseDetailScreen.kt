package com.example.purchaseregister.view.puchase

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purchaseregister.model.Invoice
import com.example.purchaseregister.navigation.DetailRoute
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.purchaseregister.viewmodel.InvoiceViewModel
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.purchaseregister.utils.*
import com.example.purchaseregister.components.*
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon

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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Estados
    var sectionActive by remember { mutableStateOf(Section.COMPRAS) }
    var isListVisible by rememberSaveable { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showSunatLogin by remember { mutableStateOf(false) }
    var showCustomDatePicker by remember { mutableStateOf(false) }
    var hasLoadedSunatData by rememberSaveable {
        mutableStateOf(SunatPrefs.getToken(context) != null)
    }
    var selectedStartMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedEndMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var showLoadingDialog by remember { mutableStateOf(false) }
    var loadingStatus by remember { mutableStateOf("Obteniendo detalle de factura...") }
    var loadingDebugInfo by remember { mutableStateOf<String?>(null) }
    var facturaCargandoId by remember { mutableStateOf<Int?>(null) }
    var esCompraCargando by remember { mutableStateOf(false) }
    var showClaveSolDialog by remember { mutableStateOf(false) }
    var claveSolInput by remember { mutableStateOf("") }
    var facturaParaDetalle by remember { mutableStateOf<Invoice?>(null) }
    var esCompraParaDetalle by remember { mutableStateOf(false) }
    var facturasConTimerActivo by remember { mutableStateOf<Set<Int>>(emptySet()) }

    val hoyMillis = remember { getHoyMillisPeru() }

    // ViewModel states
    val isLoadingViewModel by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val facturasCompras by viewModel.facturasCompras.collectAsStateWithLifecycle()
    val facturasVentas by viewModel.facturasVentas.collectAsStateWithLifecycle()

    // Usar funciones extraídas
    handleAutoRegistroFacturas(
        facturasCompras = facturasCompras,
        facturasVentas = facturasVentas,
        facturasConTimerActivo = facturasConTimerActivo,
        viewModel = viewModel,
        context = context,
        onTimerUpdate = { newTimers ->
            facturasConTimerActivo = newTimers
        }
    )

    setupCommonEffects(
        isLoadingViewModel = isLoadingViewModel,
        errorMessage = errorMessage,
        showLoadingDialog = showLoadingDialog,
        facturaCargandoId = facturaCargandoId,
        esCompraCargando = esCompraCargando,
        viewModel = viewModel,
        onIsLoadingChange = { isLoading = it },
        onLoadingDialogChange = { showLoadingDialog = it },
        onNavigateToDetalle = { id, esCompra ->
            onNavigateToDetalle(DetailRoute(id = id, esCompra = esCompra))
        },
        onLoadingStatusChange = { loadingStatus = it },
        onLoadingDebugInfoChange = { loadingDebugInfo = it },
        onFacturaCargandoIdChange = { facturaCargandoId = it }
    )

    // Efectos adicionales
    LaunchedEffect(facturasCompras, facturasVentas) {
        val totalFacturas = if (sectionActive == Section.COMPRAS) facturasCompras.size else facturasVentas.size
        if (totalFacturas > 0 && !isListVisible) {
            isListVisible = true
        }
    }

    LaunchedEffect(Unit) {
        if (SunatPrefs.getToken(context) != null && !hasLoadedSunatData) {
            hasLoadedSunatData = true
        }
    }

    LaunchedEffect(selectedStartMillis) {
        if (selectedStartMillis != null && !isListVisible && hasLoadedSunatData) {
            isListVisible = true
        }
    }

    // Calcular lista filtrada usando función extraída
    val listaFiltrada = calculateFilteredList(
        sectionActive = sectionActive,
        isListVisible = isListVisible,
        selectedStartMillis = selectedStartMillis,
        selectedEndMillis = selectedEndMillis,
        hasLoadedSunatData = hasLoadedSunatData,
        facturasCompras = facturasCompras,
        facturasVentas = facturasVentas,
        hoyMillis = hoyMillis
    )

    // Diálogos
    if (showSunatLogin) {
        SunatLoginDialog(
            onDismiss = { showSunatLogin = false },
            onLoginSuccess = {
                showSunatLogin = false
                hasLoadedSunatData = true

                val periodoInicio = convertirFechaAPeriodo(selectedStartMillis ?: hoyMillis)
                val periodoFin = convertirFechaAPeriodo(selectedEndMillis ?: hoyMillis)

                viewModel.cargarFacturasDesdeAPI(
                    periodoInicio = periodoInicio,
                    periodoFin = periodoFin,
                    esCompra = (sectionActive == Section.COMPRAS)
                )

                isListVisible = true

                Toast.makeText(context, "✅ Login exitoso. Cargando facturas...", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showCustomDatePicker) {
        CustomDatePickerDialog(
            onDismiss = { showCustomDatePicker = false },
            onPeriodoSelected = { inicio, fin ->
                selectedStartMillis = inicio
                selectedEndMillis = fin
                showCustomDatePicker = false
            },
            onRangoSelected = { inicio, fin ->
                selectedStartMillis = inicio
                selectedEndMillis = fin
                showCustomDatePicker = false
            },
            initialStartMillis = selectedStartMillis,
            initialEndMillis = selectedEndMillis
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        FacturaLoadingDialog(
            isLoading = showLoadingDialog,
            statusMessage = loadingStatus,
            debugInfo = loadingDebugInfo,
            onDismiss = {
                showLoadingDialog = false
                loadingDebugInfo = null
                facturaCargandoId = null
            }
        )

        if (showClaveSolDialog) {
            AlertDialog(
                onDismissRequest = {
                    showClaveSolDialog = false
                    claveSolInput = ""
                    facturaParaDetalle = null
                },
                title = { Text("Clave SOL requerida") },
                text = {
                    Column {
                        Text("Por seguridad ingrese su Clave SOL nuevamente.")
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = claveSolInput,
                            onValueChange = { claveSolInput = it },
                            label = { Text("Clave SOL") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (claveSolInput.isNotEmpty()) {
                                SunatPrefs.saveClaveSol(context, claveSolInput)

                                facturaParaDetalle?.let { factura ->
                                    showClaveSolDialog = false
                                    claveSolInput = ""

                                    showLoadingDialog = true
                                    facturaCargandoId = factura.id
                                    esCompraCargando = esCompraParaDetalle
                                    loadingStatus = "Obteniendo detalle de factura..."

                                    val rucEmisor = viewModel.getRucEmisor(factura.id) ?: factura.ruc

                                    viewModel.cargarDetalleFacturaXmlConUsuario(
                                        facturaId = factura.id,
                                        esCompra = esCompraParaDetalle,
                                        rucEmisor = rucEmisor,
                                        context = context
                                    ) { success, message ->
                                        if (success) {
                                            loadingStatus = "✅ " + (message ?: "Detalles obtenidos exitosamente")

                                            coroutineScope.launch {
                                                kotlinx.coroutines.delay(1500)
                                                showLoadingDialog = false
                                                loadingDebugInfo = null

                                                onNavigateToDetalle(
                                                    DetailRoute(
                                                        id = factura.id,
                                                        esCompra = esCompraParaDetalle
                                                    )
                                                )
                                            }
                                        } else {
                                            loadingStatus = "❌ " + (message ?: "Error desconocido")

                                            coroutineScope.launch {
                                                kotlinx.coroutines.delay(3000)
                                                showLoadingDialog = false
                                                loadingDebugInfo = null
                                                facturaCargandoId = null
                                            }
                                        }
                                    }
                                }

                                facturaParaDetalle = null
                            }
                        },
                        enabled = claveSolInput.isNotEmpty()
                    ) {
                        Text("Guardar y Continuar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showClaveSolDialog = false
                            claveSolInput = ""
                            facturaParaDetalle = null
                        }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Registro Contable",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 15.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    containerColor = if (sectionActive == Section.COMPRAS) Color(0xFFFF5A00) else Color.Gray
                )
            ) {
                Text(text = "Compras", style = MaterialTheme.typography.titleMedium)
            }

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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "Selecciona periodo o rango de fecha:",
                fontSize = 10.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
            DateRangeSelector(
                selectedStartMillis = selectedStartMillis,
                selectedEndMillis = selectedEndMillis,
                onDateRangeClick = { showCustomDatePicker = true },
                modifier = Modifier
            )
        }

        Spacer(modifier = Modifier.height(15.dp))

        status(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

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
                        text = "Cargando facturas desde SUNAT...",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val horizontalScrollState = rememberScrollState()
                val totalWidth = 470.dp

                Column(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                    Row(
                        modifier = Modifier
                            .width(totalWidth)
                            .background(Color.LightGray)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HeaderCell("Estado", 100.dp)
                        HeaderCell("RUC", 110.dp)
                        HeaderCell("Serie - Número", 160.dp)
                        HeaderCell("Fecha", 100.dp)
                    }

                    Column(
                        modifier = Modifier
                            .width(totalWidth)
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (!isListVisible) {
                            Text(
                                "Presione CONSULTAR para ver registros",
                                modifier = Modifier
                                    .width(totalWidth)
                                    .padding(20.dp),
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                        } else if (listaFiltrada.isEmpty()) {
                            Text(
                                "No hay facturas para mostrar",
                                modifier = Modifier
                                    .width(totalWidth)
                                    .padding(20.dp),
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                        } else {
                            listaFiltrada.forEachIndexed { index, factura ->
                                Column(
                                    modifier = Modifier.width(totalWidth)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .width(totalWidth)
                                            .background(Color(0xFFB0C4DE))
                                            .padding(vertical = 8.dp, horizontal = 8.dp)
                                    ) {
                                        Text(
                                            text = if (sectionActive == Section.COMPRAS) "${factura.razonSocial}"
                                            else "${factura.razonSocial}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Black
                                        )
                                    }
                                    Row(
                                        modifier = Modifier
                                            .width(totalWidth)
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.width(100.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        val currentId = factura.id
                                                        val currentIsCompra = (sectionActive == Section.COMPRAS)

                                                        val ruc = SunatPrefs.getRuc(context)
                                                        val usuario = SunatPrefs.getUser(context)
                                                        val claveSol = SunatPrefs.getClaveSol(context)

                                                        if (ruc == null || usuario == null) {
                                                            showSunatLogin = true
                                                            return@IconButton
                                                        }

                                                        if (factura.estado == "CON DETALLE") {
                                                            onNavigateToDetalle(
                                                                DetailRoute(
                                                                    id = factura.id,
                                                                    esCompra = currentIsCompra
                                                                )
                                                            )
                                                        } else {
                                                            if (claveSol == null) {
                                                                facturaParaDetalle = factura
                                                                esCompraParaDetalle = currentIsCompra
                                                                showClaveSolDialog = true
                                                            } else {
                                                                showLoadingDialog = true
                                                                facturaCargandoId = currentId
                                                                esCompraCargando = currentIsCompra
                                                                loadingStatus = "Obteniendo detalle de factura..."

                                                                val rucEmisor = viewModel.getRucEmisor(factura.id) ?: factura.ruc

                                                                viewModel.cargarDetalleFacturaXmlConUsuario(
                                                                    facturaId = factura.id,
                                                                    esCompra = currentIsCompra,
                                                                    rucEmisor = rucEmisor,
                                                                    context = context
                                                                ) { success, message ->
                                                                    if (success) {
                                                                        loadingStatus = "✅ " + (message ?: "Detalles obtenidos exitosamente")

                                                                        coroutineScope.launch {
                                                                            kotlinx.coroutines.delay(1500)
                                                                            showLoadingDialog = false
                                                                            loadingDebugInfo = null

                                                                            onNavigateToDetalle(
                                                                                DetailRoute(
                                                                                    id = currentId,
                                                                                    esCompra = currentIsCompra
                                                                                )
                                                                            )
                                                                        }
                                                                    } else {
                                                                        loadingStatus = "❌ " + (message ?: "Error desconocido")

                                                                        coroutineScope.launch {
                                                                            kotlinx.coroutines.delay(3000)
                                                                            showLoadingDialog = false
                                                                            loadingDebugInfo = null
                                                                            facturaCargandoId = null
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Visibility,
                                                        contentDescription = "Ver detalle",
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                InvoiceStatusCircle(factura.estado, tamano = 14.dp)
                                            }
                                        }
                                        SimpleTableCell(factura.ruc, 110.dp)
                                        Box(
                                            modifier = Modifier.width(160.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${factura.serie} - ${factura.numero}",
                                                fontSize = 13.sp,
                                                color = Color.Black
                                            )
                                        }
                                        SimpleTableCell(factura.fechaEmision, 100.dp)
                                    }
                                    if (index < listaFiltrada.size - 1) {
                                        Divider(
                                            modifier = Modifier.width(totalWidth),
                                            thickness = 0.5.dp,
                                            color = Color.LightGray
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isListVisible) {
                        Row(
                            modifier = Modifier
                                .width(totalWidth)
                                .background(Color.LightGray)
                                .padding(vertical = 10.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                text = "Facturas registradas: ${listaFiltrada.size}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }

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

                    if (token == null) {
                        showSunatLogin = true
                    } else {
                        val periodoInicio = convertirFechaAPeriodo(selectedStartMillis ?: hoyMillis)
                        val periodoFin = convertirFechaAPeriodo(selectedEndMillis ?: hoyMillis)

                        viewModel.cargarFacturasDesdeAPI(
                            periodoInicio = periodoInicio,
                            periodoFin = periodoFin,
                            esCompra = (sectionActive == Section.COMPRAS),
                        )

                        isListVisible = true
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
                ) {
                    Text("Subir Factura", style = MaterialTheme.typography.titleMedium)
                }
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
        onNavigateToDetalle = { }
    )
}