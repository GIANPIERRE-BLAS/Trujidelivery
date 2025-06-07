package pe.edu.trujidelivery.pagos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import pe.edu.trujidelivery.AddressActivity
import pe.edu.trujidelivery.R
import pe.edu.trujidelivery.seguimientos.SeguimientoPedidoActivity
import java.util.*

class ConfirmarPedidoActivity : AppCompatActivity() {

    data class Producto(
        val nombre: String,
        val cantidad: Int,
        val precio: Double,
        val imagenUrl: String? = null
    )

    data class Negocio(
        val nombre: String,
        val productos: MutableList<Producto>,
        val imagenUrl: String? = null,
        val logo: String? = null
    )

    private lateinit var textDireccion: TextView
    private lateinit var containerNegocios: LinearLayout
    private lateinit var containerProductos: LinearLayout
    private lateinit var radioGroupEnvio: RadioGroup
    private lateinit var btnEfectivo: LinearLayout
    private lateinit var btnYape: LinearLayout
    private lateinit var btnConfirmarPedido: Button
    private lateinit var btnEditarDireccion: Button
    private lateinit var textSubtotal: TextView
    private lateinit var textEnvio: TextView
    private lateinit var textServicio: TextView
    private lateinit var textTotal: TextView

    private var metodoPagoSeleccionado: String? = null
    private var datosPago: Pair<String, String>? = null
    private var costoEnvio = 0.0
    private val costoServicio = 1.0
    private var direccion: String = ""
    private var negocios: MutableList<Negocio> = mutableListOf()

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val direccionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("ConfirmarPedido", "direccionLauncher result: ${result.resultCode}")
        if (result.resultCode == RESULT_OK) {
            val nuevaDireccion = result.data?.getStringExtra("direccion")
            Log.d("ConfirmarPedido", "Nueva dirección: $nuevaDireccion")
            if (!nuevaDireccion.isNullOrEmpty() && nuevaDireccion != "Seleccionar dirección") {
                direccion = nuevaDireccion
                textDireccion.text = direccion
                val sharedPref = getSharedPreferences("pedido_data", Context.MODE_PRIVATE)
                sharedPref.edit().putString("direccion_temporal", direccion).apply()
                actualizarResumen()
            } else {
                Toast.makeText(this, "Por favor selecciona una dirección válida", Toast.LENGTH_SHORT).show()
                abrirPantallaDireccion()
            }
        } else {
            Toast.makeText(this, "Debe seleccionar una dirección válida", Toast.LENGTH_SHORT).show()
            abrirPantallaDireccion()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pago)
        supportActionBar?.hide()
        initViews()
        recibirDatosIntent()
        configurarUI()
        validarDireccionInicial()
        actualizarResumen()
    }

    override fun onResume() {
        super.onResume()
        val sharedPref = getSharedPreferences("pedido_data", Context.MODE_PRIVATE)
        val direccionTemporal = sharedPref.getString("direccion_temporal", null)
        Log.d("ConfirmarPedido", "Dirección temporal en onResume: $direccionTemporal")
        if (!direccionTemporal.isNullOrEmpty() && direccionTemporal != "Seleccionar dirección" && direccionTemporal != direccion) {
            direccion = direccionTemporal
            textDireccion.text = direccion
            actualizarResumen()
        } else if (direccion.isBlank()) {
            abrirPantallaDireccion()
        }
    }

    private fun initViews() {
        textDireccion = findViewById(R.id.textDireccion)
        containerNegocios = findViewById(R.id.containerNegocios)
        containerProductos = findViewById(R.id.containerProductos)
        radioGroupEnvio = findViewById(R.id.radioGroupEnvio)
        btnEfectivo = findViewById(R.id.btnEfectivo)
        btnYape = findViewById(R.id.btnYape)
        btnConfirmarPedido = findViewById(R.id.btnConfirmarPedido)
        btnEditarDireccion = findViewById(R.id.btnEditarDireccion)
        textSubtotal = findViewById(R.id.textSubtotal)
        textEnvio = findViewById(R.id.textEnvio)
        textServicio = findViewById(R.id.textServicio)
        textTotal = findViewById(R.id.textTotal)
    }

    private fun recibirDatosIntent() {
        val direccionRecibida = intent.getStringExtra("direccion") ?: ""
        Log.d("ConfirmarPedido", "Dirección recibida: $direccionRecibida")
        direccion = if (direccionRecibida.isBlank() || direccionRecibida == "Seleccionar dirección") "" else direccionRecibida
        Log.d("ConfirmarPedido", "Dirección asignada: $direccion")

        val productosNombres = intent.getStringArrayListExtra("productos_nombres") ?: arrayListOf()
        val productosCantidades = intent.getIntArrayExtra("productos_cantidades") ?: intArrayOf()
        val productosPrecios = intent.getDoubleArrayExtra("productos_precios") ?: doubleArrayOf()
        val productosImagenes = intent.getStringArrayListExtra("productos_imagenes") ?: arrayListOf()
        val negociosIds = intent.getStringArrayListExtra("negocios_ids") ?: arrayListOf()
        val negociosNombres = intent.getStringArrayListExtra("negocios_nombres") ?: arrayListOf()
        val negociosImagenes = intent.getStringArrayListExtra("negocios_imagenes") ?: arrayListOf()

        val negociosMap = mutableMapOf<String, Negocio>()
        for (i in productosNombres.indices) {
            if (i < productosCantidades.size && i < productosPrecios.size) {
                val negocioId = if (i < negociosIds.size) negociosIds[i] else "unknown"
                val negocioNombre = if (i < negociosNombres.size) negociosNombres[i] else "Negocio no disponible"
                val imagenNegocio = if (i < negociosImagenes.size) negociosImagenes[i] else ""
                val imagenProducto = if (i < productosImagenes.size) productosImagenes[i] else null

                val producto = Producto(
                    nombre = productosNombres[i],
                    cantidad = productosCantidades[i],
                    precio = productosPrecios[i],
                    imagenUrl = imagenProducto
                )

                if (negociosMap.containsKey(negocioId)) {
                    negociosMap[negocioId]?.productos?.add(producto)
                } else {
                    negociosMap[negocioId] = Negocio(
                        nombre = negocioNombre,
                        productos = mutableListOf(producto),
                        imagenUrl = imagenNegocio
                    )
                }
            }
        }

        negocios = negociosMap.values.toMutableList()
        Log.d("ConfirmarPedido", "Negocios cargados: ${negocios.map { it.nombre }}")
    }

    private fun configurarUI() {
        textDireccion.text = if (direccion.isBlank()) "Selecciona una dirección" else direccion

        mostrarNegocios()
        mostrarProductos()

        btnEditarDireccion.setOnClickListener {
            abrirPantallaDireccion()
        }

        radioGroupEnvio.setOnCheckedChangeListener { _, checkedId ->
            costoEnvio = when (checkedId) {
                R.id.radioPrioritario -> 4.0
                else -> 0.0
            }
            actualizarResumen()
        }
        radioGroupEnvio.check(R.id.radioBasico)

        btnEfectivo.setOnClickListener { mostrarDialogoEfectivo() }
        btnYape.setOnClickListener { mostrarDialogoYape() }

        btnConfirmarPedido.setOnClickListener { confirmarPedido() }
    }

    private fun validarDireccionInicial() {
        if (direccion.isBlank()) {
            val sharedPref = getSharedPreferences("pedido_data", Context.MODE_PRIVATE)
            sharedPref.edit().remove("direccion_temporal").apply()
            abrirPantallaDireccion()
        }
    }

    private fun mostrarNegocios() {
        containerNegocios.removeAllViews()
        for (negocio in negocios) {
            val negocioView = layoutInflater.inflate(R.layout.item_negocio_resumen, containerNegocios, false)
            val ivLogoNegocio = negocioView.findViewById<ImageView>(R.id.ivLogoNegocio)
            val tvNombreNegocio = negocioView.findViewById<TextView>(R.id.tvNombreNegocio)

            tvNombreNegocio.text = negocio.nombre
            if (!negocio.imagenUrl.isNullOrEmpty()) {
                if (negocio.imagenUrl.startsWith("http")) {
                    Glide.with(this)
                        .load(negocio.imagenUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .circleCrop()
                        .into(ivLogoNegocio)
                } else {
                    val resId = resources.getIdentifier(negocio.imagenUrl, "drawable", packageName)
                    ivLogoNegocio.setImageResource(if (resId != 0) resId else R.drawable.placeholder_image)
                }
            } else {
                ivLogoNegocio.setImageResource(R.drawable.placeholder_image)
            }

            containerNegocios.addView(negocioView)
        }
    }

    private fun mostrarProductos() {
        containerProductos.removeAllViews()
        for (negocio in negocios) {
            val negocioTitle = TextView(this).apply {
                text = negocio.nombre
                textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(resources.getColor(android.R.color.black))
                setPadding(8, 8, 8, 4)
            }
            containerProductos.addView(negocioTitle)

            for (producto in negocio.productos) {
                val itemView = layoutInflater.inflate(R.layout.item_producto_resumen, containerProductos, false)

                val ivImagen = itemView.findViewById<ImageView>(R.id.ivImagenProducto)
                val tvNombre = itemView.findViewById<TextView>(R.id.tvNombreProducto)
                val tvCantidad = itemView.findViewById<TextView>(R.id.tvCantidadProducto)
                val tvPrecio = itemView.findViewById<TextView>(R.id.tvPrecioProducto)

                if (!producto.imagenUrl.isNullOrEmpty()) {
                    if (producto.imagenUrl.startsWith("http")) {
                        Glide.with(this)
                            .load(producto.imagenUrl)
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.placeholder_image)
                            .centerCrop()
                            .into(ivImagen)
                    } else {
                        val resId = resources.getIdentifier(producto.imagenUrl, "drawable", packageName)
                        ivImagen.setImageResource(if (resId != 0) resId else R.drawable.placeholder_image)
                    }
                } else {
                    ivImagen.setImageResource(R.drawable.placeholder_image)
                }

                tvNombre.text = producto.nombre
                tvCantidad.text = "x${producto.cantidad}"
                val precioTotal = producto.cantidad * producto.precio
                tvPrecio.text = "S/ %.2f".format(precioTotal)

                containerProductos.addView(itemView)
            }
        }
    }

    private fun abrirPantallaDireccion() {
        val intent = Intent(this, AddressActivity::class.java).apply {
            putExtra("direccion_actual", if (direccion.isBlank()) "" else direccion)
            putExtra("user_email", auth.currentUser?.email)
            putExtra("name", auth.currentUser?.displayName ?: "")
            putExtra("es_pago", true)
        }
        direccionLauncher.launch(intent)
    }

    private fun mostrarDialogoEfectivo() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_pago_efectivo, null)
        val etCantidad = dialogView.findViewById<TextInputEditText>(R.id.etCantidadEfectivo)
        val total = negocios.sumOf { negocio -> negocio.productos.sumOf { it.precio * it.cantidad } } + costoEnvio + costoServicio
        dialogView.findViewById<TextView>(R.id.tvTotalEfectivo).text = "Total a pagar: S/ %.2f".format(total)

        AlertDialog.Builder(this)
            .setTitle("Pagar con Efectivo")
            .setView(dialogView)
            .setPositiveButton("Confirmar") { _, _ ->
                val cantidadStr = etCantidad.text.toString()
                if (cantidadStr.isNotBlank()) {
                    try {
                        val cantidad = cantidadStr.toDouble()
                        if (cantidad >= total) {
                            seleccionarMetodoPago("Efectivo", cantidadStr)
                        } else {
                            Toast.makeText(this, "La cantidad debe ser mayor o igual al total", Toast.LENGTH_SHORT).show()
                            mostrarDialogoEfectivo()
                        }
                    } catch (e: NumberFormatException) {
                        Toast.makeText(this, "Ingrese una cantidad válida", Toast.LENGTH_SHORT).show()
                        mostrarDialogoEfectivo()
                    }
                } else {
                    Toast.makeText(this, "Ingrese la cantidad con la que pagará", Toast.LENGTH_SHORT).show()
                    mostrarDialogoEfectivo()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoYape() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_pago_yape, null)
        val etNumeroYape = dialogView.findViewById<TextInputEditText>(R.id.etNumeroYape)
        val etCodigoYape = dialogView.findViewById<TextInputEditText>(R.id.etCodigoYape)

        AlertDialog.Builder(this)
            .setTitle("Pagar con Yape")
            .setView(dialogView)
            .setPositiveButton("Confirmar") { _, _ ->
                val numeroYape = etNumeroYape.text.toString()
                val codigoYape = etCodigoYape.text.toString()
                if (numeroYape.isNotBlank() && codigoYape.isNotBlank()) {
                    if (numeroYape.length == 9 && numeroYape.all { it.isDigit() }) {
                        seleccionarMetodoPago("Yape", "$numeroYape|$codigoYape")
                    } else {
                        Toast.makeText(this, "El número de Yape debe tener 9 dígitos", Toast.LENGTH_SHORT).show()
                        mostrarDialogoYape()
                    }
                } else {
                    Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
                    mostrarDialogoYape()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun seleccionarMetodoPago(metodo: String, datos: String = "") {
        metodoPagoSeleccionado = metodo
        datosPago = Pair(metodo, datos)
        btnEfectivo.setBackgroundResource(
            if (metodo == "Efectivo") R.drawable.boton_seleccionado
            else R.drawable.boton_no_seleccionado
        )
        btnYape.setBackgroundResource(
            if (metodo == "Yape") R.drawable.boton_seleccionado
            else R.drawable.boton_no_seleccionado
        )
        Toast.makeText(this, "Método de pago seleccionado: $metodo", Toast.LENGTH_SHORT).show()
    }

    private fun actualizarResumen() {
        val subtotal = negocios.sumOf { negocio -> negocio.productos.sumOf { it.precio * it.cantidad } }
        textSubtotal.text = "Subtotal: S/ %.2f".format(subtotal)
        textEnvio.text = "Envío: S/ %.2f".format(costoEnvio)
        textServicio.text = "Servicio: S/ %.2f".format(costoServicio)

        val total = subtotal + costoEnvio + costoServicio
        textTotal.text = "Total a pagar: S/ %.2f".format(total)
    }

    private fun limpiarCarritoFirestore(onComplete: () -> Unit) {
        val usuarioId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("usuarios").document(usuarioId).collection("carrito")
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit()
                    .addOnSuccessListener {
                        onComplete()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error limpiando carrito: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error obteniendo carrito: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun guardarPedidoFirestore(onComplete: (String) -> Unit) {
        val usuarioId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val productos = negocios.flatMap { negocio ->
            negocio.productos.map { producto ->
                mapOf(
                    "nombre" to producto.nombre,
                    "cantidad" to producto.cantidad,
                    "precio" to producto.precio,
                    "imagenUrl" to producto.imagenUrl,
                    "negocioNombre" to negocio.nombre,
                    "imagenNegocio" to negocio.imagenUrl
                )
            }
        }

        val pedido = hashMapOf(
            "negocios" to negocios.map { negocio ->
                mapOf(
                    "nombre" to negocio.nombre,
                    "imagenUrl" to negocio.imagenUrl,
                    "productos" to negocio.productos.map { producto ->
                        mapOf(
                            "nombre" to producto.nombre,
                            "cantidad" to producto.cantidad,
                            "precio" to producto.precio,
                            "imagenUrl" to producto.imagenUrl
                        )
                    }
                )
            },
            "subtotal" to negocios.sumOf { negocio -> negocio.productos.sumOf { it.precio * it.cantidad } },
            "costoEnvio" to costoEnvio,
            "costoServicio" to costoServicio,
            "total" to (negocios.sumOf { negocio -> negocio.productos.sumOf { it.precio * it.cantidad } } + costoEnvio + costoServicio),
            "direccion" to direccion,
            "metodoPago" to metodoPagoSeleccionado,
            "datosPago" to datosPago?.second,
            "fecha" to Date().time,
            "estado" to "confirmado",
            "fechaCreacion" to Date().time
        )

        db.collection("usuarios").document(usuarioId).collection("pedidos")
            .add(pedido)
            .addOnSuccessListener { documentReference ->
                Log.d("ConfirmarPedido", "Pedido guardado en Firestore con ID: ${documentReference.id}")
                onComplete(documentReference.id)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar pedido: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("ConfirmarPedido", "Error al guardar pedido", e)
            }
    }

    private fun confirmarPedido() {
        when {
            direccion.isBlank() -> {
                Toast.makeText(this, "Debe seleccionar una dirección válida", Toast.LENGTH_SHORT).show()
                abrirPantallaDireccion()
            }
            metodoPagoSeleccionado == null || datosPago == null -> {
                Toast.makeText(this, "Seleccione un método de pago y complete los datos", Toast.LENGTH_SHORT).show()
            }
            else -> {
                val sharedPref = getSharedPreferences("pedido_data", Context.MODE_PRIVATE)
                sharedPref.edit().remove("direccion_temporal").apply()

                val usuarioId = auth.currentUser?.uid
                if (usuarioId != null) {
                    db.collection("usuarios").document(usuarioId)
                        .update("direccion", direccion)
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error al guardar dirección: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }

                guardarPedidoFirestore { pedidoId ->
                    limpiarCarritoFirestore {
                        Toast.makeText(this, "Pedido confirmado exitosamente", Toast.LENGTH_SHORT).show()
                        // Pasar el nombre del primer negocio como negocio principal
                        val negocioNombre = if (negocios.isNotEmpty()) negocios[0].nombre else "Negocio no disponible"
                        Log.d("ConfirmarPedido", "Enviando nombre del negocio: $negocioNombre")
                        val intent = Intent(this, SeguimientoPedidoActivity::class.java).apply {
                            putExtra("pedido_id", pedidoId)
                            putExtra("direccion_entrega", direccion)
                            putExtra("negocio_nombre", negocioNombre) // Pasar el nombre correcto del negocio
                            putStringArrayListExtra("productos_nombres", ArrayList(negocios.flatMap { it.productos.map { p -> p.nombre } }))
                            putIntegerArrayListExtra("productos_cantidades", ArrayList(negocios.flatMap { it.productos.map { p -> p.cantidad } }))
                            putExtra("productos_precios", negocios.flatMap { it.productos.map { p -> p.precio } }.toDoubleArray())
                            putStringArrayListExtra("productos_imagenes", ArrayList(negocios.flatMap { it.productos.map { p -> p.imagenUrl ?: "" } }))
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }
    }
}