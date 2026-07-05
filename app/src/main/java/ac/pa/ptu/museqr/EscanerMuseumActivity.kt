package ac.pa.ptu.museqr

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ac.pa.ptu.museqr.databinding.ActivityEscanerMuseumBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

class EscanerMuseumActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEscanerMuseumBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEscanerMuseumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivLogout.setOnClickListener {
            cerrarSesion()
        }

        binding.btnAbrirEscanner.setOnClickListener {
            iniciarEscaneo()
        }
    }

    private fun iniciarEscaneo() {
        val scanner = GmsBarcodeScanning.getClient(this)
        
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val rawValue: String? = barcode.rawValue
                if (rawValue != null) {
                    val intent = Intent(this, ObraDetailActivity::class.java)
                    intent.putExtra("ID_OBRA_QR", rawValue)
                    startActivity(intent)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al escanear: ${e.message}", Toast.LENGTH_SHORT).show()
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
