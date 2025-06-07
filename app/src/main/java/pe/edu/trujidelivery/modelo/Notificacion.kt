package pe.edu.trujidelivery.pe.edu.trujidelivery.modelo

data class Notificacion(
    val id: String = "",
    val usuarioId: String = "",
    val titulo: String = "",
    val mensaje: String = "",
    val fechaHora: String = "",
    val pedidoId: String? = null,
    var leida: Boolean = false
)