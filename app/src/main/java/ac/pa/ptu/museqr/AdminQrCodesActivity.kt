package ac.pa.ptu.museqr

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ac.pa.ptu.museqr.databinding.ActivityAdminQrcodesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class AdminQrCodesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminQrcodesBinding
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().getReference("Obras")
    private val listaObras = mutableListOf<Obra>()
    private lateinit var adapter: QrObrasAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminQrcodesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = QrObrasAdapter(listaObras)
        binding.listViewQrObras.adapter = adapter

        obtenerObras()

        binding.btnDownloadAll.setOnClickListener {
            descargarTodas()
        }

        binding.ivLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
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
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun descargarTodas() {
        if (listaObras.isEmpty()) return
        var exitos = 0
        listaObras.forEach { obra ->
            val qr = QrUtils.generateQrCode(obra.id ?: "")
            if (qr != null) {
                if (saveQrToDownloads(qr, obra.id ?: "unknown")) {
                    exitos++
                }
            }
        }
        if (exitos > 0) {
            Toast.makeText(this, "Se han descargado $exitos códigos QR con éxito", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Error al descargar los códigos QR", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveQrToDownloads(bitmap: Bitmap, obraId: String): Boolean {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${timeStamp}_${obraId}.png"

        return try {
            val fos: OutputStream?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val imageUri: Uri? = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val image = java.io.File(imagesDir, fileName)
                fos = java.io.FileOutputStream(image)
            }

            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private inner class QrObrasAdapter(private val obras: List<Obra>) : BaseAdapter() {
        override fun getCount(): Int = obras.size
        override fun getItem(position: Int): Any = obras[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@AdminQrCodesActivity)
                .inflate(R.layout.item_obra_qr, parent, false)
            
            val obra = obras[position]
            view.findViewById<TextView>(R.id.tvObraTitulo).text = obra.titulo
            
            view.findViewById<ImageView>(R.id.ivDownloadQr).setOnClickListener {
                val qr = QrUtils.generateQrCode(obra.id ?: "")
                if (qr != null) {
                    if (saveQrToDownloads(qr, obra.id ?: "unknown")) {
                        Toast.makeText(this@AdminQrCodesActivity, "QR de '${obra.titulo}' descargado con éxito", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@AdminQrCodesActivity, "Error al descargar el QR", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            
            return view
        }
    }
}
