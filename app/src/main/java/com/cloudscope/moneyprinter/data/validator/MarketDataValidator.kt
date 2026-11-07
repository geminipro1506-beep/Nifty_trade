package com.cloudscope.moneyprinter.data.validator

import com.cloudscope.moneyprinter.data.model.MarketQuote
import com.cloudscope.moneyprinter.data.model.OptionChain
import kotlin.math.abs

/**
 * Validates incoming market quotes and option chain data before signal generation.
 * Prevents invalid prices, out-of-order ticks, stale premiums, or abnormal jumps
 * from corrupting signal engines.
 */
object MarketDataValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val isStale: Boolean,
        val latencyMs: Long,
        val reason: String = "OK"
    )

    private const val MAX_TOLERABLE_STALENESS_MS = 180_000L // 3 minutes
    private const val MAX_ACCEPTABLE_JUMP_PERCENT = 15.0 // Max 15% instant intraday spot jump without validation

    /**
     * Validates a market quote against integrity checks.
     */
    fun validateQuote(
        quote: MarketQuote?,
        previousQuote: MarketQuote? = null,
        currentTimestamp: Long = System.currentTimeMillis()
    ): ValidationResult {
        if (quote == null) {
            return ValidationResult(
                isValid = false,
                isStale = true,
                latencyMs = 0L,
                reason = "Market quote is null"
            )
        }

        // 1. Price checks
        if (quote.lastPrice <= 0.0 || quote.lastPrice.isNaN() || quote.lastPrice.isInfinite()) {
            return ValidationResult(
                isValid = false,
                isStale = false,
                latencyMs = 0L,
                reason = "Invalid spot price: ${quote.lastPrice}"
            )
        }

        if (quote.high < quote.low || quote.high <= 0.0 || quote.low <= 0.0) {
            return ValidationResult(
                isValid = false,
                isStale = false,
                latencyMs = 0L,
                reason = "Invalid High/Low range: H=${quote.high}, L=${quote.low}"
            )
        }

        // 2. Abnormal Jump Check
        if (previousQuote != null && previousQuote.lastPrice > 0.0) {
            val jumpPercent = abs(quote.lastPrice - previousQuote.lastPrice) / previousQuote.lastPrice * 100.0
            if (jumpPercent > MAX_ACCEPTABLE_JUMP_PERCENT) {
                return ValidationResult(
                    isValid = false,
                    isStale = false,
                    latencyMs = 0L,
                    reason = "Abnormal price jump detected: ${String.format(java.util.Locale.US, "%.1f", jumpPercent)}%"
                )
            }
        }

        // 3. Staleness & Latency
        val latencyMs = currentTimestamp - quote.timestamp
        val isStale = latencyMs > MAX_TOLERABLE_STALENESS_MS

        if (isStale) {
            return ValidationResult(
                isValid = false,
                isStale = true,
                latencyMs = latencyMs,
                reason = "Stale tick data: latency is ${latencyMs / 1000}s"
            )
        }

        return ValidationResult(
            isValid = true,
            isStale = false,
            latencyMs = maxOf(0L, latencyMs),
            reason = "Valid quote"
        )
    }

    /**
     * Validates Option Chain integrity.
     */
    fun validateOptionChain(
        chain: OptionChain?,
        spotPrice: Double
    ): ValidationResult {
        if (chain == null || chain.rows.isEmpty()) {
            return ValidationResult(
                isValid = false,
                isStale = true,
                latencyMs = 0L,
                reason = "Option chain empty or unavailable"
            )
        }

        if (chain.atmStrike <= 0.0 || abs(chain.atmStrike - spotPrice) > 200.0) {
            return ValidationResult(
                isValid = false,
                isStale = false,
                latencyMs = 0L,
                reason = "ATM strike mismatch: ATM=${chain.atmStrike}, Spot=$spotPrice"
            )
        }

        val atmRow = chain.rows.find { it.strikePrice == chain.atmStrike }
        if (atmRow == null || (atmRow.callContract?.lastPrice ?: 0.0) <= 0.0 || (atmRow.putContract?.lastPrice ?: 0.0) <= 0.0) {
            return ValidationResult(
                isValid = false,
                isStale = false,
                latencyMs = 0L,
                reason = "ATM contract premiums missing or zero"
            )
        }

        return ValidationResult(
            isValid = true,
            isStale = false,
            latencyMs = 0L,
            reason = "Valid option chain"
        )
    }
}
