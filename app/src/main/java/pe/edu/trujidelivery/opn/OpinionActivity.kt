package pe.edu.trujidelivery.pagos

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONException
import pe.edu.trujidelivery.R
import java.text.SimpleDateFormat
import java.util.*

class OpinionActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var pedidoId: String? = null
    private var negocioNombre: String? = null
    private var fecha: Long? = null
    private var total: Double = 0.0
    private var productos: List<Map<String, Any>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_opinion)
        supportActionBar?.hide()

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()


        pedidoId = intent.getStringExtra("pedidoId")
        negocioNombre = intent.getStringExtra("negocioNombre")
        fecha = intent.getLongExtra("fecha", 0L)
        total = intent.getDoubleExtra("total", 0.0)
        Log.d("OpinionActivity", "Datos recibidos - pedidoId: $pedidoId, negocioNombre: $negocioNombre, fecha: $fecha, total: $total")


        val productosJsonString = intent.getStringExtra("productos")
        Log.d("OpinionActivity", "Productos JSON recibidos: $productosJsonString")
        productos = try {
            productosJsonString?.let {
                if (it.isNotEmpty()) {
                    val jsonArray = JSONArray(it)
                    val productosList = mutableListOf<Map<String, Any>>()
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val map = mutableMapOf<String, Any>()
                        jsonObject.keys().forEach { key ->
                            map[key] = jsonObject.get(key)
                        }
                        productosList.add(map)
                        Log.d("OpinionActivity", "Producto $i: $map")
                    }
                    productosList
                } else {
                    Log.w("OpinionActivity", "Productos JSON vacío")
                    emptyList()
                }
            } ?: emptyList()
        } catch (e: JSONException) {
            Log.e("OpinionActivity", "Error parsing productos JSON", e)
            Toast.makeText(this, "Error al procesar productos: ${e.message}", Toast.LENGTH_SHORT).show()
            emptyList()
        } catch (e: Exception) {
            Log.e("OpinionActivity", "Error inesperado al parsear productos", e)
            Toast.makeText(this, "Error inesperado: ${e.message}", Toast.LENGTH_SHORT).show()
            emptyList()
        }

        setupUI()
    }

    private fun setupUI() {
        Log.d("OpinionActivity", "Configurando UI - negocioNombre: $negocioNombre, fecha: $fecha, total: $total, productos size: ${productos?.size}")
        findViewById<TextView>(R.id.tvNegocioOpinion).text = negocioNombre ?: "Sin nombre"
        findViewById<TextView>(R.id.tvFechaOpinion).text = fecha?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it)) } ?: "Sin fecha"

        val containerProductos = findViewById<LinearLayout>(R.id.containerProductosOpinion)
        if (containerProductos == null) {
            Log.e("OpinionActivity", "containerProductosOpinion no encontrado en el layout")
            Toast.makeText(this, "Error en el layout", Toast.LENGTH_SHORT).show()
            return
        }
        productos?.forEachIndexed { index, producto ->
            try {
                val view = layoutInflater.inflate(R.layout.item_opinion_producto, containerProductos, false)
                view.findViewById<TextView>(R.id.tvProductoNombre).text = producto["nombre"] as? String ?: "Sin nombre"
                // Mostrar el total del pedido en tvTotalOpinion
                view.findViewById<TextView>(R.id.tvTotalOpinion).text = "Total: S/ ${String.format("%.2f", total)}"
                val imagenUrl = producto["imagenUrl"] as? String ?: ""
                Log.d("OpinionActivity", "Cargando imagen para producto ${producto["nombre"]}: $imagenUrl")
                Glide.with(this)
                    .load(imagenUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(view.findViewById<ImageView>(R.id.ivProductoImagen))
                containerProductos.addView(view)

                view.findViewById<RatingBar>(R.id.ratingProducto0).id = resources.getIdentifier("ratingProducto$index", "id", packageName)
                view.findViewById<EditText>(R.id.etComentario0).id = resources.getIdentifier("etComentario$index", "id", packageName)
            } catch (e: Exception) {
                Log.e("OpinionActivity", "Error al inflar item_opinion_producto para índice $index", e)
                Toast.makeText(this, "Error al cargar producto: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnEnviarOpinion)?.setOnClickListener {
            guardarOpinion()
        } ?: Log.e("OpinionActivity", "btnEnviarOpinion no encontrado en el layout")
    }

    private fun guardarOpinion() {
        val usuarioId = auth.currentUser?.uid ?: return
        val opiniones = mutableMapOf<String, Any>()
        productos?.forEachIndexed { index, producto ->
            val ratingBar = findViewById<RatingBar>(resources.getIdentifier("ratingProducto$index", "id", packageName))
            val comentario = findViewById<EditText>(resources.getIdentifier("etComentario$index", "id", packageName))?.text?.toString() ?: ""
            if (ratingBar != null) {
                opiniones["${producto["nombre"]}_rating"] = ratingBar.rating
                if (comentario.isNotEmpty()) opiniones["${producto["nombre"]}_comentario"] = comentario
            } else {
                Log.w("OpinionActivity", "RatingBar no encontrado para índice $index")
            }
        }
        val envioRating = findViewById<RatingBar>(R.id.ratingEnvio)?.rating ?: 0f
        val envioComentario = findViewById<EditText>(R.id.etComentarioEnvio)?.text?.toString() ?: ""

        if (pedidoId != null) {
            try {
                // Guardar en la nueva colección comentarios_pedidos
                db.collection("comentarios_pedidos")
                    .document(pedidoId!!)
                    .set(mapOf(
                        "usuarioId" to usuarioId,
                        "pedidoId" to pedidoId!!,
                        "negocioNombre" to (negocioNombre ?: ""),
                        "fecha" to Date().time,
                        "opiniones" to opiniones,
                        "envioRating" to envioRating,
                        "envioComentario" to envioComentario,
                        "total" to total
                    ))
                    .addOnSuccessListener {
                        Log.d("OpinionActivity", "Opinión guardada en comentarios_pedidos con total: $total")
                        Toast.makeText(this, "Opinión enviada", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Log.e("OpinionActivity", "Error al guardar opinión", e)
                        Toast.makeText(this, "Error al guardar opinión: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Log.e("OpinionActivity", "Error inesperado al guardar opinión", e)
                Toast.makeText(this, "Error inesperado: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}