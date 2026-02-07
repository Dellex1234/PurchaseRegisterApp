// com.example.purchaseregister.utils.FormatoUtils.kt
package com.example.purchaseregister.utils

fun formatearUnidadMedida(cantidad: String, unidad: String): String {
    val unidadFormateada = when (unidad.uppercase()) {
        "KILO", "KILOS", "KILOGRAMO", "KILOGRAMOS", "KG", "KGS" -> "Kg"
        "GRAMO", "GRAMOS", "GR", "GRS", "G" -> "Gr"
        "LITRO", "LITROS", "L", "LT", "LTS" -> "Lt"
        "UNIDAD", "UNIDADES", "UN", "UND", "UNDS" -> "UN"
        "METRO", "METROS", "M", "MT", "MTS" -> "M"
        "CENTIMETRO", "CENTIMETROS", "CM", "CMS" -> "Cm"
        "MILIMETRO", "MILIMETROS", "MM", "MMS" -> "Mm"
        "PAQUETE", "PAQUETES", "PQ", "PQT", "PQTS" -> "Pq"
        "CAJA", "CAJAS", "CJ", "CJA", "CJAS" -> "Bx"
        "GALON", "US GALON", "GALONES", "GAL", "GALS" -> "Gal"
        "CASE", "CS" -> "Cs"
        else -> if (unidad.isNotBlank()) unidad else ""
    }

    return if (unidadFormateada.isNotBlank()) "$cantidad $unidadFormateada" else cantidad
}

fun limpiarMonto(texto: String): String {
    return texto.replace(Regex("[^0-9.]"), "")
}