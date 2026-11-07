package com.cloudscope.moneyprinter.domain.smartmoney

import com.cloudscope.moneyprinter.data.model.MarketQuote
import com.cloudscope.moneyprinter.data.model.OptionChain
import com.cloudscope.moneyprinter.data.model.OptionType
import com.cloudscope.moneyprinter.domain.options.ContractSpecificationService
import com.cloudscope.moneyprinter.domain.options.ExpiryCalculator
import com.cloudscope.moneyprinter.domain.options.OptionsSelector
import com.cloudscope.moneyprinter.domain.signal.ChargesCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

object SmartMoneySignalEngine {

    private val istTimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    /**
     * Generates directional options trading signals (SMART MONEY BUY CE / SMART MONEY BUY PE / NO ACTIVITY)
     * based on multi-factor Potential Smart Money Spot & Futures detection verified with Option Chain data.
     * Enforces the rule: Never generate CE/PE signals based on RSI or VWAP alone.
     */
    fun generateSignal(
        spotQuote: MarketQuote,
        smartMoneyActivity: SmartMoneyActivity,
        optionsDetails: OptionConfirmationDetails,
        optionChain: OptionChain?
    ): SmartMoneySignal {
        val detectionTimeStr = formatTimeToIst(spotQuote.timestamp)
        val expiryDate = optionChain?.expiryDates?.firstOrNull { it.isNotBlank() }
            ?: ExpiryCalculator.resolveNearestExpiry("NIFTY", spotQuote.timestamp)

        // 1. Missing Option Chain Check
        if (!optionsDetails.isAvailable || optionChain == null || optionChain.rows.isEmpty()) {
            return SmartMoneySignal(
                action = SmartMoneySignalAction.NO_TRADE,
                confidence = SmartMoneyConfidence.LOW,
                suggestedStrike = "—",
                expiryDate = expiryDate,
                entryZone = "—",
                stopLoss = "—",
                target1 = "—",
                target2 = "—",
                shortReason = "Potential Smart Money Activity: Option-chain confirmation unavailable. Live option premium required for safe entry.",
                detectionTime = detectionTimeStr,
                optionConfirmationAvailable = false,
                smartMoneyActivity = smartMoneyActivity,
                optionConfirmationDetails = optionsDetails
            )
        }

        // 2. Conflicting Signals Check
        val spotBullish = smartMoneyActivity.spotPrice >= smartMoneyActivity.vwap ||
                smartMoneyActivity.vwapState == VwapRelationship.RECLAIM ||
                smartMoneyActivity.marketStructure == MarketStructureState.BULLISH_STRUCTURE ||
                smartMoneyActivity.chochState == ChochState.BULLISH_CHOCH ||
                smartMoneyActivity.orderBlock == OrderBlockState.BULLISH_ORDER_BLOCK

        val spotBearish = smartMoneyActivity.spotPrice <= smartMoneyActivity.vwap ||
                smartMoneyActivity.vwapState == VwapRelationship.REJECTION ||
                smartMoneyActivity.marketStructure == MarketStructureState.BEARISH_STRUCTURE ||
                smartMoneyActivity.chochState == ChochState.BEARISH_CHOCH ||
                smartMoneyActivity.orderBlock == OrderBlockState.BEARISH_ORDER_BLOCK

        // Conflict: Spot indicates UP but Options indicate heavy Call Writing (resistance) or Futures indicate Short Buildup
        if (spotBullish && (optionsDetails.atmCeBuildup == OptionBuildupType.SHORT_BUILDUP ||
                    smartMoneyActivity.priceOiRegime == PriceOiRegime.SHORT_BUILDUP ||
                    optionsDetails.pcr < 0.85)
        ) {
            val conflictReason = if (optionsDetails.atmCeBuildup == OptionBuildupType.SHORT_BUILDUP) {
                "Conflicting signals: Potential Smart Money Activity conflict - Spot bullish near VWAP but Option Chain reveals heavy Call Writing resistance at ${optionsDetails.atmStrike.toInt()} CE."
            } else if (smartMoneyActivity.priceOiRegime == PriceOiRegime.SHORT_BUILDUP) {
                "Conflicting signals: Potential Smart Money Activity conflict - Spot bounce opposes NIFTY Futures Short Buildup (${String.format(Locale.US, "%.1f", smartMoneyActivity.futuresOiChangePercent)}% OI addition)."
            } else {
                "Conflicting signals: Potential Smart Money Activity conflict - Low PCR (${String.format(Locale.US, "%.2f", optionsDetails.pcr)}) indicates strong Call writing resistance overhead."
            }

            return SmartMoneySignal(
                action = SmartMoneySignalAction.NO_TRADE,
                confidence = SmartMoneyConfidence.LOW,
                suggestedStrike = "—",
                expiryDate = expiryDate,
                entryZone = "—",
                stopLoss = "—",
                target1 = "—",
                target2 = "—",
                shortReason = conflictReason,
                detectionTime = detectionTimeStr,
                smartMoneyActivity = smartMoneyActivity,
                optionConfirmationDetails = optionsDetails
            )
        }

        // Conflict: Spot indicates DOWN but Options indicate heavy Put Writing (support) or Futures indicate Long Buildup
        if (spotBearish && (optionsDetails.atmPeBuildup == OptionBuildupType.SHORT_BUILDUP ||
                    smartMoneyActivity.priceOiRegime == PriceOiRegime.LONG_BUILDUP ||
                    optionsDetails.pcr > 1.25)
        ) {
            val conflictReason = if (optionsDetails.atmPeBuildup == OptionBuildupType.SHORT_BUILDUP) {
                "Conflicting signals: Potential Smart Money Activity conflict - Spot bearish near VWAP but Option Chain reveals heavy Put Writing support floor at ${optionsDetails.atmStrike.toInt()} PE."
            } else if (smartMoneyActivity.priceOiRegime == PriceOiRegime.LONG_BUILDUP) {
                "Conflicting signals: Potential Smart Money Activity conflict - Spot dip opposes NIFTY Futures Long Buildup (+${String.format(Locale.US, "%.1f", smartMoneyActivity.futuresOiChangePercent)}% fresh longs)."
            } else {
                "Conflicting signals: Potential Smart Money Activity conflict - High PCR (${String.format(Locale.US, "%.2f", optionsDetails.pcr)}) indicates strong Put writing support underneath."
            }

            return SmartMoneySignal(
                action = SmartMoneySignalAction.NO_TRADE,
                confidence = SmartMoneyConfidence.LOW,
                suggestedStrike = "—",
                expiryDate = expiryDate,
                entryZone = "—",
                stopLoss = "—",
                target1 = "—",
                target2 = "—",
                shortReason = conflictReason,
                detectionTime = detectionTimeStr,
                smartMoneyActivity = smartMoneyActivity,
                optionConfirmationDetails = optionsDetails
            )
        }

        // 3. Sideways Market Check
        if (smartMoneyActivity.marketStructure == MarketStructureState.RANGEBOUND &&
            smartMoneyActivity.liquiditySweep == LiquiditySweepState.NONE &&
            smartMoneyActivity.chochState == ChochState.NONE &&
            smartMoneyActivity.orderBlock == OrderBlockState.NONE &&
            !smartMoneyActivity.isUnusualVolume
        ) {
            return SmartMoneySignal(
                action = SmartMoneySignalAction.NO_TRADE,
                confidence = SmartMoneyConfidence.LOW,
                suggestedStrike = "—",
                expiryDate = expiryDate,
                entryZone = "—",
                stopLoss = "—",
                target1 = "—",
                target2 = "—",
                shortReason = "Potential Smart Money Activity: Sideways consolidation without structural breakout, order blocks, or institutional volume. Awaiting edge.",
                detectionTime = detectionTimeStr,
                smartMoneyActivity = smartMoneyActivity,
                optionConfirmationDetails = optionsDetails
            )
        }

        // 4. BUY CE (Strong Bullish Confirmation)
        val isBullishSmartMoney = (smartMoneyActivity.spotPrice >= smartMoneyActivity.vwap || smartMoneyActivity.vwapState == VwapRelationship.RECLAIM) &&
                (smartMoneyActivity.marketStructure == MarketStructureState.BULLISH_STRUCTURE ||
                        smartMoneyActivity.liquiditySweep == LiquiditySweepState.BULLISH_SWEEP ||
                        smartMoneyActivity.chochState == ChochState.BULLISH_CHOCH ||
                        smartMoneyActivity.orderBlock == OrderBlockState.BULLISH_ORDER_BLOCK) &&
                (smartMoneyActivity.priceOiRegime == PriceOiRegime.LONG_BUILDUP || smartMoneyActivity.priceOiRegime == PriceOiRegime.SHORT_COVERING)

        if (isBullishSmartMoney && optionsDetails.isBullishConfirmed) {
            val atmStrike = optionsDetails.atmStrike
            val itmStrike = atmStrike - 50.0

            val suggestedStrikePrice = if (smartMoneyActivity.spotPrice > atmStrike + 15.0) itmStrike else atmStrike
            val strikeLabel = if (suggestedStrikePrice == itmStrike) {
                "${suggestedStrikePrice.toInt()} CE (1-step ITM)"
            } else {
                "${suggestedStrikePrice.toInt()} CE (ATM)"
            }

            val row = optionChain.rows.find { it.strikePrice == suggestedStrikePrice } ?: optionChain.rows.find { it.strikePrice == atmStrike }
            val contract = row?.callContract

            if (contract != null && contract.lastPrice > 0.0) {
                val actualLtp = contract.lastPrice
                val ask = if (contract.askPrice > 0.0) contract.askPrice else actualLtp
                val bid = if (contract.bidPrice > 0.0) contract.bidPrice else (actualLtp - 0.50)
                val entryPrice = ask

                val entryZone = "₹${formatPrice(bid)} - ₹${formatPrice(ask + 1.0)}"
                val riskAmount = max(15.0, OptionsSelector.roundToTickSize(entryPrice * 0.18))
                val slPrice = max(5.0, OptionsSelector.roundToTickSize(entryPrice - riskAmount))
                val t1Price = OptionsSelector.roundToTickSize(entryPrice + (riskAmount * 1.5))
                val t2Price = OptionsSelector.roundToTickSize(entryPrice + (riskAmount * 2.0))

                val lotSize = ContractSpecificationService.CURRENT_NIFTY_LOT_SIZE
                val estCharges = ChargesCalculator.calculate(entryPrice, t1Price, 1, lotSize).totalCharges
                val netReward = ((t1Price - entryPrice) * lotSize) - estCharges
                val netRisk = ((entryPrice - slPrice) * lotSize) + estCharges
                val netRr = if (netRisk > 0.0) Math.round((netReward / netRisk) * 10.0) / 10.0 else 1.5

                val isStrong = (smartMoneyActivity.marketStructure == MarketStructureState.BULLISH_STRUCTURE &&
                        smartMoneyActivity.priceOiRegime == PriceOiRegime.LONG_BUILDUP &&
                        (smartMoneyActivity.isUnusualVolume || smartMoneyActivity.liquiditySweep == LiquiditySweepState.BULLISH_SWEEP || smartMoneyActivity.orderBlock == OrderBlockState.BULLISH_ORDER_BLOCK))

                val confidence = if (isStrong) SmartMoneyConfidence.STRONG else SmartMoneyConfidence.MODERATE

                val shortReason = buildString {
                    append("Potential Smart Money Activity: ")
                    append(smartMoneyActivity.marketStructure.displayName)
                    if (smartMoneyActivity.vwapState == VwapRelationship.RECLAIM) append(" + VWAP Reclaim")
                    if (smartMoneyActivity.orderBlock == OrderBlockState.BULLISH_ORDER_BLOCK) append(" + Demand Order Block")
                    if (smartMoneyActivity.chochState == ChochState.BULLISH_CHOCH) append(" + Bullish CHoCH")
                    append(" confirmed by Futures ${smartMoneyActivity.priceOiRegime.displayName}")
                    if (smartMoneyActivity.futuresOiChangePercent != 0.0) {
                        append(" (${if (smartMoneyActivity.futuresOiChangePercent > 0) "+" else ""}${String.format(Locale.US, "%.1f", smartMoneyActivity.futuresOiChangePercent)}% OI)")
                    }
                    append(" & Put Writing floor at ${optionsDetails.atmStrike.toInt()} PE (PCR: ${String.format(Locale.US, "%.2f", optionsDetails.pcr)}).")
                }

                return SmartMoneySignal(
                    action = SmartMoneySignalAction.BUY_CE,
                    confidence = confidence,
                    suggestedStrike = strikeLabel,
                    strikePrice = suggestedStrikePrice,
                    optionType = OptionType.CE,
                    expiryDate = expiryDate,
                    entryZone = entryZone,
                    entryPrice = entryPrice,
                    stopLoss = "₹${formatPrice(slPrice)}",
                    stopLossValue = slPrice,
                    target1 = "₹${formatPrice(t1Price)}",
                    target1Value = t1Price,
                    target2 = "₹${formatPrice(t2Price)}",
                    target2Value = t2Price,
                    shortReason = shortReason,
                    riskRewardRatio = 1.5,
                    netRiskRewardRatio = netRr,
                    estimatedCharges = estCharges,
                    detectionTime = detectionTimeStr,
                    smartMoneyActivity = smartMoneyActivity,
                    optionConfirmationDetails = optionsDetails
                )
            }
        }

        // 5. BUY PE (Strong Bearish Confirmation)
        val isBearishSmartMoney = (smartMoneyActivity.spotPrice <= smartMoneyActivity.vwap || smartMoneyActivity.vwapState == VwapRelationship.REJECTION) &&
                (smartMoneyActivity.marketStructure == MarketStructureState.BEARISH_STRUCTURE ||
                        smartMoneyActivity.liquiditySweep == LiquiditySweepState.BEARISH_SWEEP ||
                        smartMoneyActivity.chochState == ChochState.BEARISH_CHOCH ||
                        smartMoneyActivity.orderBlock == OrderBlockState.BEARISH_ORDER_BLOCK) &&
                (smartMoneyActivity.priceOiRegime == PriceOiRegime.SHORT_BUILDUP || smartMoneyActivity.priceOiRegime == PriceOiRegime.LONG_UNWINDING)

        if (isBearishSmartMoney && optionsDetails.isBearishConfirmed) {
            val atmStrike = optionsDetails.atmStrike
            val itmStrike = atmStrike + 50.0

            val suggestedStrikePrice = if (smartMoneyActivity.spotPrice < atmStrike - 15.0) itmStrike else atmStrike
            val strikeLabel = if (suggestedStrikePrice == itmStrike) {
                "${suggestedStrikePrice.toInt()} PE (1-step ITM)"
            } else {
                "${suggestedStrikePrice.toInt()} PE (ATM)"
            }

            val row = optionChain.rows.find { it.strikePrice == suggestedStrikePrice } ?: optionChain.rows.find { it.strikePrice == atmStrike }
            val contract = row?.putContract

            if (contract != null && contract.lastPrice > 0.0) {
                val actualLtp = contract.lastPrice
                val ask = if (contract.askPrice > 0.0) contract.askPrice else actualLtp
                val bid = if (contract.bidPrice > 0.0) contract.bidPrice else (actualLtp - 0.50)
                val entryPrice = ask

                val entryZone = "₹${formatPrice(bid)} - ₹${formatPrice(ask + 1.0)}"
                val riskAmount = max(15.0, OptionsSelector.roundToTickSize(entryPrice * 0.18))
                val slPrice = max(5.0, OptionsSelector.roundToTickSize(entryPrice - riskAmount))
                val t1Price = OptionsSelector.roundToTickSize(entryPrice + (riskAmount * 1.5))
                val t2Price = OptionsSelector.roundToTickSize(entryPrice + (riskAmount * 2.0))

                val lotSize = ContractSpecificationService.CURRENT_NIFTY_LOT_SIZE
                val estCharges = ChargesCalculator.calculate(entryPrice, t1Price, 1, lotSize).totalCharges
                val netReward = ((t1Price - entryPrice) * lotSize) - estCharges
                val netRisk = ((entryPrice - slPrice) * lotSize) + estCharges
                val netRr = if (netRisk > 0.0) Math.round((netReward / netRisk) * 10.0) / 10.0 else 1.5

                val isStrong = (smartMoneyActivity.marketStructure == MarketStructureState.BEARISH_STRUCTURE &&
                        smartMoneyActivity.priceOiRegime == PriceOiRegime.SHORT_BUILDUP &&
                        (smartMoneyActivity.isUnusualVolume || smartMoneyActivity.liquiditySweep == LiquiditySweepState.BEARISH_SWEEP || smartMoneyActivity.orderBlock == OrderBlockState.BEARISH_ORDER_BLOCK))

                val confidence = if (isStrong) SmartMoneyConfidence.STRONG else SmartMoneyConfidence.MODERATE

                val shortReason = buildString {
                    append("Potential Smart Money Activity: ")
                    append(smartMoneyActivity.marketStructure.displayName)
                    if (smartMoneyActivity.vwapState == VwapRelationship.REJECTION) append(" + VWAP Rejection")
                    if (smartMoneyActivity.orderBlock == OrderBlockState.BEARISH_ORDER_BLOCK) append(" + Supply Order Block")
                    if (smartMoneyActivity.chochState == ChochState.BEARISH_CHOCH) append(" + Bearish CHoCH")
                    append(" confirmed by Futures ${smartMoneyActivity.priceOiRegime.displayName}")
                    if (smartMoneyActivity.futuresOiChangePercent != 0.0) {
                        append(" (${if (smartMoneyActivity.futuresOiChangePercent > 0) "+" else ""}${String.format(Locale.US, "%.1f", smartMoneyActivity.futuresOiChangePercent)}% OI)")
                    }
                    append(" & Call Writing ceiling at ${optionsDetails.atmStrike.toInt()} CE (PCR: ${String.format(Locale.US, "%.2f", optionsDetails.pcr)}).")
                }

                return SmartMoneySignal(
                    action = SmartMoneySignalAction.BUY_PE,
                    confidence = confidence,
                    suggestedStrike = strikeLabel,
                    strikePrice = suggestedStrikePrice,
                    optionType = OptionType.PE,
                    expiryDate = expiryDate,
                    entryZone = entryZone,
                    entryPrice = entryPrice,
                    stopLoss = "₹${formatPrice(slPrice)}",
                    stopLossValue = slPrice,
                    target1 = "₹${formatPrice(t1Price)}",
                    target1Value = t1Price,
                    target2 = "₹${formatPrice(t2Price)}",
                    target2Value = t2Price,
                    shortReason = shortReason,
                    riskRewardRatio = 1.5,
                    netRiskRewardRatio = netRr,
                    estimatedCharges = estCharges,
                    detectionTime = detectionTimeStr,
                    smartMoneyActivity = smartMoneyActivity,
                    optionConfirmationDetails = optionsDetails
                )
            }
        }

        // 6. Insufficient confirmation
        val missingDetails = mutableListOf<String>()
        if (!optionsDetails.isBullishConfirmed && !optionsDetails.isBearishConfirmed) {
            missingDetails.add("Option Chain OI/PCR confirmation pending")
        }
        if (smartMoneyActivity.priceOiRegime == PriceOiRegime.NEUTRAL) {
            missingDetails.add("Neutral Futures OI")
        }
        if (smartMoneyActivity.marketStructure == MarketStructureState.RANGEBOUND) {
            missingDetails.add("No structural breakout")
        }

        val missingMsg = if (missingDetails.isNotEmpty()) {
            "Potential Smart Money Activity: Insufficient confirmation (${missingDetails.joinToString(", ")}). Rule: Never trade on VWAP or RSI alone."
        } else {
            "Potential Smart Money Activity: Insufficient institutional confluence. Standby until high-probability setup aligns."
        }

        return SmartMoneySignal(
            action = SmartMoneySignalAction.NO_TRADE,
            confidence = SmartMoneyConfidence.LOW,
            suggestedStrike = "—",
            expiryDate = expiryDate,
            entryZone = "—",
            stopLoss = "—",
            target1 = "—",
            target2 = "—",
            shortReason = missingMsg,
            detectionTime = detectionTimeStr,
            smartMoneyActivity = smartMoneyActivity,
            optionConfirmationDetails = optionsDetails
        )
    }

    private fun formatPrice(price: Double): String {
        return String.format(Locale.US, "%.2f", price)
    }

    private fun formatTimeToIst(epochMillis: Long): String {
        val sdf = SimpleDateFormat("hh:mm a 'IST'", Locale.ENGLISH).apply {
            timeZone = istTimeZone
        }
        return sdf.format(Date(epochMillis))
    }
}
