package ac.pa.ptu.museqr

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ac.pa.ptu.museqr.databinding.ActivityObraDetailBinding
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.Chat // <--- CLASE CORRECTA IMPORTADA
import com.google.ai.client.generativeai.type.content
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

class ObraDetailActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityObraDetailBinding
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().getReference("Obras")

    private var tts: TextToSpeech? = null

    // SOLUCIÓN 1: Declaramos el tipo de chat correcto desde el inicio
    private var chatSession: Chat? = null
    private var obraActual: Obra? = null

    // RECUERDA: Coloca tu API KEY real de https://aistudio.google.com/
    private val API_KEY = ""

    private val sttLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val userQuestion = data?.get(0)
            if (!userQuestion.isNullOrBlank()) {
                preguntarAGemini(userQuestion)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityObraDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val idObra = intent.getStringExtra("ID_OBRA_QR") ?: ""
        tts = TextToSpeech(this, this)

        binding.ivLogout.setOnClickListener { cerrarSesion() }
        binding.ivHome.setOnClickListener { finish() }
        binding.btnMicrofono.setOnClickListener { iniciarSTT() }

        if (idObra.isNotEmpty()) {
            cargarDatosObra(idObra)
        } else {
            Toast.makeText(this, "ID de obra no válido", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun cargarDatosObra(id: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val snapshot = database.child(id).get().await()
                if (snapshot.exists()) {
                    obraActual = snapshot.getValue(Obra::class.java)
                    withContext(Dispatchers.Main) {
                        obraActual?.let {
                            binding.tvObraTituloDetail.text = it.titulo
                            binding.tvObraInfoDetail.text = it.descripcion
                            configurarGemini(it)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ObraDetailActivity, "Obra no encontrada", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ObraDetailActivity, "Error de Firebase: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun configurarGemini(obra: Obra) {
        if (API_KEY.isBlank() || API_KEY == "TU_API_KEY_AQUI") {
            Log.e("GeminiError", "Falta poner tu API KEY real de Google AI Studio en la línea 38")
            return
        }

        try {
            val generativeModel = GenerativeModel(
                modelName = "gemini-3.5-flash",
                apiKey = API_KEY,
                systemInstruction = content {
                    text("Actúa como un guía experto de museo. Obra: '${obra.titulo}'. " +
                            "Contexto: ${obra.descripcion}. " +
                            "Responde brevemente (3 frases máximo) y en español.")
                }
            )

            // Al llamar a startChat() se genera un objeto de tipo Chat perfectamente compatible
            chatSession = generativeModel.startChat()

            lifecycleScope.launch {
                delay(1500)
                hablar("Hola. Soy tu guía virtual. ¿Tienes alguna pregunta sobre '${obra.titulo}'?")
            }
        } catch (e: Exception) {
            Log.e("GeminiError", "Error al configurar: ${e.message}")
        }
    }

    private fun preguntarAGemini(pregunta: String) {
        // SOLUCIÓN 2: Eliminamos la referencia a ChatSession y validamos la sesión nativa
        val chat = chatSession ?: return

        lifecycleScope.launch {
            try {
                binding.tvGeminiResponse.setText(R.string.thinking)

                // sendMessage ahora compilará perfectamente porque pertenece a la clase Chat
                val response = chat.sendMessage(pregunta)
                val respuestaTexto = response.text ?: "No tengo respuesta para eso."

                binding.tvGeminiResponse.text = respuestaTexto
                hablar(respuestaTexto)
            } catch (e: Exception) {
                Log.e("GeminiError", "Fallo de conexión: ${e.message}")
                binding.tvGeminiResponse.setText(R.string.error_guide)
                hablar("Lo siento, hubo un error de conexión: ${e.localizedMessage}")
            }
        }
    }

    private fun iniciarSTT() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-PA")
        }
        try {
            sttLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "El micrófono no está disponible", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hablar(texto: String) {
        tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "MSG_ID")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.setLanguage(Locale.forLanguageTag("es-PA"))
        }
    }

    private fun cerrarSesion() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}