package com.cloudscope.moneyprinter.domain.options

import kotlin.math.*

object BlackScholes {

    // Standard Normal CDF approximation (Abramowitz and Stegun 7.1.26)
    fun cumulativeNormalDistribution(x: Double): Double {
        val a1 = 0.254829592
        val a2 = -0.284496736
        val a3 = 1.421413741
        val a4 = -1.453152027
        val a5 = 1.061405429
        val p = 0.3275911

        val sign = if (x < 0) -1 else 1
        val absX = abs(x) / sqrt(2.0)
        val t = 1.0 / (1.0 + p * absX)
        val erf = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * exp(-absX * absX)
        return 0.5 * (1.0 + sign * erf)
    }

    private fun normalProbabilityDensity(x: Double): Double {
        return (1.0 / sqrt(2.0 * PI)) * exp(-0.5 * x * x)
    }

    fun calculateDelta(
        spot: Double,
        strike: Double,
        timeToExpiryYears: Double,
        volatility: Double,
        isCall: Boolean,
        riskFreeRate: Double = 0.065
    ): Double {
        if (timeToExpiryYears <= 0.0 || volatility <= 0.0 || spot <= 0.0 || strike <= 0.0) {
            return if (isCall) (if (spot >= strike) 1.0 else 0.0) else (if (spot <= strike) -1.0 else 0.0)
        }

        val d1 = (ln(spot / strike) + (riskFreeRate + 0.5 * volatility * volatility) * timeToExpiryYears) /
                (volatility * sqrt(timeToExpiryYears))

        return if (isCall) {
            cumulativeNormalDistribution(d1)
        } else {
            cumulativeNormalDistribution(d1) - 1.0
        }
    }

    fun calculateGamma(
        spot: Double,
        strike: Double,
        timeToExpiryYears: Double,
        volatility: Double,
        riskFreeRate: Double = 0.065
    ): Double {
        if (timeToExpiryYears <= 0.0 || volatility <= 0.0 || spot <= 0.0 || strike <= 0.0) return 0.0
        val d1 = (ln(spot / strike) + (riskFreeRate + 0.5 * volatility * volatility) * timeToExpiryYears) /
                (volatility * sqrt(timeToExpiryYears))
        return normalProbabilityDensity(d1) / (spot * volatility * sqrt(timeToExpiryYears))
    }

    fun calculateThetaPerDay(
        spot: Double,
        strike: Double,
        timeToExpiryYears: Double,
        volatility: Double,
        isCall: Boolean,
        riskFreeRate: Double = 0.065
    ): Double {
        if (timeToExpiryYears <= 0.0 || volatility <= 0.0 || spot <= 0.0 || strike <= 0.0) return 0.0
        val sqrtT = sqrt(timeToExpiryYears)
        val d1 = (ln(spot / strike) + (riskFreeRate + 0.5 * volatility * volatility) * timeToExpiryYears) /
                (volatility * sqrtT)
        val d2 = d1 - volatility * sqrtT

        val term1 = -(spot * normalProbabilityDensity(d1) * volatility) / (2.0 * sqrtT)
        val term2 = if (isCall) {
            -riskFreeRate * strike * exp(-riskFreeRate * timeToExpiryYears) * cumulativeNormalDistribution(d2)
        } else {
            riskFreeRate * strike * exp(-riskFreeRate * timeToExpiryYears) * cumulativeNormalDistribution(-d2)
        }
        // Annualized theta divided by 365 days
        return (term1 + term2) / 365.0
    }

    // Solves for Implied Volatility using Newton-Raphson
    fun calculateImpliedVolatility(
        marketPrice: Double,
        spot: Double,
        strike: Double,
        timeToExpiryYears: Double,
        isCall: Boolean,
        riskFreeRate: Double = 0.065
    ): Double {
        if (marketPrice <= 0.0 || timeToExpiryYears <= 0.0 || spot <= 0.0 || strike <= 0.0) return 0.15

        val intrinsic = if (isCall) max(0.0, spot - strike) else max(0.0, strike - spot)
        if (marketPrice <= intrinsic) return 0.10

        var sigma = 0.20 // initial guess 20%
        val maxIterations = 20
        val tolerance = 0.01

        for (i in 0 until maxIterations) {
            val sqrtT = sqrt(timeToExpiryYears)
            val d1 = (ln(spot / strike) + (riskFreeRate + 0.5 * sigma * sigma) * timeToExpiryYears) / (sigma * sqrtT)
            val d2 = d1 - sigma * sqrtT

            val price = if (isCall) {
                spot * cumulativeNormalDistribution(d1) - strike * exp(-riskFreeRate * timeToExpiryYears) * cumulativeNormalDistribution(d2)
            } else {
                strike * exp(-riskFreeRate * timeToExpiryYears) * cumulativeNormalDistribution(-d2) - spot * cumulativeNormalDistribution(-d1)
            }

            val diff = price - marketPrice
            if (abs(diff) < tolerance) return sigma

            val vega = spot * sqrtT * normalProbabilityDensity(d1)
            if (vega < 1e-6) break

            sigma -= diff / vega
            if (sigma <= 0.01) sigma = 0.01
            if (sigma > 3.0) sigma = 3.0
        }
        return sigma
    }
}
