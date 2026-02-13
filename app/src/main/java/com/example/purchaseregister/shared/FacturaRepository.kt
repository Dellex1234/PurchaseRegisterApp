package com.example.purchaseregister.viewmodel.shared

import com.example.purchaseregister.model.Invoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object FacturaRepository {
    private val _facturasCompras = MutableStateFlow<List<Invoice>>(emptyList())
    val facturasCompras: StateFlow<List<Invoice>> = _facturasCompras.asStateFlow()

    private val _facturasVentas = MutableStateFlow<List<Invoice>>(emptyList())
    val facturasVentas: StateFlow<List<Invoice>> = _facturasVentas.asStateFlow()

    private val _rucEmisores = mutableMapOf<Int, String>()
    private val _facturasCache = mutableMapOf<String, List<Invoice>>()

    // ✅ MÉTODOS PARA ACCEDER/MODIFICAR
    fun getFacturasCompras(): List<Invoice> = _facturasCompras.value
    fun getFacturasVentas(): List<Invoice> = _facturasVentas.value

    fun updateFacturasCompras(update: (List<Invoice>) -> List<Invoice>) {
        _facturasCompras.update { update(it) }
    }

    fun updateFacturasVentas(update: (List<Invoice>) -> List<Invoice>) {
        _facturasVentas.update { update(it) }
    }

    fun setFacturasCompras(facturas: List<Invoice>) {
        _facturasCompras.value = facturas
    }

    fun setFacturasVentas(facturas: List<Invoice>) {
        _facturasVentas.value = facturas
    }

    // RUC Emisores
    fun setRucEmisor(facturaId: Int, ruc: String) {
        _rucEmisores[facturaId] = ruc
    }

    fun getRucEmisor(facturaId: Int): String? = _rucEmisores[facturaId]

    // Cache
    fun getCacheKey(esCompra: Boolean, periodoInicio: String): String {
        return "${if (esCompra) "COMPRAS" else "VENTAS"}-${periodoInicio}"
    }

    fun getCachedFacturas(key: String): List<Invoice>? = _facturasCache[key]

    fun updateCache(key: String, facturas: List<Invoice>) {
        _facturasCache[key] = facturas
    }

    fun updateFacturaInAllCaches(facturaOriginal: Invoice, nuevoEstado: String) {
        _facturasCache.forEach { (key, facturasEnCache) ->
            val facturasActualizadas = facturasEnCache.map { facturaCache ->
                if (facturaCache.ruc == facturaOriginal.ruc &&
                    facturaCache.serie == facturaOriginal.serie &&
                    facturaCache.numero == facturaOriginal.numero) {
                    facturaCache.copy(estado = nuevoEstado)
                } else {
                    facturaCache
                }
            }
            _facturasCache[key] = facturasActualizadas
        }
    }

    fun clearAll() {
        _facturasCompras.value = emptyList()
        _facturasVentas.value = emptyList()
        _facturasCache.clear()
        _rucEmisores.clear()
    }
}