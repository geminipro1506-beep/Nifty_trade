package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.domain.options.ExchangeContractMaster
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ExchangeContractMasterTest {

    @Test
    fun testThursdayAndWednesdayHolidayPreponementToTuesday() {
        val ist = ExchangeContractMaster.IST_TIME_ZONE
        // Set date to Nov 6, 2026 (Friday after first Thursday Nov 5)
        val cal = Calendar.getInstance(ist).apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 6)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
        }

        // Nov 11 & Nov 12, 2026 are official NSE trading holidays.
        // The expiry MUST prepone to Tuesday, Nov 10, 2026!
        val expiry = ExchangeContractMaster.resolveNearestValidExpiry("NIFTY", cal.timeInMillis)
        assertEquals("10 NOV 2026", expiry)
    }

    @Test
    fun testNormalThursdayExpiryWhenNotAHoliday() {
        val ist = ExchangeContractMaster.IST_TIME_ZONE
        // Set date to Nov 2, 2026 (Monday before Nov 5 Thursday)
        val cal = Calendar.getInstance(ist).apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 2)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
        }

        val expiry = ExchangeContractMaster.resolveNearestValidExpiry("NIFTY", cal.timeInMillis)
        assertEquals("05 NOV 2026", expiry)
    }

    @Test
    fun testPastCutoffRollsOver() {
        val ist = ExchangeContractMaster.IST_TIME_ZONE
        // Nov 5, 2026 at 16:00 IST (Past 15:30 market close cutoff)
        val cal = Calendar.getInstance(ist).apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 5)
            set(Calendar.HOUR_OF_DAY, 16)
            set(Calendar.MINUTE, 0)
        }

        val expiry = ExchangeContractMaster.resolveNearestValidExpiry("NIFTY", cal.timeInMillis)
        // Nov 5 is expired, rolls to next week (which prepones to Tuesday 10 NOV 2026 due to holidays)
        assertEquals("10 NOV 2026", expiry)
    }

    @Test
    fun testFinNiftyTuesdayExpiry() {
        val ist = ExchangeContractMaster.IST_TIME_ZONE
        // Set date to Nov 2, 2026 (Monday) -> FinNifty expires on Tuesday (Nov 3)
        val cal = Calendar.getInstance(ist).apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 2)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
        }

        val expiry = ExchangeContractMaster.resolveNearestValidExpiry("FINNIFTY", cal.timeInMillis)
        assertEquals("03 NOV 2026", expiry)
    }

    @Test
    fun testSensexFridayExpiry() {
        val ist = ExchangeContractMaster.IST_TIME_ZONE
        // Nov 2, 2026 (Monday) -> BSE Sensex expiry on upcoming Friday (Nov 6)
        val cal = Calendar.getInstance(ist).apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 2)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
        }

        val sensexExpiry = ExchangeContractMaster.resolveNearestValidExpiry("SENSEX", cal.timeInMillis)
        assertEquals("06 NOV 2026", sensexExpiry)
    }

    @Test
    fun testAvailableChainExpiriesHonoredWhenValid() {
        val list = listOf("19 NOV 2026", "26 NOV 2026")
        val ist = ExchangeContractMaster.IST_TIME_ZONE
        val cal = Calendar.getInstance(ist).apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 15)
        }

        val expiry = ExchangeContractMaster.resolveNearestValidExpiry("NIFTY", cal.timeInMillis, list)
        assertEquals("19 NOV 2026", expiry)
    }
}
