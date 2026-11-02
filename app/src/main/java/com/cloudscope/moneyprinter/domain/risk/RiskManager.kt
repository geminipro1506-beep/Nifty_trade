package com.cloudscope.moneyprinter.domain.risk

import com.cloudscope.moneyprinter.data.model.TradingSignal
import com.cloudscope.moneyprinter.domain.signal.ChargesCalculator
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

object RiskManager {

    const val NIFTY_LOT_SIZE = 25 // Current NSE NIFTY contract lot size

    data class PositionSizingResult(
        val lots: Int,
        val totalQuantity: Int,
        val totalCapitalRequired: Double,
        val maxRiskAmount: Double,
        val riskPercentOfCapital: Double,
        val isApproved: Boolean = true,
        val riskRejectionReason: String? = null,
        val sizingRationale: String = ""
    )

    data class InstitutionalRiskEvaluation(
        val actualCapital: Double,
        val entryPrice: Double,
        val stopLoss: Double,
        val stopLossDistance: Double,
        val roundTripSlippagePoints: Double,
        val estimatedChargesPerLot: Double,
        val totalRiskPerLot: Double,
        val maxAllowedRiskPerTrade: Double,
        val remainingDailyRiskBudget: Double,
        val calculatedLots: Int,
        val approvedLots: Int,
        val totalMarginRequired: Double,
        val isApproved: Boolean,
        val rejectionReason: String?,
        val auditRationale: String
    )

    /**
     * Institutional Position Sizing Model
     * Position sizing strictly considers:
     * 1. Actual trading capital
     * 2. Maximum acceptable rupee loss
     * 3. Stop-loss distance
     * 4. Lot size (25)
     * 5. Charges (Brokerage + STT + Turnover + GST + Stamp)
     * 6. Slippage (0.5pt per leg = 1.0pt roundtrip)
     * 7. Maximum daily risk budget
     *
     * A high AI confidence score can NEVER override risk limits.
     */
    fun calculateInstitutionalPositionSize(
        actualCapital: Double,
        entryPrice: Double,
        stopLoss: Double,
        maxAcceptableRupeeLoss: Double? = null,
        riskPercentOfCapital: Double = 2.0,
        maxDailyRiskBudget: Double = 5000.0,
        currentDailyLossAccumulated: Double = 0.0,
        lotSize: Int = NIFTY_LOT_SIZE,
        slippagePointsPerLeg: Double = 0.5,
        confidenceScore: Int = 50
    ): InstitutionalRiskEvaluation {
        if (actualCapital <= 0.0 || entryPrice <= 0.0 || stopLoss <= 0.0) {
            return InstitutionalRiskEvaluation(
                actualCapital = actualCapital,
                entryPrice = entryPrice,
                stopLoss = stopLoss,
                stopLossDistance = 0.0,
                roundTripSlippagePoints = 0.0,
                estimatedChargesPerLot = 0.0,
                totalRiskPerLot = 0.0,
                maxAllowedRiskPerTrade = 0.0,
                remainingDailyRiskBudget = 0.0,
                calculatedLots = 0,
                approvedLots = 0,
                totalMarginRequired = 0.0,
                isApproved = false,
                rejectionReason = "Invalid pricing or zero account capital",
                auditRationale = "Cannot size position: Capital or entry parameters invalid."
            )
        }

        val slDistance = abs(entryPrice - stopLoss)
        if (slDistance <= 0.01) {
            return InstitutionalRiskEvaluation(
                actualCapital = actualCapital,
                entryPrice = entryPrice,
                stopLoss = stopLoss,
                stopLossDistance = 0.0,
                roundTripSlippagePoints = 0.0,
                estimatedChargesPerLot = 0.0,
                totalRiskPerLot = 0.0,
                maxAllowedRiskPerTrade = 0.0,
                remainingDailyRiskBudget = 0.0,
                calculatedLots = 0,
                approvedLots = 0,
                totalMarginRequired = 0.0,
                isApproved = false,
                rejectionReason = "Stop-loss distance too small or zero",
                auditRationale = "Entry and SL prices are identical."
            )
        }

        val roundTripSlippagePoints = slippagePointsPerLeg * 2.0 // 1.0 pt roundtrip
        val slippageRupeesPerLot = roundTripSlippagePoints * lotSize

        // Estimate statutory charges on 1 lot for entry -> SL exit
        val chargesPerLot = ChargesCalculator.calculate(
            buyPrice = entryPrice,
            sellPrice = stopLoss,
            lots = 1,
            lotSize = lotSize
        ).totalCharges

        // Total worst-case loss per lot = (SL distance * lotSize) + slippage + statutory charges
        val marketLossPerLot = slDistance * lotSize
        val totalRiskPerLot = marketLossPerLot + slippageRupeesPerLot + chargesPerLot

        // Maximum allowed risk for this trade
        val standardTradeRiskBudget = maxAcceptableRupeeLoss ?: (actualCapital * (riskPercentOfCapital / 100.0))
        val remainingDailyBudget = max(0.0, maxDailyRiskBudget - currentDailyLossAccumulated)

        // Strict trade risk cap cannot exceed remaining daily loss limit
        val effectiveMaxRisk = min(standardTradeRiskBudget, remainingDailyBudget)

        if (effectiveMaxRisk <= 0.0) {
            return InstitutionalRiskEvaluation(
                actualCapital = actualCapital,
                entryPrice = entryPrice,
                stopLoss = stopLoss,
                stopLossDistance = round2(slDistance),
                roundTripSlippagePoints = roundTripSlippagePoints,
                estimatedChargesPerLot = round2(chargesPerLot),
                totalRiskPerLot = round2(totalRiskPerLot),
                maxAllowedRiskPerTrade = 0.0,
                remainingDailyRiskBudget = 0.0,
                calculatedLots = 0,
                approvedLots = 0,
                totalMarginRequired = 0.0,
                isApproved = false,
                rejectionReason = "Daily risk limit exhausted (Accumulated loss: ₹${round2(currentDailyLossAccumulated)} / ₹${round2(maxDailyRiskBudget)})",
                auditRationale = "Risk Guard engaged: No further risk budget available for current trading session."
            )
        }

        // Calculate lots permitted by risk limit
        val rawLots = floor(effectiveMaxRisk / totalRiskPerLot).toInt()

        if (rawLots < 1) {
            return InstitutionalRiskEvaluation(
                actualCapital = actualCapital,
                entryPrice = entryPrice,
                stopLoss = stopLoss,
                stopLossDistance = round2(slDistance),
                roundTripSlippagePoints = roundTripSlippagePoints,
                estimatedChargesPerLot = round2(chargesPerLot),
                totalRiskPerLot = round2(totalRiskPerLot),
                maxAllowedRiskPerTrade = round2(effectiveMaxRisk),
                remainingDailyRiskBudget = round2(remainingDailyBudget),
                calculatedLots = 0,
                approvedLots = 0,
                totalMarginRequired = 0.0,
                isApproved = false,
                rejectionReason = "Single lot risk (₹${round2(totalRiskPerLot)}) exceeds maximum allowed risk per trade (₹${round2(effectiveMaxRisk)})",
                auditRationale = "Trade rejected: Stop-loss distance of ₹${round2(slDistance)} plus ₹${round2(chargesPerLot)} friction breaches the 2% maximum loss rule."
            )
        }

        // Check capital/margin availability
        val marginPerLot = entryPrice * lotSize
        val maxLotsByCapital = floor(actualCapital / marginPerLot).toInt()

        // Take the conservative minimum between risk-based sizing and capital constraint
        val finalLots = min(rawLots, maxLotsByCapital)

        if (finalLots < 1) {
            return InstitutionalRiskEvaluation(
                actualCapital = actualCapital,
                entryPrice = entryPrice,
                stopLoss = stopLoss,
                stopLossDistance = round2(slDistance),
                roundTripSlippagePoints = roundTripSlippagePoints,
                estimatedChargesPerLot = round2(chargesPerLot),
                totalRiskPerLot = round2(totalRiskPerLot),
                maxAllowedRiskPerTrade = round2(effectiveMaxRisk),
                remainingDailyRiskBudget = round2(remainingDailyBudget),
                calculatedLots = rawLots,
                approvedLots = 0,
                totalMarginRequired = marginPerLot,
                isApproved = false,
                rejectionReason = "Insufficient capital (₹${round2(actualCapital)}) for minimum 1 lot margin requirement (₹${round2(marginPerLot)})",
                auditRationale = "Trade rejected due to margin inadequacy."
            )
        }

        val totalCapitalNeeded = finalLots * marginPerLot
        val worstCaseRisk = finalLots * totalRiskPerLot

        val rationale = "Approved $finalLots lot(s) (${finalLots * lotSize} qty). Risk per lot: ₹${round2(totalRiskPerLot)} (SL: ₹${round2(marketLossPerLot)}, Slippage: ₹${round2(slippageRupeesPerLot)}, Charges: ₹${round2(chargesPerLot)}). Total worst-case risk: ₹${round2(worstCaseRisk)} (${round1((worstCaseRisk / actualCapital) * 100.0)}% of capital). AI Confidence: $confidenceScore% (Risk ceiling enforced; confidence cannot increase lots)."

        return InstitutionalRiskEvaluation(
            actualCapital = actualCapital,
            entryPrice = entryPrice,
            stopLoss = stopLoss,
            stopLossDistance = round2(slDistance),
            roundTripSlippagePoints = roundTripSlippagePoints,
            estimatedChargesPerLot = round2(chargesPerLot),
            totalRiskPerLot = round2(totalRiskPerLot),
            maxAllowedRiskPerTrade = round2(effectiveMaxRisk),
            remainingDailyRiskBudget = round2(remainingDailyBudget),
            calculatedLots = rawLots,
            approvedLots = finalLots,
            totalMarginRequired = round2(totalCapitalNeeded),
            isApproved = true,
            rejectionReason = null,
            auditRationale = rationale
        )
    }

    fun calculatePositionSize(
        capital: Double,
        signal: TradingSignal,
        riskPercent: Double = 2.0
    ): PositionSizingResult {
        val eval = calculateInstitutionalPositionSize(
            actualCapital = capital,
            entryPrice = signal.entryPrice,
            stopLoss = signal.stopLoss,
            riskPercentOfCapital = riskPercent,
            confidenceScore = signal.confidencePercent
        )

        return PositionSizingResult(
            lots = eval.approvedLots,
            totalQuantity = eval.approvedLots * NIFTY_LOT_SIZE,
            totalCapitalRequired = eval.totalMarginRequired,
            maxRiskAmount = eval.approvedLots * eval.totalRiskPerLot,
            riskPercentOfCapital = if (capital > 0) ((eval.approvedLots * eval.totalRiskPerLot) / capital) * 100.0 else 0.0,
            isApproved = eval.isApproved,
            riskRejectionReason = eval.rejectionReason,
            sizingRationale = eval.auditRationale
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

    private fun round1(value: Double): Double = Math.round(value * 10.0) / 10.0
    private fun round2(value: Double): Double = Math.round(value * 100.0) / 100.0
}
