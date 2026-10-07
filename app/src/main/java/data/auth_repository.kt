package data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Contrato de autenticación y gestión de usuarios en Firebase.
 */
interface AuthRepository {
    suspend fun login(identifier: String, password: String): LoginResult
    suspend fun register(username: String, email: String, password: String): RegisterResult
    suspend fun accountExists(identifier: String): Boolean
    suspend fun changePassword(identifier: String, newPassword: String): ChangePasswordResult
    suspend fun sendPasswordReset(email: String): Boolean
}

enum class LoginResult { Success, AccountNotFound, WrongPassword }

enum class RegisterResult { Success, AlreadyExists }

sealed class ChangePasswordResult {
    data class Success(val message: String, val email: String = "") : ChangePasswordResult()
    object AccountNotFound : ChangePasswordResult()
    object WeakPassword : ChangePasswordResult()
    data class Error(val message: String) : ChangePasswordResult()
}

/**
 * Implementación de producción con Firebase Authentication y Cloud Firestore.
 */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthRepository {

    override suspend fun login(identifier: String, password: String): LoginResult {
        val trimmed = identifier.trim()
        val email = if (trimmed.contains("@")) {
            trimmed.lowercase()
        } else {
            try {
                val querySnapshot = firestore.collection("users")
                    .whereEqualTo("username", trimmed)
                    .get()
                    .await()
                if (querySnapshot.isEmpty) {
                    return LoginResult.AccountNotFound
                }
                querySnapshot.documents.firstOrNull()?.getString("email")?.lowercase()
                    ?: return LoginResult.AccountNotFound
            } catch (e: Exception) {
                Log.e("AuthRepo", "Error buscando username en Firestore", e)
                return LoginResult.AccountNotFound
            }
        }

        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                try {
                    val doc = firestore.collection("users").document(user.uid).get().await()
                    if (!doc.exists()) {
                        val profile = hashMapOf(
                            "uid" to user.uid,
                            "username" to (user.displayName ?: email.substringBefore("@")),
                            "email" to (user.email ?: email),
                            "createdAt" to System.currentTimeMillis()
                        )
                        firestore.collection("users").document(user.uid).set(profile).await()
                    }
                } catch (e: Exception) {
                    Log.e("AuthRepo", "Error asegurando perfil en Firestore", e)
                }
            }
            LoginResult.Success
        } catch (_: FirebaseAuthInvalidUserException) {
            LoginResult.AccountNotFound
        } catch (_: FirebaseAuthInvalidCredentialsException) {
            LoginResult.WrongPassword
        } catch (e: Exception) {
            Log.e("AuthRepo", "Error en login", e)
            LoginResult.WrongPassword
        }
    }

    override suspend fun register(username: String, email: String, password: String): RegisterResult {
        val trimmedUsername = username.trim()
        val trimmedEmail = email.trim().lowercase()

        // 1. Verificar si el nombre de usuario ya está registrado en Firestore
        try {
            val userCheck = firestore.collection("users")
                .whereEqualTo("username", trimmedUsername)
                .get()
                .await()
            if (!userCheck.isEmpty) {
                return RegisterResult.AlreadyExists
            }
        } catch (e: Exception) {
            Log.e("AuthRepo", "Error comprobando username", e)
        }

        // 2. Crear usuario en Firebase Authentication
        return try {
            val authResult = auth.createUserWithEmailAndPassword(trimmedEmail, password).await()
            val uid = authResult.user?.uid ?: return RegisterResult.AlreadyExists

            // 3. Guardar documento del usuario en Cloud Firestore (colección "users")
            val profile = hashMapOf(
                "uid" to uid,
                "username" to trimmedUsername,
                "email" to trimmedEmail,
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(uid).set(profile).await()

            RegisterResult.Success
        } catch (_: FirebaseAuthUserCollisionException) {
            // Si la cuenta de Auth ya se había creado (por ejemplo en un intento previo)
            try {
                val loginResult = auth.signInWithEmailAndPassword(trimmedEmail, password).await()
                val uid = loginResult.user?.uid
                if (uid != null) {
                    val profile = hashMapOf(
                        "uid" to uid,
                        "username" to trimmedUsername,
                        "email" to trimmedEmail,
                        "createdAt" to System.currentTimeMillis()
                    )
                    firestore.collection("users").document(uid).set(profile).await()
                    RegisterResult.Success
                } else {
                    RegisterResult.AlreadyExists
                }
            } catch (e: Exception) {
                Log.e("AuthRepo", "Error completando cuenta existente", e)
                RegisterResult.AlreadyExists
            }
        } catch (e: Exception) {
            Log.e("AuthRepo", "Error al registrar usuario", e)
            RegisterResult.AlreadyExists
        }
    }

    override suspend fun accountExists(identifier: String): Boolean {
        val trimmed = identifier.trim()
        val email = trimmed.lowercase()
        return try {
            if (email.contains("@")) {
                val emailCheck = firestore.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .await()
                if (!emailCheck.isEmpty) return true

                // También verificar si existe en Firebase Authentication
                try {
                    val methods = auth.fetchSignInMethodsForEmail(email).await()
                    if (methods.signInMethods?.isNotEmpty() == true) return true
                } catch (e: Exception) {
                    Log.e("AuthRepo", "fetchSignInMethodsForEmail error", e)
                }
                false
            } else {
                val userCheck = firestore.collection("users")
                    .whereEqualTo("username", trimmed)
                    .get()
                    .await()
                !userCheck.isEmpty
            }
        } catch (e: Exception) {
            Log.e("AuthRepo", "accountExists error", e)
            false
        }
    }

    override suspend fun changePassword(identifier: String, newPassword: String): ChangePasswordResult {
        val trimmed = identifier.trim()
        val email = trimmed.lowercase()

        if (newPassword.length < 6) {
            return ChangePasswordResult.WeakPassword
        }

        // 1. Obtener el correo objetivo (por correo directo o por username en Firestore)
        var targetEmail = email
        if (!email.contains("@")) {
            try {
                val query = firestore.collection("users")
                    .whereEqualTo("username", trimmed)
                    .get()
                    .await()
                if (query.isEmpty) {
                    return ChangePasswordResult.AccountNotFound
                }
                targetEmail = query.documents.firstOrNull()?.getString("email")?.lowercase()
                    ?: return ChangePasswordResult.AccountNotFound
            } catch (e: Exception) {
                Log.e("AuthRepo", "Error consultando username para cambio de password", e)
                return ChangePasswordResult.AccountNotFound
            }
        } else {
            // Si viene con correo, verificar en Firestore para sincronización
            try {
                val query = firestore.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .await()
                if (!query.isEmpty) {
                    targetEmail = query.documents.firstOrNull()?.getString("email")?.lowercase() ?: email
                }
            } catch (e: Exception) {
                Log.e("AuthRepo", "Error comprobando email en Firestore", e)
            }
        }

        // 2. Guardar la solicitud de contraseña pendiente en Firestore
        // De este modo, al abrir el enlace del correo se puede autorizar y aplicar directamente
        try {
            val pendingData = hashMapOf(
                "email" to targetEmail,
                "pendingPassword" to newPassword,
                "requestedAt" to System.currentTimeMillis()
            )
            firestore.collection("password_resets").document(targetEmail).set(pendingData).await()
        } catch (e: Exception) {
            Log.e("AuthRepo", "Error guardando pending password reset", e)
        }

        // 3. Enviar el correo oficial de verificación / confirmación de Firebase Auth
        // De esta forma, el usuario recibe el correo con el enlace de aprobación en su Gmail
        // y la contraseña solo se actualiza en Firebase cuando el usuario aprueba el cambio.
        return try {
            auth.sendPasswordResetEmail(targetEmail).await()
            ChangePasswordResult.Success(
                message = "Correo de verificación enviado a $targetEmail",
                email = targetEmail
            )
        } catch (e: FirebaseAuthInvalidUserException) {
            ChangePasswordResult.AccountNotFound
        } catch (e: Exception) {
            Log.e("AuthRepo", "Error al enviar reset email", e)
            val msg = e.localizedMessage ?: "Error al procesar la solicitud"
            if (msg.contains("no user record", ignoreCase = true) || msg.contains("user-not-found", ignoreCase = true)) {
                ChangePasswordResult.AccountNotFound
            } else {
                ChangePasswordResult.Error(msg)
            }
        }
    }

    override suspend fun sendPasswordReset(email: String): Boolean {
        return try {
            auth.sendPasswordResetEmail(email.trim().lowercase()).await()
            true
        } catch (e: Exception) {
            Log.e("AuthRepo", "sendPasswordReset error", e)
            false
        }
    }
}

object AuthProvider {
    val repository: AuthRepository = FirebaseAuthRepository()
}
