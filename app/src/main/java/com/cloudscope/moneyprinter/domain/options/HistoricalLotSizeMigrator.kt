package com.cloudscope.moneyprinter.domain.options

import com.cloudscope.moneyprinter.data.model.HistoricalSignalRecord
import com.cloudscope.moneyprinter.data.model.SignalLifecycleStatus
import com.cloudscope.moneyprinter.domain.signal.ChargesCalculator

/**
 * Historical Data Migration Engine.
 * Recalculates legacy signals generated with lot size 25 to the authorized NSE specification (65).
 * Preserves original gross P&L, net P&L, and lot size for statutory audit reconciliation.
 * Flags missing or ambiguous data for manual compliance review.
 */
object HistoricalLotSizeMigrator {

    const val STATUS_MIGRATED_LOT_SIZE_65 = "MIGRATED_LOT_SIZE_65"
    const val STATUS_MIGRATED_UNFILLED = "MIGRATED_UNFILLED"
    const val STATUS_FLAGGED_MANUAL_REVIEW = "FLAGGED_MANUAL_REVIEW"

    fun migrateRecord(record: HistoricalSignalRecord): HistoricalSignalRecord {
        if (record.lotSize != 25 && record.lotSize != 0) {
            return record
        }

        val currentLots = if (record.lots > 0) record.lots else 1
        val originalGross = record.grossPnl
        val originalNet = record.netPnl
        val originalLotSize = if (record.lotSize > 0) record.lotSize else 25

        val exitPrice = record.actualExitPrice
        return if (record.status == SignalLifecycleStatus.CLOSED && exitPrice != null && record.entryPrice > 0.0) {
            val (newGross, newNet) = ChargesCalculator.calculateNetPnl(
                entryPrice = record.entryPrice,
                exitPrice = exitPrice,
                lots = currentLots,
                lotSize = ContractSpecificationService.CURRENT_NIFTY_LOT_SIZE
            )
            val newCharges = ChargesCalculator.calculate(
                buyPrice = record.entryPrice,
                sellPrice = exitPrice,
                lots = currentLots,
                lotSize = ContractSpecificationService.CURRENT_NIFTY_LOT_SIZE
            ).totalCharges

            val newTotalQty = currentLots * ContractSpecificationService.CURRENT_NIFTY_LOT_SIZE

            record.copy(
                lots = currentLots,
                lotSize = ContractSpecificationService.CURRENT_NIFTY_LOT_SIZE,
                grossPnl = newGross,
                netPnl = newNet,
                totalCharges = newCharges,
                originalGrossPnl = originalGross,
                originalNetPnl = originalNet,
                originalLotSize = originalLotSize,
                migrationStatus = STATUS_MIGRATED_LOT_SIZE_65,
                migrationNotes = "Recalculated with authorized NIFTY lot size 65 (Qty: $newTotalQty). Original Gross: ₹$originalGross, Original Net: ₹$originalNet."
            )
        } else if (record.status == SignalLifecycleStatus.CANCELLED || record.status == SignalLifecycleStatus.EXPIRED) {
            record.copy(
                lots = currentLots,
                lotSize = ContractSpecificationService.CURRENT_NIFTY_LOT_SIZE,
                grossPnl = 0.0,
                netPnl = 0.0,
                totalCharges = 0.0,
                originalGrossPnl = originalGross,
                originalNetPnl = originalNet,
                originalLotSize = originalLotSize,
                migrationStatus = STATUS_MIGRATED_UNFILLED,
                migrationNotes = "Unfilled/Cancelled order updated to authorized specification (65 qty/lot)."
            )
        } else {
            record.copy(
                lots = currentLots,
                lotSize = ContractSpecificationService.CURRENT_NIFTY_LOT_SIZE,
                originalGrossPnl = originalGross,
                originalNetPnl = originalNet,
                originalLotSize = originalLotSize,
                migrationStatus = STATUS_FLAGGED_MANUAL_REVIEW,
                migrationNotes = "Original execution pricing unavailable. Retained original values for manual audit."
            )
        }
    }
}
