package com.example.purchaseregister.view.purchase

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
import com.example.purchaseregister.navigation.DetailRoute
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.purchaseregister.utils.*
import com.example.purchaseregister.components.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.example.purchaseregister.viewmodel.InvoiceViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.outlined.PowerSettingsNew

enum class Section { COMPRAS, VENTAS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseDetailScreen(
    purchaseViewModel: PurchaseViewModel,
    invoiceViewModel: InvoiceViewModel,
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
    var showCustomDatePicker by remember { mutableStateOf(false) }

    // Fechas
    val hoyMillis = remember { getHoyMillisPeru() }
    val primerDiaMes = remember { getPrimerDiaMesPeru(hoyMillis) }
    val ultimoDiaMes = remember { getUltimoDiaMesPeru(hoyMillis) }

    var selectedStartMillis by rememberSaveable { mutableStateOf<Long?>(primerDiaMes) }
    var selectedEndMillis by rememberSaveable { mutableStateOf<Long?>(ultimoDiaMes) }

    var showLoadingDialog by remember { mutableStateOf(false) }
    var loadingStatus by remember { mutableStateOf("Obteniendo detalle de factura...") }
    var loadingDebugInfo by remember { mutableStateOf<String?>(null) }
    var facturaCargandoId by remember { mutableStateOf<Int?>(null) }
    var esCompraCargando by remember { mutableStateOf(false) }
    var facturasConTimerActivo by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isDetallandoTodos by remember { mutableStateOf(false) }

    // Variables para el diálogo de credenciales
    var rucInput by remember { mutableStateOf("") }
    var usuarioInput by remember { mutableStateOf("") }
    var claveSolInput by remember { mutableStateOf("") }
    var showCredencialesDialog by remember { mutableStateOf(false) }
    var consultarDespuesDeLogin by remember { mutableStateOf(false) }

    // Variables para el diálogo de logout
    var showLogoutDialog by remember { mutableStateOf(false) }

    // ViewModel states
    val isLoadingViewModel by purchaseViewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by purchaseViewModel.errorMessage.collectAsStateWithLifecycle()
    val facturasCompras by purchaseViewModel.facturasCompras.collectAsStateWithLifecycle()
    val facturasVentas by purchaseViewModel.facturasVentas.collectAsStateWithLifecycle()

    // Usar funciones extraídas
    handleAutoRegistroFacturas(
        facturasCompras = facturasCompras,
        facturasVentas = facturasVentas,
        facturasConTimerActivo = facturasConTimerActivo,
        purchaseViewModel = purchaseViewModel,
        invoiceViewModel = invoiceViewModel,
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
        viewModel = purchaseViewModel,
        onIsLoadingChange = { isLoading = it },
        onLoadingDialogChange = { showLoadingDialog = it },
        onNavigateToDetalle = { id, esCompra ->
            onNavigateToDetalle(DetailRoute(id = id, esCompra = esCompra))
        },
        onLoadingStatusChange = { loadingStatus = it },
        onLoadingDebugInfoChange = { loadingDebugInfo = it },
        onFacturaCargandoIdChange = { facturaCargandoId = it }
    )

    // Efecto inicial
    LaunchedEffect(Unit) {
        delay(500)
        val ruc = SunatPrefs.getRuc(context)
        val usuario = SunatPrefs.getUser(context)
        val claveSol = SunatPrefs.getClaveSol(context)

        if (ruc != null && usuario != null && claveSol != null) {
            val periodoInicio = convertirFechaAPeriodo(selectedStartMillis ?: hoyMillis)
            val periodoFin = convertirFechaAPeriodo(selectedEndMillis ?: hoyMillis)

            purchaseViewModel.cargarFacturasDesdeAPI(
                periodoInicio = periodoInicio,
                periodoFin = periodoFin,
                esCompra = (sectionActive == Section.COMPRAS),
                ruc = ruc,
                usuario = usuario,
                claveSol = claveSol
            )

            isListVisible = true

            Toast.makeText(
                context,
                "🔄 Cargando facturas automáticamente...",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            consultarDespuesDeLogin = true
            showCredencialesDialog = true
        }
    }

    // Calcular lista filtrada
    val listaFiltrada = calculateFilteredList(
        sectionActive = sectionActive,
        isListVisible = isListVisible,
        selectedStartMillis = selectedStartMillis,
        selectedEndMillis = selectedEndMillis,
        hasLoadedSunatData = true,
        facturasCompras = facturasCompras,
        facturasVentas = facturasVentas,
        hoyMillis = hoyMillis
    )

    val hayFacturasEnProceso by remember {
        derivedStateOf {
            listaFiltrada.any { it.estado == "EN PROCESO" }
        }
    }

    // Diálogo de credenciales
    if (showCredencialesDialog) {
        var isValidando by remember { mutableStateOf(false) }
        var errorMensaje by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {
                if (!isValidando) {
                    showCredencialesDialog = false
                    rucInput = ""
                    usuarioInput = ""
                    claveSolInput = ""
                    consultarDespuesDeLogin = false
                    errorMensaje = null
                }
            },
            title = { Text("Credenciales SUNAT") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Complete sus credenciales SUNAT para continuar:")

                    // RUC
                    OutlinedTextField(
                        value = rucInput,
                        onValueChange = {
                            val nuevoValor = it.filter { char -> char.isDigit() }
                            if (nuevoValor.length <= 11) {
                                rucInput = nuevoValor
                                errorMensaje = null
                            }
                        },
                        label = { Text("RUC") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorMensaje != null,
                        supportingText = if (errorMensaje != null) {
                            { Text(errorMensaje!!, color = Color.Red) }
                        } else {
                            { Text("${rucInput.length}/11 dígitos") }
                        }
                    )

                    // Usuario (siempre mayúsculas)
                    OutlinedTextField(
                        value = usuarioInput,
                        onValueChange = {
                            usuarioInput = it.uppercase()
                            errorMensaje = null
                        },
                        label = { Text("Usuario SOL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorMensaje != null
                    )

                    // Clave SOL
                    OutlinedTextField(
                        value = claveSolInput,
                        onValueChange = {
                            if (it.length <= 12) {
                                claveSolInput = it
                                errorMensaje = null
                            }
                        },
                        label = { Text("Clave SOL") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorMensaje != null,
                        supportingText = {
                            Text("${claveSolInput.length}/12 caracteres")
                        }
                    )

                    if (isValidando) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFF1FB8B9)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Validando credenciales con SUNAT...",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            if (rucInput.length != 11) {
                                errorMensaje = "El RUC debe tener 11 dígitos"
                                return@launch
                            }

                            if (claveSolInput.isEmpty()) {
                                errorMensaje = "La clave SOL no puede estar vacía"
                                return@launch
                            }

                            isValidando = true
                            errorMensaje = null

                            val esValido = purchaseViewModel.validarCredencialesSUNAT(
                                ruc = rucInput,
                                usuario = usuarioInput,
                                claveSol = claveSolInput,
                            )

                            isValidando = false

                            if (esValido) {
                                SunatPrefs.saveRuc(context, rucInput)
                                SunatPrefs.saveUser(context, usuarioInput)
                                SunatPrefs.saveClaveSol(context, claveSolInput)

                                if (consultarDespuesDeLogin) {
                                    val periodoInicio = convertirFechaAPeriodo(selectedStartMillis ?: hoyMillis)
                                    val periodoFin = convertirFechaAPeriodo(selectedEndMillis ?: hoyMillis)

                                    purchaseViewModel.cargarFacturasDesdeAPI(
                                        periodoInicio = periodoInicio,
                                        periodoFin = periodoFin,
                                        esCompra = (sectionActive == Section.COMPRAS),
                                        ruc = rucInput,
                                        usuario = usuarioInput,
                                        claveSol = claveSolInput
                                    )

                                    isListVisible = true
                                    consultarDespuesDeLogin = false
                                }

                                showCredencialesDialog = false
                                rucInput = ""
                                usuarioInput = ""
                                claveSolInput = ""

                                Toast.makeText(
                                    context,
                                    "✅ Credenciales válidas. Guardadas correctamente.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                errorMensaje = "Credenciales incorrectas. Verifique RUC, Usuario y Clave SOL."
                            }
                        }
                    },
                    enabled = !isValidando &&
                            rucInput.length == 11 &&
                            usuarioInput.isNotEmpty() &&
                            claveSolInput.isNotEmpty() &&
                            claveSolInput.length <= 12
                ) {
                    if (isValidando) {
                        Text("Validando...")
                    } else {
                        Text("Validar y Guardar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isValidando) {
                            showCredencialesDialog = false
                            rucInput = ""
                            usuarioInput = ""
                            claveSolInput = ""
                            consultarDespuesDeLogin = false
                            errorMensaje = null
                        }
                    },
                    enabled = !isValidando
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de logout
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = {
                showLogoutDialog = false
            },
            title = { Text("Cerrar Sesión") },
            text = { Text("¿Estás seguro de que deseas cerrar sesión?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Limpiar credenciales guardadas
                        SunatPrefs.clearCredentials(context)

                        // Limpiar lista de facturas
                        purchaseViewModel.limpiarFacturas()

                        // Resetear estados
                        isListVisible = false
                        showLogoutDialog = false

                        // Volver a mostrar el diálogo de credenciales
                        consultarDespuesDeLogin = true
                        showCredencialesDialog = true

                        Toast.makeText(
                            context,
                            "Sesión cerrada. Ingrese nuevas credenciales.",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                    }
                ) {
                    Text("Cancelar")
                }
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

        Spacer(modifier = Modifier.height(16.dp))

        // TÍTULO CON ICONO DE LOGOUT A LA DERECHA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Registro Contable",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )

            IconButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.PowerSettingsNew,
                    contentDescription = "Cerrar sesión",
                    tint = Color(0xFF1FB8B9),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        if (listaFiltrada.isEmpty()) {
                            Toast.makeText(
                                context,
                                "No hay facturas en lista para detallar",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@IconButton
                        }

                        val facturasProcesables = listaFiltrada.filter { factura ->
                            factura.estado != "CON DETALLE" &&
                                    factura.estado != "REGISTRADO" &&
                                    factura.estado != "EN PROCESO"
                        }

                        if (facturasProcesables.isEmpty()) {
                            Toast.makeText(
                                context,
                                "Todas las facturas ya tienen detalle o están en proceso",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@IconButton
                        }

                        val ruc = SunatPrefs.getRuc(context)
                        val usuario = SunatPrefs.getUser(context)
                        val claveSol = SunatPrefs.getClaveSol(context)

                        if (ruc == null || usuario == null || claveSol == null) {
                            Toast.makeText(
                                context,
                                "⚠️ Primero configure sus credenciales SUNAT en el botón CONSULTAR",
                                Toast.LENGTH_LONG
                            ).show()
                            return@IconButton
                        }

                        coroutineScope.launch {
                            var exitosas = 0
                            var fallidas = 0
                            val total = facturasProcesables.size

                            isDetallandoTodos = true
                            loadingStatus = "Procesando 0/$total facturas..."

                            facturasProcesables.forEach { factura ->
                                val currentIsCompra = (sectionActive == Section.COMPRAS)
                                val rucEmisor = purchaseViewModel.getRucEmisor(factura.id) ?: factura.ruc

                                val resultado = suspendCoroutine { continuation ->
                                    purchaseViewModel.cargarDetalleFacturaXmlConUsuario(
                                        facturaId = factura.id,
                                        esCompra = currentIsCompra,
                                        rucEmisor = rucEmisor,
                                        context = context
                                    ) { success, _ ->
                                        continuation.resume(success)
                                    }
                                }

                                if (resultado) exitosas++ else fallidas++

                                withContext(Dispatchers.Main) {
                                    loadingStatus = "Procesando ${exitosas + fallidas}/$total facturas...\n✅ Exitosas: $exitosas\n❌ Fallidas: $fallidas"
                                }

                                delay(300)
                            }

                            withContext(Dispatchers.Main) {
                                isDetallandoTodos = false
                                Toast.makeText(
                                    context,
                                    "✅ Proceso completado: $exitosas exitosas, $fallidas fallidas",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    modifier = Modifier.size(40.dp),
                    enabled = !hayFacturasEnProceso && !isDetallandoTodos
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Detallar todas las facturas",
                        tint = if (hayFacturasEnProceso || isDetallandoTodos)
                            Color.Gray
                        else
                            Color(0xFF1FB8B9),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Detallar",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1FB8B9),
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "todas",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1FB8B9),
                        lineHeight = 16.sp
                    )
                }
            }

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
                                "Presione CONSULTAR para iniciar sesion",
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
                                            text = factura.razonSocial ?: "",
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
                                                        if (factura.estado == "EN PROCESO") {
                                                            return@IconButton
                                                        }

                                                        val currentIsCompra = (sectionActive == Section.COMPRAS)

                                                        val ruc = SunatPrefs.getRuc(context)
                                                        val usuario = SunatPrefs.getUser(context)
                                                        val claveSol = SunatPrefs.getClaveSol(context)

                                                        if (ruc == null || usuario == null || claveSol == null) {
                                                            Toast.makeText(
                                                                context,
                                                                "⚠️ Primero configure sus credenciales SUNAT en el botón CONSULTAR",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            return@IconButton
                                                        }

                                                        if (factura.estado == "CON DETALLE" || factura.estado == "REGISTRADO") {
                                                            onNavigateToDetalle(
                                                                DetailRoute(
                                                                    id = factura.id,
                                                                    esCompra = currentIsCompra
                                                                )
                                                            )
                                                            return@IconButton
                                                        }

                                                        val rucEmisor = purchaseViewModel.getRucEmisor(factura.id) ?: factura.ruc

                                                        purchaseViewModel.cargarDetalleFacturaXmlConUsuario(
                                                            facturaId = factura.id,
                                                            esCompra = currentIsCompra,
                                                            rucEmisor = rucEmisor,
                                                            context = context
                                                        ) { success, message ->
                                                            if (success) {
                                                                Toast.makeText(context, "✅ $message", Toast.LENGTH_SHORT).show()
                                                            } else {
                                                                Toast.makeText(context, "❌ $message", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.size(24.dp),
                                                    enabled = factura.estado != "EN PROCESO"
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Visibility,
                                                        contentDescription = "Ver detalle",
                                                        modifier = Modifier.size(20.dp),
                                                        tint = if (factura.estado == "EN PROCESO")
                                                            Color.Gray
                                                        else
                                                            Color.Black
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
                    val ruc = SunatPrefs.getRuc(context)
                    val usuario = SunatPrefs.getUser(context)
                    val claveSol = SunatPrefs.getClaveSol(context)

                    if (ruc == null || usuario == null || claveSol == null) {
                        consultarDespuesDeLogin = true
                        showCredencialesDialog = true
                        return@Button
                    }

                    val periodoInicio = convertirFechaAPeriodo(selectedStartMillis ?: hoyMillis)
                    val periodoFin = convertirFechaAPeriodo(selectedEndMillis ?: hoyMillis)

                    purchaseViewModel.cargarFacturasDesdeAPI(
                        periodoInicio = periodoInicio,
                        periodoFin = periodoFin,
                        esCompra = (sectionActive == Section.COMPRAS),
                        ruc = ruc,
                        usuario = usuario,
                        claveSol = claveSol
                    )
                    isListVisible = true
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
    val purchaseViewModel: PurchaseViewModel = viewModel()
    val invoiceViewModel: InvoiceViewModel = viewModel()
    PurchaseDetailScreen(
        purchaseViewModel = purchaseViewModel,
        invoiceViewModel = invoiceViewModel,
        onComprasClick = { },
        onVentasClick = { },
        onNavigateToRegistrar = { },
        onNavigateToDetalle = { }
    )
}