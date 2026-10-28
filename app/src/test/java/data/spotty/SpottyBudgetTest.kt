package data.spotty

import data.HomeCategory
import data.Place
import data.PlaceSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SpottyBudgetTest {

    private fun place(price: String) = Place(
        id = "x", figma = null, name = "X", category = HomeCategory.BARES, address = "", distanceM = 0,
        distance = "", walkMin = 1, nequi = false, group = true, solo = false, study = false, quiet = false,
        price = price, priceVaries = false, hours = "", hoursVaries = false, schedule = emptyList(), notes = emptyList()
    )

    private fun loadRealCatalog(): List<Place> {
        val candidates = listOf(
            File("src/main/assets/places/places.json"),
            File("app/src/main/assets/places/places.json")
        )
        val raw = candidates.first { it.exists() }.readText(Charsets.UTF_8)
        val itemRegex = Regex("""\{"id":"([^"]+)","figma":(?:null|"[^"]+"),"name":"([^"]+)","category":"([^"]+)","address":"([^"]*)"(.*?),"photo":(?:null|"[^"]+")\}""")
        val descRegex = Regex(""""description":"([^"]*)"""")
        val tagsRegex = Regex(""""tags":\[(.*?)\]""")
        val latRegex = Regex(""""lat":([-\d.]+)""")
        val lngRegex = Regex(""""lng":([-\d.]+)""")
        val strRegex = Regex(""""([^"]+)"""")

        return itemRegex.findAll(raw).map { m ->
            val id = m.groupValues[1]
            val name = m.groupValues[2]
            val cat = HomeCategory.fromKey(m.groupValues[3])!!
            val addr = m.groupValues[4]
            val body = m.groupValues[5]
            val desc = descRegex.find(body)?.groupValues?.get(1).orEmpty()
            val tagsRaw = tagsRegex.find(body)?.groupValues?.get(1).orEmpty()
            val tags = strRegex.findAll(tagsRaw).map { it.groupValues[1] }.toList()
            val lat = latRegex.find(body)?.groupValues?.get(1)?.toDoubleOrNull()
            val lng = lngRegex.find(body)?.groupValues?.get(1)?.toDoubleOrNull()

            Place(
                id = id, figma = null, name = name, category = cat, address = addr,
                lat = lat, lng = lng, distanceM = 300, distance = "300 m", walkMin = 4,
                nequi = true, group = true, solo = true, study = false, quiet = false,
                price = "$20k - $40k", priceVaries = false, hours = "12pm-9pm", hoursVaries = false,
                schedule = emptyList<PlaceSchedule>(), notes = emptyList(),
                description = desc, tags = tags
            )
        }.toList()
    }

    @Test fun parsesExcelRanges() {
        assertEquals(15000..50000, SpottyBudget.pricePerPerson("$15k - $50k"))
        assertEquals(0..20000, SpottyBudget.pricePerPerson("$0 - $20k"))
        assertEquals(5000..5000, SpottyBudget.pricePerPerson("$5k"))
        assertEquals(60000..60000, SpottyBudget.pricePerPerson("$60k+"))
        assertEquals(0..0, SpottyBudget.pricePerPerson("Gratis"))
        assertNull(SpottyBudget.pricePerPerson("varía según el evento"))
    }

    @Test fun groupOfSixSplitsAtMidpoint() {
        val g = SpottyBudget.group(place("$15k - $50k"), 6)!!
        assertEquals(90000..300000, g.total)
        assertEquals(32500, g.suggestedEach)
    }

    @Test fun formatsPesos() {
        assertEquals("$32.500", SpottyBudget.pesos(32500))
        assertEquals("$1.250.000", SpottyBudget.pesos(1250000))
        assertEquals("$0 - $20k", SpottyBudget.shortRange(0..20000))
        assertEquals("$8.5k - $20k", SpottyBudget.shortRange(8500..20000))
    }

    @Test fun distanceLabels() {
        assertEquals("A 350 m", SpottyGeo.label(340))
        assertEquals("A 1.2 km", SpottyGeo.label(1234))
    }

    @Test fun all171PlacesHaveDescriptionsAndTags() {
        val catalog = loadRealCatalog()
        assertEquals(171, catalog.size)
        for (p in catalog) {
            assertTrue("Falta descripción en ${p.id}", p.description.length >= 80)
            assertTrue("Faltan tags en ${p.id}", p.tags.size >= 15)
        }
    }

    @Test fun searchesItalianRestaurantsNearUtadeo() {
        val catalog = loadRealCatalog()
        val locator = SpottyLocator { _, _ -> SpottyOrigin(SpottyGeo.UTADEO, "la Universidad Jorge Tadeo Lozano", true) }
        val tools = SpottyTools(catalog, locator, "Quiero un restaurante de comida italiana cerca de la tadeo")
        val hits = tools.findMatching(HomeCategory.RESTAURANTES, "comida italiana")
        assertTrue(hits.size >= 5)
        val topIds = hits.take(8).map { it.place.id }
        assertTrue("Debe incluir Mamma mia (a 373 m de la Tadeo): $topIds", "mamma-mia" in topIds)
        assertTrue("Debe incluir Bella Italia: $topIds", "bella-italia" in topIds)
        assertTrue("Debe incluir La Monferrina o Bruschetta: $topIds", "la-monferrina" in topIds || "bruschetta" in topIds)
    }

    @Test fun searchesSalchipapasByTagsAndDescription() {
        val catalog = loadRealCatalog()
        val locator = SpottyLocator { _, _ -> SpottyOrigin(SpottyGeo.UTADEO, "la Universidad Jorge Tadeo Lozano", true) }
        val tools = SpottyTools(catalog, locator, "Quiero comer salchipapas")
        val hits = tools.findMatching(HomeCategory.RESTAURANTES, "salchipapas")
        val topIds = hits.take(5).map { it.place.id }
        assertTrue("Debe encontrar Papotas Gourmet Andes por sus tags de salchipapas: $topIds", "papotas-gourmet-andes" in topIds)
    }

    @Test fun searchesPeruvianFoodIncludingBakatta() {
        val catalog = loadRealCatalog()
        val locator = SpottyLocator { _, _ -> SpottyOrigin(SpottyGeo.UTADEO, "la Universidad Jorge Tadeo Lozano", true) }
        val tools = SpottyTools(catalog, locator, "Busco comida peruana cerca de la tadeo")
        val hits = tools.findMatching(HomeCategory.RESTAURANTES, "comida peruana")
        val topIds = hits.take(5).map { it.place.id }
        assertTrue("Debe encontrar Restaurante Bakatta y NOA Cocina peruana: $topIds", "restaurante-bakatta" in topIds && "noa-cocina-peruana" in topIds)
    }
}
