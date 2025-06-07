package pe.edu.trujidelivery.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import pe.edu.trujidelivery.pe.edu.trujidelivery.modelo.Negocio

class NegocioViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _negocios = MutableLiveData<List<Negocio>>()
    val negocios: LiveData<List<Negocio>> = _negocios

    fun fetchNegocios(categoriaId: String) {
        db.collection("categorias").document(categoriaId).collection("negocios")
            .get()
            .addOnSuccessListener { result ->
                val listaNegocios = result.documents.mapNotNull { doc ->
                    doc.toObject(Negocio::class.java)
                }
                _negocios.value = listaNegocios
                Log.d("NegocioViewModel", "Negocios obtenidos: ${listaNegocios.size}")
            }
            .addOnFailureListener { e ->
                Log.e("NegocioViewModel", "Error al obtener negocios", e)
            }
    }
}