package com.example.ui.screens

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.example.config.AppConfig
import com.example.data.model.UserSession
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.GraphiteBackground
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.GraphiteSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun LoginScreen(
    currentUser: UserSession?,
    onLoginSuccess: (UserSession) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showTermiCoudDialog by remember { mutableStateOf(false) }

    if (showTermiCoudDialog) {
        com.example.ui.components.TermiCoudDialog(
            onDismissRequest = { showTermiCoudDialog = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GraphiteBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GraphiteBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = GraphiteSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // App Logo / Badge
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(ElectricLime.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.termicoud_logo_official_1787959745763),
                        contentDescription = "Logo Termicoud",
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(2.dp, ElectricLime, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = AppConfig.APP_FULL_TITLE,
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricLime,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = AppConfig.APP_SUBTITLE,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Text(
                    text = "Inicia sesión para registrar entradas, salidas y auditoría en tiempo real sincronizado con Firebase Firestore.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                )

                if (currentUser != null && currentUser.email.isNotBlank()) {
                    // Logged in card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ElectricLime.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        color = GraphiteSurfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = ElectricLime,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Sesión Activa",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ElectricLime,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = currentUser.displayName.ifBlank { "Operador" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Text(
                                text = currentUser.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { onLoginSuccess(currentUser) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_continue_to_inventory"),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricLime),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Entrar al Inventario", color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = {
                                    auth.signOut()
                                    onSignOut()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_change_account"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cambiar de Cuenta", color = TextSecondary)
                            }
                        }
                    }
                } else {
                    var manualEmail by remember { mutableStateOf("") }
                    var manualName by remember { mutableStateOf("") }

                    // Direct Operator / Email Login Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, GraphiteBorder, RoundedCornerShape(10.dp)),
                        color = GraphiteSurfaceVariant,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "ACCESO DE OPERADOR / CORREO",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricLime,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            OutlinedTextField(
                                value = manualEmail,
                                onValueChange = { manualEmail = it },
                                label = { Text("Correo electrónico") },
                                placeholder = { Text("ejemplo@correo.com", color = TextMuted) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("input_manual_email"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricLime,
                                    unfocusedBorderColor = GraphiteBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            OutlinedTextField(
                                value = manualName,
                                onValueChange = { manualName = it },
                                label = { Text("Nombre del Operador") },
                                placeholder = { Text("Ej. Javier / Operador 1", color = TextMuted) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Done
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("input_manual_name"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricLime,
                                    unfocusedBorderColor = GraphiteBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            Button(
                                onClick = {
                                    if (manualEmail.isNotBlank()) {
                                        val session = UserSession(
                                            uid = "user_" + manualEmail.trim().lowercase().replace(".", "_").replace("@", "_"),
                                            email = manualEmail.trim().lowercase(),
                                            displayName = manualName.ifBlank { "Operador" },
                                            photoUrl = null
                                        )
                                        onLoginSuccess(session)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("btn_manual_login_submit"),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricLime),
                                shape = RoundedCornerShape(8.dp),
                                enabled = manualEmail.isNotBlank()
                            ) {
                                Text("Ingresar al Sistema", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(modifier = Modifier.weight(1f).height(1.dp).background(GraphiteBorder))
                        Text(
                            text = "  o con Google  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Box(modifier = Modifier.weight(1f).height(1.dp).background(GraphiteBorder))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Google Sign In Button via Credential Manager
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                try {
                                    val gmsStatus = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
                                    if (gmsStatus != ConnectionResult.SUCCESS) {
                                        errorMessage = "Google Play Services no disponible en este entorno. Ingresa tu correo o nombre arriba."
                                        isLoading = false
                                        return@launch
                                    }

                                    val credentialManager = CredentialManager.create(context)
                                    val webClientId = context.getString(com.example.R.string.default_web_client_id)

                                    val googleIdOption = GetGoogleIdOption.Builder()
                                        .setFilterByAuthorizedAccounts(false)
                                        .setServerClientId(webClientId)
                                        .setAutoSelectEnabled(false)
                                        .build()

                                    val request = GetCredentialRequest.Builder()
                                        .addCredentialOption(googleIdOption)
                                        .build()

                                    val result = credentialManager.getCredential(
                                        request = request,
                                        context = context
                                    )

                                    val credential = result.credential
                                    if (credential is CustomCredential &&
                                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                    ) {
                                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                        val idToken = googleIdTokenCredential.idToken
                                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                                        val authResult = auth.signInWithCredential(firebaseCredential).await()
                                        val firebaseUser = authResult.user

                                        if (firebaseUser != null) {
                                            val session = UserSession(
                                                uid = firebaseUser.uid,
                                                email = firebaseUser.email ?: googleIdTokenCredential.id,
                                                displayName = firebaseUser.displayName ?: googleIdTokenCredential.displayName ?: "Operador",
                                                photoUrl = firebaseUser.photoUrl?.toString()
                                            )
                                            onLoginSuccess(session)
                                        }
                                    }
                                } catch (e: GetCredentialCancellationException) {
                                    Log.d("LoginScreen", "Usuario canceló el selector de cuenta")
                                } catch (e: SecurityException) {
                                    Log.w("LoginScreen", "Google Play Services SecurityException: ${e.message}")
                                    errorMessage = "Google Play Services no disponible en este dispositivo. Ingresa tus datos arriba."
                                } catch (t: Throwable) {
                                    Log.w("LoginScreen", "Error Google Sign-In: ${t.message}")
                                    errorMessage = "Inicio con Google no disponible en este emulador. Ingresa tus datos arriba."
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("btn_google_signin"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4285F4),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Continuar con Google",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // TermiCoud Terms and Conditions Link
                TextButton(
                    onClick = { showTermiCoudDialog = true },
                    modifier = Modifier.testTag("btn_open_termicoud_login")
                ) {
                    Text(
                        text = "Términos y Condiciones (TermiCoud)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
