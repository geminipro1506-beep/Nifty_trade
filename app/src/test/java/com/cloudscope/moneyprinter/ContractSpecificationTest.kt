package com.cloudscope.moneyprinter

import com.cloudscope.moneyprinter.domain.options.ContractSpecificationService
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ContractSpecificationTest {

    @Test
    fun testNiftyCurrentLotSizeIs65() {
        assertEquals(65, ContractSpecificationService.CURRENT_NIFTY_LOT_SIZE)
        assertEquals(65, ContractSpecificationService.getLotSize("NIFTY"))
        assertEquals(65, ContractSpecificationService.getLotSize("NIFTY 50"))
    }

    @Test
    fun testAuthorizedQuantityCalculations() {
        // 1 lot = 65
        assertEquals(65, ContractSpecificationService.calculateQuantity(lots = 1, symbol = "NIFTY"))
        // 2 lots = 130
        assertEquals(130, ContractSpecificationService.calculateQuantity(lots = 2, symbol = "NIFTY"))
        // 3 lots = 195
        assertEquals(195, ContractSpecificationService.calculateQuantity(lots = 3, symbol = "NIFTY"))
        // 5 lots = 325
        assertEquals(325, ContractSpecificationService.calculateQuantity(lots = 5, symbol = "NIFTY"))
    }

    @Test
    fun testHistoricalLotSizeResolutionByTimestamp() {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }

        // Post Nov 20, 2024 -> 65
        val postNov2024 = sdf.parse("2024-11-25 10:00:00")!!.time
        assertEquals(65, ContractSpecificationService.getLotSizeForTimestamp(postNov2024, "NIFTY"))

        // Between April 26, 2024 and Nov 20, 2024 -> 25
        val mid2024 = sdf.parse("2024-07-15 11:30:00")!!.time
        assertEquals(25, ContractSpecificationService.getLotSizeForTimestamp(mid2024, "NIFTY"))

        // Prior to April 26, 2024 -> 50
        val early2024 = sdf.parse("2024-02-10 14:00:00")!!.time
        assertEquals(50, ContractSpecificationService.getLotSizeForTimestamp(early2024, "NIFTY"))
    }

    @Test
    fun testExpiryDateLotSizeResolution() {
        // Expiry in Dec 2024 -> 65
        assertEquals(65, ContractSpecificationService.getLotSize("NIFTY", "26-Dec-2024"))
        assertEquals(65, ContractSpecificationService.getLotSize("NIFTY", "Upcoming Weekly"))

        // Expiry in Jun 2024 -> 25
        assertEquals(25, ContractSpecificationService.getLotSize("NIFTY", "27-Jun-2024"))

        // Expiry in Jan 2024 -> 50
        assertEquals(50, ContractSpecificationService.getLotSize("NIFTY", "25-Jan-2024"))
    }

    @Test
    fun testMultiSymbolLotSizes() {
        assertEquals(15, ContractSpecificationService.getLotSize("BANKNIFTY"))
        assertEquals(25, ContractSpecificationService.getLotSize("FINNIFTY"))
        assertEquals(50, ContractSpecificationService.getLotSize("MIDCPNIFTY"))
    }

    @Test
    fun testSpecificationSourceMetadata() {
        val spec = ContractSpecificationService.getSpecification("NIFTY")
        assertEquals("NIFTY", spec.symbol)
        assertEquals(65, spec.lotSize)
        assertTrue(spec.isAuthorizedMaster)
        assertTrue(spec.source.contains("NSE"))
    }
}
