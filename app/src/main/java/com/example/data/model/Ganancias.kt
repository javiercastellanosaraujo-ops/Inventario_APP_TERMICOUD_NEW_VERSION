package com.example.data.model

data class UsuarioGanancia(
    val usuario: String = "",
    val ventas: Int = 0,
    val unidades: Int = 0,
    val totalUsd: Double = 0.0,
    val totalBs: Double = 0.0
)

data class GananciasMes(
    val mes: String = "",
    val usuarios: List<UsuarioGanancia> = emptyList(),
    val totalUsd: Double = 0.0,
    val totalBs: Double = 0.0,
    val isArchived: Boolean = false
)
