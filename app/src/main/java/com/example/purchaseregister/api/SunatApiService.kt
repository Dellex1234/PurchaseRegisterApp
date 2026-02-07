package com.example.purchaseregister.api

import com.example.purchaseregister.api.requests.*
import com.example.purchaseregister.api.responses.*
import retrofit2.http.*

interface SunatApiService {
    @GET("sunat/facturas")
    suspend fun obtenerFacturas(
        @Query("periodoInicio") periodoInicio: String,
        @Query("periodoFin") periodoFin: String
    ): SunatResponse

    @POST("sunat/descargar-xml")
    suspend fun obtenerDetalleFacturaXml(
        @Body request: DetalleFacturaRequest
    ): DetalleFacturaXmlResponse

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
}