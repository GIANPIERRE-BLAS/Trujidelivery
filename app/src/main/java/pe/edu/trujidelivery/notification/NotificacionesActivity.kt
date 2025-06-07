package pe.edu.trujidelivery.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.DocumentChange
import pe.edu.trujidelivery.R
import pe.edu.trujidelivery.pe.edu.trujidelivery.adapters.NotificacionesAdapter
import pe.edu.trujidelivery.pe.edu.trujidelivery.modelo.Notificacion

class NotificacionesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var noNotificationsText: TextView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notificaciones = mutableListOf<Notificacion>()
    private lateinit var notificacionesAdapter: NotificacionesAdapter
    private val NOTIFICATION_CHANNEL_ID = "delivery_tracking"
    private val NOTIFICATION_ID_BASE = 2000
    private val NOTIFICATION_PERMISSION_REQUEST = 1002
    private var notificacionesListener: ListenerRegistration? = null


    private val notificacionesProcesadas = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notificaciones)
        supportActionBar?.hide()

        recyclerView = findViewById(R.id.recyclerViewNotificaciones)
        progressBar = findViewById(R.id.progressBar)
        noNotificationsText = findViewById(R.id.noNotificationsText)

        setupRecyclerView()
        solicitarPermisosNotificaciones()
        cargarNotificaciones()
        iniciarListenerNotificaciones()
    }

    private fun solicitarPermisosNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("NotificacionesActivity", "Permiso de notificaciones concedido")
            } else {
                Log.w("NotificacionesActivity", "Permiso de notificaciones denegado")
            }
        }
    }

    private fun iniciarListenerNotificaciones() {
        val usuarioId = auth.currentUser?.uid ?: return
        notificacionesListener = db.collection("notificaciones")
            .whereEqualTo("usuario_id", usuarioId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("NotificacionesActivity", "Error en listener: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    for (change in snapshot.documentChanges) {
                        when (change.type) {
                            DocumentChange.Type.ADDED -> {
                                val doc = change.document
                                val leida = doc.getBoolean("leida") ?: false
                                if (!leida && !notificacionesProcesadas.contains(doc.id)) {
                                    Log.d("NotificacionesActivity", "Nueva notificación detectada: ${doc.id}")
                                    enviarNotificacionInmediata(doc)
                                    notificacionesProcesadas.add(doc.id)
                                    marcarComoLeida(doc.id)
                                }
                            }
                            DocumentChange.Type.MODIFIED -> {
                                Log.d("NotificacionesActivity", "Notificación modificada: ${change.document.id}")
                            }
                            DocumentChange.Type.REMOVED -> {
                                Log.d("NotificacionesActivity", "Notificación eliminada: ${change.document.id}")
                            }
                        }
                    }
                    cargarNotificaciones()
                }
            }
    }

    private fun enviarNotificacionInmediata(doc: com.google.firebase.firestore.DocumentSnapshot) {
        val titulo = doc.getString("titulo") ?: "Notificación"
        val mensaje = doc.getString("mensaje") ?: "Tienes un nuevo mensaje"
        val notificationId = NOTIFICATION_ID_BASE + doc.id.hashCode()

        Log.d("NotificacionesActivity", "Enviando notificación: $titulo - $mensaje")

        val intent = Intent(this, NotificacionesActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_delivery_notification)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
                val notificationManager = NotificationManagerCompat.from(this)
                notificationManager.notify(notificationId, notification)
                Log.d("NotificacionesActivity", "Notificación enviada exitosamente")
            } else {
                Log.w("NotificacionesActivity", "No se puede enviar notificación: falta permiso")
            }
        } else {
            val notificationManager = NotificationManagerCompat.from(this)
            notificationManager.notify(notificationId, notification)
            Log.d("NotificacionesActivity", "Notificación enviada exitosamente (versión anterior)")
        }
    }

    private fun marcarComoLeida(notificacionId: String) {
        db.collection("notificaciones")
            .document(notificacionId)
            .update("leida", true)
            .addOnSuccessListener {
                Log.d("NotificacionesActivity", "Notificación marcada como leída: $notificacionId")
            }
            .addOnFailureListener { e ->
                Log.e("NotificacionesActivity", "Error al marcar notificación como leída: ${e.message}")
            }
    }

    private fun setupRecyclerView() {
        notificacionesAdapter = NotificacionesAdapter(notificaciones) { notificacion ->
            if (!notificacion.leida) {
                marcarComoLeida(notificacion.id)
                notificacion.leida = true
                notificacionesAdapter.notifyDataSetChanged()
            }
        }
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@NotificacionesActivity)
            adapter = notificacionesAdapter
        }
    }

    private fun cargarNotificaciones() {
        val usuarioId = auth.currentUser?.uid ?: return
        progressBar.visibility = View.VISIBLE
        noNotificationsText.visibility = View.GONE

        db.collection("notificaciones")
            .whereEqualTo("usuario_id", usuarioId)
            .orderBy("fecha_hora", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                notificaciones.clear()
                for (doc in snapshot.documents) {
                    val notificacion = Notificacion(
                        id = doc.id,
                        usuarioId = doc.getString("usuario_id") ?: "",
                        titulo = doc.getString("titulo") ?: "",
                        mensaje = doc.getString("mensaje") ?: "",
                        fechaHora = doc.getString("fecha_hora") ?: "",
                        pedidoId = doc.getString("pedido_id"),
                        leida = doc.getBoolean("leida") ?: false
                    )
                    notificaciones.add(notificacion)
                    if (notificacion.leida) {
                        notificacionesProcesadas.add(doc.id)
                    }
                }
                notificacionesAdapter.notifyDataSetChanged()
                progressBar.visibility = View.GONE
                noNotificationsText.visibility = if (notificaciones.isEmpty()) View.VISIBLE else View.GONE

                Log.d("NotificacionesActivity", "Notificaciones cargadas: ${notificaciones.size}")
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                noNotificationsText.visibility = View.VISIBLE
                noNotificationsText.text = "Error al cargar notificaciones: ${e.message}"
                Log.e("NotificacionesActivity", "Error al cargar notificaciones: ${e.message}")
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        notificacionesListener?.remove()
        Log.d("NotificacionesActivity", "Listener de notificaciones removido")
    }
}