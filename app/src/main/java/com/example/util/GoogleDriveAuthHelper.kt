package com.example.util

import android.app.Activity
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.tasks.await

object GoogleDriveAuthHelper {
    private const val TAG = "GoogleDriveAuthHelper"
    val DRIVE_FILE_SCOPE = Scope("https://www.googleapis.com/auth/drive.file")

    private var pendingAuthDeferred: CompletableDeferred<String?>? = null

    /**
     * Solicita el token de acceso para el scope de Google Drive (https://www.googleapis.com/auth/drive.file).
     * Si ya fue autorizado previamente, devuelve el accessToken directamente.
     * Si requiere intervención del usuario, lanza el IntentSenderRequest mediante el launcher provisto y espera el resultado.
     */
    suspend fun solicitarPermisoDrive(
        activity: Activity,
        launchIntentSender: (IntentSenderRequest) -> Unit
    ): String? {
        try {
            val authorizationClient = Identity.getAuthorizationClient(activity)
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(DRIVE_FILE_SCOPE))
                .build()

            val result = authorizationClient.authorize(request).await()

            // Si ya cuenta con el token directo sin requerir diálogo
            if (!result.accessToken.isNullOrBlank()) {
                Log.d(TAG, "Token de Google Drive obtenido directamente sin resolución adicional")
                return result.accessToken
            }

            // Si requiere resolución (permiso del usuario)
            if (result.hasResolution() && result.pendingIntent != null) {
                val deferred = CompletableDeferred<String?>()
                pendingAuthDeferred = deferred

                val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent!!.intentSender).build()
                launchIntentSender(intentSenderRequest)

                return deferred.await()
            }

            Log.w(TAG, "No se obtuvo accessToken ni pendingIntent para resolución")
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error al solicitar autorización de Google Drive: ${e.message}", e)
            return null
        }
    }

    /**
     * Procesa la respuesta devuelta por el ActivityResultLauncher tras la interacción del usuario.
     */
    fun onActivityResult(activity: Activity, activityResult: ActivityResult) {
        val deferred = pendingAuthDeferred
        pendingAuthDeferred = null

        if (deferred == null) return

        if (activityResult.resultCode == Activity.RESULT_OK && activityResult.data != null) {
            try {
                val authResult = Identity.getAuthorizationClient(activity)
                    .getAuthorizationResultFromIntent(activityResult.data)
                val token = authResult.accessToken
                Log.d(TAG, "Token de Google Drive recibido exitosamente tras autorización del usuario")
                deferred.complete(token)
            } catch (e: Exception) {
                Log.e(TAG, "Error obteniendo token de la respuesta de autorización: ${e.message}", e)
                deferred.complete(null)
            }
        } else {
            Log.w(TAG, "Autorización de Google Drive cancelada o fallida (código: ${activityResult.resultCode})")
            deferred.complete(null)
        }
    }
}
