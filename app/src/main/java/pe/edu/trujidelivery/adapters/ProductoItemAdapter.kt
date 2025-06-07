package pe.edu.trujidelivery.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import pe.edu.trujidelivery.R

class ProductoItemAdapter(
    private val productos: List<Map<String, Any>>
) : RecyclerView.Adapter<ProductoItemAdapter.ProductoViewHolder>() {

    class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProducto: ImageView = itemView.findViewById(R.id.ivImagenProducto)
        val tvNombreProducto: TextView = itemView.findViewById(R.id.tvNombreProducto)
        val tvCantidadProducto: TextView = itemView.findViewById(R.id.tvCantidadProducto)
        val tvPrecioProducto: TextView = itemView.findViewById(R.id.tvPrecioProducto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto_resumen, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = productos[position]
        val nombre = producto["nombre"] as? String ?: "Producto desconocido"
        val cantidad = (producto["cantidad"] as? Number)?.toInt() ?: 1
        val precio = (producto["precio"] as? Number)?.toDouble() ?: 0.0
        val imagenUrl = producto["imagenUrl"] as? String ?: ""

        holder.tvNombreProducto.text = nombre
        holder.tvCantidadProducto.text = "x$cantidad"
        holder.tvPrecioProducto.text = "S/ ${String.format("%.2f", precio * cantidad)}"

        Glide.with(holder.itemView.context)
            .load(if (imagenUrl.isNotEmpty()) imagenUrl else R.drawable.placeholder_image)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.placeholder_image)
            .centerCrop()
            .into(holder.ivProducto)
    }

    override fun getItemCount() = productos.size
}