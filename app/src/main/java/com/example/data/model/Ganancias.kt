package com.example.data.model

data class UsuarioGanancia(
    val usuario: String = "",
    val ventas: Int = 0,
    val unidades: Int = 0,
    val totalUsd: Double = 0.0,
    val totalBs: Double = 0.0,
    val totalCostoUsd: Double = 0.0,
    val gananciaNetaUsd: Double = 0.0,
    val margenPorcentaje: Double = 0.0
)

data class GananciasMes(
    val mes: String = "",
    val usuarios: List<UsuarioGanancia> = emptyList(),
    val totalUsd: Double = 0.0,
    val totalBs: Double = 0.0,
    val totalCostoUsd: Double = 0.0,
    val gananciaNetaUsd: Double = 0.0,
    val margenPorcentaje: Double = 0.0,
    val isArchived: Boolean = false
)

data class CierreMensualInfo(
    val id: String = "",
    val mes: String = "",
    val mesKey: String = "",
    val totalUsd: Double = 0.0,
    val totalBs: Double = 0.0,
    val totalCostoUsd: Double = 0.0,
    val gananciaNetaUsd: Double = 0.0,
    val margenPorcentaje: Double = 0.0,
    val usuariosCount: Int = 0,
    val totalItemsInventario: Int = 0,
    val unidadesStockTotal: Int = 0,
    val valorInventarioCostoUsd: Double = 0.0,
    val valorInventarioVentaUsd: Double = 0.0,
    val cerradoEn: Long = 0L,
    val cerradoPorEmail: String = "",
    val cerradoPorNombre: String = "",
    val archivoDriveNombre: String = "",
    val archivoDriveId: String = "",
    val driveUrl: String = ""
)


