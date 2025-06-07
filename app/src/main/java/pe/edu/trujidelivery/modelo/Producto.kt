package pe.edu.trujidelivery.modelo

import com.google.firebase.firestore.PropertyName

data class Producto(
    var id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val precio: Double = 0.0,
    val imagenUrl: String = "",
    var cantidad: Int = 0,
    var comercioId: String = "",
    var categoriaId: String = "",
    @get:PropertyName("isDiscounted") @set:PropertyName("isDiscounted") var isDiscounted: Boolean = false,
    var discountPercentage: Double? = 0.0,
    var discountedPrice: Double? = 0.0,
    var descuento: Int? = null,
    var created_at: Any? = null
) {
    fun precioConDescuento(): Double {
        if (descuento != null && descuento!! > 0) {
            return precio * (1.0 - descuento!! / 100.0)
        }
        return if (isDiscounted && discountedPrice != null && discountedPrice!! > 0.0) {
            discountedPrice!!
        } else {
            precio
        }
    }

    fun tieneDescuento(): Boolean {
        return descuento != null && descuento!! > 0 || isDiscounted && discountedPrice != null && discountedPrice!! > 0.0
    }
}