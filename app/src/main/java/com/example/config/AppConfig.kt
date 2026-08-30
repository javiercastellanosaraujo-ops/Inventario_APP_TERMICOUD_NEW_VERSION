package com.example.config

/**
 * =========================================================================
 * ARCHIVO DE CONFIGURACIÓN GLOBAL / PERSONALIZACIÓN DE MARCA (WHITE-LABEL)
 * =========================================================================
 * Puedes modificar los valores de este archivo para cambiar el nombre de
 * la empresa, el nombre del sistema, el correo del administrador y los datos
 * fiscales para revender o personalizar esta aplicación fácilmente a cualquier cliente.
 */
object AppConfig {

    // =====================================================================
    // 1. CONFIGURACIÓN DEL ADMINISTRADOR (CORREO Y PROPIETARIO)
    // =====================================================================
    /**
     * Correo electrónico del administrador principal.
     * Todas las notas de entrega y reportes se enviarán a este correo.
     * Cámbialo aquí cuando desees traspasar o asignar un nuevo administrador.
     */
    const val ADMIN_EMAIL = "javiercastellanosaraujo@gmail.com"

    val ADMIN_EMAILS = setOf(
        "javiercastellanosaraujo@gmail.com",
        "glamstykemanes@gmail.com"
    )

    fun isUserAdmin(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        val clean = email.trim().lowercase()
        return ADMIN_EMAILS.any { it.equals(clean, ignoreCase = true) }
    }

    /**
     * Nombre del administrador o propietario del negocio.
     */
    const val ADMIN_NAME = "Administrador"


    // =====================================================================
    // 2. IDENTIDAD Y NOMBRE DEL SISTEMA (BRANDING)
    // =====================================================================
    /**
     * Nombre comercial o razón social de la empresa.
     */
    const val BUSINESS_NAME = "Termicoud"

    /**
     * Nombre corto / Marca del sistema.
     */
    const val APP_BRAND_NAME = "Termicoud"

    /**
     * Título principal mostrado en Dashboard, Login y Reportes.
     */
    const val APP_FULL_TITLE = "TERMICOUD INVENTARIO"

    /**
     * Subtítulo / Lema de la aplicación.
     */
    const val APP_SUBTITLE = "Control Multi-Operador"


    // =====================================================================
    // 3. DATOS DE NOTA DE ENTREGA / COMPROBANTES (PDF)
    // =====================================================================
    /**
     * RIF o Registro Fiscal del negocio para la Nota de Entrega.
     */
    const val BUSINESS_RIF = ""

    /**
     * Dirección del establecimiento para la Nota de Entrega.
     */
    const val BUSINESS_ADDRESS = "Venezuela"

    /**
     * Teléfono de contacto de la empresa.
     */
    const val BUSINESS_PHONE = "+58 412-7115239"

    /**
     * Pie de página en el comprobante PDF.
     */
    const val PDF_FOOTER_NOTE = "Comprobante emitido digitalmente por el sistema de Inventario."


    // =====================================================================
    // 4. GUARDADO AUTOMÁTICO DE NOTAS DE ENTREGA EN GOOGLE DRIVE
    // =====================================================================
    /**
     * URL del Web App de Google Apps Script vinculado a tu carpeta de Google Drive.
     * Si colocas la URL de tu script aquí, cada vez que se registre una venta,
     * el PDF de la Nota de Entrega se guardará AUTOMÁTICAMENTE en tu carpeta de Drive
     * en segundo plano, sin necesidad de que el operador tenga que presionar "Compartir".
     */
    const val GOOGLE_DRIVE_FOLDER_WEBHOOK_URL = "https://script.google.com/macros/s/AKfycbxfpycvzbagzngWoZCfIwP-pew5OahSm8_S8Oza01P9mfassW5E_isSHojiW7gQc4WV/exec"
}
