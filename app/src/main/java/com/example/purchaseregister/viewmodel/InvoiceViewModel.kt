package com.example.purchaseregister.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purchaseregister.model.Invoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InvoiceViewModel : ViewModel() {

    // Estado observable de las facturas - COMPRAS
    private val _facturasCompras = MutableStateFlow<List<Invoice>>(emptyList())
    val facturasCompras: StateFlow<List<Invoice>> = _facturasCompras.asStateFlow()

    // Estado observable de las facturas - VENTAS
    private val _facturasVentas = MutableStateFlow<List<Invoice>>(emptyList())
    val facturasVentas: StateFlow<List<Invoice>> = _facturasVentas.asStateFlow()

    init {
        // Inicializar con datos de ejemplo cuando se crea el ViewModel
        inicializarDatosEjemplo()
    }

    private fun inicializarDatosEjemplo() {
        viewModelScope.launch {
            // FACTURAS DE COMPRAS
            val compras = mutableListOf<Invoice>()
            for (i in 1..10) {
                compras.add(
                    Invoice(
                        id = i,
                        ruc = "2060123456$i",
                        serie = "F001",
                        numero = "$i",
                        fechaEmision = "22/01/2026",
                        razonSocial = "PROV COMPRAS $i",
                        tipoDocumento = "FACTURA",
                        moneda = "Dòlares (USD)",
                        costoTotal = "100.00",
                        igv = "18.00",
                        importeTotal = "118.00",
                        estado = "CONSULTADO",  // ¡ESTADO INICIAL!
                        isSelected = false
                    )
                )
            }
            _facturasCompras.value = compras

            // FACTURAS DE VENTAS
            val ventas = mutableListOf<Invoice>()
            for (i in 1..8) {
                ventas.add(
                    Invoice(
                        id = i, // IDs diferentes para evitar conflictos
                        ruc = "1040987654$i",
                        serie = "V001",
                        numero = "$i",
                        fechaEmision = "22/01/2026",
                        razonSocial = "CLIENTE VENTAS $i",
                        tipoDocumento = "FACTURA",
                        moneda = "Soles (PEN)",
                        costoTotal = "200.00",
                        igv = "36.00",
                        importeTotal = "236.00",
                        estado = "CONSULTADO",  // ¡ESTADO INICIAL!
                        isSelected = false
                    )
                )
            }
            _facturasVentas.value = ventas
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