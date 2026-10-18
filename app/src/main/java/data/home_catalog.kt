package data

/**
 * Lugares del módulo Home tal como están en Figma. Cada uno tiene su pantalla de detalle
 * ("Home App (Restaurantes)", "Home App (Cafeterias)", ...) en assets/figma/place_<key>.json.
 * [backX] / [backY] son el centro del botón "Volver" dibujado sobre la foto (dp de Figma).
 */
enum class HomePlace(val key: String, val title: String, val backX: Float, val backY: Float) {
    WOK("wok", "Wok (Asian Fusion)", 36f, 26f),
    CREPES("crepes", "Crepes & Waffles", 36f, 26f),
    JUAN_VALDEZ("juanvaldez", "Juan Valdez Café", 36f, 26f),
    STARBUCKS("starbucks", "Starbucks Coffee", 36f, 21f),
    BBC("bbc", "BBC (Bogota Beer Company)", 36f, 38f),
    BEER_BAR("beerbar", "Beer Bar", 34f, 29f),
    CINE_COLOMBIA("cinecolombia", "Cine Colombia", 36f, 32f),
    CINEMATECA("cinemateca", "Cinemateca de Bogotá", 35f, 39f),
    BILLAR("billar", "Club de Billar & PoolBar", 36f, 26f),
    TEJO("tejo", "Tejo La 7a (Cancha & Cervecería)", 36f, 26f),
    MUSEO_NACIONAL("museonacional", "Museo Nacional de Colombia", 36f, 26f),
    MUSEO_ORO("museooro", "Museo del Oro", 36f, 26f),
    CAMPUS("campus", "Campus Bar & Club", 35f, 28f),
    DISCOTECA_CENTRAL("central", "Discoteca Central", 35f, 28f),
    PANAMERICANA("panamericana", "Panamericana", 36f, 26f),
    LERNER("lerner", "Librería Lerner", 25f, 47f),
    GOYURT("goyurt", "Goyurt", 25f, 25f);

    val sceneKey get() = "place_$key"

    companion object {
        fun fromKey(key: String?): HomePlace? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Categorías del Home ("Home App (Pantalla Principal)" en Figma). [places] va en el orden de las
 * tarjetas de arriba hacia abajo. [badgeId] es el icono de la categoría junto al título (nodo de Figma).
 */
enum class HomeCategory(val key: String, val label: String, val places: List<HomePlace>, val badgeId: String) {
    RESTAURANTES("restaurantes", "Restaurantes", listOf(HomePlace.WOK, HomePlace.CREPES), "442:1694"),
    CAFES("cafes", "Cafés", listOf(HomePlace.JUAN_VALDEZ, HomePlace.STARBUCKS), "442:2037"),
    BARES("bares", "Bares", listOf(HomePlace.BBC, HomePlace.BEER_BAR), "445:2427"),
    TEATROS("teatros", "Teatros", listOf(HomePlace.CINE_COLOMBIA, HomePlace.CINEMATECA), "445:3100"),
    OCIO("ocio", "Ocio", listOf(HomePlace.BILLAR, HomePlace.TEJO), "447:3637"),
    MUSEOS("museos", "Museos", listOf(HomePlace.MUSEO_NACIONAL, HomePlace.MUSEO_ORO), "449:4295"),
    DISCOTECAS("discotecas", "Discotecas", listOf(HomePlace.CAMPUS, HomePlace.DISCOTECA_CENTRAL), "449:4780"),
    LIBRERIAS("librerias", "Librerías", listOf(HomePlace.PANAMERICANA, HomePlace.LERNER), "451:5467");

    val sceneKey get() = "list_$key"

    companion object {
        fun fromKey(key: String?): HomeCategory? = entries.firstOrNull { it.key == key }
    }
}
