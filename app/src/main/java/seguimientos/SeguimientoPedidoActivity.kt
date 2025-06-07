package pe.edu.trujidelivery.seguimientos

import android.Manifest
import android.animation.ObjectAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import pe.edu.trujidelivery.R
import pe.edu.trujidelivery.notification.NotificacionesActivity
import pe.edu.trujidelivery.ui.HomeActivity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class SeguimientoPedidoActivity : AppCompatActivity() {

    data class Producto(
        val nombre: String,
        val cantidad: Int,
        val precio: Double,
        val imagenUrl: String? = null
    )

    private lateinit var mapView: MapView
    private lateinit var controller: IMapController
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEstadoPedido: TextView
    private lateinit var tvTiempoEstimado: TextView
    private lateinit var tvNombreRepartidor: TextView
    private lateinit var ivRepartidor: ImageView
    private lateinit var layoutRepartidor: LinearLayout
    private lateinit var btnLlamarRepartidor: Button
    private lateinit var btnChatRepartidor: Button
    private lateinit var btnCancelarPedido: Button
    private lateinit var btnVerDetalles: Button
    private lateinit var btnFinalizar: Button
    private lateinit var tvNegocio: TextView
    private lateinit var tvDireccionEntrega: TextView
    private lateinit var btnAyuda: FloatingActionButton
    private lateinit var tvTiempoTranscurrido: TextView
    private lateinit var tvDistanciaEstimada: TextView

    private val LOCATION_PERMISSION_REQUEST = 1001
    private val NOTIFICATION_CHANNEL_ID = "delivery_tracking"
    private val NOTIFICATION_ID_BASE = 1001
    private val TAG = "SeguimientoPedido"

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var pedidoListener: ListenerRegistration? = null
    private var repartidorListener: ListenerRegistration? = null
    private var notificationListener: ListenerRegistration? = null

    private var pedidoId = ""
    private var estadoActual = "confirmado"
    private var tiempoInicio: Long = 0L
    private var tiempoEstimadoEntrega: Long = 0L
    private var productos: List<Producto> = emptyList()

    private var ubicacionUsuario: GeoPoint? = null
    private var ubicacionNegocio: GeoPoint? = null
    private var ubicacionRepartidor: GeoPoint? = null

    private var markerUsuario: Marker? = null
    private var markerNegocio: Marker? = null
    private var markerRepartidor: Marker? = null
    private var rutaPolyline: Polyline? = null

    private val handler = Handler(Looper.getMainLooper())
    private var simulacionRunnable: Runnable? = null
    private var tiempoRunnable: Runnable? = null
    private val notificationQueue = mutableListOf<Runnable>()
    private var lastNotificationTime: Long = 0L
    private val MIN_NOTIFICATION_INTERVAL = 15000L

    private val ubicacionesNegocios = listOf(
        GeoPoint(-8.1116, -79.0287),
        GeoPoint(-8.1150, -79.0250),
        GeoPoint(-8.1090, -79.0320),
        GeoPoint(-8.1200, -79.0200)
    )

    private var repartidorActual: Map<String, String> = mapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        setContentView(R.layout.activity_seguimiento_pedido)
        supportActionBar?.hide()

        initViews()
        setupMap()
        createNotificationChannel()
        obtenerDatosIntent()
        verificarPermisos()
        verificarOCrearRepartidores()
        iniciarContadorTiempo()
        setupNotificationListener()
    }

    private fun initViews() {
        mapView = findViewById(R.id.mapView)
        progressBar = findViewById(R.id.progressBar)
        tvEstadoPedido = findViewById(R.id.tvEstadoPedido)
        tvTiempoEstimado = findViewById(R.id.tvTiempoEstimado)
        tvNombreRepartidor = findViewById(R.id.tvNombreRepartidor)
        ivRepartidor = findViewById(R.id.ivRepartidor)
        layoutRepartidor = findViewById(R.id.layoutRepartidor)
        btnLlamarRepartidor = findViewById(R.id.btnLlamarRepartidor)
        btnChatRepartidor = findViewById(R.id.btnChatRepartidor)
        btnCancelarPedido = findViewById(R.id.btnCancelarPedido)
        btnVerDetalles = findViewById(R.id.btnVerDetalles)
        btnFinalizar = findViewById(R.id.btnFinalizar)
        tvNegocio = findViewById(R.id.tvNegocio)
        tvDireccionEntrega = findViewById(R.id.tvDireccionEntrega)
        btnAyuda = findViewById(R.id.btnAyuda)
        tvTiempoTranscurrido = findViewById(R.id.tvTiempoTranscurrido)
        tvDistanciaEstimada = findViewById(R.id.tvDistanciaEstimada)

        btnAyuda.visibility = View.VISIBLE

        setupClickListeners()
    }

    private fun setupClickListeners() {
        btnCancelarPedido.setOnClickListener { mostrarDialogoCancelacion() }
        btnVerDetalles.setOnClickListener { mostrarDetallesPedido() }
        btnAyuda.setOnClickListener {
            Log.d(TAG, "Botón Ayuda clickeado")
            mostrarAyuda()
        }
        btnChatRepartidor.setOnClickListener {
            Log.d(TAG, "Botón Chat clickeado")
            abrirChat()
        }
        btnFinalizar.setOnClickListener {
            if (estadoActual == "entregado") {
                guardarHistorialPedido("entregado")
                val intent = Intent(this, HomeActivity::class.java).apply {
                    putExtra("direccion_entrega", tvDireccionEntrega.text.toString())
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "El pedido aún no ha sido entregado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        controller = mapView.controller
        controller.setZoom(15.0)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Seguimiento de Pedidos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones del estado de tu pedido"
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupNotificationListener() {
        val usuarioId = auth.currentUser?.uid ?: return
        notificationListener = db.collection("notificaciones")
            .whereEqualTo("usuario_id", usuarioId)
            .whereEqualTo("leida", false)
            .orderBy("fecha_hora", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error al escuchar notificaciones: ${e.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    for (change in snapshot.documentChanges) {
                        if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                            val notificacion = change.document.data
                            val titulo = notificacion["titulo"] as? String ?: "Notificación"
                            val mensaje = notificacion["mensaje"] as? String ?: "Tienes un nuevo mensaje"
                            val notificationId = (NOTIFICATION_ID_BASE + change.document.id.hashCode())

                            scheduleNotification(titulo, mensaje, notificationId)
                        }
                    }
                }
            }
    }

    private fun obtenerDatosIntent() {
        pedidoId = intent.getStringExtra("pedido_id") ?: UUID.randomUUID().toString()
        val negocioNombre = intent.getStringExtra("negocio_nombre") ?: "Negocio no disponible"
        val imagenNegocio = intent.getStringExtra("imagenNegocio") ?: ""
        Log.d(TAG, "Nombre del negocio recibido: $negocioNombre")
        tvNegocio.text = negocioNombre
        tvDireccionEntrega.text = intent.getStringExtra("direccion_entrega") ?: "Av. Ejército 123, Trujillo"

        val productosNombres = intent.getStringArrayListExtra("productos_nombres") ?: arrayListOf()
        val productosCantidades = intent.getIntegerArrayListExtra("productos_cantidades") ?: arrayListOf()
        val productosPrecios = intent.getDoubleArrayExtra("productos_precios") ?: doubleArrayOf()
        val productosImagenes = intent.getStringArrayListExtra("productos_imagenes") ?: arrayListOf()

        productos = productosNombres.indices.mapNotNull { i ->
            if (i < productosCantidades.size && i < productosPrecios.size) {
                val imagen = if (i < productosImagenes.size) productosImagenes[i] else null
                Producto(
                    nombre = productosNombres[i],
                    cantidad = productosCantidades[i],
                    precio = productosPrecios[i],
                    imagenUrl = imagen
                )
            } else {
                null
            }
        }

        if (productos.isEmpty()) {
            cargarProductosDesdeFirestore()
        }

        ubicacionNegocio = ubicacionesNegocios.random()
        tiempoInicio = System.currentTimeMillis()
        tiempoEstimadoEntrega = tiempoInicio + (25 * 60 * 1000)
    }

    private fun cargarProductosDesdeFirestore() {
        val usuarioId = auth.currentUser?.uid ?: return
        db.collection("historial_pedidos").document(pedidoId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val productosData = document.get("productos") as? List<Map<String, Any>> ?: emptyList()
                    productos = productosData.mapNotNull { data ->
                        val nombre = data["nombre"] as? String
                        val cantidad = (data["cantidad"] as? Long)?.toInt()
                        val precio = data["precio"] as? Double
                        val imagenUrl = data["imagenUrl"] as? String
                        if (nombre != null && cantidad != null && precio != null) {
                            Producto(nombre, cantidad, precio, imagenUrl)
                        } else {
                            null
                        }
                    }
                    Log.d(TAG, "Productos cargados desde Firestore: $productos")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al cargar productos desde Firestore: ${e.message}")
                Toast.makeText(this, "Error al cargar productos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun verificarPermisos() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        } else {
            obtenerUbicacionUsuario()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            obtenerUbicacionUsuario()
        } else {
            Toast.makeText(this, "Permiso de ubicación necesario para el seguimiento", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun obtenerUbicacionUsuario() {
        ubicacionUsuario = GeoPoint(-8.1100, -79.0300)
        calcularDistancia()
        actualizarMapa()
    }

    private fun calcularDistancia() {
        ubicacionUsuario?.let { usuario ->
            ubicacionNegocio?.let { negocio ->
                val distancia = calcularDistanciaEnKm(usuario, negocio)
                tvDistanciaEstimada.text = String.format("%.1f km", distancia)
            }
        }
    }

    private fun calcularDistanciaEnKm(punto1: GeoPoint, punto2: GeoPoint): Double {
        val radioTierra = 6371.0
        val dLat = Math.toRadians(punto2.latitude - punto1.latitude)
        val dLon = Math.toRadians(punto2.longitude - punto1.longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(punto1.latitude)) *
                cos(Math.toRadians(punto2.latitude)) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return radioTierra * c
    }

    private fun actualizarMapa() {
        mapView.overlays.clear()

        val iconSize = 32

        ubicacionUsuario?.let {
            markerUsuario = Marker(mapView).apply {
                position = it
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Tu ubicación"
                val drawableUsuario = ContextCompat.getDrawable(this@SeguimientoPedidoActivity, R.drawable.ic_location_user)
                drawableUsuario?.let { drawable ->
                    val bitmap = (drawable as BitmapDrawable).bitmap
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, iconSize, iconSize, true)
                    icon = BitmapDrawable(resources, scaledBitmap)
                }
            }
            mapView.overlays.add(markerUsuario)
        }

        ubicacionNegocio?.let {
            markerNegocio = Marker(mapView).apply {
                position = it
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = tvNegocio.text.toString()
                val drawableNegocio = ContextCompat.getDrawable(this@SeguimientoPedidoActivity, R.drawable.ic_restaurant_marker)
                drawableNegocio?.let { drawable ->
                    val bitmap = (drawable as BitmapDrawable).bitmap
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, iconSize, iconSize, true)
                    icon = BitmapDrawable(resources, scaledBitmap)
                }
            }
            mapView.overlays.add(markerNegocio)
        }

        ubicacionRepartidor?.let {
            markerRepartidor = Marker(mapView).apply {
                position = it
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Repartidor: ${repartidorActual["nombre"]}"
                val drawableRepartidor = ContextCompat.getDrawable(this@SeguimientoPedidoActivity, R.drawable.ic_delivery_marker)
                drawableRepartidor?.let { drawable ->
                    val bitmap = (drawable as BitmapDrawable).bitmap
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, iconSize, iconSize, true)
                    icon = BitmapDrawable(resources, scaledBitmap)
                }
            }
            mapView.overlays.add(markerRepartidor)
        }

        if (estadoActual in listOf("recogido", "en_camino_cliente")) {
            dibujarRuta()
        }

        ubicacionUsuario?.let { controller.setCenter(it) }
        mapView.invalidate()
    }

    private fun dibujarRuta() {
        rutaPolyline?.let { mapView.overlays.remove(it) }

        val puntos = mutableListOf<GeoPoint>()

        when (estadoActual) {
            "en_camino_negocio" -> {
                ubicacionRepartidor?.let { puntos.add(it) }
                ubicacionNegocio?.let { puntos.add(it) }
            }
            "recogido", "en_camino_cliente" -> {
                ubicacionRepartidor?.let { puntos.add(it) }
                ubicacionUsuario?.let { puntos.add(it) }
            }
        }

        if (puntos.size >= 2) {
            rutaPolyline = Polyline().apply {
                setPoints(puntos)
                color = ContextCompat.getColor(this@SeguimientoPedidoActivity, R.color.primary_color)
                width = 8f
            }
            mapView.overlays.add(rutaPolyline)
        }
    }

    private fun verificarOCrearRepartidores() {
        db.collection("repartidores")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    crearRepartidoresFicticios()
                } else {
                    asignarRepartidorDesdeFirestore()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al verificar repartidores: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun crearRepartidoresFicticios() {
        val repartidoresFicticios = listOf(
            hashMapOf(
                "nombre" to "Juan Pérez",
                "telefono" to "+51987654321",
                "foto" to "https://randomuser.me/api/portraits/men/1.jpg"
            ),
            hashMapOf(
                "nombre" to "María García",
                "telefono" to "+51912345678",
                "foto" to "https://randomuser.me/api/portraits/women/2.jpg"
            ),
            hashMapOf(
                "nombre" to "Carlos Ramírez",
                "telefono" to "+51955512345",
                "foto" to "https://randomuser.me/api/portraits/men/3.jpg"
            ),
            hashMapOf(
                "nombre" to "Ana López",
                "telefono" to "+51977798765",
                "foto" to "https://randomuser.me/api/portraits/women/4.jpg"
            )
        )

        val batch = db.batch()
        repartidoresFicticios.forEach { repartidor ->
            val docRef = db.collection("repartidores").document()
            batch.set(docRef, repartidor)
        }

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(this, "Repartidores ficticios creados", Toast.LENGTH_SHORT).show()
                asignarRepartidorDesdeFirestore()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al crear repartidores: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun asignarRepartidorDesdeFirestore() {
        db.collection("repartidores")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Toast.makeText(this, "No hay repartidores disponibles", Toast.LENGTH_LONG).show()
                    finish()
                    return@addOnSuccessListener
                }

                val repartidoresList = snapshot.documents
                val repartidorDoc = repartidoresList.random()
                repartidorActual = mapOf(
                    "id" to repartidorDoc.id,
                    "nombre" to (repartidorDoc.getString("nombre") ?: "Repartidor Desconocido"),
                    "telefono" to (repartidorDoc.getString("telefono") ?: "Sin teléfono"),
                    "foto" to (repartidorDoc.getString("foto") ?: "https://randomuser.me/api/portraits/men/1.jpg")
                )

                mostrarInfoRepartidor()
                simularProgresoEntrega()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al obtener repartidores: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun mostrarInfoRepartidor() {
        layoutRepartidor.visibility = View.VISIBLE
        tvNombreRepartidor.text = repartidorActual["nombre"]

        Glide.with(this)
            .load(repartidorActual["foto"])
            .placeholder(R.drawable.ic_repartidor_default)
            .circleCrop()
            .into(ivRepartidor)

        btnLlamarRepartidor.setOnClickListener {
            Log.d(TAG, "Botón Llamar Repartidor clickeado")
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${repartidorActual["telefono"]}"))
            startActivity(intent)
        }

        layoutRepartidor.alpha = 0f
        layoutRepartidor.animate()
            .alpha(1f)
            .setDuration(500)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun simularProgresoEntrega() {
        val estados = listOf(
            "confirmado" to "El Local recibió tu pedido",
            "preparando" to "El Local comenzó a preparar tu pedido",
            "listo" to "Tu pedido está listo",
            "asignado" to "${repartidorActual["nombre"]} fue asignado a tu pedido",
            "en_camino_negocio" to "${repartidorActual["nombre"]} va camino al restaurante",
            "llegado_negocio" to "${repartidorActual["nombre"]} llegó al restaurante",
            "recogido" to "${repartidorActual["nombre"]} recogió tu pedido",
            "en_camino_cliente" to "${repartidorActual["nombre"]} está saliendo hacia ti",
            "llegando" to "${repartidorActual["nombre"]} está llegando a tu ubicación",
            "entregado" to "¡Entrega realizada con éxito!"
        )

        var index = 0
        val intervalos = listOf(2000, 8000, 5000, 3000, 12000, 3000, 4000, 15000, 5000, 2000)

        simulacionRunnable = object : Runnable {
            override fun run() {
                if (index < estados.size) {
                    val (estado, mensaje) = estados[index]
                    actualizarEstado(estado, mensaje)

                    when (estado) {
                        "asignado" -> {
                            ubicacionRepartidor = GeoPoint(
                                ubicacionNegocio!!.latitude + (Math.random() - 0.5) * 0.01,
                                ubicacionNegocio!!.longitude + (Math.random() - 0.5) * 0.01
                            )
                            guardarNotificacion(
                                "Repartidor Asignado",
                                "Repartidor asignado: ${repartidorActual["nombre"]}"
                            )
                        }
                        "en_camino_negocio" -> {
                            animarMovimientoRepartidor(ubicacionRepartidor, ubicacionNegocio, 12000)
                        }
                        "llegado_negocio" -> {
                            ubicacionRepartidor = ubicacionNegocio
                            guardarNotificacion(
                                "Llegada al Restaurante",
                                "${repartidorActual["nombre"]} ha llegado al restaurante"
                            )
                        }
                        "recogido" -> {
                            ubicacionRepartidor = ubicacionNegocio
                        }
                        "en_camino_cliente" -> {
                            animarMovimientoRepartidor(ubicacionNegocio, ubicacionUsuario, 15000)
                            guardarNotificacion(
                                "Saliendo hacia Ti",
                                "${repartidorActual["nombre"]} está saliendo hacia tu dirección"
                            )
                        }
                        "llegando" -> {
                            guardarNotificacion(
                                "Llegando a tu Ubicación",
                                "${repartidorActual["nombre"]} está llegando a tu ubicación"
                            )
                        }
                        "entregado" -> {
                            ubicacionRepartidor = ubicacionUsuario
                            enviarNotificacionEntrega()
                            btnFinalizar.visibility = View.VISIBLE
                            btnChatRepartidor.visibility = View.GONE
                            btnCancelarPedido.visibility = View.GONE
                            btnVerDetalles.visibility = View.GONE
                            btnFinalizar.alpha = 0f
                            btnFinalizar.animate()
                                .alpha(1f)
                                .setDuration(500)
                                .setInterpolator(AccelerateDecelerateInterpolator())
                                .start()
                        }
                    }

                    actualizarMapa()

                    index++
                    if (index < intervalos.size) {
                        handler.postDelayed(this, intervalos[index].toLong())
                    }
                }
            }
        }

        handler.post(simulacionRunnable!!)
    }

    private fun scheduleNotification(title: String, message: String, notificationId: Int) {
        val currentTime = System.currentTimeMillis()
        val timeSinceLast = currentTime - lastNotificationTime
        val delay = if (timeSinceLast < MIN_NOTIFICATION_INTERVAL) {
            MIN_NOTIFICATION_INTERVAL - timeSinceLast
        } else {
            0L
        }

        val notificationRunnable = Runnable {
            enviarNotificacionLocal(title, message, notificationId)
            lastNotificationTime = System.currentTimeMillis()
            processNextNotification()
        }

        if (delay > 0) {
            notificationQueue.add(notificationRunnable)
            handler.postDelayed({ processNextNotification() }, delay)
        } else {
            notificationRunnable.run()
        }
    }

    private fun processNextNotification() {
        if (notificationQueue.isNotEmpty()) {
            val nextNotification = notificationQueue.removeAt(0)
            nextNotification.run()
        }
    }

    private fun enviarNotificacionLocal(title: String, message: String, notificationId: Int) {
        val intent = Intent(this, NotificacionesActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_delivery_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = NotificationManagerCompat.from(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(notificationId, notification)
        }
    }

    private fun animarMovimientoRepartidor(origen: GeoPoint?, destino: GeoPoint?, duracion: Long) {
        if (origen == null || destino == null) return

        val pasos = (duracion / 1000).toInt()
        var pasoActual = 0

        val animacion = object : Runnable {
            override fun run() {
                if (pasoActual <= pasos) {
                    val progreso = pasoActual.toFloat() / pasos
                    val lat = origen.latitude + (destino.latitude - origen.latitude) * progreso
                    val lon = origen.longitude + (destino.longitude - origen.longitude) * progreso

                    ubicacionRepartidor = GeoPoint(lat, lon)
                    actualizarMapa()

                    pasoActual++
                    handler.postDelayed(this, 1000)
                }
            }
        }

        handler.post(animacion)
    }

    private fun actualizarEstado(estado: String, mensaje: String) {
        estadoActual = estado
        tvEstadoPedido.text = mensaje

        val progreso = when (estado) {
            "confirmado" -> 5
            "preparando" -> 15
            "listo" -> 25
            "asignado" -> 35
            "en_camino_negocio" -> 45
            "llegado_negocio" -> 55
            "recogido" -> 65
            "en_camino_cliente" -> 80
            "llegando" -> 95
            "entregado" -> 100
            else -> 0
        }

        val animator = ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, progreso)
        animator.duration = 500
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()

        actualizarTiempoEstimado(estado)

        val usuarioId = auth.currentUser?.uid ?: return
        db.collection("historial_pedidos").document(pedidoId)
            .update("estado", estado)
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al actualizar estado en Firestore: ${e.message}")
            }

        if (estado == "entregado") {
            guardarHistorialPedido("entregado")
        }
    }

    private fun actualizarTiempoEstimado(estado: String) {
        val tiempoRestante = when (estado) {
            "confirmado", "preparando" -> "20-25 min"
            "listo", "asignado" -> "15-20 min"
            "en_camino_negocio" -> "12-15 min"
            "llegado_negocio", "recogido" -> "8-12 min"
            "en_camino_cliente" -> "5-8 min"
            "llegando" -> "1-3 min"
            "entregado" -> "¡Entregado!"
            else -> "Calculando..."
        }

        tvTiempoEstimado.text = "Tiempo estimado: $tiempoRestante"

        tvTiempoEstimado.alpha = 0.5f
        tvTiempoEstimado.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }

    private fun guardarHistorialPedido(estadoFinal: String) {
        val usuarioId = auth.currentUser?.uid ?: return
        val tiempoTranscurrido = System.currentTimeMillis() - tiempoInicio
        val productosData = productos.map { producto ->
            mapOf(
                "nombre" to producto.nombre,
                "cantidad" to producto.cantidad,
                "precio" to producto.precio,
                "imagenUrl" to (producto.imagenUrl ?: "")
            )
        }

        val subtotal = productos.sumOf { it.cantidad * it.precio }
        val costoEnvio = 4.0
        val costoServicio = 1.0
        val total = subtotal + costoEnvio + costoServicio

        val pedidoData = hashMapOf(
            "pedido_id" to pedidoId,
            "estado" to estadoFinal,
            "usuario_id" to usuarioId,
            "negocio_nombre" to tvNegocio.text.toString(),
            "imagenNegocio" to (intent.getStringExtra("imagenNegocio") ?: ""),
            "direccion_entrega" to tvDireccionEntrega.text.toString(),
            "fecha_hora" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            "tiempo_transcurrido" to tiempoTranscurrido,
            "repartidor_id" to repartidorActual["id"],
            "repartidor_nombre" to repartidorActual["nombre"],
            "productos" to productosData,
            "subtotal" to subtotal,
            "costoEnvio" to costoEnvio,
            "costoServicio" to costoServicio,
            "total" to total
        )

        db.collection("historial_pedidos")
            .document(pedidoId)
            .set(pedidoData)
            .addOnSuccessListener {
                Log.d(TAG, "Historial guardado con estado: $estadoFinal")
                Toast.makeText(this, "Historial actualizado: $estadoFinal", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al guardar historial: ${e.message}")
                Toast.makeText(this, "Error al guardar historial: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        val userData = hashMapOf(
            "direccion" to tvDireccionEntrega.text.toString()
        )
        db.collection("usuarios").document(usuarioId)
            .set(userData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Dirección del usuario persistida: ${tvDireccionEntrega.text}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al persistir la dirección del usuario: ${e.message}")
                Toast.makeText(this, "Error al guardar la dirección: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun guardarNotificacion(titulo: String, mensaje: String) {
        val usuarioId = auth.currentUser?.uid ?: return
        val notificacionData = hashMapOf(
            "usuario_id" to usuarioId,
            "titulo" to titulo,
            "mensaje" to mensaje,
            "fecha_hora" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            "pedido_id" to pedidoId,
            "leida" to false
        )

        db.collection("notificaciones")
            .document()
            .set(notificacionData)
            .addOnSuccessListener {
                Log.d(TAG, "Notificación guardada en Firestore: $titulo - $mensaje")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al guardar notificación: ${e.message}")
                Toast.makeText(this, "Error al guardar notificación: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun enviarNotificacionEntrega() {
        val mensaje = "¡Entrega realizada con éxito!"
        guardarNotificacion("Entrega Completada", mensaje)
    }

    private fun mostrarDialogoCancelacion() {
        if (estadoActual == "confirmado") {
            AlertDialog.Builder(this)
                .setTitle("Cancelar Pedido")
                .setMessage("¿Estás seguro de que quieres cancelar tu pedido?")
                .setPositiveButton("Sí, cancelar") { _, _ ->
                    cancelarPedido()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        } else {
            AlertDialog.Builder(this)
                .setTitle("No se puede cancelar")
                .setMessage("El pedido ya está en preparación. Por favor, contacta con soporte para asistencia.")
                .setPositiveButton("Contactar soporte") { _, _ ->
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+51928489371"))
                    startActivity(intent)
                }
                .setNegativeButton("Cerrar") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun cancelarPedido() {
        simulacionRunnable?.let { handler.removeCallbacks(it) }
        tiempoRunnable?.let { handler.removeCallbacks(it) }

        guardarHistorialPedido("cancelado")
        guardarNotificacion("Pedido Cancelado", "Pedido cancelado exitosamente")
        Toast.makeText(this, "Pedido cancelado", Toast.LENGTH_SHORT).show()

        val usuarioId = auth.currentUser?.uid ?: return
        db.collection("historial_pedidos").document(pedidoId)
            .update("estado", "cancelado")
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al actualizar estado a cancelado en Firestore: ${e.message}")
            }

        finish()
    }

    private fun mostrarDetallesPedido() {
        try {
            val bottomSheet = BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.bottom_sheet_detalles_pedido, null)

            view.findViewById<TextView>(R.id.tvNumeroPedido)?.text = "Pedido #${pedidoId.take(8)}"
            view.findViewById<TextView>(R.id.tvHoraPedido)?.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(tiempoInicio))
            view.findViewById<TextView>(R.id.tvEstadoDetalle)?.text = tvEstadoPedido.text
            view.findViewById<TextView>(R.id.tvNegocioDetalle)?.text = tvNegocio.text
            view.findViewById<TextView>(R.id.tvDireccionDetalle)?.text = tvDireccionEntrega.text

            val containerProductos = view.findViewById<LinearLayout>(R.id.containerProductosDetalle)
            containerProductos?.removeAllViews()
            for (producto in productos) {
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
                        if (resId != 0) ivImagen.setImageResource(resId)
                        else ivImagen.setImageResource(R.drawable.placeholder_image)
                    }
                } else {
                    ivImagen.setImageResource(R.drawable.placeholder_image)
                }

                tvNombre.text = producto.nombre
                tvCantidad.text = "x${producto.cantidad}"
                val precioTotal = producto.cantidad * producto.precio
                tvPrecio.text = "S/ %.2f".format(precioTotal)

                containerProductos?.addView(itemView)
            }

            bottomSheet.setContentView(view)
            bottomSheet.show()
            Log.d(TAG, "BottomSheet de detalles mostrado")
        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar detalles: ${e.message}")
            Toast.makeText(this, "Error al mostrar detalles: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarAyuda() {
        try {
            val bottomSheet = BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.bottom_sheet_ayuda, null)

            view.findViewById<Button>(R.id.btnContactarSoporte)?.setOnClickListener {
                Log.d(TAG, "Contactar soporte clickeado")
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+51928489371"))
                startActivity(intent)
                bottomSheet.dismiss()
            }

            view.findViewById<Button>(R.id.btnCancelarPedidoAyuda)?.setOnClickListener {
                Log.d(TAG, "Cancelar pedido desde ayuda clickeado")
                bottomSheet.dismiss()
                mostrarDialogoCancelacion()
            }

            bottomSheet.setContentView(view)
            bottomSheet.show()
            Log.d(TAG, "BottomSheet de ayuda mostrado")
        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar ayuda: ${e.message}")
            Toast.makeText(this, "Error al mostrar ayuda: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun abrirChat() {
        try {
            Log.d(TAG, "Abriendo chat para pedido: $pedidoId")
            val bottomSheet = BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.bottom_sheet_chat, null)

            val etMensaje = view.findViewById<EditText>(R.id.etMensaje)
            val btnEnviar = view.findViewById<ImageButton>(R.id.btnEnviar)
            val btnLlamarChat = view.findViewById<Button>(R.id.btnLlamarChat)
            val tvChatMessages = view.findViewById<TextView>(R.id.tvChatMessages)

            var chatContent = ""
            db.collection("chats")
                .document(pedidoId)
                .collection("mensajes")
                .orderBy("timestamp")
                .get()
                .addOnSuccessListener { snapshot ->
                    snapshot.documents.forEach { doc ->
                        val emisor = doc.getString("emisor") ?: ""
                        val mensaje = doc.getString("mensaje") ?: ""
                        chatContent += "$emisor: $mensaje\n"
                    }
                    tvChatMessages.text = chatContent
                    Log.d(TAG, "Mensajes cargados: $chatContent")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al cargar mensajes: ${e.message}")
                    Toast.makeText(this, "Error al cargar mensajes: ${e.message}", Toast.LENGTH_SHORT).show()
                }

            btnEnviar.setOnClickListener {
                val mensaje = etMensaje.text.toString().trim()
                if (mensaje.isNotEmpty()) {
                    val usuarioId = auth.currentUser?.uid ?: "unknown"
                    val mensajeData = hashMapOf(
                        "emisor" to "Usuario",
                        "mensaje" to mensaje,
                        "timestamp" to System.currentTimeMillis(),
                        "pedido_id" to pedidoId
                    )

                    db.collection("chats")
                        .document(pedidoId)
                        .collection("mensajes")
                        .document()
                        .set(mensajeData)
                        .addOnSuccessListener {
                            chatContent += "Tú: $mensaje\n"
                            tvChatMessages.text = chatContent
                            etMensaje.setText("")
                            Log.d(TAG, "Mensaje enviado: $mensaje")

                            val respuesta = when (estadoActual) {
                                "en_camino_negocio" -> "Estoy yendo al restaurante, pronto recogeré tu pedido."
                                "llegado_negocio", "recogido" -> "Ya estoy en el restaurante, recogiendo tu pedido."
                                "en_camino_cliente" -> "Estoy en camino a tu ubicación, ¡llegaré pronto!"
                                "llegando" -> "¡Ya estoy cerca! En unos minutos estaré en tu dirección."
                                "entregado" -> "Tu pedido ya fue entregado. ¡Espero que lo disfrutes!"
                                else -> "Estoy organizando todo para tu pedido, te mantendré informado."
                            }

                            handler.postDelayed({
                                val respuestaData = hashMapOf(
                                    "emisor" to repartidorActual["nombre"],
                                    "mensaje" to respuesta,
                                    "timestamp" to System.currentTimeMillis(),
                                    "pedido_id" to pedidoId
                                )
                                db.collection("chats")
                                    .document(pedidoId)
                                    .collection("mensajes")
                                    .document()
                                    .set(respuestaData)
                                    .addOnSuccessListener {
                                        chatContent += "${repartidorActual["nombre"]}: $respuesta\n"
                                        tvChatMessages.text = chatContent
                                        Log.d(TAG, "Respuesta del repartidor: $respuesta")
                                        Toast.makeText(this, "Respuesta de ${repartidorActual["nombre"]}", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "Error al enviar respuesta: ${e.message}")
                                    }
                            }, 1000)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error al enviar mensaje: ${e.message}")
                            Toast.makeText(this, "Error al enviar mensaje: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }

            btnLlamarChat.setOnClickListener {
                Log.d(TAG, "Llamar desde chat clickeado")
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${repartidorActual["telefono"]}"))
                startActivity(intent)
                bottomSheet.dismiss()
            }

            bottomSheet.setContentView(view)
            bottomSheet.show()
            Log.d(TAG, "BottomSheet de chat mostrado")
        } catch (e: Exception) {
            Log.e(TAG, "Error al abrir chat: ${e.message}")
            Toast.makeText(this, "Error al abrir chat: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun iniciarContadorTiempo() {
        tiempoRunnable = object : Runnable {
            override fun run() {
                val tiempoTranscurrido = System.currentTimeMillis() - tiempoInicio
                val minutos = (tiempoTranscurrido / 60000).toInt()
                val segundos = ((tiempoTranscurrido % 60000) / 1000).toInt()

                tvTiempoTranscurrido.text = String.format("Tiempo transcurrido: %02d:%02d", minutos, segundos)

                handler.postDelayed(this, 1000)
            }
        }
        handler.post(tiempoRunnable!!)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        pedidoListener?.remove()
        repartidorListener?.remove()
        notificationListener?.remove()
        simulacionRunnable?.let { handler.removeCallbacks(it) }
        tiempoRunnable?.let { handler.removeCallbacks(it) }
        notificationQueue.clear()
        handler.removeCallbacksAndMessages(null)
    }
}