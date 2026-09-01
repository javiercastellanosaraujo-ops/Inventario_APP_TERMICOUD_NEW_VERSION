package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object GoogleDriveUploader {
    private const val TAG = "GoogleDriveUploader"
    private const val FOLDER_NAME = "Termicoud - Cierres Mensuales"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Sube un archivo CSV a Google Drive directamente en la carpeta "Termicoud - Cierres Mensuales".
     * Si la carpeta no existe en el Drive del usuario, la crea de forma automática en la raíz.
     * Retorna Result.success con el ID del archivo subido, o Result.failure con la causa de error.
     */
    suspend fun subirCsvADrive(
        accessToken: String,
        contenidoCsv: String,
        nombreArchivo: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Access token de Google Drive no disponible."))
        }

        try {
            // 1. Buscar si ya existe la carpeta "Termicoud - Cierres Mensuales"
            val folderId = getOrCreateFolder(accessToken, FOLDER_NAME)
            Log.d(TAG, "Carpeta de destino en Drive obtenida: ID=$folderId")

            // 2. Subir el archivo CSV dentro de esa carpeta usando multipart upload a Google Drive v3
            val uploadUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"

            val metadataJson = JSONObject().apply {
                put("name", nombreArchivo)
                put("mimeType", "text/csv")
                put("parents", JSONArray().apply { put(folderId) })
            }

            val multipartBody = MultipartBody.Builder()
                .setType("multipart/related".toMediaTypeOrNull() ?: MultipartBody.FORM)
                .addPart(
                    metadataJson.toString().toRequestBody("application/json; charset=UTF-8".toMediaTypeOrNull())
                )
                .addPart(
                    contenidoCsv.toRequestBody("text/csv; charset=UTF-8".toMediaTypeOrNull())
                )
                .build()

            val uploadRequest = Request.Builder()
                .url(uploadUrl)
                .header("Authorization", "Bearer $accessToken")
                .header("Accept", "application/json")
                .post(multipartBody)
                .build()

            val response = httpClient.newCall(uploadRequest).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Error en upload multipart a Drive (HTTP ${response.code}): $responseBody")
                return@withContext Result.failure(
                    Exception("Error de Drive (HTTP ${response.code}): $responseBody")
                )
            }

            val jsonResponse = JSONObject(responseBody)
            val fileId = jsonResponse.optString("id", "")
            Log.i(TAG, "Archivo subido exitosamente a Google Drive: $nombreArchivo (ID=$fileId)")

            Result.success(fileId)
        } catch (e: Exception) {
            Log.e(TAG, "Excepción subiendo archivo a Google Drive: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Busca o crea la carpeta de respaldo en la raíz de Google Drive.
     */
    private fun getOrCreateFolder(accessToken: String, folderName: String): String {
        // Query: name='Termicoud - Cierres Mensuales' and mimeType='application/vnd.google-apps.folder' and trashed=false
        val query = "name='$folderName' and mimeType='application/vnd.google-apps.folder' and trashed=false"
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val searchUrl = "https://www.googleapis.com/drive/v3/files?q=$encodedQuery&spaces=drive&fields=files(id,name)"

        val searchRequest = Request.Builder()
            .url(searchUrl)
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json")
            .get()
            .build()

        val searchResponse = httpClient.newCall(searchRequest).execute()
        val searchBody = searchResponse.body?.string() ?: ""

        if (searchResponse.isSuccessful) {
            val json = JSONObject(searchBody)
            val filesArray = json.optJSONArray("files")
            if (filesArray != null && filesArray.length() > 0) {
                val firstFolder = filesArray.getJSONObject(0)
                val id = firstFolder.optString("id", "")
                if (id.isNotBlank()) {
                    return id
                }
            }
        } else {
            Log.w(TAG, "Aviso buscando carpeta en Drive (HTTP ${searchResponse.code}): $searchBody. Procediendo a crearla.")
        }

        // Si no existe, crear la carpeta
        val createUrl = "https://www.googleapis.com/drive/v3/files"
        val folderMeta = JSONObject().apply {
            put("name", folderName)
            put("mimeType", "application/vnd.google-apps.folder")
        }

        val createRequest = Request.Builder()
            .url(createUrl)
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json")
            .post(folderMeta.toString().toRequestBody("application/json; charset=UTF-8".toMediaTypeOrNull()))
            .build()

        val createResponse = httpClient.newCall(createRequest).execute()
        val createBody = createResponse.body?.string() ?: ""

        if (!createResponse.isSuccessful) {
            throw Exception("No se pudo crear la carpeta en Google Drive (HTTP ${createResponse.code}): $createBody")
        }

        val createdJson = JSONObject(createBody)
        val createdId = createdJson.optString("id", "")
        if (createdId.isBlank()) {
            throw Exception("Respuesta inválida creando carpeta en Google Drive: $createBody")
        }

        return createdId
    }
}
