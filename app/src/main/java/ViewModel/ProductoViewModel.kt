package pe.edu.trujidelivery.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import pe.edu.trujidelivery.modelo.Producto

class ProductoViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _productos = MutableLiveData<List<Producto>>()
    val productos: LiveData<List<Producto>> = _productos

    fun fetchProductos(categoriaId: String, negocioId: String) {
        db.collection("categorias").document(categoriaId)
            .collection("negocios").document(negocioId)
            .collection("productos")
            .get()
            .addOnSuccessListener { result ->
                val listaProductos = result.documents.mapNotNull { doc ->
                    doc.toObject(Producto::class.java)
                }
                _productos.value = listaProductos
                Log.d("ProductoViewModel", "Productos obtenidos: ${listaProductos.size}")
            }
            .addOnFailureListener { e ->
                Log.e("ProductoViewModel", "Error al obtener productos", e)
            }
    }
}