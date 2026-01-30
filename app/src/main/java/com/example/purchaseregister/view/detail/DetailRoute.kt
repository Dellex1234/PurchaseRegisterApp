package com.example.purchaseregister.navigation

import kotlinx.serialization.Serializable

// 1. Ahora es una data class con los campos que quieres mostrar
@Serializable
data class DetailRoute(
    val id: Int,
    val rucProveedor: String,
    val serie: String,
    val numero: String,
    val fecha: String,
    val razonSocial: String,
    val tipoDocumento: String,
    val anio: String,
    val moneda: String,
    val costoTotal: String,
    val igv: String,
    val tipoCambio: String,
    val importeTotal: String,
    val esCompra: Boolean = true,
)