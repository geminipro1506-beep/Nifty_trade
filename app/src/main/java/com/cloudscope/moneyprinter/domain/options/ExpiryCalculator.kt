package com.cloudscope.moneyprinter.domain.options

import java.util.TimeZone

/**
 * Expiry Calculator for Indian Derivative Markets (NSE/BSE).
 * Delegates to ExchangeContractMaster to ensure compliance with official exchange holiday schedules,
 * holiday preponement rules, contract master data, and SEBI derivatives regulations.
 */
object ExpiryCalculator {

    val istTimeZone: TimeZone = ExchangeContractMaster.IST_TIME_ZONE

    data class ExpiryInfo(
        val currentWeekly: String,
        val nextWeekly: String,
        val monthly: String,
        val allExpiries: List<String>,
        val timeToCurrentExpiryDays: Double,
        val timeToCurrentExpiryYears: Double
    )

    /**
     * Calculates valid unexpired upcoming expiries for NIFTY 50.
     * Accurately accounts for exchange holidays and automatically prepones expiry to previous trading day.
     */
    fun calculateUpcomingExpiries(
        currentEpochMillis: Long = System.currentTimeMillis(),
        availableChainExpiries: List<String>? = null
    ): ExpiryInfo {
        return ExchangeContractMaster.resolveExpirySchedule(
            underlying = "NIFTY",
            referenceTimestamp = currentEpochMillis,
            availableChainExpiries = availableChainExpiries
        )
    }

    /**
     * Resolves the nearest valid and unexpired tradable expiry date string.
     */
    fun resolveNearestExpiry(
        underlying: String = "NIFTY",
        referenceTimestamp: Long = System.currentTimeMillis(),
        availableChainExpiries: List<String>? = null
    ): String {
        return ExchangeContractMaster.resolveNearestValidExpiry(
            underlying = underlying,
            referenceTimestamp = referenceTimestamp,
            availableChainExpiries = availableChainExpiries
        )
    }
}
