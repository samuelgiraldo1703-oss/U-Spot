package data

/**
 * Contrato de autenticación del módulo de login.
 *
 * Hoy la implementación es [FakeAuthRepository] (en memoria, solo para revisar la maquetación).
 * Cuando se conecte Firebase, basta con crear `FirebaseAuthRepository : AuthRepository`
 * (FirebaseAuth + Firestore para el nombre de usuario) y cambiar [AuthProvider.repository];
 * las pantallas no necesitan cambios.
 */
interface AuthRepository {
    fun login(identifier: String, password: String): LoginResult
    fun register(username: String, email: String, password: String): RegisterResult
    fun accountExists(identifier: String): Boolean
    fun changePassword(identifier: String, newPassword: String): Boolean
}

enum class LoginResult { Success, AccountNotFound, WrongPassword }

enum class RegisterResult { Success, AlreadyExists }

private data class User(val username: String, val email: String?, var password: String)

/**
 * Usuarios en memoria. Se reinician cada vez que se cierra la app.
 * Usuario de prueba: Admin / 1234 (se puede escribir "Admin" en los campos de correo).
 */
object FakeAuthRepository : AuthRepository {

    private val users = mutableListOf(User(username = "Admin", email = null, password = "1234"))

    private fun find(identifier: String): User? {
        val id = identifier.trim()
        return users.firstOrNull { it.username == id || (it.email != null && it.email.equals(id, ignoreCase = true)) }
    }

    override fun login(identifier: String, password: String): LoginResult {
        val user = find(identifier) ?: return LoginResult.AccountNotFound
        return if (user.password == password) LoginResult.Success else LoginResult.WrongPassword
    }

    override fun register(username: String, email: String, password: String): RegisterResult {
        if (find(username) != null || find(email) != null) return RegisterResult.AlreadyExists
        users += User(username.trim(), email.trim(), password)
        return RegisterResult.Success
    }

    override fun accountExists(identifier: String): Boolean = find(identifier) != null

    override fun changePassword(identifier: String, newPassword: String): Boolean {
        val user = find(identifier) ?: return false
        user.password = newPassword
        return true
    }
}

object AuthProvider {
    val repository: AuthRepository = FakeAuthRepository
}
