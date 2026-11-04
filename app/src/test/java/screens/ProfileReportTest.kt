package screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileReportTest {

    @Test
    fun singleLetterIsNotValidEmail() {
        assertFalse(isValidReportEmail("s"))
        assertFalse(isValidReportEmail("a"))
    }

    @Test
    fun incompleteEmailsAreNotValid() {
        assertFalse(isValidReportEmail(""))
        assertFalse(isValidReportEmail("   "))
        assertFalse(isValidReportEmail("test@"))
        assertFalse(isValidReportEmail("@domain.com"))
        assertFalse(isValidReportEmail("test@domain"))
    }

    @Test
    fun validEmailsAreAccepted() {
        assertTrue(isValidReportEmail("s@example.com"))
        assertTrue(isValidReportEmail("user@domain.co"))
        assertTrue(isValidReportEmail("  student@utadeo.edu.co  "))
        assertTrue(isValidReportEmail("john.doe+spot@gmail.com"))
    }
}

