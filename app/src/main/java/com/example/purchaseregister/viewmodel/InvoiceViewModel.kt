package com.example.purchaseregister.viewmodel

import java.util.Locale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purchaseregister.model.Invoice
import com.example.purchaseregister.model.ProductItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface SunatApiService {
    @GET("sunat/facturas")
    suspend fun obtenerFacturas(
        @Query("periodoInicio") periodoInicio: String,
        @Query("periodoFin") periodoFin: String
    ): SunatResponse
}

data class SunatResponse(
    val success: Boolean,
    val periodoInicio: String,
    val periodoFin: String,
    val resultados: List<SunatResultado>
)

data class SunatResultado(
    val periodo: String,
    val contenido: String
)

class InvoiceViewModel : ViewModel() {

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://192.168.1.39:3043/") // Cambia a tu URL real
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val sunatApiService = retrofit.create(SunatApiService::class.java)

    // Estado observable de las facturas - COMPRAS
    private val _facturasCompras = MutableStateFlow<List<Invoice>>(emptyList())
    val facturasCompras: StateFlow<List<Invoice>> = _facturasCompras.asStateFlow()

    // Estado observable de las facturas - VENTAS
    private val _facturasVentas = MutableStateFlow<List<Invoice>>(emptyList())
    val facturasVentas: StateFlow<List<Invoice>> = _facturasVentas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun cargarFacturasDesdeAPI(periodoInicio: String, periodoFin: String, esCompra: Boolean = true) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val response = sunatApiService.obtenerFacturas(periodoInicio, periodoFin)

                if (response.success) {
                    // 7. Parsear los datos del contenido
                    val facturas = parsearContenidoSunat(response.resultados)

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

    // 9. Función para parsear el contenido del API
    private fun parsearContenidoSunat(resultados: List<SunatResultado>): List<Invoice> {
        val facturas = mutableListOf<Invoice>()
        var idCounter = 1

        resultados.forEach { resultado ->
            // El contenido tiene líneas separadas por saltos de línea
            val lineas = resultado.contenido.split("\n")

            // La primera línea son los encabezados, las siguientes son datos
            if (lineas.size > 1) {
                for (i in 1 until lineas.size) { // Empezar desde 1 para saltar encabezados
                    val linea = lineas[i].trim()
                    if (linea.isNotEmpty()) {
                        val campos = linea.split("|")

                        if (campos.size >= 25) { // Verificar que tenga los campos mínimos
                            val factura = Invoice(
                                id = idCounter++,
                                ruc = campos.getOrElse(12) { "" }, // Nro Doc Identidad
                                serie = campos.getOrElse(7) { "" }, // Serie del CDP
                                numero = campos.getOrElse(9) { "" }, // Nro CP o Doc
                                fechaEmision = campos.getOrElse(4) { "" }, // Fecha de emisión
                                razonSocial = campos.getOrElse(13) { "" }, // Razón Social
                                tipoDocumento = when(campos.getOrElse(6) { "" }) { // Tipo CP/Doc
                                    "01" -> "FACTURA"
                                    "03" -> "BOLETA"
                                    else -> "DOCUMENTO"
                                },
                                moneda = campos.getOrElse(25) { "PEN" }, // Moneda
                                costoTotal = campos.getOrElse(14) { "0.00" }, // BI Gravado DG
                                igv = campos.getOrElse(15) { "0.00" }, // IGV / IPM DG
                                importeTotal = campos.getOrElse(24) { "0.00" }, // Total CP
                                estado = "CONSULTADO",
                                isSelected = false
                            )
                            facturas.add(factura)
                        }
                    }
                }
            }
        }

        return facturas
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
            println("🆕 [ViewModel] Agregando nueva factura COMPRA...")
            println("📝 Datos: RUC=$ruc, Serie=$serie, Número=$numero, Fecha=$fechaEmision")
            println("📝 Razón Social=$razonSocial, Tipo Doc=$tipoDocumento")
            println("📝 Productos: ${productos.size} productos")

            _facturasCompras.update { lista ->
                // Generar un nuevo ID (máximo actual + 1)
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
                    estado = "CONSULTADO", // Estado inicial
                    isSelected = false,
                    productos = productos // Por ahora vacío
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

    // FUNCIÓN PRINCIPAL: Actualizar estado de una factura
    fun actualizarEstadoFactura(facturaId: Int, nuevoEstado: String, esCompra: Boolean) {
        println("🔄 [ViewModel] Llamando actualizarEstadoFactura")
        println("🔄 [ViewModel] ID: $facturaId, Estado: '$nuevoEstado', esCompra: $esCompra")

        viewModelScope.launch {
            if (esCompra) {
                println("🔄 [ViewModel] Actualizando en COMPRAS")
                _facturasCompras.update { lista ->
                    println("🔄 [ViewModel] Lista COMPRAS antes: ${lista.size} elementos")
                    val nuevaLista = lista.map { factura ->
                        if (factura.id == facturaId) {
                            println("✅ [ViewModel] Factura COMPRA actualizada: ID=${factura.id}")
                            factura.copy(estado = nuevoEstado)
                        } else {
                            factura
                        }
                    }
                    println("🔄 [ViewModel] Lista COMPRAS después: ${nuevaLista.size} elementos")
                    nuevaLista
                }
            } else {
                println("🔄 [ViewModel] Actualizando en VENTAS")
                _facturasVentas.update { lista ->
                    println("🔄 [ViewModel] Lista VENTAS antes: ${lista.size} elementos")
                    val nuevaLista = lista.map { factura ->
                        if (factura.id == facturaId) {
                            println("✅ [ViewModel] Factura VENTA actualizada: ID=${factura.id}")
                            factura.copy(estado = nuevoEstado)
                        } else {
                            factura
                        }
                    }
                    println("🔄 [ViewModel] Lista VENTAS después: ${nuevaLista.size} elementos")
                    nuevaLista
                }
            }
        }
    }
    // Funciones para los checkboxes
    fun actualizarSeleccionCompras(id: Int, isSelected: Boolean) {
        println("🔄 [ViewModel] actualizarSeleccionCompras - ID: $id, Selected: $isSelected")
        viewModelScope.launch {
            _facturasCompras.update { lista ->
                lista.map { factura ->
                    if (factura.id == id) {
                        println("✅ [ViewModel] Factura COMPRA actualizada: ID=${factura.id}, Selected=$isSelected")
                        factura.copy(isSelected = isSelected)
                    } else {
                        factura
                    }
                }
            }
        }
    }

    fun actualizarSeleccionVentas(id: Int, isSelected: Boolean) {
        println("🔄 [ViewModel] actualizarSeleccionVentas - ID: $id, Selected: $isSelected")
        viewModelScope.launch {
            _facturasVentas.update { lista ->
                lista.map { factura ->
                    if (factura.id == id) {
                        println("✅ [ViewModel] Factura VENTA actualizada: ID=${factura.id}, Selected=$isSelected")
                        factura.copy(isSelected = isSelected)
                    } else {
                        factura
                    }
                }
            }
        }
    }

    fun seleccionarTodasCompras(seleccionar: Boolean) {
        viewModelScope.launch {
            _facturasCompras.update { lista ->
                lista.map { factura ->
                    factura.copy(isSelected = seleccionar)
                }
            }
        }
    }

    fun seleccionarTodasVentas(seleccionar: Boolean) {
        viewModelScope.launch {
            _facturasVentas.update { lista ->
                lista.map { factura ->
                    factura.copy(isSelected = seleccionar)
                }
            }
        }
    }
}