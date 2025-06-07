package pe.edu.trujidelivery.pe.edu.trujidelivery.cart

import pe.edu.trujidelivery.modelo.CarritoItem

object CarritoManager {
    val items = mutableListOf<CarritoItem>()

    fun agregarItem(item: CarritoItem) {
        val existente = items.find { it.id == item.id }
        if (existente != null) {
            existente.cantidad++
        } else {
            items.add(item)
        }
    }

    fun eliminarItem(itemId: String) {
        items.removeAll { it.id == itemId }
    }

    fun obtenerTotal(): Double {
        return items.sumOf { it.calcularTotal() }
    }

    fun obtenerCantidadTotal(): Int {
        return items.sumOf { it.cantidad }
    }
}
