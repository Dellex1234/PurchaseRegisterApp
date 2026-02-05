package com.example.purchaseregister.viewmodel

import java.util.Locale
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
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Headers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import java.util.Date
import android.content.Context

interface SunatApiService {
    @GET("sunat/facturas")
    suspend fun obtenerFacturas(
        @Query("periodoInicio") periodoInicio: String,
        @Query("periodoFin") periodoFin: String
    ): SunatResponse

    @POST("sunat/descargar-xml")
    suspend fun obtenerDetalleFacturaXml(
        @Body request: DetalleFacturaRequest
    ): DetalleFacturaXmlResponse

    @POST("factura/procesarFactura")
    @Headers("Content-Type: application/json")
    suspend fun registrarFacturasEnBD(
        @Body request: RegistroFacturasRequest
    ): RegistroFacturasResponse
}

data class DetalleFacturaRequest(
    val rucEmisor: String,
    val serie: String,
    val numero: String,
    val ruc: String,
    val usuario_sol: String,
    val clave_sol: String
)

data class DetalleFacturaXmlResponse(
    val id: String,
    val fechaEmision: String?,
    val horaEmision: String?,
    val moneda: String?,
    val emisor: EmisorResponse?,
    val receptor: ReceptorResponse?,
    val subtotal: Double?,
    val igv: Double?,
    val total: Double?,
    val items: List<ItemResponse>?,
    val archivoXml: String?
)

data class EmisorResponse(val ruc: String?, val nombre: String?)
data class ReceptorResponse(val ruc: String?, val nombre: String?)

data class ItemResponse(
    val cantidad: Double,
    val unidad: String,
    val codigo: String?,
    val descripcion: String,
    val valorUnitario: Double
)

data class SunatResponse(
    val success: Boolean,
    val periodoInicio: String,
    val periodoFin: String,
    val resultados: List<SunatResultado>
)

data class SunatResultado(
    val periodo: String,
    val contenido: List<ContenidoItem>
)

data class ContenidoItem(
    val rucEmisor: String,
    val razonSocialEmisor: String,
    val periodo: String,
    val carSunat: String,
    val fechaEmision: String,
    val tipoCP: String,
    val serie: String,
    val numero: String,
    val tipoDocReceptor: String,
    val nroDocReceptor: String,
    val nombreReceptor: String,
    val baseGravada: Double,
    val igv: Double,
    val montoNoGravado: Double,
    val total: Double,
    val moneda: String,
    val tipodecambio: Double?,
    val estado: String
)

data class ProductoParaRegistrar(
    val descripcion: String,
    val cantidad: String,
    val costoUnitario: String,
    val unidadMedida: String
)

data class FacturaParaRegistrar(
    val id: Int,
    val rucEmisor: String,
    val serie: String,
    val numero: String,
    val fechaEmision: String,
    val razonSocial: String,
    val tipoDocumento: String,
    val moneda: String,
    val costoTotal: String,
    val igv: String,
    val importeTotal: String,
    val productos: List<ProductoParaRegistrar>
)

data class RegistroFacturasRequest(
    val facturas: List<FacturaParaRegistrar>
)

data class RegistroFacturasResponse(
    val message: String,
    val resultados: List<ResultadoRegistro>
)

data class ResultadoRegistro(
    val success: Boolean,
    val id: Int,
    val numeroComprobante: String
)

class InvoiceViewModel : ViewModel() {

    private fun createOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Ver REQUEST y RESPONSE completos
        }

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(90, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://192.168.1.85:3043/") // Cambia a tu URL real
        .addConverterFactory(GsonConverterFactory.create())
        .client(createOkHttpClient())
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

    private val _registroCompletado = MutableStateFlow(false)
    val registroCompletado: StateFlow<Boolean> = _registroCompletado.asStateFlow()

    private val _rucEmisores = mutableMapOf<Int, String>()

    fun registrarFacturasEnBaseDeDatos(
        facturas: List<Invoice>,
        esCompra: Boolean,
        context: Context
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _registroCompletado.value = false

            try {
                println("📤 [ViewModel] Preparando ${facturas.size} facturas para registrar en BD...")
                // Obtener datos del usuario actual
                val facturasParaRegistrar = facturas.map { factura ->
                    FacturaParaRegistrar(
                        id = factura.id,
                        rucEmisor = factura.ruc,           // RUC del proveedor (emisor)
                        serie = factura.serie,
                        numero = factura.numero,
                        fechaEmision = factura.fechaEmision,
                        razonSocial = factura.razonSocial, // Nombre del proveedor
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

                println("📤 [ViewModel] Enviando ${facturasParaRegistrar.size} facturas a BD...")

                val request = RegistroFacturasRequest(facturas = facturasParaRegistrar)
                val response = sunatApiService.registrarFacturasEnBD(request)

                val todosExitosos = response.resultados.all { it.success }
                val facturasRegistradas = response.resultados.count { it.success }

                if (todosExitosos) {
                    println("✅ [ViewModel] Facturas registradas en BD: $facturasRegistradas")
                    println("✅ Mensaje del servidor: ${response.message}")

                    response.resultados.forEach { resultado ->
                        println("✅   Factura ${resultado.numeroComprobante} registrada con ID: ${resultado.id}")
                    }

                    facturas.forEach { factura ->
                        actualizarEstadoFactura(factura.id, "REGISTRADO", esCompra)
                    }

                    _registroCompletado.value = true

                } else {
                    val errores = response.resultados.filter { !it.success }
                    val errorMsg = "Algunas facturas no se pudieron registrar: ${errores.map { it.numeroComprobante }}"
                    println("❌ [ViewModel] $errorMsg")
                    _errorMessage.value = errorMsg
                }

            } catch (e: Exception) {
                val errorMsg = "Error de conexión al registrar en BD: ${e.message}"
                println("❌ [ViewModel] $errorMsg")
                _errorMessage.value = errorMsg
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Función para resetear el estado de registro
    fun resetRegistroCompletado() {
        _registroCompletado.value = false
    }

    fun cargarFacturasDesdeAPI(periodoInicio: String, periodoFin: String, esCompra: Boolean = true) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val response = sunatApiService.obtenerFacturas(periodoInicio, periodoFin)

                if (response.success) {
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

        // Obtener facturas existentes para preservar estados
        val facturasExistentesCompras = _facturasCompras.value
        val facturasExistentesVentas = _facturasVentas.value

        // Combinar todas las facturas existentes
        val todasFacturasExistentes = facturasExistentesCompras + facturasExistentesVentas

        // Encontrar el máximo ID actual para continuar desde ahí
        val maxIdActual = todasFacturasExistentes.maxOfOrNull { it.id } ?: 0
        idCounter = maxIdActual + 1

        resultados.forEach { resultado ->
            resultado.contenido.forEach { item ->
                // Buscar si ya existe una factura con estos datos
                val facturaExistente = todasFacturasExistentes.firstOrNull { factura ->
                    factura.ruc == item.nroDocReceptor &&
                            factura.serie == item.serie &&
                            factura.numero == item.numero &&
                            factura.fechaEmision == item.fechaEmision
                }

                val id = if (facturaExistente != null) {
                    // Usar el ID existente
                    facturaExistente.id
                } else {
                    // Crear nuevo ID
                    idCounter++
                }

                val tipoCambio = when {
                    // Si hay tipodecambio del backend, usarlo formateado
                    item.tipodecambio != null -> {
                        // Si es 1.0, podría ser vacío para Soles
                        if (item.moneda == "PEN" && item.tipodecambio == 1.0) {
                            ""
                        } else {
                            String.format("%.2f", item.tipodecambio)
                        }
                    }
                    // Si no hay, usar el existente o dejar vacío
                    else -> facturaExistente?.tipoCambio ?: ""
                }

                _rucEmisores[id] = item.rucEmisor

                val factura = Invoice(
                    id = id,
                    ruc = item.nroDocReceptor,
                    serie = item.serie,
                    numero = item.numero,
                    fechaEmision = item.fechaEmision,
                    razonSocial = item.nombreReceptor,
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
                    estado = facturaExistente?.estado ?: "CONSULTADO",  // ← ¡USAR ESTADO EXISTENTE!
                    isSelected = facturaExistente?.isSelected ?: false,  // ← ¡USAR SELECCIÓN EXISTENTE!
                    // Si la factura existente tenía productos, preservarlos
                    productos = facturaExistente?.productos ?: emptyList(),
                    anio = facturaExistente?.anio ?: item.periodo.take(4),
                    tipoCambio = tipoCambio
                )
                facturas.add(factura)
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

    // Función para obtener los 4 datos del XML
    fun cargarDetalleFacturaXml(
        rucEmisor: String,
        serie: String,
        numero: String,
        ruc: String,
        usuarioSol: String,
        claveSol: String,
        facturaId: Int,
        esCompra: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                println("📤 [ViewModel] Enviando solicitud XML:")
                println("📤 RUC Emisor: $rucEmisor")
                println("📤 Serie: $serie, Número: $numero")
                println("📤 RUC Receptor: $ruc")
                println("📤 Usuario: $usuarioSol")

                val request = DetalleFacturaRequest(
                    rucEmisor = rucEmisor,
                    serie = serie,
                    numero = numero,
                    ruc = ruc,
                    usuario_sol = usuarioSol,
                    clave_sol = claveSol
                )

                println("📤 Request completo: $request")

                val detalle = sunatApiService.obtenerDetalleFacturaXml(request)

                println("📥 [ViewModel] Respuesta recibida del API:")
                println("📥 ID: ${detalle.id}")
                println("📥 Fecha: ${detalle.fechaEmision}")
                println("📥 Moneda: ${detalle.moneda}")
                println("📥 Emisor: ${detalle.emisor?.ruc} - ${detalle.emisor?.nombre}")
                println("📥 Receptor: ${detalle.receptor?.ruc} - ${detalle.receptor?.nombre}")
                println("📥 Subtotal: ${detalle.subtotal}")
                println("📥 IGV: ${detalle.igv}")
                println("📥 Total: ${detalle.total}")
                println("📥 Número de items: ${detalle.items?.size ?: 0}")

                detalle.items?.forEachIndexed { index, item ->
                    println("📥 Item $index:")
                    println("📥   Cantidad: ${item.cantidad}")
                    println("📥   Unidad: ${item.unidad}")
                    println("📥   Descripción: ${item.descripcion}")
                    println("📥   Valor Unitario: ${item.valorUnitario}")
                }

                if (detalle.items != null && detalle.items.isNotEmpty()) {
                    // Convertir los items a ProductItem
                    val productos = detalle.items.map { item ->
                        ProductItem(
                            descripcion = item.descripcion,
                            cantidad = item.cantidad.toString(),
                            costoUnitario = String.format("%.2f", item.valorUnitario),
                            unidadMedida = item.unidad
                        )
                    }

                    println("✅ [ViewModel] Productos convertidos: ${productos.size}")
                    productos.forEachIndexed { index, producto ->
                        println("✅   Producto $index: ${producto.descripcion}, ${producto.cantidad} ${producto.unidadMedida}, S/.${producto.costoUnitario}")
                    }

                    // Actualizar la factura con los productos
                    actualizarProductosFactura(facturaId, productos, esCompra)

                    println("✅ Detalles XML obtenidos: ${productos.size} productos")
                } else {
                    println("⚠️ El XML no contiene items")
                    _errorMessage.value = "El XML no contiene detalles de productos"
                }
            } catch (e: Exception) {
                println("❌ Error obteniendo detalles XML: ${e.message}")
                _errorMessage.value = "Error al obtener detalles: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Función para actualizar una factura con sus productos
    private fun actualizarProductosFactura(
        facturaId: Int,
        productos: List<ProductItem>,
        esCompra: Boolean
    ) {
        println("🔥 [ViewModel] actualizarProductosFactura INICIADO")
        println("🔥 [ViewModel] facturaId: $facturaId")
        println("🔥 [ViewModel] esCompra: $esCompra")
        println("🔥 [ViewModel] productos.size: ${productos.size}")
        viewModelScope.launch {
            if (esCompra) {
                println("🔥 [ViewModel] Lista ANTES de actualizar: ${_facturasCompras.value.size} facturas")
                _facturasCompras.value.forEach { f ->
                    println("🔥   Factura ID=${f.id}, productos=${f.productos.size}")
                }

                _facturasCompras.update { lista ->
                    lista.map { factura ->
                        if (factura.id == facturaId) {
                            println("🔥 [ViewModel] ¡ENCONTRADA! Actualizando factura ID=$facturaId con ${productos.size} productos")
                            factura.copy(productos = productos)
                        } else {
                            factura
                        }
                    }
                }
                println("🔥 [ViewModel] Lista DESPUÉS de actualizar:")
                _facturasCompras.value.forEach { f ->
                    println("🔥   Factura ID=${f.id}, productos=${f.productos.size}")
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
        context: android.content.Context,
        onLoadingComplete: (success: Boolean, message: String?) -> Unit = { _, _ -> }
    ) {
        val miRuc = SunatPrefs.getRuc(context)
        val usuario = SunatPrefs.getUser(context)
        val claveSol = SunatPrefs.getClaveSol(context)

        println("🔍 [ViewModel] Datos para consulta XML:")
        println("🔍 RUC (YO): $miRuc")
        println("🔍 RUC Emisor recibido: $rucEmisor")  // ← Usa el que te pasan
        println("🔍 Tipo operación: ${if (esCompra) "COMPRA" else "VENTA"}")
        println("🔍 Usuario SOL: $usuario")

        if (miRuc == null || usuario == null || claveSol == null) {
            _errorMessage.value = "Complete sus credenciales SUNAT primero"
            onLoadingComplete(false, "Complete sus credenciales SUNAT primero")
            return
        }

        // Buscar serie y número de la factura
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

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Aquí puedes actualizar el estado a "PROCESANDO..." si quieres
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

                val detalle = sunatApiService.obtenerDetalleFacturaXml(request)

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
                    actualizarEstadoFactura(facturaId, "CON DETALLE", esCompra)

                    onLoadingComplete(true, "Detalles obtenidos exitosamente")
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