package pe.edu.trujidelivery.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import pe.edu.trujidelivery.R
import pe.edu.trujidelivery.modelo.Producto

class ProductoAdapter(
    private val productos: MutableList<Producto>,
    private val onProductoClick: (Producto) -> Unit,
    private val onAddToCartClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = productos[position]
        holder.bind(producto)
    }

    override fun getItemCount(): Int = productos.size

    fun updateProducto(nuevosProductos: List<Producto>) {
        productos.clear()
        productos.addAll(nuevosProductos)
        notifyDataSetChanged()
    }

    inner class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nombreTextView: TextView = itemView.findViewById(R.id.nombre_producto)
        private val descripcionTextView: TextView = itemView.findViewById(R.id.descripcion_producto)
        private val precioOriginalTextView: TextView = itemView.findViewById(R.id.precio_original)
        private val descuentoTextView: TextView = itemView.findViewById(R.id.descuento)
        private val precioTextView: TextView = itemView.findViewById(R.id.precio_producto)
        private val imagenImageView: ImageView = itemView.findViewById(R.id.imagen_producto)
        private val addToCartButton: ImageView = itemView.findViewById(R.id.icon_add)

        fun bind(producto: Producto) {
            nombreTextView.text = producto.nombre
            descripcionTextView.text = producto.descripcion ?: ""

            if (producto.tieneDescuento()) {
                precioOriginalTextView.visibility = View.VISIBLE
                descuentoTextView.visibility = View.VISIBLE
                precioOriginalTextView.text = String.format("S/ %.2f", producto.precio)
                precioOriginalTextView.paintFlags = precioOriginalTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                val porcentajeDescuento = producto.descuento?.toDouble() ?: producto.discountPercentage ?: 0.0
                descuentoTextView.text = String.format("%d%% OFF", porcentajeDescuento.toInt())
                precioTextView.text = String.format("S/ %.2f", producto.precioConDescuento())
            } else {
                precioOriginalTextView.visibility = View.GONE
                descuentoTextView.visibility = View.GONE
                precioTextView.text = String.format("S/ %.2f", producto.precio)
                precioOriginalTextView.paintFlags = precioOriginalTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            Glide.with(itemView.context)
                .load(producto.imagenUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(imagenImageView)

            itemView.setOnClickListener {
                onProductoClick(producto)
            }

            addToCartButton.setOnClickListener {
                onAddToCartClick(producto)
            }
        }
    }
}