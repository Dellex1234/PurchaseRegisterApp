package com.example.purchaseregister.view.purchase

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.runtime.*
import com.example.purchaseregister.model.Invoice
import com.example.purchaseregister.utils.*
import com.example.purchaseregister.viewmodel.InvoiceViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Función para convertir fecha a período
fun convertirFechaAPeriodo(millis: Long): String {
    val calendar = Calendar.getInstance(PERU_TIME_ZONE).apply {
        timeInMillis = millis
    }
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1
    return "${year}${String.format("%02d", month)}"
}

// Función para calcular lista filtrada
@Composable
fun calculateFilteredList(
    sectionActive: Section,
    isListVisible: Boolean,
    selectedStartMillis: Long?,
    selectedEndMillis: Long?,
    hasLoadedSunatData: Boolean,
    facturasCompras: List<Invoice>,
    facturasVentas: List<Invoice>,
    hoyMillis: Long
): List<Invoice> {
    return remember(
        sectionActive,
        isListVisible,
        selectedStartMillis,
        selectedEndMillis,
        hasLoadedSunatData,
        facturasCompras,
        facturasVentas
    ) {
        derivedStateOf {
            if (!hasLoadedSunatData) return@derivedStateOf emptyList()
            if (!isListVisible) return@derivedStateOf emptyList()

            val start = selectedStartMillis ?: hoyMillis
            val end = selectedEndMillis ?: start

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
                timeZone = PERU_TIME_ZONE
            }

            val listaActualBase = if (sectionActive == Section.COMPRAS) facturasCompras else facturasVentas

            val facturasFiltradas = listaActualBase.filter { factura ->
                try {
                    val fechaFacturaTime = sdf.parse(factura.fechaEmision)?.time ?: 0L
                    fechaFacturaTime in start..end
                } catch (e: Exception) {
                    false
                }
            }

            facturasFiltradas.sortedByDescending { factura ->
                try {
                    sdf.parse(factura.fechaEmision)?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
            }
        }.value
    }
}

// Función para manejar auto-registro de facturas
@Composable
fun handleAutoRegistroFacturas(
    facturasCompras: List<Invoice>,
    facturasVentas: List<Invoice>,
    facturasConTimerActivo: Set<Int>,
    viewModel: InvoiceViewModel,
    context: Context,
    onTimerUpdate: (Set<Int>) -> Unit
) {
    LaunchedEffect(facturasCompras, facturasVentas) {
        val todasLasFacturas = facturasCompras + facturasVentas

        val facturasParaAutoRegistrar = todasLasFacturas.filter { factura ->
            factura.estado == "CON DETALLE" && !facturasConTimerActivo.contains(factura.id)
        }

        facturasParaAutoRegistrar.forEach { factura ->
            val nuevosTimers = facturasConTimerActivo + factura.id
            onTimerUpdate(nuevosTimers)

            launch {
                delay(10000L)

                val estadoActual = todasLasFacturas.firstOrNull { it.id == factura.id }?.estado

                if (estadoActual == "CON DETALLE") {
                    val esCompra = facturasCompras.any { it.id == factura.id }
                    val listaFacturasParaRegistrar = listOf(factura)

                    viewModel.registrarFacturasEnBaseDeDatos(
                        facturas = listaFacturasParaRegistrar,
                        esCompra = esCompra,
                        context = context,
                        mostrarLoading = false
                    )

                    viewModel.actualizarEstadoFactura(
                        facturaId = factura.id,
                        nuevoEstado = "REGISTRADO",
                        esCompra = esCompra
                    )

                    Toast.makeText(
                        context,
                        "✅ Factura ${factura.serie}-${factura.numero} registrada automáticamente",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                onTimerUpdate(facturasConTimerActivo - factura.id)
            }
        }

        val facturasConDetalle = todasLasFacturas.filter { it.estado == "CON DETALLE" }.map { it.id }.toSet()
        val timersParaLimpiar = facturasConTimerActivo.filter { !facturasConDetalle.contains(it) }
        if (timersParaLimpiar.isNotEmpty()) {
            onTimerUpdate(facturasConTimerActivo.filter { facturasConDetalle.contains(it) }.toSet())
        }
    }
}

// Función para setup de efectos comunes
@Composable
fun setupCommonEffects(
    isLoadingViewModel: Boolean,
    errorMessage: String?,
    showLoadingDialog: Boolean,
    facturaCargandoId: Int?,
    esCompraCargando: Boolean,
    viewModel: InvoiceViewModel,
    onIsLoadingChange: (Boolean) -> Unit,
    onLoadingDialogChange: (Boolean) -> Unit,
    onNavigateToDetalle: (Int, Boolean) -> Unit,
    onLoadingStatusChange: (String) -> Unit,
    onLoadingDebugInfoChange: (String?) -> Unit,
    onFacturaCargandoIdChange: (Int?) -> Unit
) {
    // Efecto para isLoading
    LaunchedEffect(isLoadingViewModel) {
        onIsLoadingChange(isLoadingViewModel)

        if (!isLoadingViewModel && showLoadingDialog) {
            Handler(Looper.getMainLooper()).postDelayed({
                onLoadingDialogChange(false)
                onLoadingDebugInfoChange(null)

                facturaCargandoId?.let { id ->
                    onNavigateToDetalle(id, esCompraCargando)
                }
                onFacturaCargandoIdChange(null)
            }, 1500)
        }
    }

    // Efecto para errorMessage
    LaunchedEffect(errorMessage) {
        errorMessage?.let { mensaje ->
            if (showLoadingDialog) {
                onLoadingStatusChange("Error: $mensaje")
                Handler(Looper.getMainLooper()).postDelayed({
                    onLoadingDialogChange(false)
                    onLoadingDebugInfoChange(null)
                    onFacturaCargandoIdChange(null)
                    viewModel.limpiarError()
                }, 3000)
            }
        }
    }
}

// Función para el botón Consultar
fun handleConsultarClick(
    context: Context,
    selectedStartMillis: Long?,
    selectedEndMillis: Long?,
    hoyMillis: Long,
    sectionActive: Section,
    viewModel: InvoiceViewModel,
    onShowSunatLogin: () -> Unit,
    onShowList: () -> Unit
) {
    val token = SunatPrefs.getToken(context)
    val ruc = SunatPrefs.getRuc(context)
    val user = SunatPrefs.getUser(context)

    if (token == null) {
        onShowSunatLogin()
    } else {
        val periodoInicio = convertirFechaAPeriodo(selectedStartMillis ?: hoyMillis)
        val periodoFin = convertirFechaAPeriodo(selectedEndMillis ?: hoyMillis)

        viewModel.cargarFacturasDesdeAPI(
            periodoInicio = periodoInicio,
            periodoFin = periodoFin,
            esCompra = (sectionActive == Section.COMPRAS),
        )

        onShowList()
    }
}