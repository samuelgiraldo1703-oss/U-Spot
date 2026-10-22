package data.spotty

import data.HomeCategory
import data.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpottyBudgetTest {

    private fun place(price: String) = Place(
        id = "x", figma = null, name = "X", category = HomeCategory.BARES, address = "", distanceM = 0,
        distance = "", walkMin = 1, nequi = false, group = true, solo = false, study = false, quiet = false,
        price = price, priceVaries = false, hours = "", hoursVaries = false, schedule = emptyList(), notes = emptyList()
    )

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
}
