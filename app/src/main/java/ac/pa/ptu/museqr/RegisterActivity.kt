package ac.pa.ptu.museqr

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ac.pa.ptu.museqr.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var logicLogin: LogicLogin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        logicLogin = LogicLogin(this)

        binding.btnRegister.setOnClickListener {
            intentarRegistro()
        }

        binding.tvGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun intentarRegistro() {
        val email = binding.etEmailRegister.text.toString().trim()
        val pass = binding.etPasswordRegister.text.toString().trim()

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Por favor complete los campos", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val exito = logicLogin.registrarUsuario(email, pass)
            if (exito) {
                // Al registrarse, LogicLogin ya redirige o podemos hacerlo aquí
                // En este caso LogicLogin.registrarUsuario no redirige, así que lo hacemos aquí:
                // Pero según los requisitos, debe redirigir a la vista de visitante.
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    logicLogin.verificarRolYRedirigir(uid)
                }
            }
        }
    }
}
