package ac.pa.ptu.museqr

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ac.pa.ptu.museqr.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var logicLogin: LogicLogin
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        logicLogin = LogicLogin(this)

        // Verificación en segundo plano al iniciar la app
        verificarSesionActiva()

        binding.btnLogin.setOnClickListener {
            intentarLogin()
        }

        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }

    private fun verificarSesionActiva() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            lifecycleScope.launch {
                setLoading(true)
                // Usamos la lógica centralizada para verificar si el rol cambió en consola
                logicLogin.verificarRolYRedirigir(currentUser.uid)
            }
        }
    }

    private fun intentarLogin() {
        val email = binding.etEmailLogin.text.toString().trim()
        val pass = binding.etPasswordLogin.text.toString().trim()

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Por favor complete los campos", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            setLoading(true)
            val exito = logicLogin.iniciarSesion(email, pass)
            if (!exito) {
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.etEmailLogin.isEnabled = !isLoading
        binding.etPasswordLogin.isEnabled = !isLoading
    }
}
