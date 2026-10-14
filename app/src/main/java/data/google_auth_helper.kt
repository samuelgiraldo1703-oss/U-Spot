package data

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.u_spot.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Resultado de "Continuar con Google". */
sealed interface GoogleSignInResult {
    data class Success(val name: String) : GoogleSignInResult
    /** El usuario cerró el selector de cuentas: no se muestra ningún error. */
    object Cancelled : GoogleSignInResult
    object NoNetwork : GoogleSignInResult
    data class Error(val message: String) : GoogleSignInResult
}

/**
 * Inicio de sesión con Google: Credential Manager entrega el ID token de la cuenta elegida y con él
 * se inicia sesión en Firebase Auth. Si es la primera vez, se crea el documento users/{uid}.
 *
 * [context] debe ser la Activity (Credential Manager muestra el selector de cuentas sobre ella).
 */
class GoogleAuthHelper(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun signInWithGoogle(): GoogleSignInResult {
        val idToken = try {
            // Flujo del botón "Iniciar sesión con Google": muestra todas las cuentas del dispositivo
            // y permite agregar una si no hay ninguna.
            val option = GetSignInWithGoogleOption.Builder(
                context.getString(R.string.default_web_client_id)
            ).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()
            val credential = credentialManager.getCredential(context, request).credential
            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                Log.e(TAG, "Tipo de credencial inesperado: ${credential.type}")
                return GoogleSignInResult.Error("No se pudo iniciar sesión con Google")
            }
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (_: GetCredentialCancellationException) {
            return GoogleSignInResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.e(TAG, "Sin cuentas de Google disponibles", e)
            return GoogleSignInResult.Error("Agrega una cuenta de Google al dispositivo e inténtalo de nuevo")
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Error de Credential Manager (${e.type})", e)
            return GoogleSignInResult.Error("No se pudo iniciar sesión con Google")
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(TAG, "ID token de Google inválido", e)
            return GoogleSignInResult.Error("No se pudo iniciar sesión con Google")
        }

        val user = try {
            auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await().user
        } catch (_: FirebaseNetworkException) {
            return GoogleSignInResult.NoNetwork
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando sesión en Firebase con Google", e)
            return GoogleSignInResult.Error("No se pudo iniciar sesión con Google")
        } ?: return GoogleSignInResult.Error("No se pudo iniciar sesión con Google")

        // Un fallo aquí no anula la sesión: el perfil se arma igual con los datos de Firebase Auth.
        try {
            val doc = firestore.collection("users").document(user.uid)
            if (!doc.get().await().exists()) {
                val profile = hashMapOf(
                    "uid" to user.uid,
                    "username" to (user.displayName?.takeIf { it.isNotBlank() }
                        ?: user.email?.substringBefore("@") ?: "Usuario"),
                    "email" to (user.email?.lowercase() ?: ""),
                    "photoUrl" to (user.photoUrl?.toString() ?: ""),
                    "createdAt" to System.currentTimeMillis()
                )
                doc.set(profile).await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error asegurando perfil en Firestore", e)
        }

        return GoogleSignInResult.Success(user.displayName ?: user.email ?: "Usuario")
    }

    /** Cierra la sesión de Firebase y olvida la cuenta elegida para que el selector vuelva a salir. */
    suspend fun signOut() {
        auth.signOut()
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando el estado de Credential Manager", e)
        }
    }

    private companion object {
        const val TAG = "GoogleAuth"
    }
}
