package com.example.purchaseregister.api.requests

data class ProductoRequest(
    val descripcion: String,
    val cantidad: Double,
    val costoUnitario: Double,
    val unidadMedida: String
)

data class GuardarProductosRequest(
    val productos: List<ProductoRequest>
)

data class ScrapingCompletadoRequest(
    val productos: List<ProductoRequest>? = null
)

data class DetalleFacturaRequest(
    val rucEmisor: String,
    val serie: String,
    val numero: String,
    val ruc: String,
    val usuario_sol: String,
    val clave_sol: String
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

data class RegistrarFacturaDesdeSunatRequest(
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
    val usuarioId: Int = 1
)