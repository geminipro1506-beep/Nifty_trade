package com.cloudscope.moneyprinter.domain.signal

import com.cloudscope.moneyprinter.data.model.*
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.max

object SignalOutcomeTracker {

    private val IST_TIMEZONE = TimeZone.getTimeZone("Asia/Kolkata")

    /**
     * Converts a generated TradingSignal into an immutable HistoricalSignalRecord
     */
    fun createRecordFromSignal(
        signal: TradingSignal,
        indicators: TechnicalIndicators,
        isPaperTrade: Boolean = false
    ): HistoricalSignalRecord {
        return HistoricalSignalRecord(
            id = signal.signalId,
            timestamp = signal.timestamp,
            spotPrice = signal.underlyingSpot,
            marketDirection = signal.marketDirection,
            action = signal.action,
            contractSymbol = signal.contractSymbol,
            strikePrice = signal.recommendedStrike,
            optionType = signal.optionType,
            expiryDate = signal.expiryDate,
            premiumAtSignal = signal.premiumAtSignal,
            entryPrice = signal.entryPrice,
            stopLoss = signal.stopLoss,
            target1 = signal.target1,
            target2 = signal.target2,
            target3 = signal.target3,
            confidenceScore = signal.confidencePercent,
            rsi14 = indicators.rsi14,
            vwap = indicators.vwap,
            ema9 = indicators.ema9,
            ema20 = indicators.ema20,
            ema50 = indicators.ema50,
            pcr = indicators.pcr,
            vix = indicators.vix,
            reasons = signal.confirmationReasons,
            invalidationConditions = signal.invalidationConditions,
            entryMet = false,
            entryTimestamp = null,
            actualExitPrice = null,
            exitTimestamp = null,
            exitReason = null,
            status = SignalLifecycleStatus.PENDING_ENTRY,
            outcome = null,
            grossPnl = 0.0,
            netPnl = 0.0,
            totalCharges = 0.0,
            holdingTimeMinutes = 0,
            strategyType = signal.strategyType,
            timeframe = "5m",
            isPaperTrade = isPaperTrade,
            adjustmentsLog = listOf("Signal initialized in PENDING_ENTRY status at entry ₹${signal.entryPrice}")
        )
    }

    /**
     * Evaluates a signal record against current option prices (LTP, high, low) and market time.
     * Returns an updated HistoricalSignalRecord (or original if unchanged).
     */
    fun evaluateSignal(
        record: HistoricalSignalRecord,
        currentOptionLtp: Double,
        candleHigh: Double = currentOptionLtp,
        candleLow: Double = currentOptionLtp,
        currentSpotPrice: Double = record.spotPrice,
        currentTimestamp: Long = System.currentTimeMillis()
    ): HistoricalSignalRecord {
        if (record.status == SignalLifecycleStatus.CLOSED ||
            record.status == SignalLifecycleStatus.CANCELLED ||
            record.status == SignalLifecycleStatus.EXPIRED) {
            return record
        }

        val lotSize = 25
        val lots = 1
        val logs = record.adjustmentsLog.toMutableList()

        // 1. PENDING_ENTRY handling
        if (record.status == SignalLifecycleStatus.PENDING_ENTRY) {
            // Check if expired (pending for > 2 hours or after 15:00 IST without entry)
            val cal = Calendar.getInstance(IST_TIMEZONE).apply { timeInMillis = currentTimestamp }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            val isLateInDay = hour > 15 || (hour == 15 && minute >= 0)
            val twoHoursPassed = (currentTimestamp - record.timestamp) > (2 * 60 * 60 * 1000L)

            if (isLateInDay || twoHoursPassed) {
                logs.add("Signal expired unfilled after waiting period at ${formatTime(currentTimestamp)}")
                return record.copy(
                    status = SignalLifecycleStatus.EXPIRED,
                    outcome = TradeOutcome.UNFILLED,
                    exitReason = ExitReason.EXPIRED_UNFILLED,
                    exitTimestamp = currentTimestamp,
                    adjustmentsLog = logs
                )
            }

            // Entry condition: price touched entry price (market traded at or below entry price for BUY)
            val entryFilled = candleLow <= record.entryPrice || currentOptionLtp <= (record.entryPrice * 1.005)
            if (entryFilled) {
                logs.add("Entry confirmed filled at ₹${record.entryPrice} at ${formatTime(currentTimestamp)}")
                return record.copy(
                    entryMet = true,
                    entryTimestamp = currentTimestamp,
                    status = SignalLifecycleStatus.ACTIVE,
                    adjustmentsLog = logs
                )
            }

            return record
        }

        // 2. ACTIVE position handling
        val entryTime = record.entryTimestamp ?: record.timestamp
        val holdingMinutes = max(1L, (currentTimestamp - entryTime) / (60 * 1000L))

        // Check Ambiguous Candle: Target 1 and Stop Loss both triggered in the same candle
        val targetMet = candleHigh >= record.target1
        val slMet = candleLow <= record.stopLoss

        if (targetMet && slMet) {
            // Ambiguous candle resolution: do NOT assume favorable outcome!
            // Assume breakeven price with loss of transaction friction
            val exitPrice = record.entryPrice
            val (gross, net) = ChargesCalculator.calculateNetPnl(record.entryPrice, exitPrice, lots, lotSize)
            val charges = ChargesCalculator.calculate(record.entryPrice, exitPrice, lots, lotSize)
            logs.add("Ambiguous candle detected: Target and SL touched within same window. Exited at breakeven ₹$exitPrice at ${formatTime(currentTimestamp)}")

            return record.copy(
                status = SignalLifecycleStatus.CLOSED,
                actualExitPrice = exitPrice,
                exitTimestamp = currentTimestamp,
                exitReason = ExitReason.AMBIGUOUS,
                outcome = TradeOutcome.AMBIGUOUS,
                grossPnl = gross,
                netPnl = net,
                totalCharges = charges.totalCharges,
                holdingTimeMinutes = holdingMinutes,
                adjustmentsLog = logs
            )
        }

        // Target 3 Hit (Maximum target)
        if (record.target3 > 0 && candleHigh >= record.target3) {
            val exitPrice = record.target3
            val (gross, net) = ChargesCalculator.calculateNetPnl(record.entryPrice, exitPrice, lots, lotSize)
            val charges = ChargesCalculator.calculate(record.entryPrice, exitPrice, lots, lotSize)
            logs.add("Target 3 (₹$exitPrice) reached! Max profit captured at ${formatTime(currentTimestamp)}")

            return record.copy(
                status = SignalLifecycleStatus.CLOSED,
                actualExitPrice = exitPrice,
                exitTimestamp = currentTimestamp,
                exitReason = ExitReason.TARGET_3,
                outcome = TradeOutcome.WIN,
                grossPnl = gross,
                netPnl = net,
                totalCharges = charges.totalCharges,
                holdingTimeMinutes = holdingMinutes,
                adjustmentsLog = logs
            )
        }

        // Target 2 Hit
        if (candleHigh >= record.target2) {
            val exitPrice = record.target2
            val (gross, net) = ChargesCalculator.calculateNetPnl(record.entryPrice, exitPrice, lots, lotSize)
            val charges = ChargesCalculator.calculate(record.entryPrice, exitPrice, lots, lotSize)
            logs.add("Target 2 (₹$exitPrice) reached! Profit locked at ${formatTime(currentTimestamp)}")

            return record.copy(
                status = SignalLifecycleStatus.CLOSED,
                actualExitPrice = exitPrice,
                exitTimestamp = currentTimestamp,
                exitReason = ExitReason.TARGET_2,
                outcome = TradeOutcome.WIN,
                grossPnl = gross,
                netPnl = net,
                totalCharges = charges.totalCharges,
                holdingTimeMinutes = holdingMinutes,
                adjustmentsLog = logs
            )
        }

        // Target 1 Hit
        if (candleHigh >= record.target1) {
            val exitPrice = record.target1
            val (gross, net) = ChargesCalculator.calculateNetPnl(record.entryPrice, exitPrice, lots, lotSize)
            val charges = ChargesCalculator.calculate(record.entryPrice, exitPrice, lots, lotSize)
            logs.add("Target 1 (₹$exitPrice) reached at ${formatTime(currentTimestamp)}")

            return record.copy(
                status = SignalLifecycleStatus.CLOSED,
                actualExitPrice = exitPrice,
                exitTimestamp = currentTimestamp,
                exitReason = ExitReason.TARGET_1,
                outcome = TradeOutcome.WIN,
                grossPnl = gross,
                netPnl = net,
                totalCharges = charges.totalCharges,
                holdingTimeMinutes = holdingMinutes,
                adjustmentsLog = logs
            )
        }

        // Stop Loss Hit
        if (candleLow <= record.stopLoss) {
            val exitPrice = record.stopLoss
            val (gross, net) = ChargesCalculator.calculateNetPnl(record.entryPrice, exitPrice, lots, lotSize)
            val charges = ChargesCalculator.calculate(record.entryPrice, exitPrice, lots, lotSize)
            logs.add("Stop Loss triggered at ₹$exitPrice at ${formatTime(currentTimestamp)}")

            return record.copy(
                status = SignalLifecycleStatus.CLOSED,
                actualExitPrice = exitPrice,
                exitTimestamp = currentTimestamp,
                exitReason = ExitReason.STOP_LOSS,
                outcome = TradeOutcome.LOSS,
                grossPnl = gross,
                netPnl = net,
                totalCharges = charges.totalCharges,
                holdingTimeMinutes = holdingMinutes,
                adjustmentsLog = logs
            )
        }

        // 15:15 IST Mandatory Intraday Square-off
        val cal = Calendar.getInstance(IST_TIMEZONE).apply { timeInMillis = currentTimestamp }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        if (hour > 15 || (hour == 15 && minute >= 15)) {
            val exitPrice = currentOptionLtp
            val (gross, net) = ChargesCalculator.calculateNetPnl(record.entryPrice, exitPrice, lots, lotSize)
            val charges = ChargesCalculator.calculate(record.entryPrice, exitPrice, lots, lotSize)
            val outcome = when {
                gross > 0 -> TradeOutcome.WIN
                gross < 0 -> TradeOutcome.LOSS
                else -> TradeOutcome.BREAKEVEN
            }
            logs.add("15:15 IST Intraday time exit executed at ₹$exitPrice at ${formatTime(currentTimestamp)}")

            return record.copy(
                status = SignalLifecycleStatus.CLOSED,
                actualExitPrice = exitPrice,
                exitTimestamp = currentTimestamp,
                exitReason = ExitReason.SQUARE_OFF_1515,
                outcome = outcome,
                grossPnl = gross,
                netPnl = net,
                totalCharges = charges.totalCharges,
                holdingTimeMinutes = holdingMinutes,
                adjustmentsLog = logs
            )
        }

        return record
    }

    private fun formatTime(timestamp: Long): String {
        val cal = Calendar.getInstance(IST_TIMEZONE).apply { timeInMillis = timestamp }
        return String.format("%02d:%02d:%02d IST",
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            cal.get(Calendar.SECOND)
        )
    }
}
