package com.example.purchaseregister.navigation

import com.example.purchaseregister.model.ProductItem
import kotlinx.serialization.Serializable

// 1. Ahora es una data class con los campos que quieres mostrar
@Serializable
data class DetailRoute(
    val id: Int,
    val esCompra: Boolean = true
)