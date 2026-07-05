package ac.pa.ptu.museqr

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ac.pa.ptu.museqr.databinding.ActivityAdminFormularioBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AdminFormularioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminFormularioBinding
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().getReference("Obras")
    private var obraIdParaEditar: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminFormularioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Verificar si venimos de una edición
        obraIdParaEditar = intent.getStringExtra("obra_id")
        if (obraIdParaEditar != null) {
            val titulo = intent.getStringExtra("obra_titulo")
            val descripcion = intent.getStringExtra("obra_descripcion")
            
            binding.etTituloObra.setText(titulo)
            binding.etDescripcionObra.setText(descripcion)
            binding.btnGuardarObra.setText(R.string.btn_actualizar_obra)
        }

        binding.btnGuardarObra.setOnClickListener {
            guardarObra()
        }

        binding.ivLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding.ivHome.setOnClickListener {
            finish()
        }
    }

    private fun guardarObra() {
        val titulo = binding.etTituloObra.text.toString().trim()
        val descripcion = binding.etDescripcionObra.text.toString().trim()

        if (titulo.isEmpty() || descripcion.isEmpty()) {
            Toast.makeText(this, "Complete los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Si obraIdParaEditar es nulo, creamos una nueva, de lo contrario actualizamos la existente
        val id = obraIdParaEditar ?: database.push().key
        
        if (id != null) {
            val obra = Obra(id = id, titulo = titulo, descripcion = descripcion, imagenUrl = null)
            
            database.child(id).setValue(obra).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val mensaje = if (obraIdParaEditar == null) "Obra guardada" else "Obra actualizada"
                    Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
                    finish() // Regresa al dashboard o actividad anterior
                } else {
                    Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
