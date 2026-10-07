package data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class GoogleAuthHelper(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)
    private val webClientId = "153271625675-dr33hfa4e1asbc6jl9kjibjd1g7r45d5.apps.googleusercontent.com"

    suspend fun signInWithGoogle(): LoginResult {
        return try {
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
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
                val user = authResult.user
                if (user != null) {
                    val db = FirebaseFirestore.getInstance()
                    val userDoc = db.collection("users").document(user.uid).get().await()
                    if (!userDoc.exists()) {
                        val newProfile = hashMapOf(
                            "uid" to user.uid,
                            "email" to (user.email ?: ""),
                            "username" to (user.displayName ?: user.email?.substringBefore("@") ?: "Usuario"),
                            "photoUrl" to (user.photoUrl?.toString() ?: ""),
                            "createdAt" to System.currentTimeMillis()
                        )
                        db.collection("users").document(user.uid).set(newProfile).await()
                    }
                    LoginResult.Success
                } else {
                    LoginResult.AccountNotFound
                }
            } else {
                LoginResult.AccountNotFound
            }
        } catch (_: GetCredentialCancellationException) {
            // Usuario canceló la selección de cuenta
            LoginResult.WrongPassword
        } catch (e: Exception) {
            e.printStackTrace()
            LoginResult.AccountNotFound
        }
    }
}
