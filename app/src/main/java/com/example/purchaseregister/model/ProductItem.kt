package com.example.purchaseregister.model

import kotlinx.serialization.Serializable

@Serializable
data class ProductItem(
    val descripcion: String,
    val costoUnitario: String,
    val cantidad: String
)