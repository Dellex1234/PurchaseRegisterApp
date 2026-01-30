package com.example.purchaseregister.navigation

import androidx.compose.runtime.Composable
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
            println("🎯 [AppNavHost] - RUC: ${args.rucProveedor}")
            println("🎯 [AppNavHost] - esCompra: ${args.esCompra}")

            DetailScreen(
                id = args.id,
                onBack = {
                    println("🔙 [AppNavHost] Navegando BACK desde DetailScreen")
                    navController.popBackStack()
                },
                rucProveedor = args.rucProveedor,
                serie = args.serie,
                numero = args.numero,
                fecha = args.fecha,
                razonSocial = args.razonSocial,
                tipoDocumento = args.tipoDocumento,
                anio = args.anio,
                moneda = args.moneda,
                costoTotal = args.costoTotal,
                igv = args.igv,
                tipoCambio = args.tipoCambio,
                importeTotal = args.importeTotal,
                esCompra = args.esCompra,
                onAceptar = {
                    println("✅ [AppNavHost] onAceptar llamado")
                    println("✅ [AppNavHost] Actualizando factura ID: ${args.id}")
                    println("✅ [AppNavHost] Estado nuevo: CON DETALLE")
                    println("✅ [AppNavHost] esCompra: ${args.esCompra}")
                    // ¡AQUÍ ACTUALIZAMOS EL ESTADO!
                    viewModel.actualizarEstadoFactura(args.id, "CON DETALLE", args.esCompra)
                }
            )
        }
    }
}