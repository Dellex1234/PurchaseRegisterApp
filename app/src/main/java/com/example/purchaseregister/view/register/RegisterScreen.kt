package com.example.purchaseregister.view.register

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
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
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.purchaseregister.BuildConfig
import com.example.purchaseregister.model.ProductoItem
import com.example.purchaseregister.view.components.ReadOnlyField
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import com.example.purchaseregister.utils.SunatPrefs
import java.util.concurrent.TimeUnit

// FUNCIONES AUXILIARES OPTIMIZADAS
fun bitmapToBase64(bitmap: Bitmap): String {
    val stream = ByteArrayOutputStream()
    // Reducir calidad para imágenes más pequeñas
    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
    return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
}

// Optimizar imagen antes de enviar (MUY IMPORTANTE para velocidad)
fun optimizeBitmapForGemini(bitmap: Bitmap): Bitmap {
    val maxSize = 1024 // Reducir a máximo 1024px para el lado más largo

    val width = bitmap.width
    val height = bitmap.height

    // Si ya es pequeña, no hacer nada
    if (width <= maxSize && height <= maxSize) {
        return bitmap
    }

    // Calcular nuevo tamaño manteniendo proporción
    val scale = maxSize.toFloat() / maxOf(width, height)
    val newWidth = (width * scale).toInt()
    val newHeight = (height * scale).toInt()

    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}

// FUNCIÓN OPTIMIZADA PARA LLAMAR A GEMINI (RÁPIDA)
fun callGeminiFast(
    bitmap: Bitmap,
    prompt: String,
    apiKey: String,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    Log.d("GEMINI_FAST", "🚀 Iniciando llamada rápida a Gemini...")

    // Optimizar imagen ANTES de convertir a Base64
    val optimizedBitmap = optimizeBitmapForGemini(bitmap)
    Log.d("GEMINI_FAST", "Imagen optimizada: ${optimizedBitmap.width}x${optimizedBitmap.height}")

    // Cliente HTTP con timeouts adecuados
    val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)  // 15 segundos máximo
        .readTimeout(30, TimeUnit.SECONDS)     // 30 segundos para lectura
        .writeTimeout(15, TimeUnit.SECONDS)    // 15 segundos para escritura
        .build()

    val escapedPrompt = prompt.replace("\n", "\\n").replace("\"", "\\\"")

    val json = """
    {
      "contents": [{
        "parts": [
          { "text": "$escapedPrompt" },
          {
            "inline_data": {
              "mime_type": "image/jpeg",
              "data": "${bitmapToBase64(optimizedBitmap)}"
            }
          }
        ]
      }]
    }
    """.trimIndent()

    val body = json.toRequestBody("application/json".toMediaType())

    // SOLO modelos con buen free tier y rápido
    val modelsToTry = listOf(
        "gemini-flash-latest",        // ✅ Mejor free tier y rápido
        "gemini-2.0-flash-latest",    // ✅ Alternativa rápida
        "gemini-1.5-flash"            // ✅ Legacy pero funcional
    )

    tryModelsFast(0, modelsToTry, client, body, apiKey, onSuccess, onError)
}

private fun tryModelsFast(
    index: Int,
    models: List<String>,
    client: OkHttpClient,
    body: RequestBody,
    apiKey: String,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    if (index >= models.size) {
        onError("⚠️ No hay modelos disponibles ahora. Intenta más tarde.")
        return
    }

    val modelName = models[index]
    Log.d("GEMINI_FAST", "🔍 Probando: $modelName (${index + 1}/${models.size})")

    val endpoint =
        "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

    val request = Request.Builder()
        .url(endpoint)
        .post(body)
        .addHeader("Content-Type", "application/json")
        .build()

    val startTime = System.currentTimeMillis()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            val elapsed = System.currentTimeMillis() - startTime
            Log.e("GEMINI_FAST", "❌ Error red con $modelName (${elapsed}ms): ${e.message}")
            // Probar siguiente modelo inmediatamente
            tryModelsFast(index + 1, models, client, body, apiKey, onSuccess, onError)
        }

        override fun onResponse(call: Call, response: Response) {
            val elapsed = System.currentTimeMillis() - startTime
            val text = response.body?.string()

            Log.d("GEMINI_FAST", "📡 $modelName - Código: ${response.code} (${elapsed}ms)")

            when {
                response.isSuccessful -> {
                    Log.d("GEMINI_FAST", "✅ $modelName funcionó en ${elapsed}ms")
                    onSuccess(text ?: "")
                }

                response.code == 429 -> {
                    Log.w("GEMINI_FAST", "⚠️ Quota excedido para $modelName")
                    // Probar siguiente inmediatamente (SIN ESPERAR)
                    tryModelsFast(index + 1, models, client, body, apiKey, onSuccess, onError)
                }

                response.code == 404 -> {
                    Log.w("GEMINI_FAST", "🔍 $modelName no encontrado")
                    tryModelsFast(index + 1, models, client, body, apiKey, onSuccess, onError)
                }

                else -> {
                    Log.e("GEMINI_FAST", "❌ Error ${response.code} con $modelName")
                    tryModelsFast(index + 1, models, client, body, apiKey, onSuccess, onError)
                }
            }
        }
    })
}

// FUNCIÓN MEJORADA PARA EXTRAER JSON
fun extractJsonFromGeminiResponse(response: String): String {
    return try {
        // Parsear la respuesta completa
        val json = JSONObject(response)

        // Extraer camino: candidates[0].content.parts[0].text
        val candidates = json.getJSONArray("candidates")
        if (candidates.length() == 0) {
            throw Exception("No hay candidatos en la respuesta")
        }

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.getJSONObject("content")
        val parts = content.getJSONArray("parts")

        if (parts.length() == 0) {
            throw Exception("No hay partes en el contenido")
        }

        val firstPart = parts.getJSONObject(0)
        var text = firstPart.getString("text").trim()

        // Limpiar markdown
        text = cleanMarkdown(text)

        // Verificar que sea un JSON válido
        if (!text.startsWith("{") || !text.endsWith("}")) {
            // Buscar JSON dentro del texto
            val start = text.indexOf("{")
            val end = text.lastIndexOf("}")
            if (start != -1 && end != -1) {
                text = text.substring(start, end + 1)
            } else {
                throw Exception("No se encontró JSON en: ${text.take(100)}...")
            }
        }

        text
    } catch (e: Exception) {
        Log.e("EXTRACT_JSON", "Error extrayendo JSON: ${e.message}")

        // Fallback: buscar JSON directamente en la respuesta
        val start = response.indexOf("{")
        val end = response.lastIndexOf("}")
        if (start != -1 && end != -1) {
            var text = response.substring(start, end + 1).trim()
            text = cleanMarkdown(text)
            text
        } else {
            throw Exception("No se pudo extraer JSON: ${e.message}")
        }
    }
}

fun cleanMarkdown(text: String): String {
    var cleaned = text.trim()

    // Eliminar bloques de código markdown
    if (cleaned.startsWith("```json")) {
        cleaned = cleaned.substring(7).trim()
    } else if (cleaned.startsWith("```")) {
        cleaned = cleaned.substring(3).trim()
    }

    if (cleaned.endsWith("```")) {
        cleaned = cleaned.substring(0, cleaned.length - 3).trim()
    }

    // Eliminar prefijos comunes
    val prefixes = listOf("JSON:", "json:", "Response:", "response:")
    prefixes.forEach { prefix ->
        if (cleaned.startsWith(prefix)) {
            cleaned = cleaned.substring(prefix.length).trim()
        }
    }

    return cleaned
}

// FUNCIÓN PARA PRUEBA RÁPIDA (OPCIONAL)
fun testGeminiConnectionFast(apiKey: String) {
    Log.d("GEMINI_TEST", "🧪 Probando conexión rápida...")

    val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val simpleJson = """
    {
      "contents": [{
        "parts": [{ "text": "Responde 'OK' en menos de 5 palabras." }]
      }]
    }
    """.trimIndent()

    val body = simpleJson.toRequestBody("application/json".toMediaType())

    // Probar solo con el modelo más confiable
    val testModel = "gemini-flash-latest"
    val url =
        "https://generativelanguage.googleapis.com/v1beta/models/$testModel:generateContent?key=$apiKey"

    val request = Request.Builder()
        .url(url)
        .post(body)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("GEMINI_TEST", "❌ Error de conexión: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            val text = response.body?.string()
            if (response.isSuccessful) {
                Log.d("GEMINI_TEST", "✅ Conexión OK con $testModel")
            } else {
                Log.d("GEMINI_TEST", "⚠️ Conexión problema: ${response.code}")
            }
        }
    })
}

// FUNCIONES HELPER PARA MONEDA
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroCompraScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val ruc = SunatPrefs.getRuc(context)
        if (ruc.isNullOrEmpty()) {
            Toast.makeText(
                context,
                "Primero inicie sesión en SUNAT",
                Toast.LENGTH_LONG
            ).show()
            onBack()  // Regresa a la pantalla anterior
        }
    }
    val scope = rememberCoroutineScope()
    val photoFile = remember {
        File.createTempFile("factura_", ".jpg", context.cacheDir)
    }

    // --- URI USANDO FILEPROVIDER ---
    val photoUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            photoFile
        )
    }

    // --- ESTADOS DE CONTROL ---
    var fotoTomada by remember { mutableStateOf(false) }
    var modoEdicion by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // --- ESTADOS DE DATOS (Vacíos por defecto) ---
    var rucPropio by remember { mutableStateOf("") }
    var serie by remember { mutableStateOf("") }
    var numero by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf("") }
    var esImportacion by remember { mutableStateOf(false) }
    var anioImportacion by remember { mutableStateOf("") }
    var moneda by remember { mutableStateOf("") }
    var tipoDocumento by remember { mutableStateOf("") }
    var rucProveedor by remember { mutableStateOf("") }
    var razonSocialProveedor by remember { mutableStateOf("") }
    var tipoCambio by remember { mutableStateOf("") }
    var costoTotal by remember { mutableStateOf("") }
    var igv by remember { mutableStateOf("") }
    var importeTotal by remember { mutableStateOf("") }

    val listaProductos = remember {
        mutableStateListOf(ProductoItem("", "", ""))
    }

    // Función para limpiar montos
    fun limpiarMonto(texto: String): String {
        return texto.replace(Regex("[^0-9.]"), "")
    }

    // --- LÓGICA DE CÁMARA Y PERMISOS ---
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->

        if (!success) {
            Toast.makeText(context, "Error al tomar foto", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }

        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
        if (bitmap == null) {
            Toast.makeText(context, "Error al leer imagen", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }

        isLoading = true
        fotoTomada = true
        modoEdicion = false

        val prompt = """
        Analiza esta factura/boleta peruana y extrae los datos.
        La factura es de una COMPRA que YO (mi empresa) estoy haciendo a un PROVEEDOR.
        
        Identifica claramente:
        - PROVEEDOR/VENDEDOR: Quien me vende (sus datos van en ruc_provider y razon_social)
        - YO/COMPRADOR: Mi empresa (no incluir aquí, esos datos los tengo yo)
        
        INSTRUCCIONES ESPECÍFICAS:
        - Para moneda: escribir solo "Soles" o "Dólares" (sin códigos PEN/USD)
        - SI ES DÓLARES: Extraer el tipo_cambio NUMÉRICO que aparece en la factura (ej: 3.85, 3.75, 4.10)
        - El tipo_cambio NO es fijo, varía según la fecha de la factura
        
        JSON REQUERIDO (datos del PROVEEDOR/VENDEDOR):
        {
          "tipo_documento": "Factura, Boleta o Nota de Venta",
          "ruc_provider": "RUC del PROVEEDOR/VENDEDOR (11 dígitos)",
          "razon_social": "Nombre completo del PROVEEDOR/VENDEDOR",
          "serie": "Serie del documento",
          "numero": "Número del documento",
          "fecha": "DD/MM/YYYY",
          "moneda": "Dólares",  // o "Soles"
          "tipo_cambio": "3.85",  // ← EXTRAER EL VALOR REAL DE LA FACTURA
          "productos": [
            {
              "descripcion": "Nombre del producto",
              "cantidad": "Cantidad (solo números)",
              "costo_unitario": "Precio unitario (solo números)"
            }
          ],
          "costo_total": "Total sin IGV (solo números)",
          "igv": "IGV (18%) (solo números)",
          "importe_total": "Total con IGV (solo números)"
        }
        
        IMPORTANTE: 
        - ruc_provider es el RUC del que ME VENDE (proveedor/vendedor)
        - No incluir MI RUC (comprador) en la respuesta
        - Las cantidades y montos deben ser solo números, sin símbolos
        """.trimIndent()

        Log.d("GEMINI_APP", "📸 Foto tomada, llamando a Gemini...")

        // USAR LA FUNCIÓN RÁPIDA OPTIMIZADA
        callGeminiFast(
            bitmap = bitmap,
            prompt = prompt,
            apiKey = BuildConfig.GEMINI_API_KEY,
            onSuccess = { response ->
                scope.launch {
                    try {
                        Log.d("GEMINI_APP", "✅ Respuesta recibida, procesando...")

                        // Extraer el JSON de la respuesta
                        val jsonText = extractJsonFromGeminiResponse(response)
                        Log.d("GEMINI_APP", "📄 JSON extraído: ${jsonText.take(300)}...")

                        // Parsear el JSON real
                        val json = JSONObject(jsonText)

                        // Extraer datos
                        tipoDocumento = json.optString("tipo_documento")
                        rucProveedor = json.optString("ruc_provider")
                        razonSocialProveedor = json.optString("razon_social")
                        serie = json.optString("serie")
                        numero = json.optString("numero")
                        fecha = json.optString("fecha")
                        val monedaExtraida = json.optString("moneda")
                        moneda = formatearMoneda(monedaExtraida)
                        val tipoCambioExtraido = json.optString("tipo_cambio")
                        if (esMonedaDolares(moneda)) {
                            // Validar si Gemini extrajo un valor numérico válido
                            tipoCambio = when {
                                // Caso 1: Gemini devolvió un número válido (ej: "3.85")
                                tipoCambioExtraido.matches(Regex("[0-9]+(\\.[0-9]+)?")) -> {
                                    tipoCambioExtraido
                                }
                                // Caso 2: Gemini devolvió "null" o vacío
                                tipoCambioExtraido == "null" || tipoCambioExtraido.isEmpty() -> {
                                    // Podemos poner un placeholder o dejarlo vacío para que el usuario lo ingrese
                                    "" // ← Mejor vacío que un valor falso
                                }
                                // Caso 3: Cualquier otro texto
                                else -> {
                                    // Intentar extraer números del texto (por si Gemini escribió "Tipo cambio: 3.85")
                                    val numeros = Regex("[0-9]+(\\.[0-9]+)?").find(tipoCambioExtraido)
                                    numeros?.value ?: ""
                                }
                            }

                            // Si quedó vacío, mostrar mensaje para que el usuario lo ingrese
                            if (tipoCambio.isEmpty()) {
                                Log.w("TIPO_CAMBIO", "No se pudo extraer tipo de cambio para dólares")
                                // Opcional: Toast para alertar al usuario
                                Toast.makeText(
                                    context,
                                    "Ingrese manualmente el tipo de cambio",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            // Si es Soles, tipo de cambio vacío
                            tipoCambio = ""
                        }
                        costoTotal = limpiarMonto(json.optString("costo_total"))
                        igv = limpiarMonto(json.optString("igv"))
                        importeTotal = limpiarMonto(json.optString("importe_total"))
                        rucPropio = SunatPrefs.getRuc(context) ?: ""

                        // Procesar productos
                        listaProductos.clear()
                        json.optJSONArray("productos")?.let { arr ->
                            for (i in 0 until arr.length()) {
                                val p = arr.getJSONObject(i)
                                listaProductos.add(
                                    ProductoItem(
                                        p.optString("descripcion"),
                                        p.optString("costo_unitario"),
                                        p.optString("cantidad")
                                    )
                                )
                            }
                        }

                        // Si no hay productos, mantener al menos uno vacío
                        if (listaProductos.isEmpty()) {
                            listaProductos.add(ProductoItem("", "", ""))
                        }

                        // DEBUG: Verificar datos extraídos
                        Log.d("GEMINI_APP", "📊 Datos extraídos:")
                        Log.d("GEMINI_APP", "- Mi RUC: '$rucPropio'")
                        Log.d("GEMINI_APP", "- Tipo Doc: '$tipoDocumento'")
                        Log.d("GEMINI_APP", "- RUC Prov: '$rucProveedor'")
                        Log.d("GEMINI_APP", "- Razon Social: '$razonSocialProveedor'")
                        Log.d("GEMINI_APP", "- Serie: '$serie'")
                        Log.d("GEMINI_APP", "- Número: '$numero'")
                        Log.d("GEMINI_APP", "- Fecha: '$fecha'")
                        Log.d("GEMINI_APP", "- Moneda: '$moneda'")
                        Log.d("GEMINI_APP", "- Costo Total: '$costoTotal'")
                        Log.d("GEMINI_APP", "- IGV: '$igv'")
                        Log.d("GEMINI_APP", "- Importe Total: '$importeTotal'")
                        Log.d("GEMINI_APP", "- Productos: ${listaProductos.size}")

                        Toast.makeText(context, "✅ Factura analizada!", Toast.LENGTH_LONG).show()

                    } catch (e: Exception) {
                        Log.e("GEMINI_APP", "❌ Error procesando: ${e.message}")
                        Log.e("GEMINI_APP", "Stack: ${e.stackTraceToString()}")
                        Toast.makeText(
                            context,
                            "Error: ${e.message?.take(50)}...",
                            Toast.LENGTH_LONG
                        ).show()
                    } finally {
                        isLoading = false
                    }
                }
            },
            onError = { errorMsg ->
                scope.launch {
                    isLoading = false
                    Log.e("GEMINI_APP", "❌ Error: $errorMsg")
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // Lanzador para solicitar permiso de cámara
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (it) {
            cameraLauncher.launch(photoUri)
        } else {
            Toast.makeText(context, "Permiso de cámara requerido", Toast.LENGTH_LONG).show()
        }
    }

    val esDolares = esMonedaDolares(moneda)
    val monedaWeight = if (esDolares) 1.2f else 2f

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Flecha de retroceso
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.Black
                        )
                    }

                    // Título con Icono de Cámara
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Registrar factura",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            enabled = !isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Escanear",
                                tint = if (isLoading) Color.Gray else Color(0xFF1FB8B9)
                            )
                        }
                    }
                }
            }) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
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
                        onValueChange = { rucPropio = it },
                        label = "RUC",
                        isReadOnly = !modoEdicion,
                        modifier = Modifier.weight(2.8f)
                    )
                    ReadOnlyField(
                        value = serie,
                        onValueChange = { serie = it },
                        label = "Serie",
                        isReadOnly = !modoEdicion,
                        modifier = Modifier.weight(1.5f)
                    )
                    ReadOnlyField(
                        value = numero,
                        onValueChange = { numero = it },
                        label = "N°",
                        isReadOnly = !modoEdicion,
                        modifier = Modifier.weight(1f)
                    )
                    ReadOnlyField(
                        value = fecha,
                        onValueChange = { fecha = it },
                        label = "Fecha Emisión",
                        isReadOnly = !modoEdicion,
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
                        value = tipoDocumento,
                        onValueChange = { tipoDocumento = it },
                        label = "Tipo de Documento",
                        isReadOnly = !modoEdicion,
                        modifier = Modifier.weight(1.9f)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1.4f)
                    ) {
                        Text("¿Importación?", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = esImportacion,
                            onCheckedChange = { esImportacion = it },
                            enabled = modoEdicion
                        )
                    }

                    if (esImportacion) {
                        ReadOnlyField(
                            value = anioImportacion,
                            onValueChange = { anioImportacion = it },
                            label = "Año",
                            isReadOnly = !modoEdicion,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                // --- FILA 3: RUC Y RAZÓN SOCIAL EN UNA SOLA LÍNEA ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ReadOnlyField(
                        value = rucProveedor,
                        onValueChange = { rucProveedor = it },
                        label = "RUC Proveedor",
                        isReadOnly = !modoEdicion,
                        modifier = Modifier
                            .weight(1.5f)
                            .fillMaxHeight()
                    )
                    ReadOnlyField(
                        value = razonSocialProveedor,
                        onValueChange = { razonSocialProveedor = it },
                        label = "Razón Social del Proveedor",
                        isReadOnly = !modoEdicion,
                        modifier = Modifier
                            .weight(3f)
                            .fillMaxHeight(),
                        isSingleLine = false,
                        textAlign = TextAlign.Center
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
                            onValueChange = { nuevaDesc ->
                                listaProductos[index] = producto.copy(descripcion = nuevaDesc)
                            },
                            label = if (index == 0) "Descripción" else "",
                            isReadOnly = !modoEdicion,
                            modifier = Modifier
                                .weight(3f)
                                .fillMaxHeight(),
                            isSingleLine = false
                        )
                        ReadOnlyField(
                            value = producto.costoUnitario,
                            onValueChange = { nuevoCosto ->
                                listaProductos[index] = producto.copy(costoUnitario = nuevoCosto)
                            },
                            label = if (index == 0) "Costo Unit." else "",
                            isReadOnly = !modoEdicion,
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight()
                        )
                        ReadOnlyField(
                            value = producto.cantidad,
                            onValueChange = { nuevaCant ->
                                listaProductos[index] = producto.copy(cantidad = nuevaCant)
                            },
                            label = if (index == 0) "Cant." else "",
                            isReadOnly = !modoEdicion,
                            modifier = Modifier
                                .weight(0.8f)
                                .fillMaxHeight()
                        )
                    }
                }

                // --- FILA 5: MONEDA, T. CAMBIO (CONDICIONAL) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReadOnlyField(
                        value = moneda,
                        onValueChange = { moneda = it },
                        label = "Moneda",
                        isReadOnly = !modoEdicion,
                        modifier = Modifier.weight(monedaWeight)
                    )
                    if (esDolares) {
                        ReadOnlyField(
                            value = tipoCambio,
                            onValueChange = { tipoCambio = it },
                            label = "T.C",
                            isReadOnly = !modoEdicion,
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                }

                // --- FILA 6: COSTO TOTAL, IGV e IMPORTE TOTAL ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReadOnlyField(
                        value = costoTotal,
                        onValueChange = { costoTotal = it },
                        label = "Costo Total",
                        isReadOnly = !modoEdicion,
                        modifier = Modifier.weight(1.8f)
                    )
                    ReadOnlyField(
                        value = igv,
                        onValueChange = { igv = it },
                        label = "IGV",
                        isReadOnly = !modoEdicion,
                        modifier = Modifier.weight(1.5f)
                    )
                    ReadOnlyField(
                        value = importeTotal,
                        onValueChange = { importeTotal = it },
                        label = "Importe Total",
                        isReadOnly = !modoEdicion,
                        modifier = Modifier.weight(2f),
                        isHighlight = true
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
                        onClick = { modoEdicion = !modoEdicion },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (modoEdicion) Color(0xFF1FB8B9) else Color.Gray
                        ),
                        shape = MaterialTheme.shapes.medium,
                        enabled = fotoTomada && !isLoading
                    ) { Text("EDITAR", fontWeight = FontWeight.Bold) }
                }
                Spacer(modifier = Modifier.height(5.dp))
            }
        }

        // --- CAPA DE LOADING
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Analizando factura...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegistroCompraScreenPreview() {
    RegistroCompraScreen(onBack = { })
}