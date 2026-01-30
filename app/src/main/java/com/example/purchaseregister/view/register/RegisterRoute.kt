package com.example.purchaseregister.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.purchaseregister.view.register.RegistroCompraScreen
import com.example.purchaseregister.viewmodel.InvoiceViewModel
import kotlinx.serialization.Serializable

// 1. Identificador de la pantalla de Registro
@Serializable
object RegisterRoute

// 2. Extensión para configurar el acceso a la pantalla de Registro
fun NavGraphBuilder.registerPurchaseRoute(
    onBack: () -> Unit,
    viewModel: InvoiceViewModel
) {
    composable<RegisterRoute> {
        RegistroCompraScreen(
            onBack = onBack,
            viewModel = viewModel
        )
    }
}