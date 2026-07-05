package ac.pa.ptu.museqr

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ac.pa.ptu.museqr.databinding.ActivityAdminBinding
import com.google.firebase.auth.FirebaseAuth

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Navegación del Dashboard
        binding.cardObras.setOnClickListener {
            startActivity(Intent(this, AdminObrasActivity::class.java))
        }

        binding.cardFormulario.setOnClickListener {
            startActivity(Intent(this, AdminFormularioActivity::class.java))
        }

        binding.cardQrCodes.setOnClickListener {
            startActivity(Intent(this, AdminQrCodesActivity::class.java))
        }

        // Configuración del botón de cerrar sesión
        binding.ivLogout.setOnClickListener {
            cerrarSesion()
        }
    }

    private fun cerrarSesion() {
        try {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
