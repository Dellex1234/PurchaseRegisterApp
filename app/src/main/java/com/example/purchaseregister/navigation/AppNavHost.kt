package com.example.purchaseregister.navigation

import androidx.compose.runtime.Composable
import androidx.compose.material3.Text // ← AÑADIR ESTE IMPORT
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.purchaseregister.view.register.DetailScreen
import com.example.purchaseregister.viewmodel.InvoiceViewModel

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val viewModel: InvoiceViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = PurchaseDetailRoute // Punto de entrada
    ) {
        // 1. Pantalla Principal
        purchaseDetailRoute(
            viewModel = viewModel,
            onNavigateToRegistrar = {
                navController.navigate(RegisterRoute)
            },
            onNavigateToDetalle = { routeData ->
                navController.navigate(routeData)
            }
        )

        // 2. Pantalla de Registro
        registerPurchaseRoute(
            onBack = {
                navController.popBackStack() // Regresa a la pantalla anterior
            },
            viewModel = viewModel
        )

        // 3. Pantalla de Detalle de Factura
        composable<DetailRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<DetailRoute>()

            println("🎯 [AppNavHost] Recibiendo DetailRoute:")
            println("🎯 [AppNavHost] - ID: ${args.id}")
            println("🎯 [AppNavHost] - esCompra: ${args.esCompra}")

            // Buscar la factura completa en el ViewModel usando el ID
            val factura = if (args.esCompra) {
                viewModel.facturasCompras.value.firstOrNull { it.id == args.id }
            } else {
                viewModel.facturasVentas.value.firstOrNull { it.id == args.id }
            }

            if (factura != null) {
                DetailScreen(
                    id = factura.id,
                    onBack = {
                        println("🔙 [AppNavHost] Navegando BACK desde DetailScreen")
                        navController.popBackStack()
                    },
                    rucProveedor = factura.ruc,
                    serie = factura.serie,
                    numero = factura.numero,
                    fecha = factura.fechaEmision,
                    razonSocial = factura.razonSocial,
                    tipoDocumento = factura.tipoDocumento,
                    anio = factura.anio,
                    moneda = factura.moneda,
                    costoTotal = factura.costoTotal,
                    igv = factura.igv,
                    tipoCambio = factura.tipoCambio,
                    importeTotal = factura.importeTotal,
                    esCompra = args.esCompra,
                    productos = factura.productos, // ← PASAR LISTA REAL DE PRODUCTOS
                    onAceptar = {
                        println("✅ [AppNavHost] onAceptar llamado")
                        println("✅ [AppNavHost] Actualizando factura ID: ${factura.id}")
                        println("✅ [AppNavHost] Estado nuevo: CON DETALLE")
                        println("✅ [AppNavHost] esCompra: ${args.esCompra}")
                        // ¡AQUÍ ACTUALIZAMOS EL ESTADO!
                        viewModel.actualizarEstadoFactura(factura.id, "CON DETALLE", args.esCompra)
                    }
                )
            } else {
                // Mostrar mensaje de error
                Text("Factura no encontrada")
            }
        }
    }
}