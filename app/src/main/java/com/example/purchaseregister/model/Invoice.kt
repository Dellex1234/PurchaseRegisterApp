package com.example.purchaseregister.model

import kotlinx.serialization.Serializable

@Serializable
data class Invoice(
    val id: Int,
    val ruc: String,
    val razonSocial: String,
    val serie: String,
    val numero: String,
    val fechaEmision: String,
    val tipoDocumento: String,
    val anio: String = "",
    val moneda: String = "",
    val costoTotal: String = "",
    val igv: String = "",
    val tipoCambio: String = "",
    val importeTotal: String = "",
    var estado: String = "CONSULTADO",
    var isSelected: Boolean = false,
    val productos: List<ProductItem> = emptyList()
)