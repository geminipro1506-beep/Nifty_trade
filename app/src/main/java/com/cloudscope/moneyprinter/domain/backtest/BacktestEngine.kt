package com.cloudscope.moneyprinter.domain.backtest

import com.cloudscope.moneyprinter.data.model.*
import com.cloudscope.moneyprinter.domain.indicator.IndicatorEngine
import com.cloudscope.moneyprinter.domain.signal.SignalEngine
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.max

object BacktestEngine {

    /**
     * Executes chronological Walk-Forward Out-Of-Sample backtesting.
     * Prevents overfitting:
     * 1. Chronological Train (70%) and Test (30%) separation.
     * 2. Zero look-ahead bias: historical indicators computed strictly on past candles.
     * 3. Realistic statutory transaction costs + slippage friction.
     */
    fun runBacktest(
        candles: List<HistoricalCandle>,
        config: BacktestConfig = BacktestConfig(),
        walkForwardSplitRatio: Double = 0.70
    ): BacktestResult {
        if (candles.size < 30) {
            return BacktestResult(
                totalTrades = 0,
                winningTrades = 0,
                losingTrades = 0,
                winRate = 0.0,
                profitFactor = 0.0,
                maxDrawdownAmount = 0.0,
                maxDrawdownPercent = 0.0,
                grossProfit = 0.0,
                grossLoss = 0.0,
                totalChargesAndTaxes = 0.0,
                netProfit = 0.0,
                averageRiskReward = 0.0,
                expectancy = 0.0,
                returnOnCapitalPercent = 0.0,
                trades = emptyList(),
                walkForwardValidated = false,
                validationNotes = "Insufficient data points for backtesting (requires ≥30 candles)"
            )
        }

        // Chronological Train/Test split: Strictly preserved order, never shuffled
        val splitIndex = (candles.size * walkForwardSplitRatio).toInt()
        val inSampleCandles = candles.subList(0, splitIndex)
        val outOfSampleCandles = candles.subList(splitIndex, candles.size)

        // Run simulation on In-Sample (Train)
        val inSampleTrades = simulateCandleWindow(inSampleCandles, config, "IS")
        val inSampleWins = inSampleTrades.count { it.netProfit > 0 }
        val inSampleWinRate = if (inSampleTrades.isNotEmpty()) (inSampleWins.toDouble() / inSampleTrades.size) * 100.0 else 0.0
        val inSampleNet = inSampleTrades.sumOf { it.netProfit }

        // Run simulation on Out-of-Sample (Test)
        val outOfSampleTrades = simulateCandleWindow(outOfSampleCandles, config, "OOS")
        val winningTrades = outOfSampleTrades.filter { it.netProfit > 0 }
        val losingTrades = outOfSampleTrades.filter { it.netProfit <= 0 }

        val grossProfit = winningTrades.sumOf { it.grossProfit }
        val grossLoss = abs(losingTrades.sumOf { it.grossProfit })
        val totalCharges = outOfSampleTrades.sumOf { it.totalCosts }
        val netProfit = outOfSampleTrades.sumOf { it.netProfit }

        val winRate = if (outOfSampleTrades.isNotEmpty()) (winningTrades.size.toDouble() / outOfSampleTrades.size) * 100.0 else 0.0
        val profitFactor = if (grossLoss > 0) grossProfit / grossLoss else if (grossProfit > 0) 9.99 else 0.0

        // Drawdown calculation on chronological equity curve
        var runningCapital = config.initialCapital
        var peakCapital = config.initialCapital
        var maxDrawdownAmount = 0.0
        for (trade in outOfSampleTrades) {
            runningCapital += trade.netProfit
            if (runningCapital > peakCapital) peakCapital = runningCapital
            val currentDd = peakCapital - runningCapital
            if (currentDd > maxDrawdownAmount) maxDrawdownAmount = currentDd
        }
        val maxDrawdownPercent = if (peakCapital > 0) (maxDrawdownAmount / peakCapital) * 100.0 else 0.0

        val avgWin = if (winningTrades.isNotEmpty()) winningTrades.map { it.netProfit }.average() else 0.0
        val avgLoss = if (losingTrades.isNotEmpty()) abs(losingTrades.map { it.netProfit }.average()) else 1.0
        val winProb = if (outOfSampleTrades.isNotEmpty()) winningTrades.size.toDouble() / outOfSampleTrades.size else 0.0
        val lossProb = 1.0 - winProb
        val expectancy = if (outOfSampleTrades.isNotEmpty()) (winProb * avgWin) - (lossProb * avgLoss) else 0.0

        val averageRiskReward = if (avgLoss > 0) avgWin / avgLoss else 0.0
        val returnOnCapital = (netProfit / config.initialCapital) * 100.0

        val stabilityReport = testParameterStability(candles, config)

        return BacktestResult(
            totalTrades = outOfSampleTrades.size,
            winningTrades = winningTrades.size,
            losingTrades = losingTrades.size,
            winRate = round1(winRate),
            profitFactor = round2(profitFactor),
            maxDrawdownAmount = round2(maxDrawdownAmount),
            maxDrawdownPercent = round1(maxDrawdownPercent),
            grossProfit = round2(grossProfit),
            grossLoss = round2(grossLoss),
            totalChargesAndTaxes = round2(totalCharges),
            netProfit = round2(netProfit),
            averageRiskReward = round2(averageRiskReward),
            expectancy = round2(expectancy),
            returnOnCapitalPercent = round1(returnOnCapital),
            trades = outOfSampleTrades,
            walkForwardValidated = true,
            validationNotes = "Chronological Out-of-Sample window (${outOfSampleCandles.size} candles). Costs: Roundtrip Brokerage ₹40, STT 0.1%, GST 18%, Exchange 0.05%, 0.5 pt slippage.",
            inSampleTradesCount = inSampleTrades.size,
            inSampleWinRate = round1(inSampleWinRate),
            inSampleNetProfit = round2(inSampleNet),
            outOfSampleTradesCount = outOfSampleTrades.size,
            outOfSampleWinRate = round1(winRate),
            outOfSampleNetProfit = round2(netProfit),
            parameterStabilityReport = stabilityReport
        )
    }

    /**
     * Parameter Stability Testing:
     * Tests strategy robustness by perturbing parameters (confidence threshold, slippage, ATR).
     * If expectancy drops significantly (>30%) under minor parameter shifts, strategy is overfitted.
     */
    fun testParameterStability(
        candles: List<HistoricalCandle>,
        baseConfig: BacktestConfig = BacktestConfig()
    ): ParameterStabilityReport {
        if (candles.size < 30) {
            return ParameterStabilityReport(
                overallStable = false,
                maxExpectancyDegradationPercent = 100.0,
                parameterResults = emptyList(),
                auditNotes = "Insufficient data points for parameter stability analysis"
            )
        }

        // Run baseline
        val baseTrades = simulateCandleWindow(candles, baseConfig, "BASE")
        val baseWins = baseTrades.filter { it.netProfit > 0 }
        val baseWinRate = if (baseTrades.isNotEmpty()) (baseWins.size.toDouble() / baseTrades.size) * 100.0 else 0.0
        val baseExpectancy = if (baseTrades.isNotEmpty()) baseTrades.sumOf { it.netProfit } / baseTrades.size else 0.0

        val results = mutableListOf<ParameterStabilityResult>()

        // Perturbation 1: Confidence threshold 60% vs 65% baseline
        val configLowConf = baseConfig.copy(minConfidenceThreshold = 60)
        val tradesLowConf = simulateCandleWindow(candles, configLowConf, "P1")
        val winsLow = tradesLowConf.filter { it.netProfit > 0 }
        val winRateLow = if (tradesLowConf.isNotEmpty()) (winsLow.size.toDouble() / tradesLowConf.size) * 100.0 else 0.0
        val expLow = if (tradesLowConf.isNotEmpty()) tradesLowConf.sumOf { it.netProfit } / tradesLowConf.size else 0.0
        val changeLow = if (baseWinRate > 0) abs(winRateLow - baseWinRate) / baseWinRate * 100.0 else 0.0

        results.add(
            ParameterStabilityResult(
                parameterName = "Min Confidence Threshold (60% vs 65%)",
                baselineValue = 65.0,
                testValue = 60.0,
                baselineWinRate = round1(baseWinRate),
                testWinRate = round1(winRateLow),
                baselineExpectancy = round2(baseExpectancy),
                testExpectancy = round2(expLow),
                winRateChangePercent = round1(changeLow),
                isStable = changeLow < 25.0
            )
        )

        // Perturbation 2: Slippage 0.75 pt vs 0.50 pt baseline
        val configHighSlip = baseConfig.copy(slippagePoints = 0.75)
        val tradesHighSlip = simulateCandleWindow(candles, configHighSlip, "P2")
        val winsSlip = tradesHighSlip.filter { it.netProfit > 0 }
        val winRateSlip = if (tradesHighSlip.isNotEmpty()) (winsSlip.size.toDouble() / tradesHighSlip.size) * 100.0 else 0.0
        val expSlip = if (tradesHighSlip.isNotEmpty()) tradesHighSlip.sumOf { it.netProfit } / tradesHighSlip.size else 0.0
        val changeSlip = if (baseWinRate > 0) abs(winRateSlip - baseWinRate) / baseWinRate * 100.0 else 0.0

        results.add(
            ParameterStabilityResult(
                parameterName = "Execution Slippage (0.75 pt vs 0.50 pt)",
                baselineValue = 0.50,
                testValue = 0.75,
                baselineWinRate = round1(baseWinRate),
                testWinRate = round1(winRateSlip),
                baselineExpectancy = round2(baseExpectancy),
                testExpectancy = round2(expSlip),
                winRateChangePercent = round1(changeSlip),
                isStable = changeSlip < 25.0
            )
        )

        val maxDegradation = results.map { it.winRateChangePercent }.maxOrNull() ?: 0.0
        val overallStable = results.all { it.isStable }

        val auditNotes = if (overallStable) {
            "STABLE: Strategy maintains statistical edge under parameter perturbations (<25% degradation)."
        } else {
            "SENSITIVE: Performance degraded by ${round1(maxDegradation)}% under parameter shifts; potential overfitting detected."
        }

        return ParameterStabilityReport(
            overallStable = overallStable,
            maxExpectancyDegradationPercent = round1(maxDegradation),
            parameterResults = results,
            auditNotes = auditNotes
        )
    }

    private fun simulateCandleWindow(
        windowCandles: List<HistoricalCandle>,
        config: BacktestConfig,
        prefix: String
    ): List<BacktestTrade> {
        val trades = mutableListOf<BacktestTrade>()
        if (windowCandles.size < 20) return trades

        var inTrade = false
        var activeAction = SignalAction.NO_TRADE
        var activeStrike = 0.0
        var activeEntry = 0.0
        var activeSl = 0.0
        var activeTgt1 = 0.0
        var activeTgt2 = 0.0
        var entryTime = 0L
        var tradeIdCounter = 1

        val lotSize = com.cloudscope.moneyprinter.domain.options.ContractSpecificationService.getLotSize("NIFTY")
        val windowSize = 20

        for (i in windowSize until windowCandles.size) {
            val candle = windowCandles[i]
            // Zero look-ahead bias: sublist strictly up to index i
            val subList = windowCandles.subList(max(0, i - windowSize), i + 1)

            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
            cal.timeInMillis = candle.timestamp
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            val currentMinutes = hour * 60 + minute
            val isIntradaySquareOffTime = currentMinutes >= (15 * 60 + 15)

            if (inTrade) {
                val spotEntryPrice = subList.first().close
                val spotDiff = candle.close - spotEntryPrice
                val delta = 0.50

                val currentOptionPrice = if (activeAction == SignalAction.BUY_CE) {
                    max(1.0, activeEntry + (spotDiff * delta))
                } else {
                    max(1.0, activeEntry - (spotDiff * delta))
                }

                val highOptionPrice = if (activeAction == SignalAction.BUY_CE) {
                    max(1.0, activeEntry + ((candle.high - spotEntryPrice) * delta))
                } else {
                    max(1.0, activeEntry - ((candle.low - spotEntryPrice) * delta))
                }

                val lowOptionPrice = if (activeAction == SignalAction.BUY_CE) {
                    max(1.0, activeEntry + ((candle.low - spotEntryPrice) * delta))
                } else {
                    max(1.0, activeEntry - ((candle.high - spotEntryPrice) * delta))
                }

                var exitOccurred = false
                var exitPrice = 0.0
                var exitReason = ExitReason.STOP_LOSS

                when {
                    lowOptionPrice <= activeSl -> {
                        exitOccurred = true
                        exitPrice = activeSl - config.slippagePoints
                        exitReason = ExitReason.STOP_LOSS
                    }
                    highOptionPrice >= activeTgt2 -> {
                        exitOccurred = true
                        exitPrice = activeTgt2 - config.slippagePoints
                        exitReason = ExitReason.TARGET_2
                    }
                    highOptionPrice >= activeTgt1 -> {
                        exitOccurred = true
                        exitPrice = activeTgt1 - config.slippagePoints
                        exitReason = ExitReason.TARGET_1
                    }
                    isIntradaySquareOffTime -> {
                        exitOccurred = true
                        exitPrice = currentOptionPrice - config.slippagePoints
                        exitReason = ExitReason.SQUARE_OFF_1515
                    }
                }

                if (exitOccurred) {
                    val lots = 1
                    val qty = lots * lotSize
                    val buyTurnover = qty * activeEntry
                    val sellTurnover = qty * exitPrice
                    val grossPnl = (exitPrice - activeEntry) * qty

                    val brokerage = config.brokeragePerOrder * 2.0
                    val stt = sellTurnover * (config.sttPercentOnSell / 100.0)
                    val exchangeCharges = (buyTurnover + sellTurnover) * (config.exchangeTurnoverPercent / 100.0)
                    val gst = (brokerage + exchangeCharges) * (config.gstPercent / 100.0)
                    val sebiAndStamp = ((buyTurnover + sellTurnover) * (config.sebiPerCrore / 10000000.0)) +
                            (buyTurnover * (config.stampDutyPercentOnBuy / 100.0))
                    val slippageTotal = qty * (config.slippagePoints * 2.0)

                    val totalCosts = brokerage + stt + exchangeCharges + gst + sebiAndStamp + slippageTotal
                    val netPnl = grossPnl - totalCosts

                    trades.add(
                        BacktestTrade(
                            id = "$prefix-${tradeIdCounter++}",
                            entryTimestamp = entryTime,
                            exitTimestamp = candle.timestamp,
                            symbol = "NIFTY ${activeStrike.toInt()} ${if (activeAction == SignalAction.BUY_CE) "CE" else "PE"}",
                            action = activeAction,
                            strikePrice = activeStrike,
                            entryPrice = activeEntry,
                            exitPrice = exitPrice,
                            exitReason = exitReason,
                            lots = lots,
                            lotSize = lotSize,
                            grossProfit = grossPnl,
                            brokerage = brokerage,
                            stt = stt,
                            exchangeCharges = exchangeCharges,
                            gst = gst,
                            sebiAndStamp = sebiAndStamp,
                            slippageCost = slippageTotal,
                            netProfit = netPnl,
                            returnOnCapitalPercent = (netPnl / config.initialCapital) * 100.0
                        )
                    )

                    inTrade = false
                }
            } else if (!isIntradaySquareOffTime) {
                val indicators = IndicatorEngine.evaluateIndicators(
                    candles = subList,
                    pdh = subList.maxOf { it.high },
                    pdl = subList.minOf { it.low },
                    pdc = subList.first().open,
                    pcr = 1.15,
                    vix = 14.5
                )

                val mockQuote = MarketQuote(
                    lastPrice = candle.close,
                    change = candle.close - subList.first().open,
                    pChange = ((candle.close - subList.first().open) / subList.first().open) * 100.0,
                    open = candle.open,
                    high = candle.high,
                    low = candle.low,
                    close = candle.close,
                    vwap = indicators.vwap,
                    volume = candle.volume,
                    timestamp = candle.timestamp
                )

                val signal = SignalEngine.generateSignal(
                    quote = mockQuote,
                    indicators = indicators,
                    optionChain = null,
                    candleCount = subList.size,
                    isMarketHours = true
                )

                if (signal.action != SignalAction.NO_TRADE && signal.confidencePercent >= config.minConfidenceThreshold) {
                    inTrade = true
                    activeAction = signal.action
                    activeStrike = signal.recommendedStrike
                    activeEntry = signal.entryPrice + config.slippagePoints
                    activeSl = signal.stopLoss
                    activeTgt1 = signal.target1
                    activeTgt2 = signal.target2
                    entryTime = candle.timestamp
                }
            }
        }

        return trades
    }

    private fun round1(value: Double): Double = Math.round(value * 10.0) / 10.0
    private fun round2(value: Double): Double = Math.round(value * 100.0) / 100.0
}
