package com.example.purchaseregister.view.register

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.purchaseregister.model.ProductoItem
import com.example.purchaseregister.view.components.ReadOnlyField
import com.example.purchaseregister.view.puchase.obtenerRucSunat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    rucProveedor: String?,
    serie: String?,
    numero: String?,
    fecha: String?,
    razonSocial: String?,
    tipoDocumento: String?,
    anio: String?,
    moneda: String?,
    costoTotal: String?,
    igv: String?,
    tipoCambio: String?,
    importeTotal: String?
) {
    val context = LocalContext.current

    // 1. Obtiene el RUC del usuario logueado. Si es null, muestra vacío.
    val rucPropio = remember { obtenerRucSunat(context) ?: "" }

    // 3. Lista de productos (puedes dejarla vacía o con un ítem base)
    val listaProductos = remember {
        mutableStateListOf<ProductoItem>().apply {
            add(ProductoItem(descripcion = "", costoUnitario = "", cantidad = ""))
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flecha de retroceso a la izquierda
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.Black
                    )
                }

                // Título
                Text(
                    text = "Detalle de factura",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                // Spacer para equilibrar el espacio de la flecha y mantener el título centrado
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            // --- FILA 1: RUC, SERIE, NUMERO, FECHA ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ReadOnlyField(
                    value = rucPropio,
                    onValueChange = { },
                    label = "RUC Propio",
                    modifier = Modifier.weight(2.8f)
                )
                ReadOnlyField(
                    value = serie ?: "",
                    onValueChange = { },
                    label = "Serie",
                    modifier = Modifier.weight(1.5f)
                )
                ReadOnlyField(
                    value = numero ?: "",
                    onValueChange = { },
                    label = "N°",
                    modifier = Modifier.weight(1f)
                )
                ReadOnlyField(
                    value = fecha ?: "",
                    onValueChange = { },
                    label = "Fecha Emisión",
                    modifier = Modifier.weight(2.8f)
                )
            }

            // --- FILA 2: TIPO DOCUMENTO, IMPORTACIÓN, AÑO ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReadOnlyField(
                    value = tipoDocumento ?: "",
                    onValueChange = { },
                    label = "Tipo de Documento",
                    modifier = Modifier.weight(1.8f)
                )
                ReadOnlyField(
                    value = anio ?: "",
                    onValueChange = { },
                    label = "Año",
                    modifier = Modifier.weight(0.4f)
                )
            }

            // --- FILA 3: RUC Y RAZÓN SOCIAL EN UNA SOLA LÍNEA ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ReadOnlyField(
                    value = rucProveedor ?: "",
                    onValueChange = { },
                    label = "RUC Proveedor",
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight(),
                )
                ReadOnlyField(
                    value = razonSocial ?: "",
                    onValueChange = { },
                    label = "Razón Social del Proveedor",
                    modifier = Modifier
                        .weight(3f)
                        .fillMaxHeight(),
                    isSingleLine = false
                )
            }

            // --- FILA 4: DESCRIPCIÓN, COSTO UNIT, CANTIDAD ---
            listaProductos.forEachIndexed { index, producto ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ReadOnlyField(
                        value = producto.descripcion,
                        onValueChange = { },
                        label = if (index == 0) "Descripción" else "",
                        modifier = Modifier
                            .weight(3f)
                            .fillMaxHeight(),
                        isSingleLine = false
                    )
                    ReadOnlyField(
                        value = producto.costoUnitario,
                        onValueChange = { },
                        label = if (index == 0) "Costo Unit." else "",
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                    )
                    ReadOnlyField(
                        value = producto.cantidad,
                        onValueChange = { },
                        label = if (index == 0) "Cant." else "",
                        modifier = Modifier
                            .weight(0.8f)
                            .fillMaxHeight()
                    )
                }
            }

            // --- FILA 5: MONEDA Y TIPO DE CAMBIO ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReadOnlyField(
                    value = moneda ?: "",
                    onValueChange = { },
                    label = "Moneda",
                    modifier = Modifier.weight(1f)
                )
                ReadOnlyField(
                    value = tipoCambio ?: "",
                    onValueChange = { },
                    label = "T.C.",
                    modifier = Modifier.weight(1f)
                )
            }

            // --- FILA 6: COSTO TOTAL, IGV, IMPORTE TOTAL---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReadOnlyField(
                    value = costoTotal ?: "",
                    onValueChange = { },
                    label = "Costo Total",
                    modifier = Modifier.weight(1.8f)
                )
                ReadOnlyField(
                    value = igv ?: "",
                    onValueChange = { },
                    label = "IGV",
                    modifier = Modifier.weight(1.5f)
                )
                ReadOnlyField(
                    value = importeTotal ?: "",
                    onValueChange = { },
                    label = "IMPORTE TOTAL",
                    modifier = Modifier.weight(2f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- BOTONES FINALES ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { /* Lógica Registrar */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1FB8B9)),
                    shape = MaterialTheme.shapes.medium
                ) { Text("REGISTRAR", fontWeight = FontWeight.Bold) }

                Button(
                    onClick = { /* Lógica Editar */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                    shape = MaterialTheme.shapes.medium
                ) { Text("EDITAR", fontWeight = FontWeight.Bold) }
            }
            Spacer(modifier = Modifier.height(5.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DetailScreenPreview() {
    DetailScreen(
        onBack = { },
        rucProveedor = "20551234891",
        serie = "F001",
        numero = "10",
        fecha = "28/01/2026",
        razonSocial = "PRODUCTOS TECNOLOGICOS S.A.",
        tipoDocumento = "FACTURA",
        anio = "2026",
        moneda = "Soles (PEN)",
        costoTotal = "100.00",
        igv = "18.00",
        tipoCambio = "3.75",
        importeTotal = "118.00"
    )
}