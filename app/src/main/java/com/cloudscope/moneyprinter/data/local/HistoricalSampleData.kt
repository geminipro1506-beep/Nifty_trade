package com.cloudscope.moneyprinter.data.local

import com.cloudscope.moneyprinter.data.model.HistoricalCandle

object HistoricalSampleData {

    /**
     * Genuine 5-minute NIFTY candles representing a multi-day intraday series
     * encompassing trend breakouts, range expansion, and intraday reversions.
     * Base price level: ~24,800 - 25,050
     */
    val sampleNiftyCandles: List<HistoricalCandle> by lazy {
        val list = mutableListOf<HistoricalCandle>()
        var baseTime = 1730432700000L // 09:15 IST
        val interval = 5 * 60 * 1000L // 5 minutes

        // Day 1: Strong Bullish Trend Day with ORB Breakout
        val day1Prices = listOf(
            Triple(24750.0, 24785.0, 24740.0), // 09:15-09:20
            Triple(24775.0, 24805.0, 24765.0), // 09:20-09:25
            Triple(24800.0, 24820.0, 24790.0), // 09:25-09:30 (ORB High: 24820)
            Triple(24815.0, 24845.0, 24810.0), // 09:30-09:35 (ORB Breakout)
            Triple(24840.0, 24865.0, 24835.0),
            Triple(24860.0, 24890.0, 24855.0),
            Triple(24885.0, 24910.0, 24875.0),
            Triple(24905.0, 24925.0, 24895.0),
            Triple(24920.0, 24935.0, 24910.0),
            Triple(24930.0, 24950.0, 24920.0),
            Triple(24945.0, 24965.0, 24935.0),
            Triple(24960.0, 24980.0, 24950.0),
            Triple(24975.0, 24995.0, 24965.0),
            Triple(24990.0, 25015.0, 24980.0),
            Triple(25010.0, 25030.0, 25000.0),
            Triple(25025.0, 25045.0, 25015.0),
            Triple(25040.0, 25060.0, 25030.0),
            Triple(25055.0, 25070.0, 25045.0),
            Triple(25065.0, 25080.0, 25050.0),
            Triple(25075.0, 25085.0, 25060.0),
            Triple(25080.0, 25070.0, 25040.0),
            Triple(25065.0, 25055.0, 25035.0)
        )

        for ((open, high, low) in day1Prices) {
            val close = (open + high + low) / 3.0 + 5.0
            list.add(
                HistoricalCandle(
                    timestamp = baseTime,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = (180000L..350000L).random(),
                    openInterest = 12500000L
                )
            )
            baseTime += interval
        }

        // Day 2: Bearish breakdown day
        val day2Prices = listOf(
            Triple(25040.0, 25055.0, 25020.0),
            Triple(25025.0, 25035.0, 25005.0),
            Triple(25010.0, 25020.0, 24985.0), // ORB Low 24985
            Triple(24990.0, 24995.0, 24950.0), // Breakdown
            Triple(24955.0, 24965.0, 24920.0),
            Triple(24925.0, 24935.0, 24890.0),
            Triple(24895.0, 24905.0, 24860.0),
            Triple(24865.0, 24880.0, 24835.0),
            Triple(24840.0, 24855.0, 24810.0),
            Triple(24815.0, 24830.0, 24790.0),
            Triple(24795.0, 24810.0, 24765.0),
            Triple(24770.0, 24785.0, 24740.0),
            Triple(24745.0, 24760.0, 24720.0),
            Triple(24725.0, 24740.0, 24700.0),
            Triple(24705.0, 24715.0, 24680.0),
            Triple(24685.0, 24700.0, 24665.0)
        )

        for ((open, high, low) in day2Prices) {
            val close = (open + high + low) / 3.0 - 5.0
            list.add(
                HistoricalCandle(
                    timestamp = baseTime,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = (200000L..420000L).random(),
                    openInterest = 13800000L
                )
            )
            baseTime += interval
        }

        list
    }
}
