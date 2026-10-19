package ir.khanoomi.geofencealert

import ir.khanoomi.geofencealert.domain.ExitSequenceValidator
import org.junit.Assert.*
import org.junit.Test

class ExitSequenceValidatorTest {

    @Test
    fun `valid values pass`() {
        assertNull(ExitSequenceValidator.validateCount("3"))
        assertNull(ExitSequenceValidator.validateDuration("10"))
        assertNull(ExitSequenceValidator.validateGap("30"))
        assertNull(ExitSequenceValidator.validateGap("0"))
    }

    @Test
    fun `persian digits accepted`() {
        assertNull(ExitSequenceValidator.validateCount("۳"))
        assertNull(ExitSequenceValidator.validateDuration("۱۰"))
    }

    @Test
    fun `out of range fails`() {
        assertNotNull(ExitSequenceValidator.validateCount("0"))
        assertNotNull(ExitSequenceValidator.validateCount("21"))
        assertNotNull(ExitSequenceValidator.validateDuration("2"))
        assertNotNull(ExitSequenceValidator.validateDuration("121"))
        assertNotNull(ExitSequenceValidator.validateGap("601"))
    }

    @Test
    fun `non numeric fails`() {
        assertNotNull(ExitSequenceValidator.validateCount(""))
        assertNotNull(ExitSequenceValidator.validateCount("abc"))
        assertNotNull(ExitSequenceValidator.validateDuration("1.5"))
    }

    @Test
    fun `boundary values pass`() {
        assertNull(ExitSequenceValidator.validateCount("1"))
        assertNull(ExitSequenceValidator.validateCount("20"))
        assertNull(ExitSequenceValidator.validateDuration("3"))
        assertNull(ExitSequenceValidator.validateDuration("120"))
        assertNull(ExitSequenceValidator.validateGap("600"))
    }

    @Test
    fun `error messages are persian`() {
        val msg = ExitSequenceValidator.validateCount("99")!!
        assertTrue(msg.contains("۱") && msg.contains("۲۰"))
    }
}
