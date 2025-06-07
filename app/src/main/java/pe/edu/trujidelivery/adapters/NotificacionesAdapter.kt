package pe.edu.trujidelivery.pe.edu.trujidelivery.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pe.edu.trujidelivery.R
import pe.edu.trujidelivery.pe.edu.trujidelivery.modelo.Notificacion

class NotificacionesAdapter(
    private val notificaciones: List<Notificacion>,
    private val onNotificacionClick: (Notificacion) -> Unit
) : RecyclerView.Adapter<NotificacionesAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitulo: TextView = itemView.findViewById(R.id.tvNotificacionTitulo)
        val tvMensaje: TextView = itemView.findViewById(R.id.tvNotificacionMensaje)
        val tvFecha: TextView = itemView.findViewById(R.id.tvNotificacionFecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notificacion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notificacion = notificaciones[position]
        holder.tvTitulo.text = notificacion.titulo
        holder.tvMensaje.text = notificacion.mensaje
        holder.tvFecha.text = notificacion.fechaHora
        holder.itemView.setBackgroundColor(
            if (notificacion.leida) Color.WHITE else Color.parseColor("#E0F7FA")
        )
        holder.itemView.setOnClickListener { onNotificacionClick(notificacion) }
    }

    override fun getItemCount(): Int = notificaciones.size
}