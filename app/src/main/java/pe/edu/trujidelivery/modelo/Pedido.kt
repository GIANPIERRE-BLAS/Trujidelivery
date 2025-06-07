package pe.edu.trujidelivery.modelo

data class Pedido(
    val id: String,
    val negocioNombre: String,
    val imagenNegocio: String?,
    val productos: List<Map<String, Any>>,
    val subtotal: Double,
    val costoEnvio: Double,
    val costoServicio: Double,
    val total: Double,
    val direccion: String,
    val metodoPago: String,
    val datosPago: String?,
    val fecha: Long,
    val estado: String
)