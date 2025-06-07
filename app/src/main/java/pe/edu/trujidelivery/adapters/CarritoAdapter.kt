package pe.edu.trujidelivery.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import pe.edu.trujidelivery.R
import pe.edu.trujidelivery.modelo.CarritoItem


class CarritoAdapter(
    private val carritoItems: MutableList<CarritoItem>,
    private val onAddClick: (CarritoItem) -> Unit,
    private val onRemoveClick: (CarritoItem) -> Unit,
    private val onDeleteClick: (CarritoItem) -> Unit,
    private val onBackToNegocioClick: (CarritoItem) -> Unit
) : RecyclerView.Adapter<CarritoAdapter.CarritoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarritoViewHolder {
        Log.d("CarritoAdapter", "Creating view holder")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carrito, parent, false)
        return CarritoViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarritoViewHolder, position: Int) {
        Log.d("CarritoAdapter", "Binding position: $position")
        val item = carritoItems[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        Log.d("CarritoAdapter", "Item count: ${carritoItems.size}")
        return carritoItems.size
    }

    inner class CarritoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageViewProducto: ImageView = itemView.findViewById(R.id.imageViewProducto)
        private val textViewNombre: TextView = itemView.findViewById(R.id.textViewNombre)
        private val textViewPrecio: TextView = itemView.findViewById(R.id.textViewPrecio)
        private val textViewCantidad: TextView = itemView.findViewById(R.id.textViewCantidad)
        private val textViewNegocio: TextView = itemView.findViewById(R.id.textViewNegocio)
        private val imageViewAdd: ImageView = itemView.findViewById(R.id.imageViewAdd)
        private val imageViewRemove: ImageView = itemView.findViewById(R.id.imageViewRemove)
        private val imageViewBackToNegocio: ImageView = itemView.findViewById(R.id.imageViewBackToNegocio)

        fun bind(item: CarritoItem) {
            Log.d("CarritoAdapter", "Binding item: ${item.nombre}")
            try {
                textViewNombre.text = item.nombre
                textViewPrecio.text = "S/ ${String.format("%.2f", item.precio)}"
                textViewCantidad.text = "Cantidad: ${item.cantidad}"
                textViewNegocio.text = item.negocioNombre

                Glide.with(itemView.context)
                    .load(item.imagenUrl?.takeIf { it.isNotEmpty() })
                    .thumbnail(0.25f)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(imageViewProducto)


                Glide.with(itemView.context)
                    .load(item.imagenNegocio?.takeIf { it.isNotEmpty() })
                    .thumbnail(0.25f)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .circleCrop()
                    .into(imageViewBackToNegocio)

                imageViewAdd.setOnClickListener {
                    Log.d("CarritoAdapter", "Add button clicked for ${item.nombre}")
                    onAddClick(item)
                    updateRemoveIcon(item)
                }

                imageViewRemove.setOnClickListener {
                    Log.d("CarritoAdapter", "Remove button clicked for ${item.nombre}")
                    if (item.cantidad > 1) {
                        onRemoveClick(item)
                    } else {
                        onDeleteClick(item)
                    }
                    updateRemoveIcon(item)
                }

                imageViewBackToNegocio.setOnClickListener {
                    Log.d("CarritoAdapter", "Back to negocio clicked for ${item.negocioNombre}")
                    onBackToNegocioClick(item)
                }

                itemView.setOnClickListener(null)

                updateRemoveIcon(item)
            } catch (e: Exception) {
                Log.e("CarritoAdapter", "Error binding item: ${item.nombre}", e)
            }
        }

        private fun updateRemoveIcon(item: CarritoItem) {
            if (item.cantidad > 1) {
                imageViewRemove.setImageResource(R.drawable.ic_minus)
                imageViewRemove.contentDescription = "Disminuir cantidad"
            } else {
                imageViewRemove.setImageResource(android.R.drawable.ic_menu_delete)
                imageViewRemove.contentDescription = "Eliminar producto"
            }
        }
    }
}