package pe.edu.trujidelivery.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import pe.edu.trujidelivery.modelo.Categoria

class CategoriaViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _categorias = MutableLiveData<List<Categoria>>()
    val categorias: LiveData<List<Categoria>> = _categorias

    fun fetchCategorias() {
        db.collection("categorias")
            .get()
            .addOnSuccessListener { result ->
                val listaCategorias = result.documents.mapNotNull { doc ->
                    doc.toObject(Categoria::class.java)
                }
                _categorias.value = listaCategorias
                Log.d("CategoriaViewModel", "Categorías obtenidas: ${listaCategorias.size}")
            }
            .addOnFailureListener { e ->
                Log.e("CategoriaViewModel", "Error al obtener categorías", e)
            }
    }
}