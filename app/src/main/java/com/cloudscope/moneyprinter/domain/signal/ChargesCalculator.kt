package com.cloudscope.moneyprinter.domain.signal

import com.cloudscope.moneyprinter.domain.options.ContractSpecificationService
import kotlin.math.abs

data class TradeCharges(
    val brokerage: Double,
    val stt: Double,
    val exchangeCharges: Double,
    val gst: Double,
    val sebiAndStamp: Double,
    val slippageCost: Double,
    val totalCharges: Double
)

object ChargesCalculator {

    /**
     * Calculates realistic statutory charges and execution slippage for Indian NIFTY Options.
     * Standard institutional rules:
     * - Lot Size: Centralized ContractSpecificationService (Current: 65)
     * - Brokerage: ₹20 per executed order (₹40 round-trip)
     * - STT: 0.1% on sell turnover (premium * quantity)
     * - Exchange turnover fee: 0.05% on total turnover (buy + sell)
     * - GST: 18% on (brokerage + exchange turnover fee)
     * - SEBI Turnover charge: ₹10 per crore (0.0001%) + Stamp duty: 0.003% on buy turnover
     * - Slippage: 0.5 index points per lot roundtrip
     */
    fun calculate(
        buyPrice: Double,
        sellPrice: Double,
        lots: Int = 1,
        lotSize: Int = ContractSpecificationService.CURRENT_NIFTY_LOT_SIZE,
        brokeragePerOrder: Double = 20.0,
        sttPercentOnSell: Double = 0.1,
        exchangeTurnoverPercent: Double = 0.05,
        gstPercent: Double = 18.0,
        slippagePoints: Double = 0.5
    ): TradeCharges {
        val qty = lots * lotSize
        val buyTurnover = qty * buyPrice
        val sellTurnover = qty * sellPrice
        val totalTurnover = buyTurnover + sellTurnover

        val brokerage = brokeragePerOrder * 2.0 // Round-trip: ₹40
        val stt = sellTurnover * (sttPercentOnSell / 100.0)
        val exchangeCharges = totalTurnover * (exchangeTurnoverPercent / 100.0)
        val gst = (brokerage + exchangeCharges) * (gstPercent / 100.0)
        val sebiAndStamp = (totalTurnover * (10.0 / 10_000_000.0)) + (buyTurnover * (0.003 / 100.0))
        val slippageCost = qty * (slippagePoints * 2.0)

        val totalCharges = brokerage + stt + exchangeCharges + gst + sebiAndStamp + slippageCost

        return TradeCharges(
            brokerage = round2(brokerage),
            stt = round2(stt),
            exchangeCharges = round2(exchangeCharges),
            gst = round2(gst),
            sebiAndStamp = round2(sebiAndStamp),
            slippageCost = round2(slippageCost),
            totalCharges = round2(totalCharges)
        )
    }

    /**
     * Calculates gross and net P&L after friction
     * Gross P&L = (Exit Premium - Entry Premium) * Lot Size * Number of Lots.
     * Net P&L = Gross P&L - Actual applicable transaction charges.
     */
    fun calculateNetPnl(
        entryPrice: Double,
        exitPrice: Double,
        lots: Int = 1,
        lotSize: Int = ContractSpecificationService.CURRENT_NIFTY_LOT_SIZE
    ): Pair<Double, Double> {
        val qty = lots * lotSize
        val grossPnl = (exitPrice - entryPrice) * qty
        val charges = calculate(buyPrice = entryPrice, sellPrice = exitPrice, lots = lots, lotSize = lotSize)
        val netPnl = grossPnl - charges.totalCharges
        return Pair(round2(grossPnl), round2(netPnl))
    }

    private fun round2(value: Double): Double {
        return Math.round(value * 100.0) / 100.0
    }
}
