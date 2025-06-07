package pe.edu.trujidelivery.ui


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.QuerySnapshot
import pe.edu.trujidelivery.AddressActivity
import pe.edu.trujidelivery.ProfileActivity
import pe.edu.trujidelivery.R
import pe.edu.trujidelivery.adapters.CategoriaAdapter
import pe.edu.trujidelivery.adapters.NegocioAdapter
import pe.edu.trujidelivery.adapters.ProductoAdapter
import pe.edu.trujidelivery.databinding.ActivityHomeBinding
import pe.edu.trujidelivery.modelo.Categoria
import pe.edu.trujidelivery.modelo.Producto
import pe.edu.trujidelivery.notification.NotificacionesActivity
import pe.edu.trujidelivery.pagos.HistorialPedidosActivity
import pe.edu.trujidelivery.modelo.CarritoItem
import pe.edu.trujidelivery.pe.edu.trujidelivery.modelo.Negocio

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val categorias = mutableListOf<Categoria>()
    private val productosEncontrados = mutableListOf<Producto>()
    private val negociosEncontrados = mutableListOf<Negocio>()
    private lateinit var categoriaAdapter: CategoriaAdapter
    private lateinit var productosEncontradosAdapter: ProductoAdapter
    private lateinit var negociosEncontradosAdapter: NegocioAdapter
    private var userName: String? = null
    private var userEmail: String? = null
    private var userPhotoUrl: Uri? = null
    private var isSearching = false
    private var isShowingNegocioProductos = false
    private var currentNegocio: Negocio? = null
    private var lastSearchQuery: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeUserData()
        setupAddress()
        setupTopRightIcons()
        setupBottomNavigationView()
        setupCategoriasRecyclerView()
        setupSearchAdapters()
        cargarCategorias()
        setupSearch()
        actualizarBadgeCarrito()
        binding.titleCategorias.text = "Categorías"
        binding.titleCategorias.visibility = View.VISIBLE
        binding.recyclerViewCategorias.visibility = View.VISIBLE
        binding.titleDestacados.visibility = View.GONE
        binding.recyclerViewProducts.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        actualizarBadgeCarrito()
        if (!isSearching && !isShowingNegocioProductos) {
            binding.recyclerViewCategorias.apply {
                layoutManager = GridLayoutManager(this@HomeActivity, 2)
                adapter = categoriaAdapter
            }
            updateSearchVisibility()
        }
        setupAddress()
    }

    private fun initializeUserData() {
        val currentUser: FirebaseUser? = auth.currentUser
        if (currentUser != null) {
            userName = currentUser.displayName ?: "Usuario"
            userEmail = currentUser.email ?: "Correo no disponible"
            userPhotoUrl = currentUser.photoUrl
        } else {
            userName = "Usuario"
            userEmail = "Correo no disponible"
        }
    }

    private fun setupAddress() {
        val intentAddress = intent.getStringExtra("direccion")
        val usuarioId = auth.currentUser?.uid

        if (!intentAddress.isNullOrEmpty()) {
            binding.textAddress.text = getShortAddress(intentAddress)
            if (usuarioId != null) {
                db.collection("usuarios").document(usuarioId)
                    .set(mapOf("direccion" to intentAddress), com.google.firebase.firestore.SetOptions.merge())
            }
        } else if (usuarioId != null) {
            db.collection("usuarios").document(usuarioId)
                .get()
                .addOnSuccessListener { document ->
                    val storedAddress = document.getString("direccion") ?: "Seleccionar dirección"
                    binding.textAddress.text = getShortAddress(storedAddress)
                }
                .addOnFailureListener {
                    binding.textAddress.text = "Seleccionar dirección"
                }
        } else {
            binding.textAddress.text = "Seleccionar dirección"
        }

        binding.textAddress.setOnClickListener { showAddressPopupMenu(it) }
    }

    private fun showAddressPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menu.add("Seleccionar dirección")
        popupMenu.setOnMenuItemClickListener { item ->
            if (item.title == "Seleccionar dirección") {
                navigateToAddressActivity()
                true
            } else {
                false
            }
        }
        popupMenu.show()
    }

    private fun navigateToAddressActivity() {
        val intent = Intent(this, AddressActivity::class.java).apply {
            putExtra("user_email", userEmail)
            putExtra("name", userName)
        }
        startActivity(intent)
    }

    private fun getShortAddress(fullAddress: String): String {
        val parts = fullAddress.split(",")
        return if (parts.size >= 2) {
            "${parts[0].trim()}, ${parts[1].trim()}"
        } else {
            fullAddress
        }
    }

    private fun setupTopRightIcons() {
        binding.iconNotifications.setOnClickListener {
            val intent = Intent(this, NotificacionesActivity::class.java)
            startActivity(intent)
        }
        binding.iconCart.setOnClickListener {
            val intent = Intent(this, CarritoActivity::class.java)
            startActivity(intent)
        }
    }

    private fun actualizarBadgeCarrito() {
        val usuarioId = auth.currentUser?.uid ?: return
        db.collection("usuarios")
            .document(usuarioId)
            .collection("carrito")
            .get()
            .addOnSuccessListener { snapshot ->
                val cantidad = snapshot.documents.sumOf { doc ->
                    doc.getLong("cantidad")?.toInt() ?: 0
                }
                binding.cartBadge.visibility = if (cantidad > 0) View.VISIBLE else View.GONE
                binding.cartBadge.text = cantidad.toString()
            }
    }

    private fun setupBottomNavigationView() {
        binding.bottomNavigationView.selectedItemId = R.id.menu_home
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {
                    limpiarBusqueda()
                    true
                }
                R.id.menu_super -> {
                    db.collection("categorias")
                        .whereEqualTo("nombre", "Supermercados")
                        .get()
                        .addOnSuccessListener { snapshot ->
                            if (!snapshot.isEmpty) {
                                val categoria = snapshot.documents.first()
                                val categoriaId = categoria.id
                                val intent = Intent(this, NegociosActivity::class.java).apply {
                                    putExtra("CATEGORIA_ID", categoriaId)
                                    putExtra("CATEGORIA_NOMBRE", "Supermercados")
                                }
                                startActivity(intent)
                            } else {
                                showToast("Categoría 'Supermercados' no encontrada")
                            }
                        }
                        .addOnFailureListener {
                            showToast("Error al cargar la categoría")
                        }
                    true
                }
                R.id.menu_promos -> {
                    cargarProductosYNegociosPromos()
                    true
                }
                R.id.menu_orders -> {
                    startActivity(Intent(this, HistorialPedidosActivity::class.java))
                    true
                }
                R.id.menu_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun cargarProductosYNegociosPromos() {
        isSearching = true
        isShowingNegocioProductos = false
        currentNegocio = null
        lastSearchQuery = null
        productosEncontrados.clear()
        negociosEncontrados.clear()
        binding.progressBar.visibility = View.VISIBLE
        binding.noResultsText.visibility = View.GONE
        binding.titleCategorias.text = "Productos en Promoción"
        binding.titleDestacados.text = "Negocios con Promociones"
        mostrarVistasBusqueda()

        val todosProductos = mutableListOf<Pair<Producto, Negocio>>()

        db.collection("categorias").get()
            .addOnSuccessListener { categoriasSnapshot: QuerySnapshot ->
                val categorias = categoriasSnapshot.documents.map { document -> document.id }
                if (categorias.isEmpty()) {
                    productosEncontradosAdapter.notifyDataSetChanged()
                    negociosEncontradosAdapter.notifyDataSetChanged()
                    binding.progressBar.visibility = View.GONE
                    binding.noResultsText.visibility = View.VISIBLE
                    binding.noResultsText.text = "No se encontraron categorías"
                    updateSearchVisibility()
                    return@addOnSuccessListener
                }

                val negocioQueries = mutableListOf<Task<QuerySnapshot>>()
                for (categoriaId in categorias) {
                    val query = db.collection("categorias").document(categoriaId)
                        .collection("negocios").get()
                    negocioQueries.add(query)
                }

                Tasks.whenAllComplete(negocioQueries).addOnCompleteListener { task ->
                    val tasks = task.result as List<Task<QuerySnapshot>>
                    var completedQueries = 0
                    val totalNegociosQueries = tasks.sumOf { t: Task<QuerySnapshot> ->
                        if (t.isSuccessful) t.result.documents.size else 0
                    }

                    if (totalNegociosQueries == 0) {
                        productosEncontradosAdapter.notifyDataSetChanged()
                        negociosEncontradosAdapter.notifyDataSetChanged()
                        binding.progressBar.visibility = View.GONE
                        binding.noResultsText.visibility = View.VISIBLE
                        binding.noResultsText.text = "No se encontraron negocios"
                        updateSearchVisibility()
                        return@addOnCompleteListener
                    }

                    for (t in tasks) {
                        if (t.isSuccessful) {
                            val negociosSnapshot = t.result
                            val categoriaId = categorias[negocioQueries.indexOf(t)]
                            for (negocioDoc in negociosSnapshot.documents) {
                                val negocio = negocioDoc.toObject(Negocio::class.java)?.apply {
                                    id = negocioDoc.id
                                    this.categoriaId = categoriaId
                                }
                                if (negocio != null && negocio.id.isNotBlank() && negocio.nombre.isNotBlank()) {
                                    db.collection("categorias").document(categoriaId)
                                        .collection("negocios").document(negocio.id)
                                        .collection("productos")
                                        .whereEqualTo("isDiscounted", true)
                                        .get()
                                        .addOnSuccessListener { productosSnapshot: QuerySnapshot ->
                                            for (productoDoc in productosSnapshot.documents) {
                                                val producto = productoDoc.toObject(Producto::class.java)?.apply {
                                                    id = productoDoc.id
                                                    this.categoriaId = categoriaId
                                                    this.comercioId = negocio.id
                                                }
                                                if (producto != null && producto.nombre.isNotBlank() && producto.tieneDescuento()) {
                                                    todosProductos.add(Pair(producto, negocio))
                                                }
                                            }
                                            completedQueries++
                                            if (completedQueries == totalNegociosQueries) {
                                                val maxProductos = 10
                                                val productosAleatorios = todosProductos.shuffled().take(maxProductos)
                                                productosAleatorios.forEach { (producto, negocio) ->
                                                    productosEncontrados.add(producto)
                                                    if (!negociosEncontrados.any { it.id == negocio.id && it.categoriaId == negocio.categoriaId }) {
                                                        negociosEncontrados.add(negocio)
                                                    }
                                                }
                                                productosEncontradosAdapter.notifyDataSetChanged()
                                                negociosEncontradosAdapter.notifyDataSetChanged()
                                                binding.progressBar.visibility = View.GONE
                                                updateSearchVisibility()
                                            }
                                        }
                                        .addOnFailureListener {
                                            completedQueries++
                                            if (completedQueries == totalNegociosQueries) {
                                                val maxProductos = 10
                                                val productosAleatorios = todosProductos.shuffled().take(maxProductos)
                                                productosAleatorios.forEach { (producto, negocio) ->
                                                    productosEncontrados.add(producto)
                                                    if (!negociosEncontrados.any { it.id == negocio.id && it.categoriaId == negocio.categoriaId }) {
                                                        negociosEncontrados.add(negocio)
                                                    }
                                                }
                                                productosEncontradosAdapter.notifyDataSetChanged()
                                                negociosEncontradosAdapter.notifyDataSetChanged()
                                                binding.progressBar.visibility = View.GONE
                                                updateSearchVisibility()
                                            }
                                        }
                                }
                            }
                        } else {
                            completedQueries++
                            if (completedQueries == totalNegociosQueries) {
                                val maxProductos = 10
                                val productosAleatorios = todosProductos.shuffled().take(maxProductos)
                                productosAleatorios.forEach { (producto, negocio) ->
                                    productosEncontrados.add(producto)
                                    if (!negociosEncontrados.any { it.id == negocio.id && it.categoriaId == negocio.categoriaId }) {
                                        negociosEncontrados.add(negocio)
                                    }
                                }
                                productosEncontradosAdapter.notifyDataSetChanged()
                                negociosEncontradosAdapter.notifyDataSetChanged()
                                binding.progressBar.visibility = View.GONE
                                updateSearchVisibility()
                            }
                        }
                    }

                    if (negociosEncontrados.isEmpty() && productosEncontrados.isEmpty()) {
                        productosEncontradosAdapter.notifyDataSetChanged()
                        negociosEncontradosAdapter.notifyDataSetChanged()
                        binding.progressBar.visibility = View.GONE
                        binding.noResultsText.visibility = View.VISIBLE
                        binding.noResultsText.text = "No se encontraron productos en promoción"
                        updateSearchVisibility()
                    }
                }
                    .addOnFailureListener {
                        binding.progressBar.visibility = View.GONE
                        showToast("Error al cargar promociones")
                        binding.noResultsText.visibility = View.VISIBLE
                        binding.noResultsText.text = "Error al cargar promociones"
                        updateSearchVisibility()
                    }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                showToast("Error al cargar promociones")
                binding.noResultsText.visibility = View.VISIBLE
                binding.noResultsText.text = "Error al cargar promociones"
                updateSearchVisibility()
            }
    }

    private fun setupCategoriasRecyclerView() {
        categoriaAdapter = CategoriaAdapter(categorias) { categoria ->
            if (categoria.id.isNotBlank() && categoria.nombre.isNotBlank()) {
                val intent = Intent(this, NegociosActivity::class.java).apply {
                    putExtra("CATEGORIA_ID", categoria.id)
                    putExtra("CATEGORIA_NOMBRE", categoria.nombre)
                }
                startActivity(intent)
            } else {
                showToast("Error: Categoría inválida")
            }
        }
        binding.recyclerViewCategorias.apply {
            layoutManager = GridLayoutManager(this@HomeActivity, 2)
            adapter = categoriaAdapter
        }
    }

    private fun setupSearchAdapters() {
        productosEncontradosAdapter = ProductoAdapter(
            productosEncontrados,
            onProductoClick = { producto -> addToCart(producto) },
            onAddToCartClick = { producto -> addToCart(producto) }
        )
        negociosEncontradosAdapter = NegocioAdapter(negociosEncontrados) { negocio ->
            if (negocio.id.isNotBlank() && negocio.nombre.isNotBlank() && negocio.categoriaId.isNotBlank()) {
                currentNegocio = negocio
                isSearching = false
                isShowingNegocioProductos = true
                cargarProductosDeNegocio(negocio)
            } else {
                showToast("Error: Datos del negocio incompletos")
            }
        }
        binding.recyclerViewCategorias.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = productosEncontradosAdapter
        }
        binding.recyclerViewProducts.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = negociosEncontradosAdapter
        }
    }

    private fun cargarCategorias() {
        binding.progressBar.visibility = View.VISIBLE
        binding.noResultsText.visibility = View.GONE
        db.collection("categorias").get()
            .addOnSuccessListener { snapshot ->
                categorias.clear()
                for (document in snapshot.documents) {
                    val categoria = document.toObject(Categoria::class.java)?.copy(id = document.id)
                    if (categoria != null && categoria.nombre.isNotBlank() && categoria.id.isNotBlank()) {
                        categorias.add(categoria)
                    }
                }
                categoriaAdapter.notifyDataSetChanged()
                binding.progressBar.visibility = View.GONE
                updateSearchVisibility()
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                showToast("Error al cargar categorías")
                binding.noResultsText.visibility = View.VISIBLE
                binding.noResultsText.text = "Error al cargar categorías"
                updateSearchVisibility()
            }
    }

    private fun setupSearch() {
        binding.searchEditText.setOnEditorActionListener { _, _, _ ->
            val query = binding.searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                realizarBusqueda(query)
            } else {
                limpiarBusqueda()
            }
            true
        }
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.isEmpty()) {
                    limpiarBusqueda()
                } else {
                    realizarBusqueda(query)
                }
            }
        })
        binding.searchEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && binding.searchEditText.text.toString().trim().isEmpty()) {
                limpiarBusqueda()
            }
        }
    }

    private fun realizarBusqueda(query: String) {
        isSearching = true
        isShowingNegocioProductos = false
        currentNegocio = null
        lastSearchQuery = query
        binding.progressBar.visibility = View.VISIBLE
        binding.noResultsText.visibility = View.GONE
        mostrarVistasBusqueda()
        binding.titleCategorias.text = "Productos encontrados"
        binding.titleDestacados.text = "Negocios encontrados"
        buscarProductos(query)
        buscarNegocios(query)
    }

    private fun buscarProductos(query: String) {
        val queryLowerCase = query.trim().lowercase().replace("\\s+".toRegex(), " ")
        productosEncontrados.clear()
        db.collection("categorias").get()
            .addOnSuccessListener { categoriasSnapshot ->
                val categorias = categoriasSnapshot.documents.map { it.id }
                if (categorias.isEmpty()) {
                    productosEncontradosAdapter.notifyDataSetChanged()
                    updateSearchVisibility()
                    return@addOnSuccessListener
                }

                var completedQueries = 0
                val totalCategorias = categorias.size

                for (categoriaId in categorias) {
                    db.collection("categorias").document(categoriaId)
                        .collection("negocios").get()
                        .addOnSuccessListener { negociosSnapshot ->
                            var completedNegocioQueries = 0
                            val totalNegocios = negociosSnapshot.documents.size

                            if (totalNegocios == 0) {
                                completedQueries++
                                if (completedQueries == totalCategorias) {
                                    productosEncontradosAdapter.notifyDataSetChanged()
                                    updateSearchVisibility()
                                }
                                return@addOnSuccessListener
                            }

                            for (negocioDoc in negociosSnapshot.documents) {
                                val negocio = negocioDoc.toObject(Negocio::class.java)?.apply {
                                    id = negocioDoc.id
                                    this.categoriaId = categoriaId
                                }

                                if (negocio != null) {
                                    db.collection("categorias").document(categoriaId)
                                        .collection("negocios").document(negocio.id)
                                        .collection("productos").get()
                                        .addOnSuccessListener { productosSnapshot ->
                                            for (productoDoc in productosSnapshot.documents) {
                                                val producto = productoDoc.toObject(Producto::class.java)
                                                producto?.let {
                                                    if (it.nombre.isNotBlank() &&
                                                        (it.nombre.lowercase().contains(queryLowerCase) ||
                                                                it.descripcion?.lowercase()?.contains(queryLowerCase) == true)) {
                                                        producto.id = productoDoc.id
                                                        producto.categoriaId = categoriaId
                                                        producto.comercioId = negocio.id
                                                        if (!productosEncontrados.any { existing ->
                                                                existing.id == producto.id &&
                                                                        existing.comercioId == producto.comercioId
                                                            }) {
                                                            productosEncontrados.add(producto)
                                                        }
                                                        if (!negociosEncontrados.any { existing ->
                                                                existing.id == negocio.id &&
                                                                        existing.categoriaId == negocio.categoriaId
                                                            }) {
                                                            negociosEncontrados.add(negocio)
                                                        }
                                                    }
                                                }
                                            }

                                            completedNegocioQueries++
                                            if (completedNegocioQueries == totalNegocios) {
                                                completedQueries++
                                                if (completedQueries == totalCategorias) {
                                                    productosEncontradosAdapter.notifyDataSetChanged()
                                                    negociosEncontradosAdapter.notifyDataSetChanged()
                                                    updateSearchVisibility()
                                                }
                                            }
                                        }
                                        .addOnFailureListener {
                                            completedNegocioQueries++
                                            if (completedNegocioQueries == totalNegocios) {
                                                completedQueries++
                                                if (completedQueries == totalCategorias) {
                                                    productosEncontradosAdapter.notifyDataSetChanged()
                                                    negociosEncontradosAdapter.notifyDataSetChanged()
                                                    updateSearchVisibility()
                                                }
                                            }
                                        }
                                } else {
                                    completedNegocioQueries++
                                    if (completedNegocioQueries == totalNegocios) {
                                        completedQueries++
                                        if (completedQueries == totalCategorias) {
                                            productosEncontradosAdapter.notifyDataSetChanged()
                                            negociosEncontradosAdapter.notifyDataSetChanged()
                                            updateSearchVisibility()
                                        }
                                    }
                                }
                            }
                        }
                        .addOnFailureListener {
                            completedQueries++
                            if (completedQueries == totalCategorias) {
                                productosEncontradosAdapter.notifyDataSetChanged()
                                negociosEncontradosAdapter.notifyDataSetChanged()
                                updateSearchVisibility()
                            }
                        }
                }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                showToast("Error al buscar productos")
                updateSearchVisibility()
            }
    }

    private fun buscarNegocios(query: String) {
        val queryLowerCase = query.trim().lowercase().replace("\\s+".toRegex(), " ")
        negociosEncontrados.clear()
        db.collection("categorias").get()
            .addOnSuccessListener { categoriasSnapshot ->
                val categorias = categoriasSnapshot.documents.map { it.id }
                if (categorias.isEmpty()) {
                    negociosEncontradosAdapter.notifyDataSetChanged()
                    updateSearchVisibility()
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
                                    if (it.id.isNotBlank() && it.nombre.isNotBlank() && it.nombre.lowercase().contains(queryLowerCase)) {
                                        negocio.id = doc.id
                                        negocio.categoriaId = categoriaId
                                        if (!negociosEncontrados.any { existing -> existing.id == negocio.id && existing.categoriaId == negocio.categoriaId }) {
                                            negociosEncontrados.add(negocio)
                                        }
                                    }
                                }
                            }
                            completedQueries++
                            if (completedQueries == categorias.size) {
                                negociosEncontradosAdapter.notifyDataSetChanged()
                                updateSearchVisibility()
                            }
                        }
                        .addOnFailureListener {
                            completedQueries++
                            if (completedQueries == categorias.size) {
                                negociosEncontradosAdapter.notifyDataSetChanged()
                                updateSearchVisibility()
                            }
                        }
                }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                showToast("Error al buscar negocios")
                updateSearchVisibility()
            }
    }

    private fun cargarProductosDeNegocio(negocio: Negocio) {
        binding.progressBar.visibility = View.VISIBLE
        binding.noResultsText.visibility = View.GONE
        binding.titleCategorias.text = "Productos de ${negocio.nombre}"
        productosEncontrados.clear()
        db.collection("categorias")
            .document(negocio.categoriaId)
            .collection("negocios")
            .document(negocio.id)
            .collection("productos")
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    val producto = doc.toObject(Producto::class.java)
                    producto?.let {
                        if (it.nombre.isNotBlank()) {
                            producto.id = doc.id
                            productosEncontrados.add(it)
                        }
                    }
                }
                productosEncontradosAdapter.notifyDataSetChanged()
                binding.progressBar.visibility = View.GONE
                updateSearchVisibility()
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                showToast("Error al cargar productos")
                updateSearchVisibility()
            }
    }


    private fun addToCart(producto: Producto) {
        val usuarioId = auth.currentUser?.uid
        if (usuarioId == null) {
            showToast("Debe iniciar sesión para añadir al carrito")
            return
        }

        val negocioInfo = if (isShowingNegocioProductos && currentNegocio != null) {
            Triple(currentNegocio!!.nombre, currentNegocio!!.imagenUrl ?: "",
                Pair(currentNegocio!!.id, currentNegocio!!.categoriaId))
        } else if (producto.comercioId.isNotBlank() && producto.categoriaId.isNotBlank()) {
            val negocio = negociosEncontrados.find {
                it.id == producto.comercioId && it.categoriaId == producto.categoriaId
            }
            if (negocio != null) {
                Triple(negocio.nombre, negocio.imagenUrl ?: "",
                    Pair(negocio.id, negocio.categoriaId))
            } else {
                buscarNegocioParaProducto(usuarioId, producto)
                return
            }
        } else {
            buscarNegocioParaProducto(usuarioId, producto)
            return
        }

        saveToCart(usuarioId, producto, negocioInfo.first, negocioInfo.second,
            negocioInfo.third.first, negocioInfo.third.second)
    }

    private fun buscarNegocioParaProducto(usuarioId: String, producto: Producto) {
        db.collection("categorias").get()
            .addOnSuccessListener { categoriasSnapshot ->
                val categorias = categoriasSnapshot.documents.map { it.id }
                var completedQueries = 0
                var foundNegocio: Triple<String, String, Pair<String, String>>? = null

                for (categoriaId in categorias) {
                    db.collection("categorias").document(categoriaId)
                        .collection("negocios").get()
                        .addOnSuccessListener { negociosSnapshot ->
                            var completedNegocioQueries = 0
                            val totalNegocios = negociosSnapshot.documents.size

                            for (negocioDoc in negociosSnapshot.documents) {
                                db.collection("categorias").document(categoriaId)
                                    .collection("negocios").document(negocioDoc.id)
                                    .collection("productos").document(producto.id)
                                    .get()
                                    .addOnSuccessListener { productoDoc ->
                                        if (productoDoc.exists() && foundNegocio == null) {
                                            val negocioNombre = negocioDoc.getString("nombre") ?: "Desconocido"
                                            val negocioImagenUrl = negocioDoc.getString("imagenUrl") ?: ""
                                            val negocioId = negocioDoc.id
                                            foundNegocio = Triple(negocioNombre, negocioImagenUrl,
                                                Pair(negocioId, categoriaId))

                                            saveToCart(usuarioId, producto, foundNegocio!!.first,
                                                foundNegocio!!.second, foundNegocio!!.third.first,
                                                foundNegocio!!.third.second)
                                        }

                                        completedNegocioQueries++
                                        if (completedNegocioQueries == totalNegocios) {
                                            completedQueries++
                                            if (completedQueries == categorias.size && foundNegocio == null) {
                                                saveToCart(usuarioId, producto, "Desconocido", "", "", "")
                                            }
                                        }
                                    }
                                    .addOnFailureListener {
                                        completedNegocioQueries++
                                        if (completedNegocioQueries == totalNegocios) {
                                            completedQueries++
                                            if (completedQueries == categorias.size && foundNegocio == null) {
                                                saveToCart(usuarioId, producto, "Desconocido", "", "", "")
                                            }
                                        }
                                    }
                            }

                            if (totalNegocios == 0) {
                                completedQueries++
                                if (completedQueries == categorias.size && foundNegocio == null) {
                                    saveToCart(usuarioId, producto, "Desconocido", "", "", "")
                                }
                            }
                        }
                        .addOnFailureListener {
                            completedQueries++
                            if (completedQueries == categorias.size && foundNegocio == null) {
                                saveToCart(usuarioId, producto, "Desconocido", "", "", "")
                            }
                        }
                }
            }
            .addOnFailureListener {
                showToast("Error al buscar negocio")
                saveToCart(usuarioId, producto, "Desconocido", "", "", "")
            }
    }

    private fun saveToCart(usuarioId: String, producto: Producto, negocioNombre: String, negocioImagenUrl: String, negocioId: String, categoriaId: String) {
        Log.d("HomeActivity", "Agregando a carrito: ${producto.id} para usuario: $usuarioId")
        val carritoItem = CarritoItem(
            id = producto.id,
            nombre = producto.nombre,
            precio = producto.precioConDescuento(),
            imagenUrl = producto.imagenUrl ?: "",
            cantidad = 1,
            negocioNombre = negocioNombre,
            negocioId = negocioId,
            categoriaId = categoriaId,
            imagenNegocio = negocioImagenUrl
        )

        db.collection("usuarios")
            .document(usuarioId)
            .collection("carrito")
            .document(producto.id)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val currentCantidad = document.getLong("cantidad")?.toInt() ?: 1
                    db.collection("usuarios")
                        .document(usuarioId)
                        .collection("carrito")
                        .document(producto.id)
                        .update("cantidad", currentCantidad + 1)
                        .addOnSuccessListener {
                            finalizeAddToCart(producto)
                        }
                        .addOnFailureListener {
                            showToast("Error al actualizar carrito")
                        }
                } else {
                    db.collection("usuarios")
                        .document(usuarioId)
                        .collection("carrito")
                        .document(producto.id)
                        .set(carritoItem)
                        .addOnSuccessListener {
                            finalizeAddToCart(producto)
                        }
                        .addOnFailureListener {
                            showToast("Error al añadir al carrito")
                        }
                }
            }
            .addOnFailureListener {
                showToast("Error al verificar carrito")
            }
    }

    private fun finalizeAddToCart(producto: Producto) {
        showToast("Producto agregado al carrito")
        actualizarBadgeCarrito()
    }

    private fun mostrarVistasBusqueda() {
        binding.recyclerViewCategorias.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = productosEncontradosAdapter
        }
        binding.recyclerViewProducts.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = negociosEncontradosAdapter
        }
    }

    private fun limpiarBusqueda() {
        if (!isSearching && !isShowingNegocioProductos) return
        isSearching = false
        isShowingNegocioProductos = false
        currentNegocio = null
        lastSearchQuery = null
        binding.progressBar.visibility = View.GONE
        binding.noResultsText.visibility = View.GONE
        binding.titleCategorias.text = "Categorías"
        binding.titleDestacados.text = "Negocios"
        binding.titleCategorias.visibility = View.VISIBLE
        binding.recyclerViewCategorias.apply {
            layoutManager = GridLayoutManager(this@HomeActivity, 2)
            adapter = categoriaAdapter
        }
        binding.recyclerViewProducts.visibility = View.GONE
        binding.titleDestacados.visibility = View.GONE
        productosEncontrados.clear()
        negociosEncontrados.clear()
        productosEncontradosAdapter.notifyDataSetChanged()
        negociosEncontradosAdapter.notifyDataSetChanged()
        binding.searchEditText.setText("")
        binding.searchEditText.clearFocus()
        updateSearchVisibility()
    }

    private fun updateSearchVisibility() {
        binding.progressBar.visibility = View.GONE
        if (isShowingNegocioProductos) {
            binding.titleCategorias.visibility = if (productosEncontrados.isNotEmpty()) View.VISIBLE else View.GONE
            binding.recyclerViewCategorias.visibility = if (productosEncontrados.isNotEmpty()) View.VISIBLE else View.GONE
            binding.titleDestacados.visibility = View.GONE
            binding.recyclerViewProducts.visibility = View.GONE
            binding.noResultsText.visibility = if (productosEncontrados.isEmpty()) View.VISIBLE else View.GONE
            if (productosEncontrados.isEmpty()) {
                binding.noResultsText.text = "Este negocio no tiene productos disponibles"
            }
        } else if (isSearching) {
            binding.titleCategorias.visibility = if (productosEncontrados.isNotEmpty()) View.VISIBLE else View.GONE
            binding.recyclerViewCategorias.visibility = if (productosEncontrados.isNotEmpty()) View.VISIBLE else View.GONE
            binding.titleDestacados.visibility = if (negociosEncontrados.isNotEmpty()) View.VISIBLE else View.GONE
            binding.recyclerViewProducts.visibility = if (negociosEncontrados.isNotEmpty()) View.VISIBLE else View.GONE
            binding.noResultsText.visibility = if (productosEncontrados.isEmpty() && negociosEncontrados.isEmpty()) View.VISIBLE else View.GONE
            if (productosEncontrados.isEmpty() && negociosEncontrados.isEmpty()) {
                binding.noResultsText.text = "No se encontraron resultados"
            }
        } else {
            binding.titleCategorias.visibility = if (categorias.isNotEmpty()) View.VISIBLE else View.GONE
            binding.recyclerViewCategorias.visibility = if (categorias.isNotEmpty()) View.VISIBLE else View.GONE
            binding.titleDestacados.visibility = View.GONE
            binding.recyclerViewProducts.visibility = View.GONE
            binding.noResultsText.visibility = if (categorias.isEmpty()) View.VISIBLE else View.GONE
            if (categorias.isEmpty()) {
                binding.noResultsText.text = "No se encontraron categorías"
            }
            binding.recyclerViewCategorias.apply {
                layoutManager = GridLayoutManager(this@HomeActivity, 2)
                adapter = categoriaAdapter
            }
        }
    }

    override fun onBackPressed() {
        if (isShowingNegocioProductos) {
            isShowingNegocioProductos = false
            currentNegocio = null
            productosEncontrados.clear()
            negociosEncontrados.clear()
            productosEncontradosAdapter.notifyDataSetChanged()
            negociosEncontradosAdapter.notifyDataSetChanged()
            if (lastSearchQuery != null) {
                realizarBusqueda(lastSearchQuery!!)
            } else {
                limpiarBusqueda()
            }
        } else if (isSearching) {
            limpiarBusqueda()
        } else {
            super.onBackPressed()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}