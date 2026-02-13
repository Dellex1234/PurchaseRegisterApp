package com.example.purchaseregister.view.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purchaseregister.api.RetrofitClient
import com.example.purchaseregister.api.requests.RegistrarFacturaDesdeSunatRequest
import com.example.purchaseregister.model.Invoice
import com.example.purchaseregister.model.ProductItem
import com.example.purchaseregister.viewmodel.shared.FacturaRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class PurchaseRegistrationViewModel : ViewModel() {
    private val apiService = RetrofitClient.sunatApiService

    fun agregarNuevaFacturaCompra(
        ruc: String,
        razonSocial: String,
        serie: String,
        numero: String,
        fechaEmision: String,
        tipoDocumento: String,
        moneda: String = "",
        costoTotal: String = "",
        igv: String = "",
        importeTotal: String = "",
        anio: String = "",
        tipoCambio: String = "",
        productos: List<ProductItem> = emptyList()
    ) {
        viewModelScope.launch {
            try {
                // 🔴 PASO 1: Primero registrar en la base de datos
                val request = RegistrarFacturaDesdeSunatRequest(
                    rucEmisor = ruc,
                    serie = serie,
                    numero = numero,
                    fechaEmision = fechaEmision,
                    razonSocial = razonSocial,
                    tipoDocumento = tipoDocumento,
                    moneda = moneda,
                    costoTotal = costoTotal,
                    igv = igv,
                    importeTotal = importeTotal,
                    usuarioId = 1
                )

                val response = apiService.registrarFacturaDesdeSunat(request)

                // 🔴 PASO 2: Si se registró exitosamente en BD
                if (response.success && response.idFactura != null) {
                    val estadoInicial = if (productos.isNotEmpty()) "CON DETALLE" else "CONSULTADO"

                    // Guardar en el repositorio local usando el ID real de la BD
                    FacturaRepository.updateFacturasCompras { lista ->
                        val nuevaFactura = Invoice(
                            id = response.idFactura!!,  // Usar el ID real de la BD
                            ruc = ruc,
                            razonSocial = razonSocial,
                            serie = serie,
                            numero = numero,
                            fechaEmision = fechaEmision,
                            tipoDocumento = tipoDocumento,
                            anio = anio,
                            moneda = moneda,
                            costoTotal = costoTotal,
                            igv = igv,
                            tipoCambio = tipoCambio,
                            importeTotal = importeTotal,
                            estado = estadoInicial,
                            isSelected = false,
                            productos = productos
                        )

                        (lista + nuevaFactura).sortedBy { factura ->
                            try {
                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(factura.fechaEmision)?.time ?: 0L
                            } catch (e: Exception) {
                                0L
                            }
                        }
                    }
                } else {
                    // Si falla el registro en BD, mostrar error
                    println("❌ Error registrando factura en BD: ${response.message}")
                }
            } catch (e: Exception) {
                println("❌ Excepción al registrar factura: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}