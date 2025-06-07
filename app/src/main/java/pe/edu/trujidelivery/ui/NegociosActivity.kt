package pe.edu.trujidelivery.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore

import pe.edu.trujidelivery.R
import pe.edu.trujidelivery.adapters.NegocioAdapter
import pe.edu.trujidelivery.databinding.ActivityNegociosBinding
import pe.edu.trujidelivery.pe.edu.trujidelivery.modelo.Negocio

class NegociosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNegociosBinding
    private val db = FirebaseFirestore.getInstance()
    private val negocios = mutableListOf<Negocio>()
    private val negociosFiltrados = mutableListOf<Negocio>()
    private lateinit var negocioAdapter: NegocioAdapter

    private var categoriaId: String = ""
    private var categoriaNombre: String = ""

    private var ordenarAscendente = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNegociosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        categoriaId = intent.getStringExtra("CATEGORIA_ID") ?: ""
        categoriaNombre = intent.getStringExtra("CATEGORIA_NOMBRE") ?: ""

        setupRecyclerView()
        setupFiltroYBusqueda()
        setupBanner()
        cargarNegocios()
    }

    private fun setupRecyclerView() {
        negocioAdapter = NegocioAdapter(negociosFiltrados) { negocio ->
            val intent = Intent(this, ProductosActivity::class.java).apply {
                putExtra("NEGOCIO_ID", negocio.id)
                putExtra("NEGOCIO_NOMBRE", negocio.nombre)
                putExtra("CATEGORIA_ID", categoriaId)
            }
            startActivity(intent)
        }
        binding.recyclerViewNegocios.apply {
            layoutManager = LinearLayoutManager(this@NegociosActivity)
            adapter = negocioAdapter
        }
    }

    private fun setupFiltroYBusqueda() {
        binding.editTextBuscar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filtrarNegocios(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.filterEntregaGratis.setOnClickListener { mostrarTodosLosNegocios() }
        binding.filterMasRapido.setOnClickListener { ordenarPorNombre() }
        binding.filterMejorValorado.setOnClickListener { ordenarPorNombreDesc() }
        binding.filterOrdenar.setOnClickListener {
            if (ordenarAscendente) {
                ordenarPorNombre()
            } else {
                ordenarPorNombreDesc()
            }
            ordenarAscendente = !ordenarAscendente
        }
    }

    private fun setupBanner() {
        binding.closeButton.setOnClickListener {
            binding.promotionalBanner.visibility = View.GONE
        }
    }

    private fun cargarNegocios() {
        db.collection("categorias")
            .document(categoriaId)
            .collection("negocios")
            .get()
            .addOnSuccessListener { snapshot ->
                negocios.clear()
                negociosFiltrados.clear()

                for (document in snapshot.documents) {
                    val negocio = document.toObject(Negocio::class.java)
                    if (negocio != null) {
                        negocio.id = document.id
                        negocios.add(negocio)
                    }
                }

                negociosFiltrados.addAll(negocios)
                negocioAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("NegociosActivity", "Error al cargar negocios", e)
            }
    }

    private fun filtrarNegocios(query: String) {
        negociosFiltrados.clear()
        if (query.isEmpty()) {
            negociosFiltrados.addAll(negocios)
        } else {
            negociosFiltrados.addAll(
                negocios.filter { it.nombre.contains(query, ignoreCase = true) }
            )
        }
        negocioAdapter.notifyDataSetChanged()
    }

    private fun mostrarTodosLosNegocios() {
        negociosFiltrados.clear()
        negociosFiltrados.addAll(negocios)
        negocioAdapter.notifyDataSetChanged()
    }

    private fun ordenarPorNombre() {
        negociosFiltrados.sortBy { it.nombre.lowercase() }
        negocioAdapter.notifyDataSetChanged()
    }

    private fun ordenarPorNombreDesc() {
        negociosFiltrados.sortByDescending { it.nombre.lowercase() }
        negocioAdapter.notifyDataSetChanged()
    }
}
