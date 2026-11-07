package com.cloudscope.moneyprinter.domain.options

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Exchange Contract Master Service
 * Manages authorized NSE/BSE contract master schedules, trading holidays,
 * and valid unexpired expiry dates for NIFTY, BANKNIFTY, and SENSEX.
 */
object ExchangeContractMaster {

    val IST_TIME_ZONE: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")
    const val EXPIRY_UNAVAILABLE = "EXPIRY DATA UNAVAILABLE"

    data class TradableContractMeta(
        val symbol: String,
        val underlying: String,
        val expiryDate: String,
        val expiryTimestamp: Long,
        val strikeInterval: Double,
        val lotSize: Int,
        val tickSize: Double = 0.05,
        val isWeekly: Boolean = true
    )

    // Official NSE Trading Holidays (YYYY-MM-DD) for 2024, 2025, 2026, 2027
    private val nseTradingHolidays = setOf(
        // 2024
        "2024-01-22", "2024-01-26", "2024-03-08", "2024-03-25", "2024-03-29",
        "2024-04-11", "2024-04-17", "2024-05-01", "2024-05-20", "2024-06-17",
        "2024-07-17", "2024-08-15", "2024-10-02", "2024-11-01", "2024-11-15",
        "2024-12-25",
        // 2025
        "2025-01-26", "2025-02-26", "2025-03-14", "2025-03-31", "2025-04-10",
        "2025-04-14", "2025-04-18", "2025-05-01", "2025-06-07", "2025-08-15",
        "2025-08-27", "2025-10-02", "2025-10-21", "2025-10-22", "2025-11-05",
        "2025-12-25",
        // 2026 (Including Diwali / Bhai Dooj / Gurunanak holiday week where Nov 12 is an exchange holiday)
        "2026-01-26", "2026-02-16", "2026-03-04", "2026-03-20", "2026-04-03",
        "2026-04-14", "2026-05-01", "2026-05-27", "2026-08-15", "2026-08-26",
        "2026-10-02", "2026-10-20", "2026-11-09", "2026-11-10", "2026-11-12", // Nov 12, 2026 NSE Holiday
        "2026-11-24", "2026-12-25",
        // 2027
        "2027-01-26", "2027-03-08", "2027-03-23", "2027-03-26", "2027-04-14",
        "2027-05-01", "2027-08-15", "2027-10-02", "2027-11-01", "2027-12-25"
    )

    // Dynamic registry populated from exchange contract master
    private val liveContractMasterRegistry = mutableListOf<TradableContractMeta>()

    /**
     * Updates the registry when new exchange contract master is loaded from broker or NSE feed.
     */
    fun updateContractMaster(contracts: List<TradableContractMeta>) {
        synchronized(liveContractMasterRegistry) {
            liveContractMasterRegistry.clear()
            liveContractMasterRegistry.addAll(contracts)
        }
    }

    /**
     * Checks if a specific date (YYYY-MM-DD) is an official NSE trading holiday.
     */
    fun isTradingHoliday(dateStr: String): Boolean {
        return nseTradingHolidays.contains(dateStr)
    }

    /**
     * Formats calendar date into standard YYYY-MM-DD in IST.
     */
    fun toIsoDate(cal: Calendar): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply {
            timeZone = IST_TIME_ZONE
        }
        return sdf.format(cal.time)
    }

    /**
     * Formats calendar date into standard exchange display format (e.g. "11 NOV 2026").
     */
    fun toDisplayDate(cal: Calendar): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).apply {
            timeZone = IST_TIME_ZONE
        }
        return sdf.format(cal.time).uppercase()
    }

    /**
     * Parses a display date ("dd MMM yyyy" or "dd-MMM-yyyy") to epoch millis in IST.
     */
    fun parseDateToEpoch(dateStr: String): Long? {
        val formats = listOf("dd MMM yyyy", "dd-MMM-yyyy", "yyyy-MM-dd", "ddMMMyyyy")
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.ENGLISH).apply {
                    timeZone = IST_TIME_ZONE
                }
                val parsed = sdf.parse(dateStr.trim())
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
            }
        }
        return null
    }

    /**
     * Resolves the nearest valid and unexpired tradable expiry for the requested index.
     * Accounts for exchange holidays and schedule changes (preponement to previous trading day).
     *
     * @param underlying "NIFTY", "BANKNIFTY", or "SENSEX"
     * @param referenceTimestamp Market reference timestamp (defaults to current time)
     * @param availableChainExpiries Optional explicit list of expiries from live option chain
     * @return Formatted expiry string (e.g. "11 NOV 2026") or "EXPIRY DATA UNAVAILABLE"
     */
    fun resolveNearestValidExpiry(
        underlying: String = "NIFTY",
        referenceTimestamp: Long = System.currentTimeMillis(),
        availableChainExpiries: List<String>? = null
    ): String {
        val normalizedUnderlying = when {
            underlying.contains("BANK", ignoreCase = true) -> "BANKNIFTY"
            underlying.contains("SENSEX", ignoreCase = true) -> "SENSEX"
            else -> "NIFTY"
        }

        // 1. If live option chain provided valid expiries, filter unexpired and pick nearest
        if (!availableChainExpiries.isNullOrEmpty()) {
            val validFutureExpiries = availableChainExpiries.mapNotNull { expStr ->
                val epoch = parseDateToEpoch(expStr)
                if (epoch != null) {
                    val expiryCloseTime = getExpiryCloseTime(epoch)
                    if (expiryCloseTime > referenceTimestamp) {
                        Pair(expStr.uppercase(), expiryCloseTime)
                    } else null
                } else null
            }.sortedBy { it.second }

            if (validFutureExpiries.isNotEmpty()) {
                return validFutureExpiries.first().first
            }
        }

        // 2. Check live contract master registry if populated
        synchronized(liveContractMasterRegistry) {
            val matching = liveContractMasterRegistry
                .filter { it.underlying.equals(normalizedUnderlying, ignoreCase = true) }
                .filter { it.expiryTimestamp > referenceTimestamp }
                .sortedBy { it.expiryTimestamp }

            if (matching.isNotEmpty()) {
                return matching.first().expiryDate.uppercase()
            }
        }

        // 3. Fallback: Algorithmic calculation based on exchange rules & holiday calendar
        val cal = Calendar.getInstance(IST_TIME_ZONE).apply {
            timeInMillis = referenceTimestamp
        }

        val nearestCal = findNextValidExpiryCalendar(normalizedUnderlying, cal, referenceTimestamp)
            ?: return EXPIRY_UNAVAILABLE

        return toDisplayDate(nearestCal)
    }

    /**
     * Resolves upcoming expiry schedule (current weekly, next weekly, monthly).
     */
    fun resolveExpirySchedule(
        underlying: String = "NIFTY",
        referenceTimestamp: Long = System.currentTimeMillis(),
        availableChainExpiries: List<String>? = null
    ): ExpiryCalculator.ExpiryInfo {
        val currentExpiryStr = resolveNearestValidExpiry(underlying, referenceTimestamp, availableChainExpiries)

        if (currentExpiryStr == EXPIRY_UNAVAILABLE) {
            return ExpiryCalculator.ExpiryInfo(
                currentWeekly = EXPIRY_UNAVAILABLE,
                nextWeekly = EXPIRY_UNAVAILABLE,
                monthly = EXPIRY_UNAVAILABLE,
                allExpiries = emptyList(),
                timeToCurrentExpiryDays = 0.0,
                timeToCurrentExpiryYears = 0.0
            )
        }

        val currentEpoch = parseDateToEpoch(currentExpiryStr) ?: referenceTimestamp
        val currentCloseTime = getExpiryCloseTime(currentEpoch)

        val cal = Calendar.getInstance(IST_TIME_ZONE).apply {
            timeInMillis = currentCloseTime + (24 * 3600 * 1000L) // day after current expiry
        }

        val nextWeeklyCal = findNextValidExpiryCalendar(underlying, cal, cal.timeInMillis)
        val nextWeeklyStr = nextWeeklyCal?.let { toDisplayDate(it) } ?: currentExpiryStr

        val monthlyCal = findMonthlyExpiryCalendar(underlying, currentEpoch)
        val monthlyStr = monthlyCal?.let { toDisplayDate(it) } ?: currentExpiryStr

        val allList = mutableListOf(currentExpiryStr)
        if (!allList.contains(nextWeeklyStr) && nextWeeklyStr != EXPIRY_UNAVAILABLE) {
            allList.add(nextWeeklyStr)
        }
        if (!allList.contains(monthlyStr) && monthlyStr != EXPIRY_UNAVAILABLE) {
            allList.add(monthlyStr)
        }

        val millisRemaining = kotlin.math.max(0L, currentCloseTime - referenceTimestamp)
        val daysRemaining = kotlin.math.max(0.05, millisRemaining / (1000.0 * 60.0 * 60.0 * 24.0))
        val yearsRemaining = daysRemaining / 365.0

        return ExpiryCalculator.ExpiryInfo(
            currentWeekly = currentExpiryStr,
            nextWeekly = nextWeeklyStr,
            monthly = monthlyStr,
            allExpiries = allList,
            timeToCurrentExpiryDays = daysRemaining,
            timeToCurrentExpiryYears = yearsRemaining
        )
    }

    /**
     * Finds next valid unexpired expiry calendar taking holidays and preponement into account.
     */
    private fun findNextValidExpiryCalendar(
        underlying: String,
        baseCal: Calendar,
        referenceTimestamp: Long
    ): Calendar? {
        val targetDayOfWeek = when (underlying) {
            "SENSEX" -> Calendar.FRIDAY // BSE Sensex expiries on Friday
            "BANKNIFTY" -> Calendar.THURSDAY // BankNifty monthly on Thursday
            else -> Calendar.THURSDAY // Nifty weekly on Thursday
        }

        val cal = (baseCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Loop forward up to 60 days to find upcoming valid expiry
        for (i in 0..60) {
            val candidateCal = (cal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, i)
            }

            // Expiry day candidate
            if (candidateCal.get(Calendar.DAY_OF_WEEK) == targetDayOfWeek) {
                // Check if this scheduled day is a holiday; if so, prepone to previous trading day
                val validExpiryCal = adjustForTradingHolidays(candidateCal)

                if (validExpiryCal.timeInMillis > referenceTimestamp) {
                    return validExpiryCal
                }
            }
        }
        return null
    }

    /**
     * If the scheduled expiry day is a trading holiday or weekend,
     * prepone to the immediately preceding valid exchange trading day.
     */
    private fun adjustForTradingHolidays(scheduledCal: Calendar): Calendar {
        val checkCal = scheduledCal.clone() as Calendar
        while (isHolidayOrWeekend(checkCal)) {
            checkCal.add(Calendar.DAY_OF_MONTH, -1) // Prepone by 1 day
        }
        return checkCal
    }

    private fun isHolidayOrWeekend(cal: Calendar): Boolean {
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return true
        }
        val isoDate = toIsoDate(cal)
        return isTradingHoliday(isoDate)
    }

    private fun findMonthlyExpiryCalendar(underlying: String, fromTimestamp: Long): Calendar? {
        val cal = Calendar.getInstance(IST_TIME_ZONE).apply {
            timeInMillis = fromTimestamp
        }
        val targetDayOfWeek = when (underlying) {
            "SENSEX" -> Calendar.FRIDAY
            else -> Calendar.THURSDAY
        }

        val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, lastDay)
        cal.set(Calendar.HOUR_OF_DAY, 15)
        cal.set(Calendar.MINUTE, 30)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        while (cal.get(Calendar.DAY_OF_WEEK) != targetDayOfWeek) {
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }

        val adjusted = adjustForTradingHolidays(cal)
        return if (adjusted.timeInMillis > fromTimestamp) {
            adjusted
        } else {
            // Roll to next month
            val nextMonthCal = (cal.clone() as Calendar).apply {
                add(Calendar.MONTH, 1)
                val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                set(Calendar.DAY_OF_MONTH, maxDay)
            }
            while (nextMonthCal.get(Calendar.DAY_OF_WEEK) != targetDayOfWeek) {
                nextMonthCal.add(Calendar.DAY_OF_MONTH, -1)
            }
            adjustForTradingHolidays(nextMonthCal)
        }
    }

    private fun getExpiryCloseTime(epochMillis: Long): Long {
        val cal = Calendar.getInstance(IST_TIME_ZONE).apply {
            timeInMillis = epochMillis
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
