package com.example.purchaseregister.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purchaseregister.model.Invoice
import com.example.purchaseregister.model.ProductItem
import com.example.purchaseregister.utils.SunatPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import android.content.Context
import com.example.purchaseregister.api.RetrofitClient
import com.example.purchaseregister.api.requests.*
import com.example.purchaseregister.api.responses.*

class InvoiceViewModel : ViewModel() {
    private val apiService = RetrofitClient.sunatApiService

    private val _facturasCompras = MutableStateFlow<List<Invoice>>(emptyList())
    val facturasCompras: StateFlow<List<Invoice>> = _facturasCompras.asStateFlow()

    private val _facturasVentas = MutableStateFlow<List<Invoice>>(emptyList())
    val facturasVentas: StateFlow<List<Invoice>> = _facturasVentas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _registroCompletado = MutableStateFlow(false)
    val registroCompletado: StateFlow<Boolean> = _registroCompletado.asStateFlow()

    private val _rucEmisores = mutableMapOf<Int, String>()
    private val _facturasCache = mutableMapOf<String, List<Invoice>>()

    fun registrarFacturasEnBaseDeDatos(
        facturas: List<Invoice>,
        esCompra: Boolean,
        context: Context,
        mostrarLoading: Boolean = true  // ← Añade este parámetro
    ) {
        viewModelScope.launch {
            if (mostrarLoading) {  // ← Solo activar loading si se solicita
                _isLoading.value = true
            }
            _errorMessage.value = null
            _registroCompletado.value = false

            try {
                val facturasParaRegistrar = facturas.map { factura ->
                    FacturaParaRegistrar(
                        id = factura.id,
                        rucEmisor = factura.ruc,
                        serie = factura.serie,
                        numero = factura.numero,
                        fechaEmision = factura.fechaEmision,
                        razonSocial = factura.razonSocial,
                        tipoDocumento = factura.tipoDocumento,
                        moneda = factura.moneda,
                        costoTotal = factura.costoTotal,
                        igv = factura.igv,
                        importeTotal = factura.importeTotal,
                        productos = factura.productos.map { producto ->
                            ProductoParaRegistrar(
                                descripcion = producto.descripcion,
                                cantidad = producto.cantidad,
                                costoUnitario = producto.costoUnitario,
                                unidadMedida = producto.unidadMedida
                            )
                        }
                    )
                }

                val request = RegistroFacturasRequest(facturas = facturasParaRegistrar)
                val response = apiService.registrarFacturasEnBD(request)

                val todosExitosos = response.resultados.all { it.success }
                val facturasRegistradas = response.resultados.count { it.success }

                if (todosExitosos) {
                    response.resultados.forEach { resultado ->
                        // Puedes dejar esto vacío o agregar logs si necesitas
                    }

                    facturas.forEach { factura ->
                        actualizarEstadoFactura(factura.id, "REGISTRADO", esCompra)
                    }

                    _registroCompletado.value = true

                } else {
                    val errores = response.resultados.filter { !it.success }
                    val errorMsg = "Algunas facturas no se pudieron registrar: ${errores.map { it.numeroComprobante }}"
                    _errorMessage.value = errorMsg
                }

            } catch (e: Exception) {
                val errorMsg = "Error de conexión al registrar en BD: ${e.message}"
                _errorMessage.value = errorMsg
            } finally {
                if (mostrarLoading) {  // ← Solo desactivar loading si se activó
                    _isLoading.value = false
                }
            }
        }
    }

    private fun getCacheKey(esCompra: Boolean, periodoInicio: String): String {
        return "${if (esCompra) "COMPRAS" else "VENTAS"}-${periodoInicio}"
    }

    fun cargarFacturasDesdeAPI(periodoInicio: String, periodoFin: String, esCompra: Boolean = true) {
        viewModelScope.launch {
            val cacheKey = getCacheKey(esCompra, periodoInicio)
            val facturasEnCache = _facturasCache[cacheKey]

            _errorMessage.value = null

            // ✅ 1. SI HAY CACHE: Solo actualizar estados SIN loading
            if (facturasEnCache != null) {
                try {
                    val facturasActualizadas = mutableListOf<Invoice>()

                    for (factura in facturasEnCache) {
                        try {
                            val numeroComprobante = "${factura.serie}-${factura.numero}"
                            val facturaUI = apiService.obtenerFacturaParaUI(numeroComprobante)

                            val estadoActual = facturaUI.factura.estado

                            val productosActuales = if (facturaUI.factura.detalles != null) {
                                facturaUI.factura.detalles.map { detalle ->
                                    ProductItem(
                                        descripcion = detalle.descripcion,
                                        cantidad = detalle.cantidad,
                                        costoUnitario = detalle.costoUnitario,
                                        unidadMedida = detalle.unidadMedida
                                    )
                                }
                            } else {
                                factura.productos
                            }

                            val facturaActualizada = factura.copy(
                                estado = estadoActual,
                                productos = productosActuales
                            )

                            facturasActualizadas.add(facturaActualizada)

                        } catch (e: Exception) {
                            // Si falla, mantener la factura original del cache
                            facturasActualizadas.add(factura)
                        }
                    }

                    // ✅ Actualizar el cache con los nuevos estados
                    _facturasCache[cacheKey] = facturasActualizadas

                    // ✅ Mostrar en UI
                    if (esCompra) {
                        _facturasCompras.value = facturasActualizadas
                    } else {
                        _facturasVentas.value = facturasActualizadas
                    }

                } catch (e: Exception) {
                    _errorMessage.value = "Error actualizando estados: ${e.message}"
                    // Si falla, mostrar cache original
                    if (esCompra) {
                        _facturasCompras.value = facturasEnCache
                    } else {
                        _facturasVentas.value = facturasEnCache
                    }
                }

                // ✅ IMPORTANTE: NO activamos isLoading ni lo desactivamos
                return@launch
            }

            // ✅ 2. NO HAY CACHE: Consultar SUNAT CON loading
            _isLoading.value = true

            try {
                val response = apiService.obtenerFacturas(periodoInicio, periodoFin)

                if (response.success) {
                    val facturas = parsearContenidoSunat(response.resultados, esCompra)

                    _facturasCache[cacheKey] = facturas

                    if (esCompra) {
                        _facturasCompras.value = facturas
                    } else {
                        _facturasVentas.value = facturas
                    }
                } else {
                    _errorMessage.value = "Error en la respuesta del servidor"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error al conectar con el servidor: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun limpiarError() {
        _errorMessage.value = null
    }

    private suspend fun parsearContenidoSunat(
        resultados: List<SunatResultado>,
        esCompra: Boolean
    ): List<Invoice> {
        val facturas = mutableListOf<Invoice>()
        var idCounter = 1

        val facturasExistentesCompras = _facturasCompras.value
        val facturasExistentesVentas = _facturasVentas.value
        val todasFacturasExistentes = facturasExistentesCompras + facturasExistentesVentas

        val maxIdActual = todasFacturasExistentes.maxOfOrNull { it.id } ?: 0
        idCounter = maxIdActual + 1

        val facturasParaRegistrarEnBD = mutableListOf<RegistrarFacturaDesdeSunatRequest>()

        resultados.forEach { resultado ->
            resultado.contenido.forEach { item ->
                val numeroComprobante = "${item.serie}-${item.numero}"
                var estadoDesdeBD = "CONSULTADO"
                var productosDesdeBD: List<ProductItem> = emptyList()

                try {
                    val facturaUI = apiService.obtenerFacturaParaUI(numeroComprobante)
                    estadoDesdeBD = facturaUI.factura.estado

                    if (facturaUI.factura.detalles != null) {
                        productosDesdeBD = facturaUI.factura.detalles.map { detalle ->
                            ProductItem(
                                descripcion = detalle.descripcion,
                                cantidad = detalle.cantidad,
                                costoUnitario = detalle.costoUnitario,
                                unidadMedida = detalle.unidadMedida
                            )
                        }
                    }
                } catch (e: Exception) {
                    estadoDesdeBD = "CONSULTADO"

                    val razonSocialRespectiva = if (esCompra) {
                        // Para COMPRAS: El proveedor es el emisor
                        item.razonSocialEmisor
                    } else {
                        // Para VENTAS: El cliente es el receptor
                        item.nombreReceptor
                    }

                    val facturaRequest = RegistrarFacturaDesdeSunatRequest(
                        rucEmisor = item.rucEmisor,
                        serie = item.serie,
                        numero = item.numero,
                        fechaEmision = item.fechaEmision,
                        razonSocial = razonSocialRespectiva,
                        tipoDocumento = when (item.tipoCP) {
                            "01" -> "FACTURA"
                            "03" -> "BOLETA"
                            else -> "DOCUMENTO"
                        },
                        moneda = when (item.moneda) {
                            "PEN" -> "Soles (PEN)"
                            "USD" -> "Dólares (USD)"
                            else -> item.moneda
                        },
                        costoTotal = String.format("%.2f", item.baseGravada),
                        igv = String.format("%.2f", item.igv),
                        importeTotal = String.format("%.2f", item.total),
                        usuarioId = 1
                    )
                    facturasParaRegistrarEnBD.add(facturaRequest)
                }

                val facturaExistente = todasFacturasExistentes.firstOrNull { factura ->
                    factura.ruc == item.nroDocReceptor &&
                            factura.serie == item.serie &&
                            factura.numero == item.numero
                }

                val id = if (facturaExistente != null) {
                    facturaExistente.id
                } else {
                    idCounter++
                }

                val tipoCambio = when {
                    item.tipodecambio != null -> {
                        if (item.moneda == "PEN" && item.tipodecambio == 1.0) {
                            ""
                        } else {
                            String.format("%.2f", item.tipodecambio)
                        }
                    }
                    else -> facturaExistente?.tipoCambio ?: ""
                }

                _rucEmisores[id] = item.rucEmisor

                val razonSocialCorrecta = if (esCompra) {
                    item.razonSocialEmisor  // Para compras
                } else {
                    item.nombreReceptor     // Para ventas
                }

                val factura = Invoice(
                    id = id,
                    ruc = item.nroDocReceptor,
                    serie = item.serie,
                    numero = item.numero,
                    fechaEmision = item.fechaEmision,
                    razonSocial = razonSocialCorrecta,
                    tipoDocumento = when (item.tipoCP) {
                        "01" -> "FACTURA"
                        "03" -> "BOLETA"
                        else -> "DOCUMENTO"
                    },
                    moneda = when (item.moneda) {
                        "PEN" -> "Soles (PEN)"
                        "USD" -> "Dólares (USD)"
                        else -> item.moneda
                    },
                    costoTotal = String.format("%.2f", item.baseGravada),
                    igv = String.format("%.2f", item.igv),
                    importeTotal = String.format("%.2f", item.total),
                    estado = estadoDesdeBD,
                    isSelected = facturaExistente?.isSelected ?: false,
                    productos = productosDesdeBD,
                    anio = facturaExistente?.anio ?: item.periodo.take(4),
                    tipoCambio = tipoCambio
                )
                facturas.add(factura)
            }
        }

        if (facturasParaRegistrarEnBD.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    facturasParaRegistrarEnBD.forEach { request ->
                        try {
                            val response = apiService.registrarFacturaDesdeSunat(request)
                        } catch (e: Exception) {
                            _errorMessage.value = "Error al registrar algunas facturas: ${e.message}"
                        }
                    }
                } catch (e: Exception) {
                    _errorMessage.value = "Error al registrar facturas en BD: ${e.message}"
                }
            }
        }

        return facturas.sortedBy { factura ->
            try {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(factura.fechaEmision)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
    }

    private fun actualizarProductosFactura(
        facturaId: Int,
        productos: List<ProductItem>,
        esCompra: Boolean
    ) {
        viewModelScope.launch {
            if (esCompra) {
                _facturasCompras.update { lista ->
                    lista.map { factura ->
                        if (factura.id == facturaId) {
                            factura.copy(productos = productos)
                        } else {
                            factura
                        }
                    }
                }
            } else {
                _facturasVentas.update { lista ->
                    lista.map { factura ->
                        if (factura.id == facturaId) {
                            factura.copy(productos = productos)
                        } else {
                            factura
                        }
                    }
                }
            }
        }
    }

    fun getRucEmisor(facturaId: Int): String? = _rucEmisores[facturaId]

    fun cargarDetalleFacturaXmlConUsuario(
        facturaId: Int,
        esCompra: Boolean,
        rucEmisor: String,
        context: Context,
        onLoadingComplete: (success: Boolean, message: String?) -> Unit = { _, _ -> }
    ) {
        val miRuc = SunatPrefs.getRuc(context)
        val usuario = SunatPrefs.getUser(context)
        val claveSol = SunatPrefs.getClaveSol(context)

        if (miRuc == null || usuario == null || claveSol == null) {
            _errorMessage.value = "Complete sus credenciales SUNAT primero"
            onLoadingComplete(false, "Complete sus credenciales SUNAT primero")
            return
        }

        val factura = if (esCompra) {
            _facturasCompras.value.firstOrNull { it.id == facturaId }
        } else {
            _facturasVentas.value.firstOrNull { it.id == facturaId }
        }

        if (factura == null) {
            _errorMessage.value = "Factura no encontrada"
            onLoadingComplete(false, "Factura no encontrada")
            return
        }

        val numeroComprobante = "${factura.serie}-${factura.numero}"

        if (factura.estado == "CON DETALLE" || factura.estado == "REGISTRADO") {
            if (factura.productos.isNotEmpty()) {
                onLoadingComplete(true, "Detalles ya cargados")
            } else {
                _errorMessage.value = "No hay detalles disponibles"
                onLoadingComplete(false, "No hay detalles disponibles")
            }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                try {
                    val facturaEnBD = apiService.verificarFacturaRegistrada(numeroComprobante)
                } catch (e: Exception) {
                    val registroRequest = RegistrarFacturaDesdeSunatRequest(
                        rucEmisor = rucEmisor,
                        serie = factura.serie,
                        numero = factura.numero,
                        fechaEmision = factura.fechaEmision,
                        razonSocial = factura.razonSocial,
                        tipoDocumento = factura.tipoDocumento,
                        moneda = factura.moneda,
                        costoTotal = factura.costoTotal,
                        igv = factura.igv,
                        importeTotal = factura.importeTotal,
                        usuarioId = 1
                    )

                    val registroResponse = apiService.registrarFacturaDesdeSunat(registroRequest)
                }

                actualizarEstadoFactura(facturaId, "PROCESANDO...", esCompra)

                val rucEmisorParaAPI = if (esCompra) factura.ruc else miRuc
                val rucReceptorParaAPI = if (esCompra) miRuc else factura.ruc

                val request = DetalleFacturaRequest(
                    rucEmisor = rucEmisorParaAPI,
                    serie = factura.serie,
                    numero = factura.numero,
                    ruc = rucReceptorParaAPI,
                    usuario_sol = usuario,
                    clave_sol = claveSol
                )

                val detalle = apiService.obtenerDetalleFacturaXml(request)

                if (detalle.items != null && detalle.items.isNotEmpty()) {
                    val productos = detalle.items.map { item ->
                        ProductItem(
                            descripcion = item.descripcion,
                            cantidad = item.cantidad.toString(),
                            costoUnitario = String.format("%.2f", item.valorUnitario),
                            unidadMedida = item.unidad
                        )
                    }

                    actualizarProductosFactura(facturaId, productos, esCompra)

                    val productosParaGuardar = productos.map { producto ->
                        ProductoRequest(
                            descripcion = producto.descripcion,
                            cantidad = producto.cantidad.toDoubleOrNull() ?: 0.0,
                            costoUnitario = producto.costoUnitario.toDoubleOrNull() ?: 0.0,
                            unidadMedida = producto.unidadMedida
                        )
                    }

                    try {
                        val guardarProductosResponse = apiService.guardarProductosFactura(
                            numeroComprobante,
                            GuardarProductosRequest(productos = productosParaGuardar)
                        )
                    } catch (e: Exception) {
                    }

                    try {
                        val scrapingRequest = ScrapingCompletadoRequest(
                            productos = productosParaGuardar
                        )

                        val respuestaBackend = apiService.marcarScrapingCompletado(
                            numeroComprobante,
                            scrapingRequest
                        )
                    } catch (e: Exception) {
                        _errorMessage.value = "Detalles obtenidos, pero error al guardar en servidor"
                    }

                    val estadoActual = if (esCompra) {
                        _facturasCompras.value.firstOrNull { it.id == facturaId }?.estado
                    } else {
                        _facturasVentas.value.firstOrNull { it.id == facturaId }?.estado
                    }

                    if (estadoActual != "REGISTRADO") {
                        actualizarEstadoFactura(facturaId, "CON DETALLE", esCompra)
                    }

                    onLoadingComplete(true, "Detalles obtenidos y guardados exitosamente")

                } else {
                    _errorMessage.value = "El XML no contiene detalles de productos"
                    onLoadingComplete(false, "El XML no contiene detalles de productos")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error al obtener detalles: ${e.message}"
                onLoadingComplete(false, "Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun agregarNuevaFacturaCompra(
        ruc: String,
        razonSocial: String,
        serie: String,
        numero: String,
        fechaEmision: String,
        tipoDocumento: String,
        moneda: String = "",
        costoTotal: String = "",
        igv: String = "",
        importeTotal: String = "",
        anio: String = "",
        tipoCambio: String = "",
        productos: List<ProductItem> = emptyList()
    ) {
        viewModelScope.launch {
            val estadoInicial = if (productos.isNotEmpty()) {
                "CON DETALLE"
            } else {
                "CONSULTADO"
            }

            _facturasCompras.update { lista ->
                val nuevoId = if (lista.isEmpty()) 1 else lista.maxOf { it.id } + 1

                val nuevaFactura = Invoice(
                    id = nuevoId,
                    ruc = ruc,
                    razonSocial = razonSocial,
                    serie = serie,
                    numero = numero,
                    fechaEmision = fechaEmision,
                    tipoDocumento = tipoDocumento,
                    anio = anio,
                    moneda = moneda,
                    costoTotal = costoTotal,
                    igv = igv,
                    tipoCambio = tipoCambio,
                    importeTotal = importeTotal,
                    estado = estadoInicial,
                    isSelected = false,
                    productos = productos
                )

                (lista + nuevaFactura).sortedBy { factura ->
                    try {
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(factura.fechaEmision)?.time ?: 0L
                    } catch (e: Exception) {
                        0L
                    }
                }
            }
        }
    }

    fun actualizarEstadoFactura(facturaId: Int, nuevoEstado: String, esCompra: Boolean) {
        viewModelScope.launch {
            if (esCompra) {
                _facturasCompras.update { lista ->
                    lista.map { factura ->
                        if (factura.id == facturaId) {
                            actualizarFacturaEnCaches(factura, nuevoEstado)
                            factura.copy(estado = nuevoEstado)
                        } else {
                            factura
                        }
                    }
                }
            } else {
                _facturasVentas.update { lista ->
                    lista.map { factura ->
                        if (factura.id == facturaId) {
                            actualizarFacturaEnCaches(factura, nuevoEstado)
                            factura.copy(estado = nuevoEstado)
                        } else {
                            factura
                        }
                    }
                }
            }
        }
    }

    private fun actualizarFacturaEnCaches(facturaOriginal: Invoice, nuevoEstado: String) {
        _facturasCache.forEach { (key, facturasEnCache) ->
            val facturasActualizadas = facturasEnCache.map { facturaCache ->
                // Comparar por ruc, serie y número (no por id que puede cambiar)
                if (facturaCache.ruc == facturaOriginal.ruc &&
                    facturaCache.serie == facturaOriginal.serie &&
                    facturaCache.numero == facturaOriginal.numero) {
                    facturaCache.copy(estado = nuevoEstado)
                } else {
                    facturaCache
                }
            }
            _facturasCache[key] = facturasActualizadas
        }
    }
}