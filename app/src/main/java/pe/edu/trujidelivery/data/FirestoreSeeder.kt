package pe.edu.trujidelivery.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreSeeder {
    private val db = FirebaseFirestore.getInstance()

    fun cargarCategoriasConProductos() {
        val configDocRef = db.collection("config").document("seed")
        configDocRef.get()
            .addOnSuccessListener { doc ->
                val yaCargado = doc.getBoolean("datosCargados") ?: false
                if (yaCargado) {
                    Log.d("FirestoreSeeder", "Datos ya existen, actualizando...")
                    eliminarCategoriasYCargarNuevosDatos {
                        configDocRef.set(mapOf("datosCargados" to true))
                        Log.d("FirestoreSeeder", "Datos actualizados y marcado config")
                    }
                } else {
                    Log.d("FirestoreSeeder", "Carga inicial de datos...")
                    eliminarCategoriasYCargarNuevosDatos {
                        configDocRef.set(mapOf("datosCargados" to true))
                        Log.d("FirestoreSeeder", "Carga inicial terminada y marcado config")
                    }
                }
            }
            .addOnFailureListener {
                Log.e("FirestoreSeeder", "Error leyendo config seed", it)
            }
    }

    private fun eliminarCategoriasYCargarNuevosDatos(onComplete: () -> Unit) {
        db.collection("categorias").get()
            .addOnSuccessListener { categoriasSnapshot ->
                if (categoriasSnapshot.isEmpty) {
                    insertarCategoriasNegociosYProductos(onComplete)
                    return@addOnSuccessListener
                }

                val batch = db.batch()
                val categoryDocs = categoriasSnapshot.documents

                fun eliminarProductosYCategorias(i: Int) {
                    if (i >= categoryDocs.size) {
                        batch.commit()
                            .addOnSuccessListener { insertarCategoriasNegociosYProductos(onComplete) }
                            .addOnFailureListener { e -> Log.e("FirestoreSeeder", "Error borrando datos", e) }
                        return
                    }

                    val catDoc = categoryDocs[i]
                    val negociosCol = db.collection("categorias").document(catDoc.id).collection("negocios")
                    negociosCol.get()
                        .addOnSuccessListener { negociosSnapshot ->
                            for (negocioDoc in negociosSnapshot.documents) {
                                val productosCol = negociosCol.document(negocioDoc.id).collection("productos")
                                productosCol.get()
                                    .addOnSuccessListener { productosSnapshot ->
                                        for (productoDoc in productosSnapshot.documents) {
                                            batch.delete(productoDoc.reference)
                                        }
                                        batch.delete(negocioDoc.reference)
                                        if (negociosSnapshot.documents.last() == negocioDoc) {
                                            batch.delete(catDoc.reference)
                                            eliminarProductosYCategorias(i + 1)
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("FirestoreSeeder", "Error eliminando productos", e)
                                    }
                            }
                            if (negociosSnapshot.isEmpty) {
                                batch.delete(catDoc.reference)
                                eliminarProductosYCategorias(i + 1)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirestoreSeeder", "Error eliminando negocios", e)
                        }
                }

                eliminarProductosYCategorias(0)
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreSeeder", "Error obteniendo categorías", e)
            }
    }

    private fun insertarCategoriasNegociosYProductos(onComplete: () -> Unit) {
        val batch = db.batch()

        // Definir categorías
        val categorias = listOf(
            mapOf(
                "id" to "restaurantes",
                "nombre" to "Restaurantes",
                "descripcion" to "Los mejores restaurantes",
                "imagenUrl" to "https://cdn-icons-png.flaticon.com/512/3075/3075977.png"
            ),
            mapOf(
                "id" to "farmacias",
                "nombre" to "Farmacias",
                "descripcion" to "Farmacias confiables",
                "imagenUrl" to "https://cdn-icons-png.flaticon.com/512/2620/2620265.png"
            ),
            mapOf(
                "id" to "juguerias",
                "nombre" to "Juguerías",
                "descripcion" to "Jugos naturales y frescos",
                "imagenUrl" to "https://cdn-icons-png.flaticon.com/512/2674/2674080.png"
            ),
            mapOf(
                "id" to "supermercados",
                "nombre" to "Supermercados",
                "descripcion" to "Productos de primera necesidad",
                "imagenUrl" to "https://cdn-icons-png.flaticon.com/512/3081/3081558.png"
            ),
            mapOf(
                "id" to "envios",
                "nombre" to "Envíos",
                "descripcion" to "Servicios de delivery",
                "imagenUrl" to "https://cdn-icons-png.flaticon.com/512/2631/2631421.png"
            )
        )

        // Definir negocios por categoría
        val negociosPorCategoria = mapOf(
            "restaurantes" to listOf(
                mapOf("id" to "la_mar", "nombre" to "La Mar", "imagenUrl" to "https://lamar.pe/logo.png"),
                mapOf("id" to "pardos_chicken", "nombre" to "Pardos Chicken", "imagenUrl" to "https://pardoschicken.com.pe/logo.png"),
                mapOf("id" to "rokys", "nombre" to "Rokys", "imagenUrl" to "https://rokys.com.pe/logo.png"),
                mapOf("id" to "tanta", "nombre" to "Tanta", "imagenUrl" to "https://tanta.pe/logo.png"),
                mapOf("id" to "trujillo", "nombre" to "Trujillo", "imagenUrl" to "https://elrincondetrujillo.pe/logo.png"),
                mapOf("id" to "huarique", "nombre" to "Huarique", "imagenUrl" to "https://huarique.pe/logo.png"),
                mapOf("id" to "panchita", "nombre" to "Panchita", "imagenUrl" to "https://panchita.pe/logo.png"),
                mapOf("id" to "astrid", "nombre" to "Astrid y Gastón", "imagenUrl" to "https://astridygaston.com/logo.png"),
                mapOf("id" to "madreselveh", "nombre" to "Madreselva", "imagenUrl" to "https://madreselva.pe/logo.png"),
                mapOf("id" to "chifa_tan", "nombre" to "Chifa Tan", "imagenUrl" to "https://chifata.pe/logo.png")
            ),
            "farmacias" to listOf(
                mapOf("id" to "farmacias_peruanas", "nombre" to "Farmacias Peruanas", "imagenUrl" to "https://farmaciasperuanas.com/logo.png"),
                mapOf("id" to "boticas_del_sur", "nombre" to "Boticas del Sur", "imagenUrl" to "https://boticasdelsur.com/logo.png"),
                mapOf("id" to "farmacia_inka", "nombre" to "Farmacia Inka", "imagenUrl" to "https://farmaciainka.com/logo.png"),
                mapOf("id" to "boticas_internacionales", "nombre" to "Boticas Internacionales", "imagenUrl" to "https://boticasinternacionales.com/logo.png"),
                mapOf("id" to "farmacia_medicare", "nombre" to "Farmacia Medicare", "imagenUrl" to "https://farmaciamedicare.com/logo.png"),
                mapOf("id" to "boticas_buenos_aires", "nombre" to "Boticas Buenos Aires", "imagenUrl" to "https://boticasbuenosaires.com/logo.png"),
                mapOf("id" to "farmacia_san_pedro", "nombre" to "Farmacia San Pedro", "imagenUrl" to "https://farmaciasanpedro.com/logo.png"),
                mapOf("id" to "boticas_la_salud", "nombre" to "Boticas La Salud", "imagenUrl" to "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRJ5IeDinFFCKExnZkBE-hmvc4PXR3xj5hE1Q&s")
            ),
            "juguerias" to listOf(
                mapOf("id" to "jugueria_natural", "nombre" to "Juguería Natural", "imagenUrl" to "https://juguerianatural.com/logo.png"),
                mapOf("id" to "fruta_fresca", "nombre" to "Fruta Fresca", "imagenUrl" to "https://frutafresca.com/logo.png"),
                mapOf("id" to "jugos_del_valle", "nombre" to "Jugos del Valle", "imagenUrl" to "https://jugosdelvalle.com/logo.png"),
                mapOf("id" to "jugueria_express", "nombre" to "Juguería Express", "imagenUrl" to "https://jugueriaexpress.com/logo.png"),
                mapOf("id" to "vitaminas_ya", "nombre" to "Vitaminas Ya", "imagenUrl" to "https://vitaminasya.com/logo.png"),
                mapOf("id" to "jugos_tropicales", "nombre" to "Jugos Tropicales", "imagenUrl" to "https://jugostropicales.com/logo.png"),
                mapOf("id" to "jugos_y_mas", "nombre" to "Jugos y Más", "imagenUrl" to "https://jugosymas.com/logo.png"),
                mapOf("id" to "natural_juice", "nombre" to "Natural Juice", "imagenUrl" to "https://naturaljuice.com/logo.png"),
                mapOf("id" to "jugos_peru", "nombre" to "Jugos Perú", "imagenUrl" to "https://jugosperu.com/logo.png"),
                mapOf("id" to "vitaminas_frescas", "nombre" to "Vitaminas Frescas", "imagenUrl" to "https://vitaminasfrescas.com/logo.png")
            ),
            "supermercados" to listOf(
                mapOf("id" to "tottus", "nombre" to "Tottus", "imagenUrl" to "https://tottus.com.pe/logo.png"),
                mapOf("id" to "plaza_vea", "nombre" to "Plaza Vea", "imagenUrl" to "https://plazavea.com.pe/logo.png"),
                mapOf("id" to "metro", "nombre" to "Metro", "imagenUrl" to "https://metro.pe/logo.png"),
                mapOf("id" to "vivanda", "nombre" to "Vivanda", "imagenUrl" to "https://vivanda.com.pe/logo.png"),
                mapOf("id" to "mayorsa", "nombre" to "Mayorsa", "imagenUrl" to "https://mayorsa.com/logo.png"),
                mapOf("id" to "laive", "nombre" to "Laive Market", "imagenUrl" to "https://laive.com/logo.png"),
                mapOf("id" to "supermercados_peru", "nombre" to "Supermercados Perú", "imagenUrl" to "https://supermercadosperu.com/logo.png"),
                mapOf("id" to "plaza_solares", "nombre" to "Plaza Solares", "imagenUrl" to "https://plazasolares.com/logo.png"),
                mapOf("id" to "super_compras", "nombre" to "Super Compras", "imagenUrl" to "https://supercompras.com/logo.png"),
                mapOf("id" to "supermercados_el_sol", "nombre" to "Supermercados El Sol", "imagenUrl" to "https://supermercadoselsol.com/logo.png")
            ),
            "envios" to listOf(
                mapOf("id" to "rappi", "nombre" to "Rappi", "imagenUrl" to "https://rappi.com/logo.png"),
                mapOf("id" to "uber_eats", "nombre" to "Uber Eats", "imagenUrl" to "https://ubereats.com/logo.png"),
                mapOf("id" to "glovo", "nombre" to "Glovo", "imagenUrl" to "https://glovoapp.com/logo.png"),
                mapOf("id" to "pedidosya", "nombre" to "PedidosYa", "imagenUrl" to "https://pedidosya.com/logo.png"),
                mapOf("id" to "domicilios", "nombre" to "Domicilios", "imagenUrl" to "https://domicilios.com/logo.png"),
                mapOf("id" to "delivery_hero", "nombre" to "Delivery Hero", "imagenUrl" to "https://deliveryhero.com/logo.png"),
                mapOf("id" to "foodpanda", "nombre" to "Food Panda", "imagenUrl" to "https://foodpanda.com/logo.png"),
                mapOf("id" to "just_eat", "nombre" to "Just Eat", "imagenUrl" to "https://justeat.com/logo.png"),
                mapOf("id" to "grubhub", "nombre" to "Grubhub", "imagenUrl" to "https://grubhub.com/logo.png"),
                mapOf("id" to "postmates", "nombre" to "Postmates", "imagenUrl" to "https://postmates.com/logo.png")
            )
        )

        // Definir productos por negocio
        val productosPorNegocio = mapOf(
            // Restaurantes
            "la_mar" to listOf(
                mapOf("id" to "ceviche_clasico", "nombre" to "Ceviche Clásico", "descripcion" to "Pescado fresco con limón, cilantro y aji", "precio" to 45.0, "imagenUrl" to "https://lamar.pe/ceviche.png"),
                mapOf("id" to "tiradito_nikkei", "nombre" to "Tiradito Nikkei", "descripcion" to "Pescado en salsa de soya y limón", "precio" to 50.0, "imagenUrl" to "https://lamar.pe/tiradito.png"),
                mapOf("id" to "arroz_con_mariscos", "nombre" to "Arroz con Mariscos", "descripcion" to "Arroz con mariscos frescos", "precio" to 55.0, "imagenUrl" to "https://lamar.pe/arroz.png"),
                mapOf("id" to "jalea_mixta", "nombre" to "Jalea Mixta", "descripcion" to "Mariscos fritos con yuca", "precio" to 60.0, "imagenUrl" to "https://lamar.pe/jalea.png"),
                mapOf("id" to "sudado_de_pescado", "nombre" to "Sudado de Pescado", "descripcion" to "Pescado en caldo de tomate", "precio" to 48.0, "imagenUrl" to "https://lamar.pe/sudado.png")
            ),
            "pardos_chicken" to listOf(
                mapOf("id" to "pollo_a_la_brasa", "nombre" to "Pollo a la Brasa", "descripcion" to "Pollo entero asado con papas", "precio" to 65.0, "imagenUrl" to "https://pardoschicken.com.pe/pollo.png"),
                mapOf("id" to "mostrito", "nombre" to "Mostrito", "descripcion" to "Pollo con papas y chaufa", "precio" to 35.0, "imagenUrl" to "https://pardoschicken.com.pe/mostrito.png"),
                mapOf("id" to "ensalada_fresca", "nombre" to "Ensalada Fresca", "descripcion" to "Ensalada con vegetales frescos", "precio" to 20.0, "imagenUrl" to "https://pardoschicken.com.pe/ensalada.png"),
                mapOf("id" to "anticuchos", "nombre" to "Anticuchos", "descripcion" to "Brochetas de corazón a la parrilla", "precio" to 30.0, "imagenUrl" to "https://pardoschicken.com.pe/anticuchos.png"),
                mapOf("id" to "chaufa_de_pollo", "nombre" to "Chaufa de Pollo", "descripcion" to "Arroz chaufa con pollo", "precio" to 28.0, "imagenUrl" to "https://pardoschicken.com.pe/chaufa.png")
            ),
            "rokys" to listOf(
                mapOf("id" to "pollo_al_carbon", "nombre" to "Pollo al Carbón", "descripcion" to "Pollo asado con papas fritas", "precio" to 60.0, "imagenUrl" to "https://rokys.com.pe/pollo.png"),
                mapOf("id" to "combo_familiar", "nombre" to "Combo Familiar", "descripcion" to "Pollo con ensalada y papas", "precio" to 80.0, "imagenUrl" to "https://rokys.com.pe/combo.png"),
                mapOf("id" to "salchipapas", "nombre" to "Salchipapas", "descripcion" to "Papas fritas con salchicha", "precio" to 15.0, "imagenUrl" to "https://rokys.com.pe/salchipapas.png"),
                mapOf("id" to "tequeños", "nombre" to "Tequeños", "descripcion" to "Tequeños con guacamole", "precio" to 18.0, "imagenUrl" to "https://rokys.com.pe/tequenos.png"),
                mapOf("id" to "ensalada_cesar", "nombre" to "Ensalada César", "descripcion" to "Ensalada con pollo y aderezo", "precio" to 22.0, "imagenUrl" to "https://rokys.com.pe/cesar.png")
            ),
            "tanta" to listOf(
                mapOf("id" to "lomo_saltado", "nombre" to "Lomo Saltado", "descripcion" to "Lomo fino con papas y arroz", "precio" to 48.0, "imagenUrl" to "https://tanta.pe/lomo.png"),
                mapOf("id" to "aji_de_gallina", "nombre" to "Aji de Gallina", "descripcion" to "Pollo en salsa de aji", "precio" to 35.0, "imagenUrl" to "https://tanta.pe/aji.png"),
                mapOf("id" to "causa_rellena", "nombre" to "Causa Rellena", "descripcion" to "Causa con pollo y palta", "precio" to 30.0, "imagenUrl" to "https://tanta.pe/causa.png"),
                mapOf("id" to "papa_rellena", "nombre" to "Papa Rellena", "descripcion" to "Papa rellena con carne", "precio" to 25.0, "imagenUrl" to "https://tanta.pe/papa.png"),
                mapOf("id" to "tallarines_verdes", "nombre" to "Tallarines Verdes", "descripcion" to "Pasta con salsa de albahaca", "precio" to 32.0, "imagenUrl" to "https://tanta.pe/tallarines.png")
            ),
            "trujillo" to listOf(
                mapOf("id" to "cabrito_a_la_norteña", "nombre" to "Cabrito a la Norteña", "descripcion" to "Cabrito con frijoles y arroz", "precio" to 50.0, "imagenUrl" to "https://elrincondetrujillo.pe/cabrito.png"),
                mapOf("id" to "shambar", "nombre" to "Shambar", "descripcion" to "Sopa norteña con menestras", "precio" to 20.0, "imagenUrl" to "https://elrincondetrujillo.pe/shambar.png"),
                mapOf("id" to "seco_de_carne", "nombre" to "Seco de Carne", "descripcion" to "Carne guisada con culantro", "precio" to 45.0, "imagenUrl" to "https://elrincondetrujillo.pe/seco.png"),
                mapOf("id" to "ceviche_norteño", "nombre" to "Ceviche Norteño", "descripcion" to "Ceviche con chifles", "precio" to 40.0, "imagenUrl" to "https://elrincondetrujillo.pe/ceviche.png"),
                mapOf("id" to "arroz_con_pato", "nombre" to "Arroz con Pato", "descripcion" to "Pato con arroz y culantro", "precio" to 55.0, "imagenUrl" to "https://elrincondetrujillo.pe/pato.png")
            ),
            "huarique" to listOf(
                mapOf("id" to "anticucho_combo", "nombre" to "Anticucho Combo", "descripcion" to "Anticuchos con rachi y papa", "precio" to 35.0, "imagenUrl" to "https://huarique.pe/anticucho.png"),
                mapOf("id" to "chanfainita", "nombre" to "Chanfainita", "descripcion" to "Bofe guisado con papa", "precio" to 25.0, "imagenUrl" to "https://huarique.pe/chanfainita.png"),
                mapOf("id" to "cau_cau", "nombre" to "Cau Cau", "descripcion" to "Mondongo con papa y aji", "precio" to 28.0, "imagenUrl" to "https://huarique.pe/caucau.png"),
                mapOf("id" to "pollo_broaster", "nombre" to "Pollo Broaster", "descripcion" to "Pollo frito con papas", "precio" to 30.0, "imagenUrl" to "https://huarique.pe/broaster.png"),
                mapOf("id" to "sopa_de_menudencia", "nombre" to "Sopa de Menudencia", "descripcion" to "Sopa con visceras", "precio" to 20.0, "imagenUrl" to "https://huarique.pe/sopa.png")
            ),
            "panchita" to listOf(
                mapOf("id" to "anticuchos_panchita", "nombre" to "Anticuchos Panchita", "descripcion" to "Anticuchos con salsa especial", "precio" to 38.0, "imagenUrl" to "https://panchita.pe/anticucho.png"),
                mapOf("id" to "picarones", "nombre" to "Picarones", "descripcion" to "Postre de camote y zapallo", "precio" to 15.0, "imagenUrl" to "https://panchita.pe/picarones.png"),
                mapOf("id" to "lomo_al_jugo", "nombre" to "Lomo al Jugo", "descripcion" to "Lomo con salsa criolla", "precio" to 45.0, "imagenUrl" to "https://panchita.pe/lomo.png"),
                mapOf("id" to "causa_ferrenafe", "nombre" to "Causa Ferreñafe", "descripcion" to "Causa con atún y palta", "precio" to 30.0, "imagenUrl" to "https://panchita.pe/causa.png"),
                mapOf("id" to "arroz_chaufa", "nombre" to "Arroz Chaufa", "descripcion" to "Chaufa con carne y verduras", "precio" to 35.0, "imagenUrl" to "https://panchita.pe/chaufa.png")
            ),
            "astrid" to listOf(
                mapOf("id" to "degustacion_menu", "nombre" to "Menú Degustación", "descripcion" to "Menú de 5 tiempos gourmet", "precio" to 150.0, "imagenUrl" to "https://astridygaston.com/degustacion.png"),
                mapOf("id" to "cordero_al_cilindro", "nombre" to "Cordero al Cilindro", "descripcion" to "Cordero asado con hierbas", "precio" to 80.0, "imagenUrl" to "https://astridygaston.com/cordero.png"),
                mapOf("id" to "tiradito_fusion", "nombre" to "Tiradito Fusion", "descripcion" to "Pescado en salsa de maracuyá", "precio" to 60.0, "imagenUrl" to "https://astridygaston.com/tiradito.png"),
                mapOf("id" to "risotto_de_quinua", "nombre" to "Risotto de Quinua", "descripcion" to "Quinua con queso andino", "precio" to 55.0, "imagenUrl" to "https://astridygaston.com/risotto.png"),
                mapOf("id" to "postre_de_lucuma", "nombre" to "Postre de Lúcuma", "descripcion" to "Mousse de lúcuma", "precio" to 30.0, "imagenUrl" to "https://astridygaston.com/lucuma.png")
            ),
            "madreselveh" to listOf(
                mapOf("id" to "seco_de_cordero", "nombre" to "Seco de Cordero", "descripcion" to "Cordero guisado con culantro", "precio" to 50.0, "imagenUrl" to "https://madreselva.pe/seco.png"),
                mapOf("id" to "arroz_con_pollo", "nombre" to "Arroz con Pollo", "descripcion" to "Pollo con arroz y culantro", "precio" to 40.0, "imagenUrl" to "https://madreselva.pe/arroz.png"),
                mapOf("id" to "ceviche_mixto", "nombre" to "Ceviche Mixto", "descripcion" to "Ceviche de pescado y mariscos", "precio" to 45.0, "imagenUrl" to "https://madreselva.pe/ceviche.png"),
                mapOf("id" to "tacu_tacu", "nombre" to "Tacu Tacu", "descripcion" to "Tacu tacu con lomo saltado", "precio" to 48.0, "imagenUrl" to "https://madreselva.pe/tacu.png"),
                mapOf("id" to "mazamorra_morada", "nombre" to "Mazamorra Morada", "descripcion" to "Postre de maíz morado", "precio" to 15.0, "imagenUrl" to "https://madreselva.pe/mazamorra.png")
            ),
            "chifa_tan" to listOf(
                mapOf("id" to "chaufa_especial", "nombre" to "Chaufa Especial", "descripcion" to "Chaufa con pollo, cerdo y camarones", "precio" to 35.0, "imagenUrl" to "https://chifata.pe/chaufa.png"),
                mapOf("id" to "wantan_frito", "nombre" to "Wantán Frito", "descripcion" to "Wantanes fritos con salsa", "precio" to 20.0, "imagenUrl" to "https://chifata.pe/wantan.png"),
                mapOf("id" to "pollo_chi_jao_kay", "nombre" to "Pollo Chi Jao Kay", "descripcion" to "Pollo con verduras en salsa", "precio" to 30.0, "imagenUrl" to "https://chifata.pe/pollo.png"),
                mapOf("id" to "sopa_wantan", "nombre" to "Sopa Wantán", "descripcion" to "Sopa con wantanes y verduras", "precio" to 25.0, "imagenUrl" to "https://chifata.pe/sopa.png"),
                mapOf("id" to "kam_lu_wantan", "nombre" to "Kam Lu Wantán", "descripcion" to "Wantán con tamarindo", "precio" to 28.0, "imagenUrl" to "https://chifata.pe/kamlu.png")
            ),
            // Farmacias
            "farmacias_peruanas" to listOf(
                mapOf("id" to "diclofenaco", "nombre" to "Diclofenaco 50mg", "descripcion" to "Antiinflamatorio", "precio" to 7.0, "imagenUrl" to "https://farmaciasperuanas.com/diclofenaco.png"),
                mapOf("id" to "multivitaminico", "nombre" to "Multivitamínico", "descripcion" to "Suplemento diario", "precio" to 25.0, "imagenUrl" to "https://farmaciasperuanas.com/multivitaminico.png"),
                mapOf("id" to "termometro_digital", "nombre" to "Termómetro Digital", "descripcion" to "Mide la temperatura corporal", "precio" to 30.0, "imagenUrl" to "https://farmaciasperuanas.com/termometro.png"),
                mapOf("id" to "gel_antibacterial", "nombre" to "Gel Antibacterial 300ml", "descripcion" to "Desinfectante para manos", "precio" to 12.0, "imagenUrl" to "https://farmaciasperuanas.com/gel.png"),
                mapOf("id" to "amoxicilina", "nombre" to "Amoxicilina 500mg", "descripcion" to "Antibiótico de amplio espectro", "precio" to 15.0, "imagenUrl" to "https://farmaciasperuanas.com/amoxicilina.png")
            ),
            "boticas_del_sur" to listOf(
                mapOf("id" to "naproxeno", "nombre" to "Naproxeno 500mg", "descripcion" to "Analgésico y antiinflamatorio", "precio" to 9.0, "imagenUrl" to "https://boticasdelsur.com/naproxeno.png"),
                mapOf("id" to "jarabe_tos", "nombre" to "Jarabe para la Tos", "descripcion" to "Alivio para tos seca", "precio" to 18.0, "imagenUrl" to "https://boticasdelsur.com/jarabe.png"),
                mapOf("id" to "vitamina_b12", "nombre" to "Vitamina B12", "descripcion" to "Suplemento vitamínico", "precio" to 20.0, "imagenUrl" to "https://boticasdelsur.com/vitamina_b12.png"),
                mapOf("id" to "antiseptico", "nombre" to "Antiséptico 500ml", "descripcion" to "Limpieza de heridas", "precio" to 10.0, "imagenUrl" to "https://boticasdelsur.com/antiseptico.png"),
                mapOf("id" to "paracetamol_jarabe", "nombre" to "Paracetamol Jarabe", "descripcion" to "Analgésico infantil", "precio" to 12.0, "imagenUrl" to "https://boticasdelsur.com/jarabe_paracetamol.png")
            ),
            "farmacia_inka" to listOf(
                mapOf("id" to "cetirizina", "nombre" to "Cetirizina 10mg", "descripcion" to "Antihistamínico para alergias", "precio" to 10.0, "imagenUrl" to "https://farmaciainka.com/cetirizina.png"),
                mapOf("id" to "calcio_600", "nombre" to "Calcio 600mg", "descripcion" to "Suplemento para huesos", "precio" to 18.0, "imagenUrl" to "https://farmaciainka.com/calcio.png"),
                mapOf("id" to "termometro_infrarrojo", "nombre" to "Termómetro Infrarrojo", "descripcion" to "Mide temperatura sin contacto", "precio" to 50.0, "imagenUrl" to "https://farmaciainka.com/termometro.png"),
                mapOf("id" to "mascarilla_3m", "nombre" to "Mascarilla 3M", "descripcion" to "Mascarilla de alta protección", "precio" to 8.0, "imagenUrl" to "https://farmaciainka.com/mascarilla.png"),
                mapOf("id" to "antigripal_niños", "nombre" to "Antigripal Niños", "descripcion" to "Alivio para resfriados infantiles", "precio" to 15.0, "imagenUrl" to "https://farmaciainka.com/antigripal_niños.png")
            ),
            "boticas_internacionales" to listOf(
                mapOf("id" to "metformina", "nombre" to "Metformina 850mg", "descripcion" to "Control de glucosa", "precio" to 12.0, "imagenUrl" to "https://boticasinternacionales.com/metformina.png"),
                mapOf("id" to "vitamina_e", "nombre" to "Vitamina E 400UI", "descripcion" to "Antioxidante natural", "precio" to 20.0, "imagenUrl" to "https://boticasinternacionales.com/vitamina_e.png"),
                mapOf("id" to "alcohol_gel", "nombre" to "Alcohol en Gel 500ml", "descripcion" to "Desinfectante para manos", "precio" to 15.0, "imagenUrl" to "https://boticasinternacionales.com/gel.png"),
                mapOf("id" to "paracetamol_650", "nombre" to "Paracetamol 650mg", "descripcion" to "Analgésico y antipirético", "precio" to 6.0, "imagenUrl" to "https://boticasinternacionales.com/paracetamol.png"),
                mapOf("id" to "oximetro", "nombre" to "Oxímetro de Pulso", "descripcion" to "Mide oxigenación en sangre", "precio" to 80.0, "imagenUrl" to "https://boticasinternacionales.com/oximetro.png")
            ),
            "farmacia_medicare" to listOf(
                mapOf("id" to "losartan_50", "nombre" to "Losartan 50mg", "descripcion" to "Control de hipertensión", "precio" to 15.0, "imagenUrl" to "https://farmaciamedicare.com/losartan.png"),
                mapOf("id" to "omega_3", "nombre" to "Omega 3 1000mg", "descripcion" to "Suplemento para el corazón", "precio" to 25.0, "imagenUrl" to "https://farmaciamedicare.com/omega_3.png"),
                mapOf("id" to "antiseptico_bucal", "nombre" to "Antiséptico Bucal", "descripcion" to "Enjuague bucal 500ml", "precio" to 12.0, "imagenUrl" to "https://farmaciamedicare.com/antiseptico.png"),
                mapOf("id" to "ibuprofeno_jarabe", "nombre" to "Ibuprofeno Jarabe", "descripcion" to "Analgésico infantil", "precio" to 10.0, "imagenUrl" to "https://farmaciamedicare.com/ibuprofeno_jarabe.png"),
                mapOf("id" to "mascarilla_n95", "nombre" to "Mascarilla N95", "descripcion" to "Mascarilla de alta protección", "precio" to 6.0, "imagenUrl" to "https://farmaciamedicare.com/mascarilla.png")
            ),
            "boticas_buenos_aires" to listOf(
                mapOf("id" to "aspirina_500", "nombre" to "Aspirina 500mg", "descripcion" to "Analgésico y antipirético", "precio" to 7.0, "imagenUrl" to "https://boticasbuenosaires.com/aspirina.png"),
                mapOf("id" to "vitamina_c_jarabe", "nombre" to "Vitamina C Jarabe", "descripcion" to "Suplemento vitamínico infantil", "precio" to 15.0, "imagenUrl" to "https://boticasbuenosaires.com/vitamina_c.png"),
                mapOf("id" to "termometro_clinico", "nombre" to "Termómetro Clínico", "descripcion" to "Mide temperatura corporal", "precio" to 25.0, "imagenUrl" to "https://boticasbuenosaires.com/termometro.png"),
                mapOf("id" to "gel_antibacterial_500", "nombre" to "Gel Antibacterial 500ml", "descripcion" to "Desinfectante para manos", "precio" to 18.0, "imagenUrl" to "https://boticasbuenosaires.com/gel.png"),
                mapOf("id" to "paracetamol_infantil", "nombre" to "Paracetamol Infantil", "descripcion" to "Analgésico para niños", "precio" to 10.0, "imagenUrl" to "https://boticasbuenosaires.com/paracetamol.png")
            ),
            "farmacia_san_pedro" to listOf(
                mapOf("id" to "atorvastatina", "nombre" to "Atorvastatina 20mg", "descripcion" to "Control de colesterol", "precio" to 20.0, "imagenUrl" to "https://farmaciasanpedro.com/atorvastatina.png"),
                mapOf("id" to "vitamina_b_complex", "nombre" to "Vitamina B Complex", "descripcion" to "Suplemento energético", "precio" to 18.0, "imagenUrl" to "https://farmaciasanpedro.com/vitamina_b.png"),
                mapOf("id" to "alcohol_96", "nombre" to "Alcohol 96%", "descripcion" to "Desinfectante 500ml", "precio" to 12.0, "imagenUrl" to "https://farmaciasanpedro.com/alcohol.png"),
                mapOf("id" to "antigripal_forte", "nombre" to "Antigripal Forte", "descripcion" to "Alivio para resfriados fuertes", "precio" to 15.0, "imagenUrl" to "https://farmaciasanpedro.com/antigripal.png"),
                mapOf("id" to "mascarilla_quirurgica", "nombre" to "Mascarilla Quirúrgica", "descripcion" to "Mascarilla desechable", "precio" to 3.0, "imagenUrl" to "https://farmaciasanpedro.com/mascarilla.png")
            ),
            "boticas_la_salud" to listOf(
                mapOf("id" to "omeprazol_40", "nombre" to "Omeprazol 40mg", "descripcion" to "Protector gástrico", "precio" to 12.0, "imagenUrl" to "https://boticaslasalud.com/omeprazol.png"),
                mapOf("id" to "vitamina_d3", "nombre" to "Vitamina D3 4000UI", "descripcion" to "Suplemento para inmunidad", "precio" to 22.0, "imagenUrl" to "https://boticaslasalud.com/vitamina_d3.png"),
                mapOf("id" to "jarabe_antialergico", "nombre" to "Jarabe Antialérgico", "descripcion" to "Alivio para alergias infantiles", "precio" to 15.0, "imagenUrl" to "https://boticaslasalud.com/jarabe.png"),
                mapOf("id" to "alcohol_gel_300", "nombre" to "Alcohol en Gel 300ml", "descripcion" to "Desinfectante para manos", "precio" to 10.0, "imagenUrl" to "https://boticaslasalud.com/gel.png"),
                mapOf("id" to "termometro_digital_2", "nombre" to "Termómetro Digital", "descripcion" to "Mide temperatura corporal", "precio" to 28.0, "imagenUrl" to "https://boticaslasalud.com/termometro.png")
            ),
            // Juguerías
            "jugueria_natural" to listOf(
                mapOf("id" to "jugo_naranja", "nombre" to "Jugo de Naranja", "descripcion" to "Jugo natural de naranja", "precio" to 8.0, "imagenUrl" to "https://juguerianatural.com/naranja.png"),
                mapOf("id" to "jugo_piña", "nombre" to "Jugo de Piña", "descripcion" to "Jugo fresco de piña", "precio" to 9.0, "imagenUrl" to "https://juguerianatural.com/pina.png"),
                mapOf("id" to "batido_fresa", "nombre" to "Batido de Fresa", "descripcion" to "Batido con fresas frescas", "precio" to 12.0, "imagenUrl" to "https://juguerianatural.com/fresa.png"),
                mapOf("id" to "jugo_mango", "nombre" to "Jugo de Mango", "descripcion" to "Jugo fresco de mango", "precio" to 10.0, "imagenUrl" to "https://juguerianatural.com/mango.png"),
                mapOf("id" to "smoothie_verde", "nombre" to "Smoothie Verde", "descripcion" to "Espinaca, piña y jengibre", "precio" to 15.0, "imagenUrl" to "https://juguerianatural.com/smoothie.png")
            ),
            "fruta_fresca" to listOf(
                mapOf("id" to "jugo_manzana", "nombre" to "Jugo de Manzana", "descripcion" to "Jugo fresco de manzana", "precio" to 9.0, "imagenUrl" to "https://frutafresca.com/manzana.png"),
                mapOf("id" to "batido_maracuya", "nombre" to "Batido de Maracuyá", "descripcion" to "Batido con maracuyá fresco", "precio" to 12.0, "imagenUrl" to "https://frutafresca.com/maracuya.png"),
                mapOf("id" to "jugo_papaya", "nombre" to "Jugo de Papaya", "descripcion" to "Jugo fresco de papaya", "precio" to 10.0, "imagenUrl" to "https://frutafresca.com/papaya.png"),
                mapOf("id" to "smoothie_tropical", "nombre" to "Smoothie Tropical", "descripcion" to "Mango, piña y coco", "precio" to 15.0, "imagenUrl" to "https://frutafresca.com/smoothie.png"),
                mapOf("id" to "jugo_naranja_fresa", "nombre" to "Jugo Naranja-Fresa", "descripcion" to "Mezcla de naranja y fresa", "precio" to 11.0, "imagenUrl" to "https://frutafresca.com/naranja_fresa.png")
            ),
            "jugos_del_valle" to listOf(
                mapOf("id" to "jugo_melon", "nombre" to "Jugo de Melón", "descripcion" to "Jugo fresco de melón", "precio" to 9.0, "imagenUrl" to "https://jugosdelvalle.com/melon.png"),
                mapOf("id" to "batido_naranja", "nombre" to "Batido de Naranja", "descripcion" to "Batido con naranja fresca", "precio" to 12.0, "imagenUrl" to "https://jugosdelvalle.com/naranja.png"),
                mapOf("id" to "jugo_sandia", "nombre" to "Jugo de Sandía", "descripcion" to "Jugo fresco de sandía", "precio" to 10.0, "imagenUrl" to "https://jugosdelvalle.com/sandia.png"),
                mapOf("id" to "smoothie_energia", "nombre" to "Smoothie Energía", "descripcion" to "Banano, avena y miel", "precio" to 15.0, "imagenUrl" to "https://jugosdelvalle.com/energia.png"),
                mapOf("id" to "jugo_pina_mango", "nombre" to "Jugo Piña-Mango", "descripcion" to "Mezcla de piña y mango", "precio" to 11.0, "imagenUrl" to "https://jugosdelvalle.com/pina_mango.png")
            ),
            "jugueria_express" to listOf(
                mapOf("id" to "jugo_granadilla", "nombre" to "Jugo de Granadilla", "descripcion" to "Jugo fresco de granadilla", "precio" to 10.0, "imagenUrl" to "https://jugueriaexpress.com/granadilla.png"),
                mapOf("id" to "batido_papaya", "nombre" to "Batido de Papaya", "descripcion" to "Batido con papaya fresca", "precio" to 12.0, "imagenUrl" to "https://jugueriaexpress.com/papaya.png"),
                mapOf("id" to "jugo_mango_naranja", "nombre" to "Jugo Mango-Naranja", "descripcion" to "Mezcla de mango y naranja", "precio" to 11.0, "imagenUrl" to "https://jugueriaexpress.com/mango_naranja.png"),
                mapOf("id" to "smoothie_detox", "nombre" to "Smoothie Detox", "descripcion" to "Apio, manzana y limón", "precio" to 15.0, "imagenUrl" to "https://jugueriaexpress.com/detox.png"),
                mapOf("id" to "jugo_fresa", "nombre" to "Jugo de Fresa", "descripcion" to "Jugo fresco de fresa", "precio" to 10.0, "imagenUrl" to "https://jugueriaexpress.com/fresa.png")
            ),
            "vitaminas_ya" to listOf(
                mapOf("id" to "jugo_detox", "nombre" to "Jugo Detox", "descripcion" to "Apio y manzana", "precio" to 12.0, "imagenUrl" to "https://vitaminasya.com/detox.png"),
                mapOf("id" to "batido_kiwi", "nombre" to "Batido de Kiwi", "descripcion" to "Batido con kiwi fresco", "precio" to 12.0, "imagenUrl" to "https://vitaminasya.com/kiwi.png"),
                mapOf("id" to "jugo_granadilla", "nombre" to "Jugo de Granadilla", "descripcion" to "Jugo fresco de granadilla", "precio" to 10.0, "imagenUrl" to "https://vitaminasya.com/granadilla.png"),
                mapOf("id" to "smoothie_energia", "nombre" to "Smoothie Energía", "descripcion" to "Banano, avena y miel", "precio" to 15.0, "imagenUrl" to "https://vitaminasya.com/energia.png"),
                mapOf("id" to "jugo_manzana_verde", "nombre" to "Jugo de Manzana Verde", "descripcion" to "Jugo de manzana verde", "precio" to 9.0, "imagenUrl" to "https://vitaminasya.com/manzana.png")
            ),
            "jugos_tropicales" to listOf(
                mapOf("id" to "jugo_guanabana", "nombre" to "Jugo de Guanábana", "descripcion" to "Jugo fresco de guanábana", "precio" to 10.0, "imagenUrl" to "https://jugostropicales.com/guanabana.png"),
                mapOf("id" to "batido_maracuya", "nombre" to "Batido de Maracuyá", "descripcion" to "Batido con maracuyá fresco", "precio" to 12.0, "imagenUrl" to "https://jugostropicales.com/maracuya.png"),
                mapOf("id" to "jugo_coco", "nombre" to "Jugo de Coco", "descripcion" to "Jugo natural de coco", "precio" to 11.0, "imagenUrl" to "https://jugostropicales.com/coco.png"),
                mapOf("id" to "smoothie_tropical_2", "nombre" to "Smoothie Tropical", "descripcion" to "Piña, mango y coco", "precio" to 15.0, "imagenUrl" to "https://jugostropicales.com/smoothie.png"),
                mapOf("id" to "jugo_papaya_naranja", "nombre" to "Jugo Papaya-Naranja", "descripcion" to "Mezcla de papaya y naranja", "precio" to 10.0, "imagenUrl" to "https://jugostropicales.com/papaya_naranja.png")
            ),
            "jugos_y_mas" to listOf(
                mapOf("id" to "jugo_fresa_kiwi", "nombre" to "Jugo Fresa-Kiwi", "descripcion" to "Mezcla de fresa y kiwi", "precio" to 11.0, "imagenUrl" to "https://jugosymas.com/fresa_kiwi.png"),
                mapOf("id" to "batido_manzana", "nombre" to "Batido de Manzana", "descripcion" to "Batido con manzana fresca", "precio" to 12.0, "imagenUrl" to "https://jugosymas.com/manzana.png"),
                mapOf("id" to "jugo_sandia_melon", "nombre" to "Jugo Sandía-Melón", "descripcion" to "Mezcla de sandía y melón", "precio" to 10.0, "imagenUrl" to "https://jugosymas.com/sandia_melon.png"),
                mapOf("id" to "smoothie_proteico", "nombre" to "Smoothie Proteico", "descripcion" to "Banano, proteína y almendras", "precio" to 15.0, "imagenUrl" to "https://jugosymas.com/proteico.png"),
                mapOf("id" to "jugo_naranja_mango", "nombre" to "Jugo Naranja-Mango", "descripcion" to "Mezcla de naranja y mango", "precio" to 11.0, "imagenUrl" to "https://jugosymas.com/naranja_mango.png")
            ),
            "natural_juice" to listOf(
                mapOf("id" to "jugo_kiwi_manzana", "nombre" to "Jugo Kiwi-Manzana", "descripcion" to "Mezcla de kiwi y manzana", "precio" to 11.0, "imagenUrl" to "https://naturaljuice.com/kiwi_manzana.png"),
                mapOf("id" to "batido_sandia", "nombre" to "Batido de Sandía", "descripcion" to "Batido con sandía fresca", "precio" to 12.0, "imagenUrl" to "https://naturaljuice.com/sandia.png"),
                mapOf("id" to "jugo_maracuya_naranja", "nombre" to "Jugo Maracuyá-Naranja", "descripcion" to "Mezcla de maracuyá y naranja", "precio" to 10.0, "imagenUrl" to "https://naturaljuice.com/maracuya_naranja.png"),
                mapOf("id" to "smoothie_verde_2", "nombre" to "Smoothie Verde", "descripcion" to "Kale, manzana y jengibre", "precio" to 15.0, "imagenUrl" to "https://naturaljuice.com/smoothie.png"),
                mapOf("id" to "jugo_pina_coco", "nombre" to "Jugo Piña-Coco", "descripcion" to "Mezcla de piña y coco", "precio" to 11.0, "imagenUrl" to "https://naturaljuice.com/pina_coco.png")
            ),
            "jugos_peru" to listOf(
                mapOf("id" to "jugo_lucuma", "nombre" to "Jugo de Lúcuma", "descripcion" to "Jugo fresco de lúcuma", "precio" to 10.0, "imagenUrl" to "https://jugosperu.com/lucuma.png"),
                mapOf("id" to "batido_mango_2", "nombre" to "Batido de Mango", "descripcion" to "Batido con mango fresco", "precio" to 12.0, "imagenUrl" to "https://jugosperu.com/mango.png"),
                mapOf("id" to "jugo_fresa_naranja", "nombre" to "Jugo Fresa-Naranja", "descripcion" to "Mezcla de fresa y naranja", "precio" to 11.0, "imagenUrl" to "https://jugosperu.com/fresa_naranja.png"),
                mapOf("id" to "smoothie_andino", "nombre" to "Smoothie Andino", "descripcion" to "Lúcuma, banano y yogurt", "precio" to 15.0, "imagenUrl" to "https://jugosperu.com/smoothie.png"),
                mapOf("id" to "jugo_papaya_melon", "nombre" to "Jugo Papaya-Melón", "descripcion" to "Mezcla de papaya y melón", "precio" to 10.0, "imagenUrl" to "https://jugosperu.com/papaya_melon.png")
            ),
            "vitaminas_frescas" to listOf(
                mapOf("id" to "jugo_aguaymanto", "nombre" to "Jugo de Aguaymanto", "descripcion" to "Jugo fresco de aguaymanto", "precio" to 10.0, "imagenUrl" to "https://vitaminasfrescas.com/aguaymanto.png"),
                mapOf("id" to "batido_pina_2", "nombre" to "Batido de Piña", "descripcion" to "Batido con piña fresca", "precio" to 12.0, "imagenUrl" to "https://vitaminasfrescas.com/pina.png"),
                mapOf("id" to "jugo_naranja_kiwi", "nombre" to "Jugo Naranja-Kiwi", "descripcion" to "Mezcla de naranja y kiwi", "precio" to 11.0, "imagenUrl" to "https://vitaminasfrescas.com/naranja_kiwi.png"),
                mapOf("id" to "smoothie_detox_2", "nombre" to "Smoothie Detox", "descripcion" to "Apio, pepino y limón", "precio" to 15.0, "imagenUrl" to "https://vitaminasfrescas.com/detox.png"),
                mapOf("id" to "jugo_manzana_fresa", "nombre" to "Jugo Manzana-Fresa", "descripcion" to "Mezcla de manzana y fresa", "precio" to 10.0, "imagenUrl" to "https://vitaminasfrescas.com/manzana_fresa.png")
            ),
            // Supermercados
            "tottus" to listOf(
                mapOf("id" to "arroz_integral", "nombre" to "Arroz Integral 1kg", "descripcion" to "Arroz integral de alta calidad", "precio" to 5.0, "imagenUrl" to "https://tottus.com.pe/arroz_integral.png"),
                mapOf("id" to "leche_entera", "nombre" to "Leche Entera 1L", "descripcion" to "Leche pasteurizada", "precio" to 4.5, "imagenUrl" to "https://tottus.com.pe/leche.png"),
                mapOf("id" to "aceite_oliva", "nombre" to "Aceite de Oliva 500ml", "descripcion" to "Aceite extra virgen", "precio" to 20.0, "imagenUrl" to "https://tottus.com.pe/aceite.png"),
                mapOf("id" to "detergente_liquido", "nombre" to "Detergente Líquido 3L", "descripcion" to "Detergente para ropa", "precio" to 15.0, "imagenUrl" to "https://tottus.com.pe/detergente.png"),
                mapOf("id" to "pasta_spaghetti", "nombre" to "Pasta Spaghetti 500g", "descripcion" to "Pasta de trigo duro", "precio" to 3.5, "imagenUrl" to "https://tottus.com.pe/spaghetti.png")
            ),
            "plaza_vea" to listOf(
                mapOf("id" to "azucar_blanca", "nombre" to "Azúcar Blanca 1kg", "descripcion" to "Azúcar refinada", "precio" to 4.0, "imagenUrl" to "https://plazavea.com.pe/azucar.png"),
                mapOf("id" to "leche_evaporada", "nombre" to "Leche Evaporada 400g", "descripcion" to "Leche en lata", "precio" to 3.0, "imagenUrl" to "https://plazavea.com.pe/leche.png"),
                mapOf("id" to "papel_higienico", "nombre" to "Papel Higiénico 4 unid", "descripcion" to "Papel de doble hoja", "precio" to 10.0, "imagenUrl" to "https://plazavea.com.pe/papel.png"),
                mapOf("id" to "fideos_tornillo", "nombre" to "Fideos Tornillo 500g", "descripcion" to "Pasta tipo tornillo", "precio" to 3.0, "imagenUrl" to "https://plazavea.com.pe/tornillo.png"),
                mapOf("id" to "aceite_vegetal", "nombre" to "Aceite Vegetal 1L", "descripcion" to "Aceite para cocina", "precio" to 8.0, "imagenUrl" to "https://plazavea.com.pe/aceite.png")
            ),
            "metro" to listOf(
                mapOf("id" to "arroz_blanco", "nombre" to "Arroz Blanco 1kg", "descripcion" to "Arroz de grano largo", "precio" to 4.5, "imagenUrl" to "https://metro.pe/arroz.png"),
                mapOf("id" to "leche_uht", "nombre" to "Leche UHT 1L", "descripcion" to "Leche ultrapasteurizada", "precio" to 4.0, "imagenUrl" to "https://metro.pe/leche.png"),
                mapOf("id" to "jabon_liquido", "nombre" to "Jabón Líquido 500ml", "descripcion" to "Jabón para manos", "precio" to 10.0, "imagenUrl" to "https://metro.pe/jabon.png"),
                mapOf("id" to "galletas_soda", "nombre" to "Galletas Soda 200g", "descripcion" to "Galletas saladas", "precio" to 3.0, "imagenUrl" to "https://metro.pe/galletas.png"),
                mapOf("id" to "atun_lata", "nombre" to "Atún en Lata 170g", "descripcion" to "Atún en agua", "precio" to 5.0, "imagenUrl" to "https://metro.pe/atun.png")
            ),
            "vivanda" to listOf(
                mapOf("id" to "quinoa_500g", "nombre" to "Quinoa 500g", "descripcion" to "Quinoa orgánica", "precio" to 12.0, "imagenUrl" to "https://vivanda.com.pe/quinoa.png"),
                mapOf("id" to "leche_organica", "nombre" to "Leche Orgánica 1L", "descripcion" to "Leche orgánica pasteurizada", "precio" to 6.0, "imagenUrl" to "https://vivanda.com.pe/leche.png"),
                mapOf("id" to "aceite_coco", "nombre" to "Aceite de Coco 500ml", "descripcion" to "Aceite de coco virgen", "precio" to 25.0, "imagenUrl" to "https://vivanda.com.pe/aceite.png"),
                mapOf("id" to "detergente_polvo", "nombre" to "Detergente en Polvo 2kg", "descripcion" to "Detergente para ropa", "precio" to 15.0, "imagenUrl" to "https://vivanda.com.pe/detergente.png"),
                mapOf("id" to "pasta_fusilli", "nombre" to "Pasta Fusilli 500g", "descripcion" to "Pasta tipo fusilli", "precio" to 4.0, "imagenUrl" to "https://vivanda.com.pe/fusilli.png")
            ),
            "mayorsa" to listOf(
                mapOf("id" to "azucar_morena", "nombre" to "Azúcar Morena 1kg", "descripcion" to "Azúcar sin refinar", "precio" to 5.0, "imagenUrl" to "https://mayorsa.com/azucar.png"),
                mapOf("id" to "leche_condensada", "nombre" to "Leche Condensada 400g", "descripcion" to "Leche condensada dulce", "precio" to 4.5, "imagenUrl" to "https://mayorsa.com/leche.png"),
                mapOf("id" to "papel_toalla", "nombre" to "Papel Toalla 2 unid", "descripcion" to "Papel absorbente", "precio" to 8.0, "imagenUrl" to "https://mayorsa.com/papel.png"),
                mapOf("id" to "fideos_rigati", "nombre" to "Fideos Rigati 500g", "descripcion" to "Pasta tipo rigati", "precio" to 3.5, "imagenUrl" to "https://mayorsa.com/rigati.png"),
                mapOf("id" to "sardinas_lata", "nombre" to "Sardinas en Lata 170g", "descripcion" to "Sardinas en salsa de tomate", "precio" to 4.0, "imagenUrl" to "https://mayorsa.com/sardinas.png")
            ),
            "laive" to listOf(
                mapOf("id" to "leche_laive", "nombre" to "Leche Laive 1L", "descripcion" to "Leche entera pasteurizada", "precio" to 4.5, "imagenUrl" to "https://laive.com/leche.png"),
                mapOf("id" to "yogurt_natural", "nombre" to "Yogurt Natural 1L", "descripcion" to "Yogurt sin azúcar", "precio" to 10.0, "imagenUrl" to "https://laive.com/yogurt.png"),
                mapOf("id" to "mantequilla", "nombre" to "Mantequilla 200g", "descripcion" to "Mantequilla sin sal", "precio" to 8.0, "imagenUrl" to "https://laive.com/mantequilla.png"),
                mapOf("id" to "queso_fresco", "nombre" to "Queso Fresco 500g", "descripcion" to "Queso fresco natural", "precio" to 12.0, "imagenUrl" to "https://laive.com/queso.png"),
                mapOf("id" to "leche_chocolate", "nombre" to "Leche Chocolate 1L", "descripcion" to "Leche sabor chocolate", "precio" to 5.0, "imagenUrl" to "https://laive.com/chocolate.png")
            ),
            "supermercados_peru" to listOf(
                mapOf("id" to "arroz_extra", "nombre" to "Arroz Extra 1kg", "descripcion" to "Arroz de grano corto", "precio" to 4.0, "imagenUrl" to "https://supermercadosperu.com/arroz.png"),
                mapOf("id" to "leche_deslactosada", "nombre" to "Leche Deslactosada 1L", "descripcion" to "Leche sin lactosa", "precio" to 5.0, "imagenUrl" to "https://supermercadosperu.com/leche.png"),
                mapOf("id" to "detergente_liquido_2", "nombre" to "Detergente Líquido 2L", "descripcion" to "Detergente para ropa", "precio" to 12.0, "imagenUrl" to "https://supermercadosperu.com/detergente.png"),
                mapOf("id" to "galletas_chocolate", "nombre" to "Galletas de Chocolate 200g", "descripcion" to "Galletas con chispas", "precio" to 4.0, "imagenUrl" to "https://supermercadosperu.com/galletas.png"),
                mapOf("id" to "aceite_soya", "nombre" to "Aceite de Soya 1L", "descripcion" to "Aceite para cocina", "precio" to 7.0, "imagenUrl" to "https://supermercadosperu.com/aceite.png")
            ),
            "plaza_solares" to listOf(
                mapOf("id" to "fideos_canelones", "nombre" to "Fideos Canelones 500g", "descripcion" to "Pasta para canelones", "precio" to 5.0, "imagenUrl" to "https://plazasolares.com/canelones.png"),
                mapOf("id" to "leche_almendra", "nombre" to "Leche de Almendra 1L", "descripcion" to "Leche vegetal", "precio" to 10.0, "imagenUrl" to "https://plazasolares.com/leche.png"),
                mapOf("id" to "jabon_barra", "nombre" to "Jabón en Barra 200g", "descripcion" to "Jabón para ropa", "precio" to 3.0, "imagenUrl" to "https://plazasolares.com/jabon.png"),
                mapOf("id" to "arroz_basmati", "nombre" to "Arroz Basmati 1kg", "descripcion" to "Arroz aromático", "precio" to 6.0, "imagenUrl" to "https://plazasolares.com/arroz.png"),
                mapOf("id" to "atun_aceite", "nombre" to "Atún en Aceite 170g", "descripcion" to "Atún en aceite de oliva", "precio" to 5.5, "imagenUrl" to "https://plazasolares.com/atun.png")
            ),
            "super_compras" to listOf(
                mapOf("id" to "pasta_penne", "nombre" to "Pasta Penne 500g", "descripcion" to "Pasta tipo penne", "precio" to 3.5, "imagenUrl" to "https://supercompras.com/penne.png"),
                mapOf("id" to "leche_soya", "nombre" to "Leche de Soya 1L", "descripcion" to "Leche vegetal de soya", "precio" to 8.0, "imagenUrl" to "https://supercompras.com/leche.png"),
                mapOf("id" to "detergente_polvo_2", "nombre" to "Detergente en Polvo 1kg", "descripcion" to "Detergente para ropa", "precio" to 10.0, "imagenUrl" to "https://supercompras.com/detergente.png"),
                mapOf("id" to "galletas_vainilla", "nombre" to "Galletas de Vainilla 200g", "descripcion" to "Galletas dulces", "precio" to 3.5, "imagenUrl" to "https://supercompras.com/galletas.png"),
                mapOf("id" to "aceite_girasol", "nombre" to "Aceite de Girasol 1L", "descripcion" to "Aceite para cocina", "precio" to 7.5, "imagenUrl" to "https://supercompras.com/aceite.png")
            ),
            "supermercados_el_sol" to listOf(
                mapOf("id" to "arroz_arborio", "nombre" to "Arroz Arborio 1kg", "descripcion" to "Arroz para risotto", "precio" to 6.0, "imagenUrl" to "https://supermercadoselsol.com/arroz.png"),
                mapOf("id" to "leche_coco", "nombre" to "Leche de Coco 400ml", "descripcion" to "Leche de coco natural", "precio" to 8.0, "imagenUrl" to "https://supermercadoselsol.com/leche.png"),
                mapOf("id" to "papel_higienico_6", "nombre" to "Papel Higiénico 6 unid", "descripcion" to "Papel de triple hoja", "precio" to 12.0, "imagenUrl" to "https://supermercadoselsol.com/papel.png"),
                mapOf("id" to "fideos_linguini", "nombre" to "Fideos Linguini 500g", "descripcion" to "Pasta tipo linguini", "precio" to 4.0, "imagenUrl" to "https://supermercadoselsol.com/linguini.png"),
                mapOf("id" to "sardinas_aceite", "nombre" to "Sardinas en Aceite 170g", "descripcion" to "Sardinas en aceite", "precio" to 4.5, "imagenUrl" to "https://supermercadoselsol.com/sardinas.png")
            ),
            // Envíos
            "rappi" to listOf(
                mapOf("id" to "envio_express", "nombre" to "Envío Express", "descripcion" to "Entrega en 30 minutos", "precio" to 10.0, "imagenUrl" to "https://rappi.com/envio_express.png"),
                mapOf("id" to "envio_programado", "nombre" to "Envío Programado", "descripcion" to "Entrega en horario elegido", "precio" to 8.0, "imagenUrl" to "https://rappi.com/envio_programado.png"),
                mapOf("id" to "envio_restaurante", "nombre" to "Envío de Restaurante", "descripcion" to "Comida de restaurantes", "precio" to 12.0, "imagenUrl" to "https://rappi.com/envio_restaurante.png"),
                mapOf("id" to "envio_supermercado", "nombre" to "Envío de Supermercado", "descripcion" to "Compra de supermercado", "precio" to 15.0, "imagenUrl" to "https://rappi.com/envio_supermercado.png"),
                mapOf("id" to "envio_farmacia", "nombre" to "Envío de Farmacia", "descripcion" to "Medicinas a domicilio", "precio" to 10.0, "imagenUrl" to "https://rappi.com/envio_farmacia.png")
            ),
            "uber_eats" to listOf(
                mapOf("id" to "envio_rapido", "nombre" to "Envío Rápido", "descripcion" to "Entrega en 25-40 minutos", "precio" to 11.0, "imagenUrl" to "https://ubereats.com/envio_rapido.png"),
                mapOf("id" to "envio_economico", "nombre" to "Envío Económico", "descripcion" to "Entrega en 1 hora", "precio" to 7.0, "imagenUrl" to "https://ubereats.com/envio_economico.png"),
                mapOf("id" to "envio_comida", "nombre" to "Envío de Comida", "descripcion" to "Pedidos de restaurantes", "precio" to 13.0, "imagenUrl" to "https://ubereats.com/envio_comida.png"),
                mapOf("id" to "envio_tienda", "nombre" to "Envío de Tienda", "descripcion" to "Productos de tiendas locales", "precio" to 14.0, "imagenUrl" to "https://ubereats.com/envio_tienda.png"),
                mapOf("id" to "envio_prioritario", "nombre" to "Envío Prioritario", "descripcion" to "Entrega en 20 minutos", "precio" to 15.0, "imagenUrl" to "https://ubereats.com/envio_prioritario.png")
            ),
            "glovo" to listOf(
                mapOf("id" to "envio_inmediato", "nombre" to "Envío Inmediato", "descripcion" to "Entrega en 30-45 minutos", "precio" to 9.0, "imagenUrl" to "https://glovoapp.com/envio_inmediato.png"),
                mapOf("id" to "envio_planificado", "nombre" to "Envío Planificado", "descripcion" to "Entrega programada", "precio" to 7.5, "imagenUrl" to "https://glovoapp.com/envio_planificado.png"),
                mapOf("id" to "envio_restaurante", "nombre" to "Envío de Restaurante", "descripcion" to "Comida a domicilio", "precio" to 11.5, "imagenUrl" to "https://glovoapp.com/envio_restaurante.png"),
                mapOf("id" to "envio_farmacia", "nombre" to "Envío de Farmacia", "descripcion" to "Medicamentos a domicilio", "precio" to 10.0, "imagenUrl" to "https://glovoapp.com/envio_farmacia.png"),
                mapOf("id" to "envio_supermercado", "nombre" to "Envío de Supermercado", "descripcion" to "Productos de supermercado", "precio" to 13.0, "imagenUrl" to "https://glovoapp.com/envio_supermercado.png")
            ),
            "pedidosya" to listOf(
                mapOf("id" to "envio_express", "nombre" to "Envío Express", "descripcion" to "Entrega en 30 minutos", "precio" to 10.5, "imagenUrl" to "https://pedidosya.com/envio_express.png"),
                mapOf("id" to "envio_estandar", "nombre" to "Envío Estándar", "descripcion" to "Entrega en 1 hora", "precio" to 8.0, "imagenUrl" to "https://pedidosya.com/envio_estandar.png"),
                mapOf("id" to "envio_comida", "nombre" to "Envío de Comida", "descripcion" to "Pedidos de restaurantes", "precio" to 12.0, "imagenUrl" to "https://pedidosya.com/envio_comida.png"),
                mapOf("id" to "envio_mercado", "nombre" to "Envío de Mercado", "descripcion" to "Compra de supermercado", "precio" to 14.0, "imagenUrl" to "https://pedidosya.com/envio_mercado.png"),
                mapOf("id" to "envio_farmacia", "nombre" to "Envío de Farmacia", "descripcion" to "Medicinas a domicilio", "precio" to 9.5, "imagenUrl" to "https://pedidosya.com/envio_farmacia.png")
            ),
            "domicilios" to listOf(
                mapOf("id" to "envio_rapido", "nombre" to "Envío Rápido", "descripcion" to "Entrega en 35 minutos", "precio" to 10.0, "imagenUrl" to "https://domicilios.com/envio_rapido.png"),
                mapOf("id" to "envio_programado", "nombre" to "Envío Programado", "descripcion" to "Entrega en horario elegido", "precio" to 8.0, "imagenUrl" to "https://domicilios.com/envio_programado.png"),
                mapOf("id" to "envio_restaurante", "nombre" to "Envío de Restaurante", "descripcion" to "Comida de restaurantes", "precio" to 11.0, "imagenUrl" to "https://domicilios.com/envio_restaurante.png"),
                mapOf("id" to "envio_tienda", "nombre" to "Envío de Tienda", "descripcion" to "Productos de tiendas locales", "precio" to 12.0, "imagenUrl" to "https://domicilios.com/envio_tienda.png"),
                mapOf("id" to "envio_farmacia", "nombre" to "Envío de Farmacia", "descripcion" to "Medicinas a domicilio", "precio" to 9.0, "imagenUrl" to "https://domicilios.com/envio_farmacia.png")
            ),
            "delivery_hero" to listOf(
                mapOf("id" to "envio_inmediato", "nombre" to "Envío Inmediato", "descripcion" to "Entrega en 30-45 minutos", "precio" to 11.0, "imagenUrl" to "https://deliveryhero.com/envio_inmediato.png"),
                mapOf("id" to "envio_economico", "nombre" to "Envío Económico", "descripcion" to "Entrega en 1 hora", "precio" to 7.0, "imagenUrl" to "https://deliveryhero.com/envio_economico.png"),
                mapOf("id" to "envio_comida", "nombre" to "Envío de Comida", "descripcion" to "Pedidos de restaurantes", "precio" to 12.5, "imagenUrl" to "https://deliveryhero.com/envio_comida.png"),
                mapOf("id" to "envio_supermercado", "nombre" to "Envío de Supermercado", "descripcion" to "Productos de supermercado", "precio" to 14.0, "imagenUrl" to "https://deliveryhero.com/envio_supermercado.png"),
                mapOf("id" to "envio_farmacia", "nombre" to "Envío de Farmacia", "descripcion" to "Medicinas a domicilio", "precio" to 10.0, "imagenUrl" to "https://deliveryhero.com/envio_farmacia.png")
            ),
            "foodpanda" to listOf(
                mapOf("id" to "envio_express", "nombre" to "Envío Express", "descripcion" to "Entrega en 30 minutos", "precio" to 10.0, "imagenUrl" to "https://foodpanda.com/envio_express.png"),
                mapOf("id" to "envio_estandar", "nombre" to "Envío Estándar", "descripcion" to "Entrega en 1 hora", "precio" to 7.5, "imagenUrl" to "https://foodpanda.com/envio_estandar.png"),
                mapOf("id" to "envio_restaurante", "nombre" to "Envío de Restaurante", "descripcion" to "Comida de restaurantes", "precio" to 11.5, "imagenUrl" to "https://foodpanda.com/envio_restaurante.png"),
                mapOf("id" to "envio_mercado", "nombre" to "Envío de Mercado", "descripcion" to "Compra de supermercado", "precio" to 13.0, "imagenUrl" to "https://foodpanda.com/envio_mercado.png"),
                mapOf("id" to "envio_farmacia", "nombre" to "Envío de Farmacia", "descripcion" to "Medicinas a domicilio", "precio" to 9.5, "imagenUrl" to "https://foodpanda.com/envio_farmacia.png")
            ),
            "just_eat" to listOf(
                mapOf("id" to "envio_rapido", "nombre" to "Envío Rápido", "descripcion" to "Entrega en 35 minutos", "precio" to 10.5, "imagenUrl" to "https://justeat.com/envio_rapido.png"),
                mapOf("id" to "envio_programado", "nombre" to "Envío Programado", "descripcion" to "Entrega en horario elegido", "precio" to 8.0, "imagenUrl" to "https://justeat.com/envio_programado.png"),
                mapOf("id" to "envio_comida", "nombre" to "Envío de Comida", "descripcion" to "Pedidos de restaurantes", "precio" to 12.0, "imagenUrl" to "https://justeat.com/envio_comida.png"),
                mapOf("id" to "envio_tienda", "nombre" to "Envío de Tienda", "descripcion" to "Productos de tiendas locales", "precio" to 13.0, "imagenUrl" to "https://justeat.com/envio_tienda.png"),
                mapOf("id" to "envio_farmacia", "nombre" to "Envío de Farmacia", "descripcion" to "Medicinas a domicilio", "precio" to 10.0, "imagenUrl" to "https://justeat.com/envio_farmacia.png")
            ),
            "grubhub" to listOf(
                mapOf("id" to "envio_inmediato", "nombre" to "Envío Inmediato", "descripcion" to "Entrega en 30-45 minutos", "precio" to 11.0, "imagenUrl" to "https://grubhub.com/envio_inmediato.png"),
                mapOf("id" to "envio_economico", "nombre" to "Envío Económico", "descripcion" to "Entrega en 1 hora", "precio" to 7.0, "imagenUrl" to "https://grubhub.com/envio_economico.png"),
                mapOf("id" to "envio_restaurante", "nombre" to "Envío de Restaurante", "descripcion" to "Comida de restaurantes", "precio" to 12.5, "imagenUrl" to "https://grubhub.com/envio_restaurante.png"),
                mapOf("id" to "envio_supermercado", "nombre" to "Envío de Supermercado", "descripcion" to "Productos de supermercado", "precio" to 14.0, "imagenUrl" to "https://grubhub.com/envio_supermercado.png"),
                mapOf("id" to "envio_farmacia", "nombre" to "Envío de Farmacia", "descripcion" to "Medicinas a domicilio", "precio" to 10.0, "imagenUrl" to "https://grubhub.com/envio_farmacia.png")
            ),
            "postmates" to listOf(
                mapOf("id" to "envio_express", "nombre" to "Envío Express", "descripcion" to "Entrega en 30 minutos", "precio" to 10.5, "imagenUrl" to "https://postmates.com/envio_express.png"),
                mapOf("id" to "envio_estandar", "nombre" to "Envío Estándar", "descripcion" to "Entrega en 1 hora", "precio" to 7.5, "imagenUrl" to "https://postmates.com/envio_estandar.png"),
                mapOf("id" to "envio_comida", "nombre" to "Envío de Comida", "descripcion" to "Pedidos de restaurantes", "precio" to 12.0, "imagenUrl" to "https://postmates.com/envio_comida.png"),
                mapOf("id" to "envio_tienda", "nombre" to "Envío de Tienda", "descripcion" to "Productos de tiendas locales", "precio" to 13.0, "imagenUrl" to "https://postmates.com/envio_tienda.png"),
                mapOf("id" to "envio_farmacia", "nombre" to "Envío de Farmacia", "descripcion" to "Medicinas a domicilio", "precio" to 9.5, "imagenUrl" to "https://postmates.com/envio_farmacia.png")
            )
        )

        // Insertar categorías
        categorias.forEach { categoria ->
            val catRef = db.collection("categorias").document(categoria["id"] as String)
            batch.set(catRef, categoria)

            // Insertar negocios para la categoría
            val negocios = negociosPorCategoria[categoria["id"] as String] ?: emptyList()
            negocios.forEach { negocio ->
                val negocioRef = catRef.collection("negocios").document(negocio["id"] as String)
                batch.set(negocioRef, negocio)

                // Insertar productos para el negocio
                val productos = productosPorNegocio[negocio["id"] as String] ?: emptyList()
                productos.forEach { producto ->
                    val productoRef = negocioRef.collection("productos").document(producto["id"] as String)
                    batch.set(productoRef, producto)
                }
            }
        }

        // Ejecutar el batch
        batch.commit()
            .addOnSuccessListener {
                Log.d("FirestoreSeeder", "Datos insertados correctamente")
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreSeeder", "Error insertando datos", e)
            }
    }
}