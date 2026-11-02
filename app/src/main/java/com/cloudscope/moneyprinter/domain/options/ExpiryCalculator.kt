package com.cloudscope.moneyprinter.domain.options

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

object ExpiryCalculator {

    private val istTimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    data class ExpiryInfo(
        val currentWeekly: String,
        val nextWeekly: String,
        val monthly: String,
        val allExpiries: List<String>,
        val timeToCurrentExpiryDays: Double,
        val timeToCurrentExpiryYears: Double
    )

    fun calculateUpcomingExpiries(currentEpochMillis: Long = System.currentTimeMillis()): ExpiryInfo {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).apply {
            timeZone = istTimeZone
        }

        val cal = Calendar.getInstance(istTimeZone).apply {
            timeInMillis = currentEpochMillis
        }

        // If today is Thursday after 15:30 IST, current contract is expired
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val isThursdayAfterMarket = (dayOfWeek == Calendar.THURSDAY) && (hour > 15 || (hour == 15 && minute >= 30))

        // Find upcoming Thursday (Current Weekly)
        val currentWeeklyCal = (cal.clone() as Calendar).apply {
            if (isThursdayAfterMarket) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            while (get(Calendar.DAY_OF_WEEK) != Calendar.THURSDAY) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Next Weekly (+7 days)
        val nextWeeklyCal = (currentWeeklyCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 7)
        }

        // Monthly Expiry (Last Thursday of current month, or next month if already passed)
        val monthlyCal = findMonthlyExpiry(currentWeeklyCal)

        val currentWeeklyStr = dateFormat.format(currentWeeklyCal.time).uppercase()
        val nextWeeklyStr = dateFormat.format(nextWeeklyCal.time).uppercase()
        val monthlyStr = dateFormat.format(monthlyCal.time).uppercase()

        val millisRemaining = max(0L, currentWeeklyCal.timeInMillis - currentEpochMillis)
        val daysRemaining = max(0.1, millisRemaining / (1000.0 * 60.0 * 60.0 * 24.0))
        val yearsRemaining = daysRemaining / 365.0

        val list = mutableListOf(currentWeeklyStr, nextWeeklyStr)
        if (!list.contains(monthlyStr)) {
            list.add(monthlyStr)
        }

        return ExpiryInfo(
            currentWeekly = currentWeeklyStr,
            nextWeekly = nextWeeklyStr,
            monthly = monthlyStr,
            allExpiries = list,
            timeToCurrentExpiryDays = daysRemaining,
            timeToCurrentExpiryYears = yearsRemaining
        )
    }

    private fun findMonthlyExpiry(fromCal: Calendar): Calendar {
        val checkMonth = fromCal.clone() as Calendar
        val lastDayOfMonth = checkMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        checkMonth.set(Calendar.DAY_OF_MONTH, lastDayOfMonth)

        while (checkMonth.get(Calendar.DAY_OF_WEEK) != Calendar.THURSDAY) {
            checkMonth.add(Calendar.DAY_OF_MONTH, -1)
        }
        checkMonth.set(Calendar.HOUR_OF_DAY, 15)
        checkMonth.set(Calendar.MINUTE, 30)

        // If fromCal is already after this month's last Thursday, find next month's last Thursday
        return if (fromCal.timeInMillis > checkMonth.timeInMillis) {
            val nextMonthCal = fromCal.clone() as Calendar
            nextMonthCal.add(Calendar.MONTH, 1)
            val nextMaxDay = nextMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            nextMonthCal.set(Calendar.DAY_OF_MONTH, nextMaxDay)
            while (nextMonthCal.get(Calendar.DAY_OF_WEEK) != Calendar.THURSDAY) {
                nextMonthCal.add(Calendar.DAY_OF_MONTH, -1)
            }
            nextMonthCal.set(Calendar.HOUR_OF_DAY, 15)
            nextMonthCal.set(Calendar.MINUTE, 30)
            nextMonthCal
        } else {
            checkMonth
        }
    }
}
