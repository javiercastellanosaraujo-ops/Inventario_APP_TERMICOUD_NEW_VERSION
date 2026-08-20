package com.example.data.model

import java.util.UUID

data class ComboComponente(
    val fila: Int = 0,
    val nombre: String = "",
    val cantidadPorCombo: Int = 1,
    val stockDisponible: Int = 0
)

data class Combo(
    val fila: Int = 0,
    val id: String = UUID.randomUUID().toString(),
    val nombre: String = "",
    val precioUsd: Double = 0.0,
    val categoria: String = "Combos",
    val componentes: List<ComboComponente> = emptyList(),
    val disponibles: Int = 0
) {
    val recetaTexto: String
        get() = if (componentes.isEmpty()) {
            "Sin componentes definidos"
        } else {
            componentes.joinToString(" + ") { "${it.cantidadPorCombo}× ${it.nombre}" }
        }
}
