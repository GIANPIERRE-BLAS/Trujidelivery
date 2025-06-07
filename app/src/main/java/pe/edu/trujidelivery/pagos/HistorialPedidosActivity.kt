package pe.edu.trujidelivery.pagos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import pe.edu.trujidelivery.databinding.ActivityHistorialPedidosBinding
import pe.edu.trujidelivery.adapters.HistorialPedidosAdapter
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import pe.edu.trujidelivery.pagos.ConfirmarPedidoActivity

class HistorialPedidosActivity : AppCompatActivity() {

    data class Pedido(
        val id: String,
        val negocioNombre: String,
        val imagenNegocio: String?,
        val productos: List<Map<String, Any>>,
        val subtotal: Double,
        val costoEnvio: Double,
        val costoServicio: Double,
        val total: Double,
        val direccion: String,
        val metodoPago: String,
        val datosPago: String?,
        val fecha: Long,
        val estado: String
    )

    private lateinit var binding: ActivityHistorialPedidosBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val pedidos = mutableListOf<Pedido>()
    private lateinit var adapter: HistorialPedidosAdapter
    private var filteredPedidos = mutableListOf<Pedido>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistorialPedidosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        setupRecyclerView()
        setupFilters()
        cargarPedidos()
    }

    private fun setupRecyclerView() {
        Log.d("HistorialPedidos", "Setting up RecyclerView")
        adapter = HistorialPedidosAdapter(
            filteredPedidos,
            onOpinarClick = { pedido ->
                try {
                    Log.d("HistorialPedidos", "Pedido seleccionado: id=${pedido.id}, negocio=${pedido.negocioNombre}, productos size=${pedido.productos.size}, total=${pedido.total}")
                    val productosJson = JSONArray().apply {
                        if (pedido.productos.isNotEmpty()) {
                            pedido.productos.forEach { producto ->
                                val productoJson = JSONObject(producto)
                                if (!productoJson.has("imagenUrl")) {
                                    productoJson.put("imagenUrl", producto["imagen"] as? String ?: "")
                                }
                                Log.d("HistorialPedidos", "Producto: ${producto["nombre"]}, imagenUrl: ${productoJson.getString("imagenUrl")}")
                                put(productoJson)
                            }
                        } else {
                            Log.w("HistorialPedidos", "Productos vacíos para el pedido ${pedido.id}")
                        }
                    }
                    Log.d("HistorialPedidos", "Productos JSON: $productosJson")
                    val intent = Intent(this, OpinionActivity::class.java).apply {
                        putExtra("pedidoId", pedido.id)
                        putExtra("negocioNombre", pedido.negocioNombre)
                        putExtra("imagenNegocio", pedido.imagenNegocio ?: "")
                        putExtra("fecha", pedido.fecha)
                        putExtra("total", pedido.total)
                        putExtra("productos", if (pedido.productos.isNotEmpty()) productosJson.toString() else "")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("HistorialPedidos", "Error al iniciar OpinionActivity", e)
                    Toast.makeText(this, "Error al abrir Opinión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            onEliminarClick = { pedido ->
                eliminarPedido(pedido.id)
            },
            onRepetirClick = { pedido ->
                repetirPedido(pedido)
            }
        )
        binding.recyclerViewPedidos.apply {
            layoutManager = LinearLayoutManager(this@HistorialPedidosActivity)
            adapter = this@HistorialPedidosActivity.adapter
        }
    }

    private fun setupFilters() {
        binding.btnFilterEntregados.setOnClickListener { filterPedidos("Entregado") }
        binding.btnFilterCancelados.setOnClickListener { filterPedidos("Cancelado") }
        binding.btnFilterPeriodo.setOnClickListener {
            filterPedidosByPeriod(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000)
        }
    }

    private fun cargarPedidos() {
        val usuarioId = auth.currentUser?.uid
        if (usuarioId == null) {
            Log.w("HistorialPedidos", "User not logged in")
            Toast.makeText(this, "Debe iniciar sesión para ver el historial", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d("HistorialPedidos", "Loading pedidos for user: $usuarioId")
        binding.progressBar.visibility = View.VISIBLE
        binding.noResultsText.visibility = View.GONE
        db.collection("historial_pedidos")
            .whereEqualTo("usuario_id", usuarioId)
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d("HistorialPedidos", "Firestore data loaded, documents: ${snapshot.documents.size}")
                pedidos.clear()
                for (doc in snapshot.documents) {
                    val id = doc.id
                    val negocioNombre = doc.getString("negocio_nombre") ?: ""
                    val imagenNegocio = doc.getString("imagenNegocio") ?: ""
                    val productos = doc.get("productos") as? List<Map<String, Any>> ?: emptyList()
                    val productosList = productos.map { producto ->
                        val productoMap = producto.toMutableMap()
                        if (!productoMap.containsKey("imagenUrl")) {
                            productoMap["imagenUrl"] = producto["imagen"] as? String ?: ""
                        }
                        Log.d("HistorialPedidos", "Producto: ${productoMap["nombre"]}, imagenUrl: ${productoMap["imagenUrl"]}")
                        productoMap
                    }
                    val subtotal = doc.getDouble("subtotal") ?: 0.0
                    val costoEnvio = doc.getDouble("costoEnvio") ?: 0.0
                    val costoServicio = doc.getDouble("costoServicio") ?: 0.0
                    val total = doc.getDouble("total") ?: 0.0
                    val direccion = doc.getString("direccion_entrega") ?: ""
                    val metodoPago = doc.getString("metodoPago") ?: ""
                    val datosPago = doc.getString("datosPago")
                    val fecha = doc.getString("fecha_hora")?.let {
                        try {
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(it)?.time ?: 0L
                        } catch (e: Exception) {
                            Log.e("HistorialPedidos", "Error parsing date: ${e.message}")
                            0L
                        }
                    } ?: 0L
                    val estado = doc.getString("estado") ?: "Entregado"

                    val pedido = Pedido(
                        id, negocioNombre, imagenNegocio, productosList, subtotal, costoEnvio, costoServicio,
                        total, direccion, metodoPago, datosPago, fecha, estado
                    )
                    pedidos.add(pedido)
                }
                filteredPedidos.clear()
                filteredPedidos.addAll(pedidos)
                adapter.notifyDataSetChanged()
                binding.progressBar.visibility = View.GONE
                binding.noResultsText.visibility = if (filteredPedidos.isEmpty()) View.VISIBLE else View.GONE
                if (filteredPedidos.isEmpty()) {
                    binding.noResultsText.text = "No hay pedidos en el historial"
                }
                Log.d("HistorialPedidos", "Pedidos cargados: ${pedidos.size}")
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.noResultsText.visibility = View.VISIBLE
                binding.noResultsText.text = "Error al cargar el historial"
                Log.e("HistorialPedidos", "Error al cargar pedidos", e)
                Toast.makeText(this, "Error al cargar el historial: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterPedidos(estado: String) {
        Log.d("HistorialPedidos", "Filtrando por estado: $estado")
        filteredPedidos.clear()
        filteredPedidos.addAll(pedidos.filter { it.estado.lowercase() == estado.lowercase() })
        adapter.notifyDataSetChanged()
        binding.noResultsText.visibility = if (filteredPedidos.isEmpty()) View.VISIBLE else View.GONE
        if (filteredPedidos.isEmpty()) {
            binding.noResultsText.text = "No hay $estado en el historial"
        } else {
            Log.d("HistorialPedidos", "Pedidos filtrados: ${filteredPedidos.size}")
        }
    }

    private fun filterPedidosByPeriod(startTime: Long) {
        Log.d("HistorialPedidos", "Filtrando por período desde: $startTime")
        filteredPedidos.clear()
        filteredPedidos.addAll(pedidos.filter { it.fecha >= startTime })
        adapter.notifyDataSetChanged()
        binding.noResultsText.visibility = if (filteredPedidos.isEmpty()) View.VISIBLE else View.GONE
        if (filteredPedidos.isEmpty()) {
            binding.noResultsText.text = "No hay pedidos en el período seleccionado"
        } else {
            Log.d("HistorialPedidos", "Pedidos filtrados: ${filteredPedidos.size}")
        }
    }

    private fun eliminarPedido(pedidoId: String) {
        val usuarioId = auth.currentUser?.uid
        if (usuarioId != null) {
            db.collection("historial_pedidos")
                .document(pedidoId)
                .delete()
                .addOnSuccessListener {
                    Log.d("HistorialPedidos", "Pedido eliminado: $pedidoId")
                    pedidos.removeAll { it.id == pedidoId }
                    filteredPedidos.removeAll { it.id == pedidoId }
                    adapter.notifyDataSetChanged()
                    binding.noResultsText.visibility = if (filteredPedidos.isEmpty()) View.VISIBLE else View.GONE
                    if (filteredPedidos.isEmpty()) {
                        binding.noResultsText.text = "No hay pedidos en el historial"
                    }
                    Toast.makeText(this, "Pedido eliminado", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("HistorialPedidos", "Error al eliminar pedido", e)
                    Toast.makeText(this, "Error al eliminar pedido: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun repetirPedido(pedido: Pedido) {
        val usuarioId = auth.currentUser?.uid
        if (usuarioId == null) {
            Toast.makeText(this, "Debe iniciar sesión para repetir el pedido", Toast.LENGTH_SHORT).show()
            return
        }


        val carritoRef = db.collection("carrito").document(usuarioId)
        val productos = pedido.productos.map { producto ->
            mapOf(
                "productoId" to (producto["id"]?.toString() ?: UUID.randomUUID().toString()),
                "nombre" to (producto["nombre"]?.toString() ?: ""),
                "precio" to ((producto["precio"] as? Number)?.toDouble() ?: 0.0),
                "cantidad" to ((producto["cantidad"] as? Number)?.toInt() ?: 1),
                "imagenUrl" to (producto["imagenUrl"] as? String ?: "")
            )
        }

        val carritoData = mapOf(
            "usuario_id" to usuarioId,
            "negocio_nombre" to pedido.negocioNombre,
            "productos" to productos,
            "fecha" to System.currentTimeMillis()
        )

        carritoRef.set(carritoData)
            .addOnSuccessListener {
                Log.d("HistorialPedidos", "Productos añadidos al carrito para el usuario $usuarioId")
                // Redirigir a la actividad de confirmación de pedido
                val intent = Intent(this, ConfirmarPedidoActivity::class.java).apply {
                    putExtra("negocio_nombre", pedido.negocioNombre)
                    putExtra("imagenNegocio", pedido.imagenNegocio ?: "")
                    putExtra("productos", JSONArray(productos).toString())
                    putExtra("subtotal", pedido.subtotal)
                    putExtra("costoEnvio", pedido.costoEnvio)
                    putExtra("costoServicio", pedido.costoServicio)
                    putExtra("total", pedido.total)
                    putExtra("direccion_entrega", pedido.direccion)
                    putExtra("metodoPago", pedido.metodoPago)
                    putExtra("pedido_id", UUID.randomUUID().toString()) // Nuevo ID para el pedido
                }
                startActivity(intent)
                Toast.makeText(this, "Pedido añadido al carrito", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("HistorialPedidos", "Error al añadir al carrito", e)
                Toast.makeText(this, "Error al repetir pedido: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}