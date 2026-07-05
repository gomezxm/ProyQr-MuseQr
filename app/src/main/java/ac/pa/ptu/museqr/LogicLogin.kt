package ac.pa.ptu.museqr

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LogicLogin(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    /**
     * Registra un usuario y lo guarda en Realtime Database con rol VISITANTE.
     */
    suspend fun registrarUsuario(email: String, pass: String): Boolean {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            val uid = result.user?.uid
            
            if (uid != null) {
                val userMap = mapOf(
                    "correo" to email,
                    "rol" to "VISITANTE"
                )
                // Guardar en Realtime Database
                database.child("usuarios").child(uid).setValue(userMap).await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            mostrarMensaje("Error en registro: ${e.message}")
            false
        }
    }

    /**
     * Inicia sesión y verifica el rol del usuario para redirigir.
     */
    suspend fun iniciarSesion(email: String, pass: String): Boolean {
        return try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            val uid = result.user?.uid
            if (uid != null) {
                verificarRolYRedirigir(uid)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            mostrarMensaje("Error en login: ${e.message}")
            false
        }
    }

    /**
     * Verifica el rol en Realtime Database y redirige a la vista correspondiente.
     */
    suspend fun verificarRolYRedirigir(uid: String) {
        try {
            // Obtenemos el dato directamente del servidor (.get()) para ver cambios manuales en la consola
            val snapshot = database.child("usuarios").child(uid).get().await()
            
            if (snapshot.exists()) {
                val rol = snapshot.child("rol").value?.toString()
                
                withContext(Dispatchers.Main) {
                    when (rol) {
                        "ADMIN" -> {
                            val intent = Intent(context, AdminActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(intent)
                        }
                        "VISITANTE" -> {
                            val intent = Intent(context, EscanerMuseumActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(intent)
                        }
                        else -> {
                            cerrarSesionConError("Acceso denegado: Rol no reconocido.")
                        }
                    }
                }
            } else {
                cerrarSesionConError("No se encontró perfil de usuario.")
            }
        } catch (e: Exception) {
            cerrarSesionConError("Error de permisos: ${e.message}")
        }
    }

    private suspend fun cerrarSesionConError(mensaje: String) {
        withContext(Dispatchers.Main) {
            auth.signOut()
            mostrarMensaje(mensaje)
            // Redirigir al login si ocurre un error de rol
            val intent = Intent(context, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
    }

    private suspend fun mostrarMensaje(mensaje: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
        }
    }
}
