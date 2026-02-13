package com.example.purchaseregister.api.responses

data class GuardarProductosResponse(
    val success: Boolean,
    val message: String,
    val productosGuardados: Int,
    val facturaId: Int?,
    val estadoActualizado: Boolean?
)

data class FacturaUIResponse(
    val success: Boolean,
    val message: String,
    val factura: FacturaRegistradaResponse,
    val nota: String
)

data class FacturasUIResponse(
    val success: Boolean,
    val message: String,
    val count: Int,
    val distribucionEstados: Map<String, Int>,
    val facturas: List<FacturaRegistradaResponse>,
    val nota: String
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

data class RegistroFacturasResponse(
    val message: String,
    val resultados: List<ResultadoRegistro>
)

data class ResultadoRegistro(
    val success: Boolean,
    val id: Int,
    val numeroComprobante: String
)

data class FacturaRegistradaResponse(
    val idFactura: Int,
    val numeroComprobante: String,
    val fechaEmision: String,
    val estado: String,
    val proveedorRuc: String,
    val costoTotal: String,
    val igv: String,
    val importeTotal: String,
    val moneda: String,
    val numero: String,
    val serie: String,
    val detalles: List<DetalleRegistrado>?,
    val proveedor: ProveedorRegistrado?
)

data class DetalleRegistrado(
    val descripcion: String,
    val cantidad: String,
    val costoUnitario: String,
    val unidadMedida: String
)

data class ProveedorRegistrado(
    val rucProveedor: String,
    val razonSocial: String
)

data class ScrapingCompletadoResponse(
    val message: String,
    val timestamp: String,
    val factura: FacturaScrapingResponse?,
    val estado: String?,
    val productosGuardados: Int?,
    val advertencia: String?
)

data class FacturaScrapingResponse(
    val idFactura: Int,
    val numeroComprobante: String,
    val estado: String
)

data class RegistrarFacturaDesdeSunatResponse(
    val success: Boolean,
    val idFactura: Int?,
    val numeroComprobante: String,
    val message: String
)

data class EncoladoResponse(
    val success: Boolean,
    val jobId: String,
    val message: String
)

data class EstadoJobResponse(
    val id: String,
    val state: String,
    val progress: Int,
    val result: JobResult?,
    val reason: String?
)

data class JobResult(
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

data class ValidarCredencialesResponse(
    val valido: Boolean,
    val mensaje: String? = null,
    val token: String? = null
)