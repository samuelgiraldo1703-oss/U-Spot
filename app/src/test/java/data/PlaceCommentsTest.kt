package data

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceCommentsTest {
    private val now = 1_800_000_000_000L
    private val minute = 60_000L

    @Test fun ageLabels() {
        assertEquals("", commentAgeLabel(0L, now))
        assertEquals("ahora", commentAgeLabel(now - 20_000L, now))
        assertEquals("5 min", commentAgeLabel(now - 5 * minute, now))
        assertEquals("2 h", commentAgeLabel(now - 120 * minute, now))
        assertEquals("3 d", commentAgeLabel(now - 3 * 1440 * minute, now))
        assertEquals("2 sem", commentAgeLabel(now - 15 * 1440 * minute, now))
    }

    @Test fun oldCommentsShowTheirYear() {
        assert(commentAgeLabel(now - 90 * 1440 * minute, now).endsWith("2026"))
    }

    @Test fun buttonLabels() {
        assertEquals("Ver y escribir comentarios", commentsButtonLabel(hasComments = true))
        assertEquals("Escribir un comentario", commentsButtonLabel(hasComments = false))
    }

    private fun comment(uid: String, name: String = "", id: String = "c1") =
        PlaceComment(id, uid, name, null, "hola", null, 0L)

    @Test fun fillerAvatarIsStableAndInRange() {
        val ids = (1..200).map { fillerAvatarIndex(comment("uid-$it"), 8) }
        assert(ids.all { it in 0 until 8 })
        assert(ids.toSet().size > 1)
        assertEquals(fillerAvatarIndex(comment("abc", id = "c1"), 8), fillerAvatarIndex(comment("abc", id = "c2"), 8))
        // Sin uid se reparte por nombre: la misma persona conserva su imagen
        assertEquals(fillerAvatarIndex(comment("", "Ana", "c1"), 8), fillerAvatarIndex(comment("", "Ana", "c2"), 8))
    }

    @Test fun shortNames() {
        assertEquals("Juan T.", shortAuthorName("Juan Pablo Torres"))
        assertEquals("Maria", shortAuthorName("Maria"))
        assertEquals("Usuario", shortAuthorName("  "))
    }
}
