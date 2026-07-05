package ac.pa.ptu.museqr

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ac.pa.ptu.museqr.databinding.ActivityAdminObrasBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AdminObrasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminObrasBinding
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().getReference("Obras")
    private val listaObras = mutableListOf<Obra>()
    private val listaFiltrada = mutableListOf<Obra>()
    private lateinit var adapter: ObrasAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminObrasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ObrasAdapter(listaFiltrada)
        binding.listViewObras.adapter = adapter

        obtenerObras()

        binding.etSearchObra.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarObras(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

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

    private fun obtenerObras() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaObras.clear()
                for (postSnapshot in snapshot.children) {
                    val obra = postSnapshot.getValue(Obra::class.java)
                    if (obra != null) listaObras.add(obra)
                }
                filtrarObras(binding.etSearchObra.text.toString())
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AdminObrasActivity, "Error al cargar datos", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filtrarObras(texto: String) {
        listaFiltrada.clear()
        if (texto.isEmpty()) {
            listaFiltrada.addAll(listaObras)
        } else {
            for (obra in listaObras) {
                if (obra.titulo?.lowercase()?.contains(texto.lowercase()) == true) {
                    listaFiltrada.add(obra)
                }
            }
        }
        adapter.notifyDataSetChanged()
    }

    private inner class ObrasAdapter(private val obras: List<Obra>) : BaseAdapter() {
        override fun getCount(): Int = obras.size
        override fun getItem(position: Int): Any = obras[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@AdminObrasActivity)
                .inflate(R.layout.item_obra, parent, false)
            
            val obra = obras[position]
            view.findViewById<TextView>(R.id.tvObraTitulo).text = obra.titulo
            
            view.findViewById<ImageView>(R.id.ivEditObra).setOnClickListener {
                val intent = Intent(this@AdminObrasActivity, AdminFormularioActivity::class.java)
                intent.putExtra("obra_id", obra.id)
                intent.putExtra("obra_titulo", obra.titulo)
                intent.putExtra("obra_descripcion", obra.descripcion)
                startActivity(intent)
            }
            
            return view
        }
    }
}
