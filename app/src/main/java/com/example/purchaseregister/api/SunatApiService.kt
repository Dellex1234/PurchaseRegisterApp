package com.example.purchaseregister.api

import com.example.purchaseregister.api.requests.*
import com.example.purchaseregister.api.responses.*
import retrofit2.http.*
import okhttp3.ResponseBody

interface SunatApiService {
    @GET("sunat/facturas")
    suspend fun obtenerFacturas(
        @Query("periodoInicio") periodoInicio: String,
        @Query("periodoFin") periodoFin: String,
        @Query("ruc") ruc: String,
        @Query("usuario") usuario: String,
        @Query("claveSol") claveSol: String
    ): SunatResponse

    @PUT("factura/scraping-completado/{numeroComprobante}")
    @Headers("Content-Type: application/json")
    suspend fun marcarScrapingCompletado(
        @Path("numeroComprobante") numeroComprobante: String,
        @Body request: ScrapingCompletadoRequest? = null
    ): ScrapingCompletadoResponse

    @POST("factura/guardar-productos/{numeroComprobante}")
    @Headers("Content-Type: application/json")
    suspend fun guardarProductosFactura(
        @Path("numeroComprobante") numeroComprobante: String,
        @Body request: GuardarProductosRequest
    ): GuardarProductosResponse

    @POST("factura/procesarFactura")
    @Headers("Content-Type: application/json")
    suspend fun registrarFacturasEnBD(
        @Body request: RegistroFacturasRequest
    ): RegistroFacturasResponse

    @GET("factura/{numeroComprobante}")
    suspend fun verificarFacturaRegistrada(
        @Path("numeroComprobante") numeroComprobante: String
    ): FacturaRegistradaResponse

    @POST("factura/registrar-desde-sunat")
    @Headers("Content-Type: application/json")
    suspend fun registrarFacturaDesdeSunat(
        @Body request: RegistrarFacturaDesdeSunatRequest
    ): RegistrarFacturaDesdeSunatResponse

    @GET("factura/ui/{numeroComprobante}")
    suspend fun obtenerFacturaParaUI(
        @Path("numeroComprobante") numeroComprobante: String
    ): FacturaUIResponse

    @GET("factura/ui/usuario/{usuarioId}")
    suspend fun obtenerFacturasUsuarioParaUI(
        @Path("usuarioId") usuarioId: String
    ): FacturasUIResponse

    @POST("sunat/descargar-xml")
    suspend fun descargarXmlConCola(
        @Body request: DetalleFacturaRequest
    ): EncoladoResponse

    @GET("sunat/job/{jobId}")
    suspend fun obtenerEstadoJob(
        @Path("jobId") jobId: String
    ): EstadoJobResponse

    @GET("factura/descargar/{numeroComprobante}/{tipo}")
    @Headers("Content-Type: application/octet-stream")
    suspend fun descargarArchivo(
        @Path("numeroComprobante") numeroComprobante: String,
        @Path("tipo") tipo: String
    ): ResponseBody

    @POST("sunat/validar-credenciales")
    suspend fun validarCredenciales(
        @Body request: ValidarCredencialesRequest
    ): ValidarCredencialesResponse
}