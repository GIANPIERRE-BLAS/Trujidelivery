package pe.edu.trujidelivery.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import pe.edu.trujidelivery.R
import pe.edu.trujidelivery.modelo.Categoria


class CategoriaAdapter(
    private val categorias: List<Categoria>,
    private val onCategoriaClick: (Categoria) -> Unit
) : RecyclerView.Adapter<CategoriaAdapter.CategoriaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_categoria, parent, false)
        return CategoriaViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoriaViewHolder, position: Int) {
        val categoria = categorias[position]
        holder.bind(categoria)
    }

    override fun getItemCount(): Int = categorias.size

    inner class CategoriaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageCategoria: ImageView = itemView.findViewById(R.id.imageCategoria)
        private val textCategoria: TextView = itemView.findViewById(R.id.textCategoria)

        fun bind(categoria: Categoria) {
            textCategoria.text = categoria.nombre
            Glide.with(itemView.context)
                .load(categoria.imagenUrl)
                .placeholder(R.drawable.placeholder_image)
                .into(imageCategoria)
            itemView.setOnClickListener { onCategoriaClick(categoria) }
        }
    }
}