package pe.edu.trujidelivery.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import pe.edu.trujidelivery.R
import pe.edu.trujidelivery.pe.edu.trujidelivery.modelo.Negocio

class NegocioAdapter(
    private val negocios: List<Negocio>,
    private val onItemClick: (Negocio) -> Unit
) : RecyclerView.Adapter<NegocioAdapter.NegocioViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NegocioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_negocio, parent, false)
        return NegocioViewHolder(view)
    }

    override fun onBindViewHolder(holder: NegocioViewHolder, position: Int) {
        val negocio = negocios[position]
        holder.bind(negocio)
    }

    override fun getItemCount(): Int = negocios.size

    inner class NegocioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageNegocio: ImageView = itemView.findViewById(R.id.imageNegocio)
        private val textNegocio: TextView = itemView.findViewById(R.id.textNegocio)

        fun bind(negocio: Negocio) {
            textNegocio.text = negocio.nombre

            Glide.with(itemView.context)
                .load(negocio.imagenUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(imageNegocio)

            itemView.setOnClickListener {
                onItemClick(negocio)
            }
        }
    }
}
