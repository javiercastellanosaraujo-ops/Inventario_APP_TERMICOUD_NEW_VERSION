package com.example.data.model

data class AppUser(
    val email: String = "",
    val nombre: String = "",
    val estado: String = "pendiente", // "pendiente", "aprobado", "rechazado"
    val fechaSolicitud: Long = System.currentTimeMillis(),
    val fechaAprobacion: Long? = null,
    val aprobadoPorEmail: String? = null,
    val rol: String = "operador" // "admin", "operador"
)
