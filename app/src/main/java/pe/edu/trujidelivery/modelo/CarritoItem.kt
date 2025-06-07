package pe.edu.trujidelivery.modelo
import com.google.firebase.firestore.PropertyName

data class CarritoItem(
    val id: String = "",
    val nombre: String = "",
    val precio: Double = 0.0,
    val imagenUrl: String = "",
    var cantidad: Int = 1,
    val negocioNombre: String = "",
    val negocioId: String = "",
    val categoriaId: String = "",
    @PropertyName("negocioImagenUrl") val imagenNegocio: String = "",
    var descuento: Int? = null,
) {
    fun calcularTotal(): Double = precio * cantidad
}