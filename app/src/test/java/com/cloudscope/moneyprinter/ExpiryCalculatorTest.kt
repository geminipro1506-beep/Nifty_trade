package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.domain.options.ExpiryCalculator
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ExpiryCalculatorTest {

    @Test
    fun testUpcomingExpiriesCalculation() {
        val ist = TimeZone.getTimeZone("Asia/Kolkata")
        val cal = Calendar.getInstance(ist).apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 2) // Monday
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
        }

        val expiryInfo = ExpiryCalculator.calculateUpcomingExpiries(cal.timeInMillis)

        assertNotNull(expiryInfo.currentWeekly)
        assertNotNull(expiryInfo.nextWeekly)
        assertNotNull(expiryInfo.monthly)
        assertTrue(expiryInfo.currentWeekly.isNotEmpty())
        assertTrue(expiryInfo.currentWeekly != expiryInfo.nextWeekly)
        assertTrue(expiryInfo.timeToCurrentExpiryDays >= 0.0)
    }

    @Test
    fun testExpiryOnThursdayBeforeCutoff() {
        val ist = TimeZone.getTimeZone("Asia/Kolkata")
        val cal = Calendar.getInstance(ist).apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 5) // Thursday
            set(Calendar.HOUR_OF_DAY, 11) // Before 15:30
            set(Calendar.MINUTE, 0)
        }

        val expiryInfo = ExpiryCalculator.calculateUpcomingExpiries(cal.timeInMillis)
        assertTrue(expiryInfo.timeToCurrentExpiryDays < 1.0)
        assertTrue(expiryInfo.currentWeekly.contains("05 NOV 2026"))
    }

    @Test
    fun testExpiryOnThursdayAfterCutoffRollsOver() {
        val ist = TimeZone.getTimeZone("Asia/Kolkata")
        val cal = Calendar.getInstance(ist).apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 5) // Thursday
            set(Calendar.HOUR_OF_DAY, 16) // After 15:30
            set(Calendar.MINUTE, 0)
        }

        val expiryInfo = ExpiryCalculator.calculateUpcomingExpiries(cal.timeInMillis)
        // Nov 11 & Nov 12, 2026 are exchange holidays, so expiry prepones to Tuesday (10 NOV 2026)
        assertTrue(expiryInfo.currentWeekly.contains("10 NOV 2026"))
        assertTrue(expiryInfo.timeToCurrentExpiryDays > 4.0)
    }
}
