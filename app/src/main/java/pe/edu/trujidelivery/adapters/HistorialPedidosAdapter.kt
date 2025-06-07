package pe.edu.trujidelivery.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pe.edu.trujidelivery.R
import pe.edu.trujidelivery.pagos.HistorialPedidosActivity
import java.text.SimpleDateFormat
import java.util.*

class HistorialPedidosAdapter(
    private val pedidos: MutableList<HistorialPedidosActivity.Pedido>,
    private val onOpinarClick: (HistorialPedidosActivity.Pedido) -> Unit,
    private val onEliminarClick: (HistorialPedidosActivity.Pedido) -> Unit,
    private val onRepetirClick: (HistorialPedidosActivity.Pedido) -> Unit // Nuevo callback
) : RecyclerView.Adapter<HistorialPedidosAdapter.PedidoViewHolder>() {

    class PedidoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvEstadoFecha: TextView = itemView.findViewById(R.id.tvEstadoFecha)
        val tvNegocio: TextView = itemView.findViewById(R.id.tvNegocio)
        val rvProductos: RecyclerView = itemView.findViewById(R.id.rvProductos)
        val tvTotal: TextView = itemView.findViewById(R.id.tvTotal)
        val ivOpinar: ImageView = itemView.findViewById(R.id.ivOpinar)
        val ivRepetir: ImageView = itemView.findViewById(R.id.ivRepetir)
        val ivEliminar: ImageView = itemView.findViewById(R.id.ivEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pedido, parent, false)
        return PedidoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) {
        val pedido = pedidos[position]
        val sdf = SimpleDateFormat("EEE dd 'de' MMM '·' hh:mm a", Locale("es", "ES"))
        holder.tvEstadoFecha.text = "${pedido.estado} ${sdf.format(Date(pedido.fecha))}"
        holder.tvNegocio.text = pedido.negocioNombre
        holder.tvTotal.text = "Importe total S/: ${String.format("%.2f", pedido.total)}"

        val productoAdapter = ProductoItemAdapter(pedido.productos)
        holder.rvProductos.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = productoAdapter
        }

        holder.ivOpinar.setOnClickListener {
            Log.d("HistorialPedidosAdapter", "Clic en ícono opinar para pedido: ${pedido.id}")
            onOpinarClick(pedido)
        }

        holder.ivRepetir.setOnClickListener {
            Log.d("HistorialPedidosAdapter", "Clic en ícono repetir para pedido: ${pedido.id}")
            onRepetirClick(pedido)
        }

        holder.ivEliminar.setOnClickListener {
            Log.d("HistorialPedidosAdapter", "Clic en ícono eliminar para pedido: ${pedido.id}")
            onEliminarClick(pedido)
        }
    }

    override fun getItemCount() = pedidos.size
}