package pe.edu.trujidelivery.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import pe.edu.trujidelivery.AddressActivity
import pe.edu.trujidelivery.databinding.ActivityCarritoBinding
import pe.edu.trujidelivery.pagos.ConfirmarPedidoActivity
import pe.edu.trujidelivery.adapters.CarritoAdapter
import pe.edu.trujidelivery.modelo.CarritoItem

class CarritoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCarritoBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val carritoItems = mutableListOf<CarritoItem>()
    private lateinit var adapter: CarritoAdapter
    private lateinit var sharedPreferences: SharedPreferences

    private val addressLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val direccion = result.data?.getStringExtra("direccion")
            if (!direccion.isNullOrEmpty() && direccion != "Seleccionar dirección") {
                val userEmail = auth.currentUser?.email ?: "unknown_user"
                val userName = auth.currentUser?.displayName ?: "Usuario"
                sharedPreferences.edit().putString("direccion", direccion).apply()
                irAConfirmarPedido(direccion, userEmail, userName)
            } else {
                Toast.makeText(this, "Debe seleccionar una dirección válida", Toast.LENGTH_SHORT).show()
                irASeleccionarDireccion(auth.currentUser?.email ?: "", auth.currentUser?.displayName ?: "")
            }
        } else {
            Toast.makeText(this, "Debe seleccionar una dirección válida", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarritoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        sharedPreferences = getSharedPreferences("trujidelivery_prefs", MODE_PRIVATE)
        setupRecyclerView()
        cargarCarrito()
        binding.btnIrAPagar.setOnClickListener {
            if (carritoItems.isEmpty()) {
                Toast.makeText(this, "El carrito está vacío", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val userEmail = auth.currentUser?.email ?: "unknown_user"
            val userName = auth.currentUser?.displayName ?: "Usuario"
            val direccionGuardada = sharedPreferences.getString("direccion", null)
            if (direccionGuardada.isNullOrEmpty() || direccionGuardada == "Seleccionar dirección") {
                sharedPreferences.edit().remove("direccion").apply()
                irASeleccionarDireccion(auth.currentUser?.email ?: "", auth.currentUser?.displayName ?: "")
            } else {
                irAConfirmarPedido(direccionGuardada, userEmail, userName)
            }
        }
        binding.btnIrAPagar.isEnabled = false
        binding.iconBack.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun irASeleccionarDireccion(userEmail: String, userName: String) {
        val productosNombres = arrayListOf<String>()
        val productosCantidades = mutableListOf<Int>()
        val productosPrecios = mutableListOf<Double>()
        val productosImagenes = arrayListOf<String>()
        val negociosIds = arrayListOf<String>()
        val negociosNombres = arrayListOf<String>()
        val negociosImagenes = arrayListOf<String>()
        for (item in carritoItems) {
            productosNombres.add(item.nombre)
            productosCantidades.add(item.cantidad)
            productosPrecios.add(item.precio)
            productosImagenes.add(item.imagenUrl)
            negociosIds.add(item.negocioId)
            negociosNombres.add(item.negocioNombre)
            negociosImagenes.add(item.imagenNegocio)
        }
        val intent = Intent(this, AddressActivity::class.java).apply {
            putExtra("es_pago", true)
            putExtra("user_email", userEmail)
            putExtra("name", userName)
            putStringArrayListExtra("productos_nombres", productosNombres)
            putExtra("productos_cantidades", productosCantidades.toIntArray())
            putExtra("productos_precios", productosPrecios.toDoubleArray())
            putStringArrayListExtra("productos_imagenes", productosImagenes)
            putStringArrayListExtra("negocios_ids", negociosIds)
            putStringArrayListExtra("negocios_nombres", negociosNombres)
            putStringArrayListExtra("negocios_imagenes", negociosImagenes)
        }
        addressLauncher.launch(intent)
    }

    private fun irAConfirmarPedido(direccion: String, userEmail: String, userName: String) {
        val productosNombres = arrayListOf<String>()
        val productosCantidades = mutableListOf<Int>()
        val productosPrecios = mutableListOf<Double>()
        val productosImagenes = arrayListOf<String>()
        val negociosIds = arrayListOf<String>()
        val negociosNombres = arrayListOf<String>()
        val negociosImagenes = arrayListOf<String>()
        for (item in carritoItems) {
            productosNombres.add(item.nombre)
            productosCantidades.add(item.cantidad)
            productosPrecios.add(item.precio)
            productosImagenes.add(item.imagenUrl)
            negociosIds.add(item.negocioId)
            negociosNombres.add(item.negocioNombre)
            negociosImagenes.add(item.imagenNegocio)
        }
        val intent = Intent(this, ConfirmarPedidoActivity::class.java).apply {
            putExtra("direccion", direccion)
            putExtra("user_email", userEmail)
            putExtra("name", userName)
            putStringArrayListExtra("productos_nombres", productosNombres)
            putExtra("productos_cantidades", productosCantidades.toIntArray())
            putExtra("productos_precios", productosPrecios.toDoubleArray())
            putStringArrayListExtra("productos_imagenes", productosImagenes)
            putStringArrayListExtra("negocios_ids", negociosIds)
            putStringArrayListExtra("negocios_nombres", negociosNombres)
            putStringArrayListExtra("negocios_imagenes", negociosImagenes)
        }
        startActivity(intent)
    }

    private fun setupRecyclerView() {
        adapter = CarritoAdapter(
            carritoItems,
            onAddClick = { item ->
                incrementarCantidad(item)
            },
            onRemoveClick = { item ->
                decrementarCantidad(item)
            },
            onDeleteClick = { item ->
                eliminarItemDelCarrito(item.id)
            },
            onBackToNegocioClick = { item ->
                val intent = Intent(this, ProductosActivity::class.java).apply {
                    putExtra("CATEGORIA_ID", item.categoriaId)
                    putExtra("NEGOCIO_ID", item.negocioId)
                    putExtra("NEGOCIO_NOMBRE", item.negocioNombre)
                }
                startActivity(intent)
            }
        )
        binding.recyclerViewCarrito.apply {
            layoutManager = LinearLayoutManager(this@CarritoActivity)
            adapter = this@CarritoActivity.adapter
        }
    }

    private fun cargarCarrito() {
        val usuarioId = auth.currentUser?.uid
        if (usuarioId == null) {
            Toast.makeText(this, "Debe iniciar sesión para ver el carrito", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        binding.progressBar.visibility = View.VISIBLE
        binding.noResultsText.visibility = View.GONE
        db.collection("usuarios")
            .document(usuarioId)
            .collection("carrito")
            .get()
            .addOnSuccessListener { snapshot ->
                carritoItems.clear()
                for (doc in snapshot.documents) {
                    val id = doc.id
                    val nombre = doc.getString("nombre") ?: ""
                    val precio = doc.getDouble("precio") ?: 0.0
                    val imagenUrl = doc.getString("imagenUrl") ?: ""
                    val cantidad = doc.getLong("cantidad")?.toInt() ?: 1
                    val negocioNombre = doc.getString("negocioNombre") ?: "Desconocido"
                    val negocioId = doc.getString("negocioId") ?: ""
                    val categoriaId = doc.getString("categoriaId") ?: ""
                    val imagenNegocio = doc.getString("imagenNegocio") ?: ""
                    val item = CarritoItem(id, nombre, precio, imagenUrl, cantidad, negocioNombre, negocioId, categoriaId, imagenNegocio)
                    carritoItems.add(item)
                }
                adapter.notifyDataSetChanged()
                actualizarTotal()
                binding.progressBar.visibility = View.GONE
                binding.noResultsText.visibility = if (carritoItems.isEmpty()) View.VISIBLE else View.GONE
                if (carritoItems.isEmpty()) {
                    binding.noResultsText.text = "El carrito está vacío"
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.noResultsText.visibility = View.VISIBLE
                binding.noResultsText.text = "Error al cargar el carrito"
                Toast.makeText(this, "Error al cargar el carrito: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun incrementarCantidad(item: CarritoItem) {
        val usuarioId = auth.currentUser?.uid ?: return
        val itemIndex = carritoItems.indexOfFirst { it.id == item.id }
        if (itemIndex != -1) {
            carritoItems[itemIndex].cantidad += 1
            adapter.notifyItemChanged(itemIndex)
            actualizarTotal()
            db.collection("usuarios")
                .document(usuarioId)
                .collection("carrito")
                .document(item.id)
                .update("cantidad", carritoItems[itemIndex].cantidad)
                .addOnSuccessListener {
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al actualizar cantidad: ${e.message}", Toast.LENGTH_SHORT).show()
                    carritoItems[itemIndex].cantidad -= 1
                    adapter.notifyItemChanged(itemIndex)
                    actualizarTotal()
                }
        }
    }

    private fun decrementarCantidad(item: CarritoItem) {
        val usuarioId = auth.currentUser?.uid ?: return
        val itemIndex = carritoItems.indexOfFirst { it.id == item.id }
        if (itemIndex != -1 && carritoItems[itemIndex].cantidad > 1) {
            carritoItems[itemIndex].cantidad -= 1
            adapter.notifyItemChanged(itemIndex)
            actualizarTotal()
            db.collection("usuarios")
                .document(usuarioId)
                .collection("carrito")
                .document(item.id)
                .update("cantidad", carritoItems[itemIndex].cantidad)
                .addOnSuccessListener {
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al actualizar cantidad: ${e.message}", Toast.LENGTH_SHORT).show()
                    carritoItems[itemIndex].cantidad += 1
                    adapter.notifyItemChanged(itemIndex)
                    actualizarTotal()
                }
        } else if (itemIndex != -1 && carritoItems[itemIndex].cantidad == 1) {
            eliminarItemDelCarrito(item.id)
        }
    }

    private fun eliminarItemDelCarrito(itemId: String) {
        val usuarioId = auth.currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE
        db.collection("usuarios")
            .document(usuarioId)
            .collection("carrito")
            .document(itemId)
            .delete()
            .addOnSuccessListener {
                val index = carritoItems.indexOfFirst { it.id == itemId }
                if (index != -1) {
                    carritoItems.removeAt(index)
                    adapter.notifyItemRemoved(index)
                    actualizarTotal()
                    binding.noResultsText.visibility = if (carritoItems.isEmpty()) View.VISIBLE else View.GONE
                    if (carritoItems.isEmpty()) {
                        binding.noResultsText.text = "El carrito está vacío"
                    }
                    Toast.makeText(this, "Producto eliminado del carrito", Toast.LENGTH_SHORT).show()
                }
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error al eliminar producto: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun actualizarTotal() {
        val total = carritoItems.sumOf { it.precio * it.cantidad }
        binding.textTotal.text = "Total: S/. %.2f".format(total)
        binding.btnIrAPagar.isEnabled = carritoItems.isNotEmpty()
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            cargarCarrito()
        }
    }
}