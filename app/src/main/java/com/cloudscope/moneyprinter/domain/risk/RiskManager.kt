package com.cloudscope.moneyprinter.domain.risk

import com.cloudscope.moneyprinter.data.model.TradingSignal
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.floor
import kotlin.math.max

object RiskManager {

    const val NIFTY_LOT_SIZE = 25 // Current NSE NIFTY contract lot size

    data class PositionSizingResult(
        val lots: Int,
        val totalQuantity: Int,
        val totalCapitalRequired: Double,
        val maxRiskAmount: Double,
        val riskPercentOfCapital: Double
    )

    fun calculatePositionSize(
        capital: Double,
        signal: TradingSignal,
        riskPercent: Double = 2.0
    ): PositionSizingResult {
        if (capital <= 0.0 || signal.entryPrice <= 0.0 || signal.stopLoss <= 0.0) {
            return PositionSizingResult(0, 0, 0.0, 0.0, 0.0)
        }

        val riskPerContract = signal.entryPrice - signal.stopLoss
        if (riskPerContract <= 0.0) return PositionSizingResult(0, 0, 0.0, 0.0, 0.0)

        val maxAllowedLoss = capital * (riskPercent / 100.0)
        val riskPerLot = riskPerContract * NIFTY_LOT_SIZE

        val calculatedLots = floor(maxAllowedLoss / riskPerLot).toInt()
        val lots = max(1, calculatedLots) // Minimum 1 lot if within bounds

        val totalQty = lots * NIFTY_LOT_SIZE
        val totalCapital = totalQty * signal.entryPrice
        val totalRisk = totalQty * riskPerContract
        val actualRiskPercent = (totalRisk / capital) * 100.0

        return PositionSizingResult(
            lots = lots,
            totalQuantity = totalQty,
            totalCapitalRequired = totalCapital,
            maxRiskAmount = totalRisk,
            riskPercentOfCapital = actualRiskPercent
        )
    }

    fun isWithinTradingHours(currentEpochMillis: Long = System.currentTimeMillis()): Boolean {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        cal.timeInMillis = currentEpochMillis

        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return false // Weekend closed
        }

        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val currentMinutes = hour * 60 + minute

        val marketOpen = 9 * 60 + 15 // 09:15 IST
        val marketClose = 15 * 60 + 30 // 15:30 IST

        return currentMinutes in marketOpen..marketClose
    }

    fun isAutoSquareOffTime(currentEpochMillis: Long = System.currentTimeMillis()): Boolean {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        cal.timeInMillis = currentEpochMillis
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val currentMinutes = hour * 60 + minute
        return currentMinutes >= (15 * 60 + 15) // 15:15 IST
    }
}
