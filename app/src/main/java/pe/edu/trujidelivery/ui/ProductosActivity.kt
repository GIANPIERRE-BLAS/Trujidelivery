package pe.edu.trujidelivery.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import pe.edu.trujidelivery.R
import pe.edu.trujidelivery.adapters.ProductoAdapter
import pe.edu.trujidelivery.databinding.ActivityProductosBinding
import pe.edu.trujidelivery.modelo.Producto

class ProductosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductosBinding
    private val db = FirebaseFirestore.getInstance()
    private val productos = mutableListOf<Producto>()
    private val productosFiltrados = mutableListOf<Producto>()
    private lateinit var productoAdapter: ProductoAdapter
    private val auth = FirebaseAuth.getInstance()
    private var categoriaId: String = ""
    private var negocioId: String = ""
    private var negocioNombre: String = ""
    private var isSortAscending = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        categoriaId = intent.getStringExtra("CATEGORIA_ID") ?: ""
        negocioId = intent.getStringExtra("NEGOCIO_ID") ?: ""
        negocioNombre = intent.getStringExtra("NEGOCIO_NOMBRE") ?: ""
        title = negocioNombre

        setupRecyclerView()
        cargarProductos(categoriaId, negocioId)
        setupBusqueda()
        setupFiltros()
        setupFab()
    }

    private fun setupRecyclerView() {
        productoAdapter = ProductoAdapter(
            productosFiltrados,
            onProductoClick = { producto ->
                Toast.makeText(this, "Producto: ${producto.nombre}", Toast.LENGTH_SHORT).show()
            },
            onAddToCartClick = { producto ->
                producto.cantidad = (producto.cantidad ?: 0) + 1
                productoAdapter.notifyDataSetChanged()
                agregarAlCarrito(producto)
            }
        )

        binding.recyclerViewProductos.apply {
            layoutManager = LinearLayoutManager(this@ProductosActivity)
            adapter = productoAdapter
        }
    }

    private fun setupBusqueda() {
        val etBuscar = findViewById<EditText>(R.id.etBuscar)
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarProductos(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupFiltros() {
        findViewById<View>(R.id.filterOrdenar).setOnClickListener {
            isSortAscending = !isSortAscending
            sortProductosByName()
            Toast.makeText(this, if (isSortAscending) "Ordenado A-Z" else "Ordenado Z-A", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.filter_entrega_gratis).setOnClickListener {
            productosFiltrados.clear()
            productosFiltrados.addAll(productos)
            productoAdapter.notifyDataSetChanged()
            Toast.makeText(this, "Mostrando todos los productos", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.filter_mejor_valorado).setOnClickListener {
            sortProductosByPrice()
            Toast.makeText(this, "Ordenado por precio", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupFab() {
        val fabCarrito = findViewById<FloatingActionButton>(R.id.fabCarrito)
        fabCarrito.setOnClickListener {
            val intent = Intent(this, CarritoActivity::class.java)
            startActivity(intent)
        }
    }

    private fun filtrarProductos(query: String) {
        productosFiltrados.clear()
        if (query.isEmpty()) {
            productosFiltrados.addAll(productos)
        } else {
            val queryLower = query.lowercase()
            productosFiltrados.addAll(productos.filter {
                it.nombre.lowercase().contains(queryLower) ||
                        (it.descripcion?.lowercase()?.contains(queryLower) == true)
            })
        }
        productoAdapter.notifyDataSetChanged()
    }

    private fun sortProductosByName() {
        productosFiltrados.sortWith { p1, p2 ->
            if (isSortAscending) p1.nombre.compareTo(p2.nombre)
            else p2.nombre.compareTo(p1.nombre)
        }
        productoAdapter.notifyDataSetChanged()
    }

    private fun sortProductosByPrice() {
        productosFiltrados.sortBy { it.precio }
        productoAdapter.notifyDataSetChanged()
    }

    private fun cargarProductos(categoriaId: String, negocioId: String) {
        db.collection("categorias")
            .document(categoriaId)
            .collection("negocios")
            .document(negocioId)
            .collection("productos")
            .get()
            .addOnSuccessListener { snapshot ->
                productos.clear()
                productosFiltrados.clear()
                for (document in snapshot.documents) {
                    val producto = document.toObject(Producto::class.java)
                    if (producto != null) {
                        producto.id = document.id
                        producto.cantidad = 0
                        productos.add(producto)
                    }
                }
                productosFiltrados.addAll(productos)
                productoAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar productos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun agregarAlCarrito(producto: Producto) {
        val usuarioId = auth.currentUser?.uid
        if (usuarioId == null) {
            Toast.makeText(this, "Debe iniciar sesión para agregar al carrito", Toast.LENGTH_SHORT).show()
            return
        }
        db.collection("categorias")
            .document(categoriaId)
            .collection("negocios")
            .document(negocioId)
            .get()
            .addOnSuccessListener { document ->
                val imagenNegocio = document.getString("imagenUrl") ?: ""
                val carritoRef = db.collection("usuarios").document(usuarioId).collection("carrito")
                carritoRef.whereEqualTo("id", producto.id)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            val doc = querySnapshot.documents[0]
                            val cantidadActual = doc.getLong("cantidad") ?: 1L
                            carritoRef.document(doc.id)
                                .update("cantidad", cantidadActual + 1)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Cantidad actualizada en el carrito", Toast.LENGTH_SHORT).show()
                                    showFab()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Error al actualizar cantidad", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            val productoMap = hashMapOf(
                                "id" to producto.id,
                                "nombre" to producto.nombre,
                                "descripcion" to producto.descripcion,
                                "precio" to producto.precio,
                                "imagenUrl" to producto.imagenUrl,
                                "cantidad" to 1,
                                "negocioId" to negocioId,
                                "categoriaId" to categoriaId,
                                "negocioNombre" to negocioNombre,
                                "imagenNegocio" to imagenNegocio // Agregar la imagen del negocio
                            )
                            carritoRef.add(productoMap)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Producto agregado al carrito", Toast.LENGTH_SHORT).show()
                                    showFab()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Error al agregar al carrito", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al consultar el carrito", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al obtener datos del negocio", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showFab() {
        val fabCarrito = findViewById<FloatingActionButton>(R.id.fabCarrito)
        fabCarrito.visibility = View.VISIBLE
    }
}