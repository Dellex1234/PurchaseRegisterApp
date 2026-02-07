// com.example.purchaseregister.utils.MonedaUtils.kt
package com.example.purchaseregister.utils

object MonedaUtils {

    fun esMonedaDolares(moneda: String): Boolean {
        return moneda.contains("USD", ignoreCase = true) ||
                moneda.contains("Dólar", ignoreCase = true) ||
                moneda.contains("Dolar", ignoreCase = true) ||
                moneda.contains("US$", ignoreCase = false) ||
                moneda.contains("U$", ignoreCase = false)
    }

    fun esMonedaSoles(moneda: String): Boolean {
        return moneda.contains("Soles", ignoreCase = true) ||
                moneda.contains("SOL", ignoreCase = true) ||
                moneda.contains("PEN", ignoreCase = true) ||
                moneda.contains("S/", ignoreCase = false)
    }

    fun formatearMoneda(moneda: String): String {
        return when {
            esMonedaSoles(moneda) -> "Soles (PEN)"
            esMonedaDolares(moneda) -> "Dólares (USD)"
            moneda == "Soles" -> "Soles (PEN)"
            moneda == "Dólares" -> "Dólares (USD)"
            moneda == "Dolares" -> "Dólares (USD)"
            else -> moneda
        }
    }
}