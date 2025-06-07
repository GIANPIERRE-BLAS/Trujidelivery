package pe.edu.trujidelivery.notification

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.firestore.FirebaseFirestore

object NotificationUtils {
    private val db = FirebaseFirestore.getInstance()

    fun eliminarNotificacionesUsuario(context: Context, usuarioId: String) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancelAll()
        db.collection("notificaciones")
            .whereEqualTo("usuario_id", usuarioId)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit()
                    .addOnSuccessListener {
                        Log.d("NotificationUtils", "Notificaciones eliminadas de Firestore")
                    }
                    .addOnFailureListener { e ->
                        Log.e("NotificationUtils", "Error al eliminar notificaciones: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("NotificationUtils", "Error al consultar notificaciones: ${e.message}")
            }
    }
}