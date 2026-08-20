package com.example.data.model

import java.util.UUID

data class ComboComponente(
    val fila: Int = 0,
    val nombre: String = "",
    val cantidadPorCombo: Int = 1,
    val stockDisponible: Int = 0,
    val precioCompraUnitario: Double = 0.0,
    val precioVentaUnitario: Double = 0.0
)

data class Combo(
    val fila: Int = 0,
    val id: String = UUID.randomUUID().toString(),
    val nombre: String = "",
    val precioUsd: Double = 0.0,
    val categoria: String = "Combos",
    val componentes: List<ComboComponente> = emptyList(),
    val disponibles: Int = 0,
    val costoTotal: Double = 0.0
) {
    val costoCalculado: Double
        get() = if (costoTotal > 0) costoTotal else componentes.sumOf { it.precioCompraUnitario * it.cantidadPorCombo }

    val sumaPreciosIndividuales: Double
        get() = componentes.sumOf { it.precioVentaUnitario * it.cantidadPorCombo }

    val gananciaEstimada: Double
        get() = if (costoCalculado > 0) (precioUsd - costoCalculado).coerceAtLeast(0.0) else precioUsd

    val margenGananciaPorcentaje: Double
        get() = if (precioUsd > 0 && costoCalculado > 0) {
            ((precioUsd - costoCalculado) / precioUsd) * 100.0
        } else 0.0

    val recetaTexto: String
        get() = if (componentes.isEmpty()) {
            "Sin componentes definidos"
        } else {
            componentes.joinToString(" + ") { "${it.cantidadPorCombo}× ${it.nombre}" }
        }
}

