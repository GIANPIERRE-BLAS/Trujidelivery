package pe.edu.trujidelivery.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import pe.edu.trujidelivery.R
import pe.edu.trujidelivery.adapters.NegocioAdapter
import pe.edu.trujidelivery.adapters.ProductoAdapter
import pe.edu.trujidelivery.databinding.ActivitySearchResultsBinding

import pe.edu.trujidelivery.modelo.Producto
import pe.edu.trujidelivery.pe.edu.trujidelivery.modelo.Negocio

class SearchResultsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchResultsBinding
    private lateinit var productosAdapter: ProductoAdapter
    private lateinit var negociosAdapter: NegocioAdapter
    private val db = FirebaseFirestore.getInstance()
    private val productosList = mutableListOf<Producto>()
    private val negociosList = mutableListOf<Negocio>()
    private var isShowingNegocioProductos = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val searchTerm = intent.getStringExtra("QUERY") ?: ""
        binding.searchEditText.setText(searchTerm)
        setupSearch()
        setupBottomNavigation()
        setupAdapters()
        buscarProductos(searchTerm)
        buscarNegocios(searchTerm)
    }

    private fun setupAdapters() {
        productosAdapter = ProductoAdapter(
            productosList,
            onProductoClick = { producto -> abrirDetalleProducto(producto) },
            onAddToCartClick = { producto -> agregarProducto(producto) }
        )
        binding.recyclerProductos.layoutManager = LinearLayoutManager(this)
        binding.recyclerProductos.adapter = productosAdapter

        negociosAdapter = NegocioAdapter(negociosList) { negocio ->
            if (negocio.id.isNotBlank() && negocio.nombre.isNotBlank() && negocio.categoriaId.isNotBlank()) {
                Log.d("SearchResultsActivity", "Navegando a ProductosActivity: negocioId=${negocio.id}, nombre=${negocio.nombre}, categoriaId=${negocio.categoriaId}")
                val intent = Intent(this, ProductosActivity::class.java).apply {
                    putExtra("NEGOCIO_ID", negocio.id)
                    putExtra("NEGOCIO_NOMBRE", negocio.nombre)
                    putExtra("CATEGORIA_ID", negocio.categoriaId)
                }
                startActivity(intent)
            } else {
                Log.e("SearchResultsActivity", "Negocio inválido: id=${negocio.id}, nombre=${negocio.nombre}, categoriaId=${negocio.categoriaId}")
                Toast.makeText(this, "Error: Datos del negocio incompletos", Toast.LENGTH_SHORT).show()
            }
        }
        binding.recyclerNegocios.layoutManager = LinearLayoutManager(this)
        binding.recyclerNegocios.adapter = negociosAdapter
    }

    private fun setupSearch() {
        binding.searchEditText.setOnEditorActionListener { _, _, _ ->
            val query = binding.searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                isShowingNegocioProductos = false
                buscarProductos(query)
                buscarNegocios(query)
            }
            true
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.selectedItemId = R.id.menu_home
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {
                    finish()
                    true
                }
                R.id.menu_super -> {
                    Toast.makeText(this, "Supermercado pulsado", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_promos -> {
                    Toast.makeText(this, "Promociones pulsadas", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_orders -> {
                    Toast.makeText(this, "Pedidos pulsados", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_profile -> {
                    Toast.makeText(this, "Perfil pulsado", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }

    private fun buscarProductos(query: String) {
        val queryLower = query.lowercase().trim().replace("\\s+", " ")
        productosList.clear()
        db.collectionGroup("productos")
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    val producto = doc.toObject(Producto::class.java)
                    producto?.let {
                        if (it.nombre.isNotBlank() && (it.nombre.lowercase().contains(queryLower) ||
                                    it.descripcion?.lowercase()?.contains(queryLower) == true)) {
                            producto.id = doc.id
                            if (!productosList.any { p -> p.id == it.id }) {
                                productosList.add(it)
                            }
                        }
                    }
                }
                productosAdapter.notifyDataSetChanged()
                updateUI()
                Log.d("SearchResultsActivity", "Productos encontrados: ${productosList.size}")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al buscar productos: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("SearchResultsActivity", "Error al buscar productos", e)
                updateUI()
            }
    }

    private fun buscarNegocios(query: String) {
        val queryLower = query.lowercase().trim().replace("\\s+", " ")
        negociosList.clear()
        db.collection("categorias").get()
            .addOnSuccessListener { categoriasSnapshot ->
                val categorias = categoriasSnapshot.documents.map { it.id }
                if (categorias.isEmpty()) {
                    negociosAdapter.notifyDataSetChanged()
                    updateUI()
                    Log.d("SearchResultsActivity", "No se encontraron categorías")
                    return@addOnSuccessListener
                }
                var completedQueries = 0
                for (categoriaId in categorias) {
                    db.collection("categorias").document(categoriaId)
                        .collection("negocios")
                        .get()
                        .addOnSuccessListener { snapshot ->
                            for (doc in snapshot.documents) {
                                val negocio = doc.toObject(Negocio::class.java)
                                negocio?.let {
                                    if (it.id.isNotBlank() && it.nombre.isNotBlank() && it.nombre.lowercase().contains(queryLower)) {
                                        negocio.id = doc.id
                                        negocio.categoriaId = categoriaId
                                        if (!negociosList.any { n -> n.id == negocio.id && n.categoriaId == negocio.categoriaId }) {
                                            negociosList.add(negocio)
                                        }
                                    }
                                }
                            }
                            completedQueries++
                            if (completedQueries == categorias.size) {
                                negociosAdapter.notifyDataSetChanged()
                                updateUI()
                                Log.d("SearchResultsActivity", "Negocios encontrados: ${negociosList.size}")
                            }
                        }
                        .addOnFailureListener { e ->
                            completedQueries++
                            Log.e("SearchResultsActivity", "Error al buscar negocios en categoria $categoriaId", e)
                            if (completedQueries == categorias.size) {
                                negociosAdapter.notifyDataSetChanged()
                                updateUI()
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al buscar negocios: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("SearchResultsActivity", "Error al obtener categorías", e)
                updateUI()
            }
    }

    private fun updateUI() {
        if (isShowingNegocioProductos) {
            binding.tvProductosTitle.visibility = if (productosList.isNotEmpty()) View.VISIBLE else View.GONE
            binding.recyclerProductos.visibility = if (productosList.isNotEmpty()) View.VISIBLE else View.GONE
            binding.emptyResultsLayout.visibility = if (productosList.isEmpty()) View.VISIBLE else View.GONE
            if (productosList.isEmpty()) {
                binding.tvEmptyResultsMessage.text = "Este negocio no tiene productos disponibles"
            }
        } else {
            binding.tvProductosTitle.visibility = if (productosList.isNotEmpty()) View.VISIBLE else View.GONE
            binding.recyclerProductos.visibility = if (productosList.isNotEmpty()) View.VISIBLE else View.GONE
            binding.tvNegociosTitle.visibility = if (negociosList.isNotEmpty()) View.VISIBLE else View.GONE
            binding.recyclerNegocios.visibility = if (negociosList.isNotEmpty()) View.VISIBLE else View.GONE
            binding.emptyResultsLayout.visibility = if (productosList.isEmpty() && negociosList.isEmpty()) View.VISIBLE else View.GONE
            if (productosList.isEmpty() && negociosList.isEmpty()) {
                binding.tvEmptyResultsMessage.text = "No se encontraron resultados"
            }
        }
        Log.d("SearchResultsActivity", "UI actualizada: productos=${productosList.size}, negocios=${negociosList.size}, isShowingNegocioProductos=$isShowingNegocioProductos")
    }

    private fun agregarProducto(producto: Producto) {
        Toast.makeText(this, "${producto.nombre} añadido al carrito", Toast.LENGTH_SHORT).show()
    }

    private fun abrirDetalleProducto(producto: Producto) {
        Toast.makeText(this, "Producto: ${producto.nombre}", Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        if (isShowingNegocioProductos) {
            isShowingNegocioProductos = false
            productosList.clear()
            negociosList.clear()
            val query = binding.searchEditText.text.toString().trim()
            buscarProductos(query)
            buscarNegocios(query)
        } else {
            super.onBackPressed()
        }
    }
}