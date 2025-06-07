package pe.edu.trujidelivery.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.google.firebase.firestore.FirebaseFirestore
import pe.edu.trujidelivery.R
import pe.edu.trujidelivery.adapters.ProductoAdapter

import pe.edu.trujidelivery.modelo.Producto

class ProductosPorCategoriaActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductoAdapter
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_productos_por_categoria)

        recyclerView = findViewById(R.id.recyclerViewCategoria)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ProductoAdapter(
            mutableListOf(),
            onProductoClick = { producto ->
                Toast.makeText(this, "Producto seleccionado: ${producto.nombre}", Toast.LENGTH_SHORT).show()
            },
            onAddToCartClick = { producto ->
                Toast.makeText(this, "Agregar al carrito: ${producto.nombre}", Toast.LENGTH_SHORT).show()
            }
        )
        recyclerView.adapter = adapter

        db = FirebaseFirestore.getInstance()

        val categoriaId = intent.getStringExtra("categoriaId")
        val categoriaNombre = intent.getStringExtra("categoriaNombre")
        title = categoriaNombre ?: "Productos"

        if (categoriaId != null) {
            cargarProductosPorCategoria(categoriaId)
        } else {
            Toast.makeText(this, "Categoría no válida", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun cargarProductosPorCategoria(categoriaId: String) {
        db.collection("categorias").document(categoriaId)
            .collection("productos")
            .get()
            .addOnSuccessListener { result ->
                val productos = result.mapNotNull { doc ->
                    Producto(
                        id = doc.id,
                        nombre = doc.getString("nombre") ?: "",
                        descripcion = doc.getString("descripcion") ?: "",
                        precio = doc.getDouble("precio") ?: 0.0,
                        imagenUrl = doc.getString("imagenUrl") ?: ""
                    )
                }
                adapter.updateProducto(productos)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar productos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
