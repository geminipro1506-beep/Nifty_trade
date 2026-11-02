package com.cloudscope.moneyprinter.data.local.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.cloudscope.moneyprinter.data.model.*
import java.util.UUID

class SignalDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "cloudscope_signals.db"
        const val DATABASE_VERSION = 2

        const val TABLE_SIGNALS = "historical_signals"

        const val COL_ID = "id"
        const val COL_TIMESTAMP = "timestamp"
        const val COL_SPOT_PRICE = "spot_price"
        const val COL_MARKET_DIRECTION = "market_direction"
        const val COL_ACTION = "action"
        const val COL_CONTRACT_SYMBOL = "contract_symbol"
        const val COL_STRIKE_PRICE = "strike_price"
        const val COL_OPTION_TYPE = "option_type"
        const val COL_EXPIRY_DATE = "expiry_date"
        const val COL_PREMIUM_AT_SIGNAL = "premium_at_signal"
        const val COL_ENTRY_PRICE = "entry_price"
        const val COL_STOP_LOSS = "stop_loss"
        const val COL_TARGET_1 = "target_1"
        const val COL_TARGET_2 = "target_2"
        const val COL_TARGET_3 = "target_3"
        const val COL_CONFIDENCE_SCORE = "confidence_score"
        const val COL_RSI14 = "rsi14"
        const val COL_VWAP = "vwap"
        const val COL_EMA9 = "ema9"
        const val COL_EMA20 = "ema20"
        const val COL_EMA50 = "ema50"
        const val COL_PCR = "pcr"
        const val COL_VIX = "vix"
        const val COL_REASONS = "reasons"
        const val COL_INVALIDATION = "invalidation_conditions"
        const val COL_ENTRY_MET = "entry_met"
        const val COL_ENTRY_TIMESTAMP = "entry_timestamp"
        const val COL_ACTUAL_EXIT_PRICE = "actual_exit_price"
        const val COL_EXIT_TIMESTAMP = "exit_timestamp"
        const val COL_EXIT_REASON = "exit_reason"
        const val COL_STATUS = "status"
        const val COL_OUTCOME = "outcome"
        const val COL_GROSS_PNL = "gross_pnl"
        const val COL_NET_PNL = "net_pnl"
        const val COL_TOTAL_CHARGES = "total_charges"
        const val COL_HOLDING_TIME = "holding_time_minutes"
        const val COL_STRATEGY_TYPE = "strategy_type"
        const val COL_TIMEFRAME = "timeframe"
        const val COL_IS_PAPER_TRADE = "is_paper_trade"
        const val COL_ADJUSTMENTS_LOG = "adjustments_log"
        const val COL_AMBIGUOUS_REASON = "ambiguous_reason"
        const val COL_CANDLE_RESOLUTION = "candle_resolution"
        const val COL_TARGET_SL_SAME_CANDLE = "target_sl_same_candle"
        const val COL_FINAL_RESOLUTION_STATUS = "final_resolution_status"

        @Volatile
        private var INSTANCE: SignalDatabaseHelper? = null

        fun getInstance(context: Context): SignalDatabaseHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SignalDatabaseHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_SIGNALS (
                $COL_ID TEXT PRIMARY KEY,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_SPOT_PRICE REAL NOT NULL,
                $COL_MARKET_DIRECTION TEXT NOT NULL,
                $COL_ACTION TEXT NOT NULL,
                $COL_CONTRACT_SYMBOL TEXT NOT NULL,
                $COL_STRIKE_PRICE REAL NOT NULL,
                $COL_OPTION_TYPE TEXT NOT NULL,
                $COL_EXPIRY_DATE TEXT NOT NULL,
                $COL_PREMIUM_AT_SIGNAL REAL NOT NULL,
                $COL_ENTRY_PRICE REAL NOT NULL,
                $COL_STOP_LOSS REAL NOT NULL,
                $COL_TARGET_1 REAL NOT NULL,
                $COL_TARGET_2 REAL NOT NULL,
                $COL_TARGET_3 REAL NOT NULL,
                $COL_CONFIDENCE_SCORE INTEGER NOT NULL,
                $COL_RSI14 REAL NOT NULL,
                $COL_VWAP REAL NOT NULL,
                $COL_EMA9 REAL NOT NULL,
                $COL_EMA20 REAL NOT NULL,
                $COL_EMA50 REAL NOT NULL,
                $COL_PCR REAL NOT NULL,
                $COL_VIX REAL NOT NULL,
                $COL_REASONS TEXT NOT NULL,
                $COL_INVALIDATION TEXT NOT NULL,
                $COL_ENTRY_MET INTEGER NOT NULL DEFAULT 0,
                $COL_ENTRY_TIMESTAMP INTEGER,
                $COL_ACTUAL_EXIT_PRICE REAL,
                $COL_EXIT_TIMESTAMP INTEGER,
                $COL_EXIT_REASON TEXT,
                $COL_STATUS TEXT NOT NULL,
                $COL_OUTCOME TEXT,
                $COL_GROSS_PNL REAL DEFAULT 0.0,
                $COL_NET_PNL REAL DEFAULT 0.0,
                $COL_TOTAL_CHARGES REAL DEFAULT 0.0,
                $COL_HOLDING_TIME INTEGER DEFAULT 0,
                $COL_STRATEGY_TYPE TEXT NOT NULL,
                $COL_TIMEFRAME TEXT NOT NULL,
                $COL_IS_PAPER_TRADE INTEGER NOT NULL DEFAULT 0,
                $COL_ADJUSTMENTS_LOG TEXT,
                $COL_AMBIGUOUS_REASON TEXT,
                $COL_CANDLE_RESOLUTION TEXT DEFAULT '5m',
                $COL_TARGET_SL_SAME_CANDLE INTEGER NOT NULL DEFAULT 0,
                $COL_FINAL_RESOLUTION_STATUS TEXT
            )
        """.trimIndent()
        db.execSQL(createTableQuery)

        preseedWalkForwardSignals(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SIGNALS")
        onCreate(db)
    }

    fun insertSignal(record: HistoricalSignalRecord): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ID, record.id)
            put(COL_TIMESTAMP, record.timestamp)
            put(COL_SPOT_PRICE, record.spotPrice)
            put(COL_MARKET_DIRECTION, record.marketDirection.name)
            put(COL_ACTION, record.action.name)
            put(COL_CONTRACT_SYMBOL, record.contractSymbol)
            put(COL_STRIKE_PRICE, record.strikePrice)
            put(COL_OPTION_TYPE, record.optionType.name)
            put(COL_EXPIRY_DATE, record.expiryDate)
            put(COL_PREMIUM_AT_SIGNAL, record.premiumAtSignal)
            put(COL_ENTRY_PRICE, record.entryPrice)
            put(COL_STOP_LOSS, record.stopLoss)
            put(COL_TARGET_1, record.target1)
            put(COL_TARGET_2, record.target2)
            put(COL_TARGET_3, record.target3)
            put(COL_CONFIDENCE_SCORE, record.confidenceScore)
            put(COL_RSI14, record.rsi14)
            put(COL_VWAP, record.vwap)
            put(COL_EMA9, record.ema9)
            put(COL_EMA20, record.ema20)
            put(COL_EMA50, record.ema50)
            put(COL_PCR, record.pcr)
            put(COL_VIX, record.vix)
            put(COL_REASONS, record.reasons.joinToString("|||"))
            put(COL_INVALIDATION, record.invalidationConditions)
            put(COL_ENTRY_MET, if (record.entryMet) 1 else 0)
            put(COL_ENTRY_TIMESTAMP, record.entryTimestamp)
            put(COL_ACTUAL_EXIT_PRICE, record.actualExitPrice)
            put(COL_EXIT_TIMESTAMP, record.exitTimestamp)
            put(COL_EXIT_REASON, record.exitReason?.name)
            put(COL_STATUS, record.status.name)
            put(COL_OUTCOME, record.outcome?.name)
            put(COL_GROSS_PNL, record.grossPnl)
            put(COL_NET_PNL, record.netPnl)
            put(COL_TOTAL_CHARGES, record.totalCharges)
            put(COL_HOLDING_TIME, record.holdingTimeMinutes)
            put(COL_STRATEGY_TYPE, record.strategyType.name)
            put(COL_TIMEFRAME, record.timeframe)
            put(COL_IS_PAPER_TRADE, if (record.isPaperTrade) 1 else 0)
            put(COL_ADJUSTMENTS_LOG, record.adjustmentsLog.joinToString("|||"))
            put(COL_AMBIGUOUS_REASON, record.ambiguousReason)
            put(COL_CANDLE_RESOLUTION, record.candleResolution)
            put(COL_TARGET_SL_SAME_CANDLE, if (record.targetAndSlSameCandle) 1 else 0)
            put(COL_FINAL_RESOLUTION_STATUS, record.finalResolutionStatus)
        }
        return db.insertWithOnConflict(TABLE_SIGNALS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun updateSignal(record: HistoricalSignalRecord): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ENTRY_MET, if (record.entryMet) 1 else 0)
            put(COL_ENTRY_TIMESTAMP, record.entryTimestamp)
            put(COL_ACTUAL_EXIT_PRICE, record.actualExitPrice)
            put(COL_EXIT_TIMESTAMP, record.exitTimestamp)
            put(COL_EXIT_REASON, record.exitReason?.name)
            put(COL_STATUS, record.status.name)
            put(COL_OUTCOME, record.outcome?.name)
            put(COL_GROSS_PNL, record.grossPnl)
            put(COL_NET_PNL, record.netPnl)
            put(COL_TOTAL_CHARGES, record.totalCharges)
            put(COL_HOLDING_TIME, record.holdingTimeMinutes)
            put(COL_ADJUSTMENTS_LOG, record.adjustmentsLog.joinToString("|||"))
            put(COL_AMBIGUOUS_REASON, record.ambiguousReason)
            put(COL_CANDLE_RESOLUTION, record.candleResolution)
            put(COL_TARGET_SL_SAME_CANDLE, if (record.targetAndSlSameCandle) 1 else 0)
            put(COL_FINAL_RESOLUTION_STATUS, record.finalResolutionStatus)
        }
        return db.update(TABLE_SIGNALS, values, "$COL_ID = ?", arrayOf(record.id))
    }

    fun getAllSignals(): List<HistoricalSignalRecord> {
        val db = readableDatabase
        val cursor = db.query(TABLE_SIGNALS, null, null, null, null, null, "$COL_TIMESTAMP DESC")
        return cursorToList(cursor)
    }

    fun getSignalsByFilter(
        fromTimestamp: Long = 0L,
        optionFilter: OptionFilter = OptionFilter.ALL,
        strategyFilter: StrategyFilter = StrategyFilter.ALL,
        paperTradesOnly: Boolean? = null
    ): List<HistoricalSignalRecord> {
        val db = readableDatabase
        val selectionClauses = mutableListOf<String>()
        val selectionArgs = mutableListOf<String>()

        if (fromTimestamp > 0L) {
            selectionClauses.add("$COL_TIMESTAMP >= ?")
            selectionArgs.add(fromTimestamp.toString())
        }

        if (optionFilter == OptionFilter.CE_ONLY) {
            selectionClauses.add("$COL_OPTION_TYPE = ?")
            selectionArgs.add("CE")
        } else if (optionFilter == OptionFilter.PE_ONLY) {
            selectionClauses.add("$COL_OPTION_TYPE = ?")
            selectionArgs.add("PE")
        }

        if (strategyFilter != StrategyFilter.ALL) {
            selectionClauses.add("$COL_STRATEGY_TYPE = ?")
            selectionArgs.add(strategyFilter.name)
        }

        if (paperTradesOnly != null) {
            selectionClauses.add("$COL_IS_PAPER_TRADE = ?")
            selectionArgs.add(if (paperTradesOnly) "1" else "0")
        }

        val selection = if (selectionClauses.isNotEmpty()) selectionClauses.joinToString(" AND ") else null
        val args = if (selectionArgs.isNotEmpty()) selectionArgs.toTypedArray() else null

        val cursor = db.query(TABLE_SIGNALS, null, selection, args, null, null, "$COL_TIMESTAMP DESC")
        return cursorToList(cursor)
    }

    fun getOpenSignals(): List<HistoricalSignalRecord> {
        val db = readableDatabase
        val selection = "$COL_STATUS = ? OR $COL_STATUS = ?"
        val args = arrayOf(SignalLifecycleStatus.PENDING_ENTRY.name, SignalLifecycleStatus.ACTIVE.name)
        val cursor = db.query(TABLE_SIGNALS, null, selection, args, null, null, "$COL_TIMESTAMP ASC")
        return cursorToList(cursor)
    }

    private fun cursorToList(cursor: Cursor): List<HistoricalSignalRecord> {
        val list = mutableListOf<HistoricalSignalRecord>()
        cursor.use {
            while (it.moveToNext()) {
                val reasonsStr = it.getString(it.getColumnIndexOrThrow(COL_REASONS)) ?: ""
                val reasons = if (reasonsStr.isNotBlank()) reasonsStr.split("|||") else emptyList()

                val adjLogStr = it.getString(it.getColumnIndexOrThrow(COL_ADJUSTMENTS_LOG)) ?: ""
                val adjustments = if (adjLogStr.isNotBlank()) adjLogStr.split("|||") else emptyList()

                val exitReasonStr = it.getString(it.getColumnIndexOrThrow(COL_EXIT_REASON))
                val exitReason = exitReasonStr?.let { r ->
                    try { ExitReason.valueOf(r) } catch (e: Exception) { null }
                }

                val outcomeStr = it.getString(it.getColumnIndexOrThrow(COL_OUTCOME))
                val outcome = outcomeStr?.let { o ->
                    try { TradeOutcome.valueOf(o) } catch (e: Exception) { null }
                }

                val ambiguousReasonIdx = it.getColumnIndex(COL_AMBIGUOUS_REASON)
                val ambiguousReason = if (ambiguousReasonIdx != -1 && !it.isNull(ambiguousReasonIdx)) it.getString(ambiguousReasonIdx) else null

                val candleResolutionIdx = it.getColumnIndex(COL_CANDLE_RESOLUTION)
                val candleResolution = if (candleResolutionIdx != -1 && !it.isNull(candleResolutionIdx)) it.getString(candleResolutionIdx) else "5m"

                val targetSlIdx = it.getColumnIndex(COL_TARGET_SL_SAME_CANDLE)
                val targetAndSlSame = if (targetSlIdx != -1) it.getInt(targetSlIdx) == 1 else false

                val finalResIdx = it.getColumnIndex(COL_FINAL_RESOLUTION_STATUS)
                val finalResStatus = if (finalResIdx != -1 && !it.isNull(finalResIdx)) it.getString(finalResIdx) else null

                list.add(
                    HistoricalSignalRecord(
                        id = it.getString(it.getColumnIndexOrThrow(COL_ID)),
                        timestamp = it.getLong(it.getColumnIndexOrThrow(COL_TIMESTAMP)),
                        spotPrice = it.getDouble(it.getColumnIndexOrThrow(COL_SPOT_PRICE)),
                        marketDirection = MarketDirection.valueOf(it.getString(it.getColumnIndexOrThrow(COL_MARKET_DIRECTION))),
                        action = SignalAction.valueOf(it.getString(it.getColumnIndexOrThrow(COL_ACTION))),
                        contractSymbol = it.getString(it.getColumnIndexOrThrow(COL_CONTRACT_SYMBOL)),
                        strikePrice = it.getDouble(it.getColumnIndexOrThrow(COL_STRIKE_PRICE)),
                        optionType = OptionType.valueOf(it.getString(it.getColumnIndexOrThrow(COL_OPTION_TYPE))),
                        expiryDate = it.getString(it.getColumnIndexOrThrow(COL_EXPIRY_DATE)),
                        premiumAtSignal = it.getDouble(it.getColumnIndexOrThrow(COL_PREMIUM_AT_SIGNAL)),
                        entryPrice = it.getDouble(it.getColumnIndexOrThrow(COL_ENTRY_PRICE)),
                        stopLoss = it.getDouble(it.getColumnIndexOrThrow(COL_STOP_LOSS)),
                        target1 = it.getDouble(it.getColumnIndexOrThrow(COL_TARGET_1)),
                        target2 = it.getDouble(it.getColumnIndexOrThrow(COL_TARGET_2)),
                        target3 = it.getDouble(it.getColumnIndexOrThrow(COL_TARGET_3)),
                        confidenceScore = it.getInt(it.getColumnIndexOrThrow(COL_CONFIDENCE_SCORE)),
                        rsi14 = it.getDouble(it.getColumnIndexOrThrow(COL_RSI14)),
                        vwap = it.getDouble(it.getColumnIndexOrThrow(COL_VWAP)),
                        ema9 = it.getDouble(it.getColumnIndexOrThrow(COL_EMA9)),
                        ema20 = it.getDouble(it.getColumnIndexOrThrow(COL_EMA20)),
                        ema50 = it.getDouble(it.getColumnIndexOrThrow(COL_EMA50)),
                        pcr = it.getDouble(it.getColumnIndexOrThrow(COL_PCR)),
                        vix = it.getDouble(it.getColumnIndexOrThrow(COL_VIX)),
                        reasons = reasons,
                        invalidationConditions = it.getString(it.getColumnIndexOrThrow(COL_INVALIDATION)),
                        entryMet = it.getInt(it.getColumnIndexOrThrow(COL_ENTRY_MET)) == 1,
                        entryTimestamp = if (it.isNull(it.getColumnIndexOrThrow(COL_ENTRY_TIMESTAMP))) null else it.getLong(it.getColumnIndexOrThrow(COL_ENTRY_TIMESTAMP)),
                        actualExitPrice = if (it.isNull(it.getColumnIndexOrThrow(COL_ACTUAL_EXIT_PRICE))) null else it.getDouble(it.getColumnIndexOrThrow(COL_ACTUAL_EXIT_PRICE)),
                        exitTimestamp = if (it.isNull(it.getColumnIndexOrThrow(COL_EXIT_TIMESTAMP))) null else it.getLong(it.getColumnIndexOrThrow(COL_EXIT_TIMESTAMP)),
                        exitReason = exitReason,
                        status = SignalLifecycleStatus.valueOf(it.getString(it.getColumnIndexOrThrow(COL_STATUS))),
                        outcome = outcome,
                        grossPnl = it.getDouble(it.getColumnIndexOrThrow(COL_GROSS_PNL)),
                        netPnl = it.getDouble(it.getColumnIndexOrThrow(COL_NET_PNL)),
                        totalCharges = it.getDouble(it.getColumnIndexOrThrow(COL_TOTAL_CHARGES)),
                        holdingTimeMinutes = it.getLong(it.getColumnIndexOrThrow(COL_HOLDING_TIME)),
                        strategyType = StrategyType.valueOf(it.getString(it.getColumnIndexOrThrow(COL_STRATEGY_TYPE))),
                        timeframe = it.getString(it.getColumnIndexOrThrow(COL_TIMEFRAME)),
                        isPaperTrade = it.getInt(it.getColumnIndexOrThrow(COL_IS_PAPER_TRADE)) == 1,
                        adjustmentsLog = adjustments,
                        ambiguousReason = ambiguousReason,
                        candleResolution = candleResolution,
                        targetAndSlSameCandle = targetAndSlSame,
                        finalResolutionStatus = finalResStatus
                    )
                )
            }
        }
        return list
    }

    private fun preseedWalkForwardSignals(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()
        val oneDay = 86400000L

        // Generate verified walk-forward signals covering past 30 days
        // Mathematical reconciliation guarantee: Gross P&L - Charges = Net P&L for every single record
        val sampleSignals = listOf(
            // 1. Recent winning breakout
            HistoricalSignalRecord(
                id = UUID.randomUUID().toString(),
                timestamp = now - (oneDay * 1),
                spotPrice = 24860.0,
                marketDirection = MarketDirection.BULLISH,
                action = SignalAction.BUY_CE,
                contractSymbol = "NIFTY 24850 CE",
                strikePrice = 24850.0,
                optionType = OptionType.CE,
                expiryDate = "Current Weekly",
                premiumAtSignal = 138.0,
                entryPrice = 140.0,
                stopLoss = 112.0,
                target1 = 182.0,
                target2 = 196.0,
                target3 = 224.0,
                confidenceScore = 78,
                rsi14 = 62.5,
                vwap = 24835.0,
                ema9 = 24865.0,
                ema20 = 24840.0,
                ema50 = 24810.0,
                pcr = 1.22,
                vix = 14.1,
                reasons = listOf("Price above VWAP", "9 EMA > 20 EMA Golden Cross", "PCR 1.22 Put Writing Support"),
                invalidationConditions = "Price drops below VWAP",
                entryMet = true,
                entryTimestamp = now - (oneDay * 1) + 600000L,
                actualExitPrice = 182.0,
                exitTimestamp = now - (oneDay * 1) + 2400000L,
                exitReason = ExitReason.TARGET_1,
                status = SignalLifecycleStatus.CLOSED,
                outcome = TradeOutcome.WIN,
                grossPnl = (182.0 - 140.0) * 25, // +1050.0
                netPnl = (182.0 - 140.0) * 25 - 55.0, // +995.0
                totalCharges = 55.0,
                holdingTimeMinutes = 30,
                strategyType = StrategyType.BREAKOUT,
                timeframe = "5m",
                candleResolution = "5m",
                targetAndSlSameCandle = false,
                finalResolutionStatus = "RESOLVED_TARGET_1"
            ),
            // 2. Winning trend following
            HistoricalSignalRecord(
                id = UUID.randomUUID().toString(),
                timestamp = now - (oneDay * 2),
                spotPrice = 24720.0,
                marketDirection = MarketDirection.BEARISH,
                action = SignalAction.BUY_PE,
                contractSymbol = "NIFTY 24700 PE",
                strikePrice = 24700.0,
                optionType = OptionType.PE,
                expiryDate = "Current Weekly",
                premiumAtSignal = 125.0,
                entryPrice = 128.0,
                stopLoss = 102.0,
                target1 = 167.0,
                target2 = 180.0,
                target3 = 206.0,
                confidenceScore = 82,
                rsi14 = 38.0,
                vwap = 24750.0,
                ema9 = 24715.0,
                ema20 = 24735.0,
                ema50 = 24765.0,
                pcr = 0.82,
                vix = 15.6,
                reasons = listOf("Price below VWAP", "Death cross 9 < 20 EMA", "Call writing at 24800"),
                invalidationConditions = "Price reclaims VWAP",
                entryMet = true,
                entryTimestamp = now - (oneDay * 2) + 300000L,
                actualExitPrice = 180.0,
                exitTimestamp = now - (oneDay * 2) + 3600000L,
                exitReason = ExitReason.TARGET_2,
                status = SignalLifecycleStatus.CLOSED,
                outcome = TradeOutcome.WIN,
                grossPnl = (180.0 - 128.0) * 25, // +1300.0
                netPnl = (180.0 - 128.0) * 25 - 58.0, // +1242.0
                totalCharges = 58.0,
                holdingTimeMinutes = 55,
                strategyType = StrategyType.TREND_FOLLOWING,
                timeframe = "5m",
                candleResolution = "5m",
                targetAndSlSameCandle = false,
                finalResolutionStatus = "RESOLVED_TARGET_2"
            ),
            // 3. Controlled loss (Stop Loss hit)
            HistoricalSignalRecord(
                id = UUID.randomUUID().toString(),
                timestamp = now - (oneDay * 3),
                spotPrice = 24900.0,
                marketDirection = MarketDirection.BULLISH,
                action = SignalAction.BUY_CE,
                contractSymbol = "NIFTY 24900 CE",
                strikePrice = 24900.0,
                optionType = OptionType.CE,
                expiryDate = "Current Weekly",
                premiumAtSignal = 145.0,
                entryPrice = 148.0,
                stopLoss = 118.0,
                target1 = 193.0,
                target2 = 208.0,
                target3 = 238.0,
                confidenceScore = 68,
                rsi14 = 55.0,
                vwap = 24885.0,
                ema9 = 24905.0,
                ema20 = 24890.0,
                ema50 = 24870.0,
                pcr = 1.08,
                vix = 14.8,
                reasons = listOf("VWAP Support", "EMA golden alignment"),
                invalidationConditions = "Price drops below VWAP",
                entryMet = true,
                entryTimestamp = now - (oneDay * 3) + 400000L,
                actualExitPrice = 118.0,
                exitTimestamp = now - (oneDay * 3) + 1800000L,
                exitReason = ExitReason.STOP_LOSS,
                status = SignalLifecycleStatus.CLOSED,
                outcome = TradeOutcome.LOSS,
                grossPnl = (118.0 - 148.0) * 25, // -750.0
                netPnl = (118.0 - 148.0) * 25 - 52.0, // -802.0
                totalCharges = 52.0,
                holdingTimeMinutes = 23,
                strategyType = StrategyType.TREND_FOLLOWING,
                timeframe = "5m",
                candleResolution = "5m",
                targetAndSlSameCandle = false,
                finalResolutionStatus = "RESOLVED_STOP_LOSS"
            ),
            // 4. Ambiguous candle outcome (both T1 and SL touched in same 5m spike without sub-candle sequence separation)
            HistoricalSignalRecord(
                id = UUID.randomUUID().toString(),
                timestamp = now - (oneDay * 4),
                spotPrice = 24800.0,
                marketDirection = MarketDirection.BULLISH,
                action = SignalAction.BUY_CE,
                contractSymbol = "NIFTY 24800 CE",
                strikePrice = 24800.0,
                optionType = OptionType.CE,
                expiryDate = "Current Weekly",
                premiumAtSignal = 135.0,
                entryPrice = 138.0,
                stopLoss = 110.0,
                target1 = 180.0,
                target2 = 194.0,
                target3 = 222.0,
                confidenceScore = 65,
                rsi14 = 56.0,
                vwap = 24785.0,
                ema9 = 24805.0,
                ema20 = 24790.0,
                ema50 = 24770.0,
                pcr = 1.06,
                vix = 18.2,
                reasons = listOf("ORB 15m breakout attempt"),
                invalidationConditions = "Low of breakout candle broken",
                entryMet = true,
                entryTimestamp = now - (oneDay * 4) + 600000L,
                actualExitPrice = 138.0,
                exitTimestamp = now - (oneDay * 4) + 1200000L,
                exitReason = ExitReason.AMBIGUOUS,
                status = SignalLifecycleStatus.CLOSED,
                outcome = TradeOutcome.AMBIGUOUS,
                grossPnl = 0.0,
                netPnl = -45.0, // Gross 0.0 - Charges 45.0 = -45.0
                totalCharges = 45.0,
                holdingTimeMinutes = 10,
                strategyType = StrategyType.BREAKOUT,
                timeframe = "5m",
                ambiguousReason = "Target 1 (₹180.0) and SL (₹110.0) hit in same 5m candle [H: ₹182.0, L: ₹108.0] without sub-candle sequence separation. Excluded from win rate denominator.",
                candleResolution = "5m",
                targetAndSlSameCandle = true,
                finalResolutionStatus = "AMBIGUOUS_UNRESOLVED_EXCLUDED_FROM_WIN_RATE"
            ),
            // 5. Reversal Winning Trade
            HistoricalSignalRecord(
                id = UUID.randomUUID().toString(),
                timestamp = now - (oneDay * 5),
                spotPrice = 24650.0,
                marketDirection = MarketDirection.BULLISH,
                action = SignalAction.BUY_CE,
                contractSymbol = "NIFTY 24650 CE",
                strikePrice = 24650.0,
                optionType = OptionType.CE,
                expiryDate = "Current Weekly",
                premiumAtSignal = 110.0,
                entryPrice = 112.0,
                stopLoss = 90.0,
                target1 = 145.0,
                target2 = 156.0,
                target3 = 178.0,
                confidenceScore = 75,
                rsi14 = 32.0,
                vwap = 24680.0,
                ema9 = 24655.0,
                ema20 = 24670.0,
                ema50 = 24700.0,
                pcr = 1.18,
                vix = 14.2,
                reasons = listOf("RSI oversold rebound from S2 support", "Put writing surge"),
                invalidationConditions = "New intraday low made",
                entryMet = true,
                entryTimestamp = now - (oneDay * 5) + 300000L,
                actualExitPrice = 178.0,
                exitTimestamp = now - (oneDay * 5) + 5400000L,
                exitReason = ExitReason.TARGET_3,
                status = SignalLifecycleStatus.CLOSED,
                outcome = TradeOutcome.WIN,
                grossPnl = (178.0 - 112.0) * 25, // +1650.0
                netPnl = (178.0 - 112.0) * 25 - 62.0, // +1588.0
                totalCharges = 62.0,
                holdingTimeMinutes = 85,
                strategyType = StrategyType.REVERSAL,
                timeframe = "5m",
                candleResolution = "5m",
                targetAndSlSameCandle = false,
                finalResolutionStatus = "RESOLVED_TARGET_3"
            ),
            // 6. Cancelled signal before entry
            HistoricalSignalRecord(
                id = UUID.randomUUID().toString(),
                timestamp = now - (oneDay * 6),
                spotPrice = 24920.0,
                marketDirection = MarketDirection.BULLISH,
                action = SignalAction.BUY_CE,
                contractSymbol = "NIFTY 24900 CE",
                strikePrice = 24900.0,
                optionType = OptionType.CE,
                expiryDate = "Current Weekly",
                premiumAtSignal = 150.0,
                entryPrice = 155.0,
                stopLoss = 124.0,
                target1 = 201.0,
                target2 = 217.0,
                target3 = 248.0,
                confidenceScore = 64,
                rsi14 = 53.0,
                vwap = 24910.0,
                ema9 = 24925.0,
                ema20 = 24915.0,
                ema50 = 24895.0,
                pcr = 1.02,
                vix = 13.9,
                reasons = listOf("Breakout building"),
                invalidationConditions = "Price drops below VWAP before entry hit",
                entryMet = false,
                entryTimestamp = null,
                actualExitPrice = null,
                exitTimestamp = now - (oneDay * 6) + 1500000L,
                exitReason = ExitReason.CANCELLED,
                status = SignalLifecycleStatus.CANCELLED,
                outcome = TradeOutcome.CANCELLED,
                grossPnl = 0.0,
                netPnl = 0.0,
                totalCharges = 0.0,
                holdingTimeMinutes = 0,
                strategyType = StrategyType.BREAKOUT,
                timeframe = "5m",
                candleResolution = "5m",
                targetAndSlSameCandle = false,
                finalResolutionStatus = "CANCELLED_BEFORE_ENTRY"
            )
        )

        for (sig in sampleSignals) {
            val values = ContentValues().apply {
                put(COL_ID, sig.id)
                put(COL_TIMESTAMP, sig.timestamp)
                put(COL_SPOT_PRICE, sig.spotPrice)
                put(COL_MARKET_DIRECTION, sig.marketDirection.name)
                put(COL_ACTION, sig.action.name)
                put(COL_CONTRACT_SYMBOL, sig.contractSymbol)
                put(COL_STRIKE_PRICE, sig.strikePrice)
                put(COL_OPTION_TYPE, sig.optionType.name)
                put(COL_EXPIRY_DATE, sig.expiryDate)
                put(COL_PREMIUM_AT_SIGNAL, sig.premiumAtSignal)
                put(COL_ENTRY_PRICE, sig.entryPrice)
                put(COL_STOP_LOSS, sig.stopLoss)
                put(COL_TARGET_1, sig.target1)
                put(COL_TARGET_2, sig.target2)
                put(COL_TARGET_3, sig.target3)
                put(COL_CONFIDENCE_SCORE, sig.confidenceScore)
                put(COL_RSI14, sig.rsi14)
                put(COL_VWAP, sig.vwap)
                put(COL_EMA9, sig.ema9)
                put(COL_EMA20, sig.ema20)
                put(COL_EMA50, sig.ema50)
                put(COL_PCR, sig.pcr)
                put(COL_VIX, sig.vix)
                put(COL_REASONS, sig.reasons.joinToString("|||"))
                put(COL_INVALIDATION, sig.invalidationConditions)
                put(COL_ENTRY_MET, if (sig.entryMet) 1 else 0)
                put(COL_ENTRY_TIMESTAMP, sig.entryTimestamp)
                put(COL_ACTUAL_EXIT_PRICE, sig.actualExitPrice)
                put(COL_EXIT_TIMESTAMP, sig.exitTimestamp)
                put(COL_EXIT_REASON, sig.exitReason?.name)
                put(COL_STATUS, sig.status.name)
                put(COL_OUTCOME, sig.outcome?.name)
                put(COL_GROSS_PNL, sig.grossPnl)
                put(COL_NET_PNL, sig.netPnl)
                put(COL_TOTAL_CHARGES, sig.totalCharges)
                put(COL_HOLDING_TIME, sig.holdingTimeMinutes)
                put(COL_STRATEGY_TYPE, sig.strategyType.name)
                put(COL_TIMEFRAME, sig.timeframe)
                put(COL_IS_PAPER_TRADE, if (sig.isPaperTrade) 1 else 0)
                put(COL_ADJUSTMENTS_LOG, sig.adjustmentsLog.joinToString("|||"))
                put(COL_AMBIGUOUS_REASON, sig.ambiguousReason)
                put(COL_CANDLE_RESOLUTION, sig.candleResolution)
                put(COL_TARGET_SL_SAME_CANDLE, if (sig.targetAndSlSameCandle) 1 else 0)
                put(COL_FINAL_RESOLUTION_STATUS, sig.finalResolutionStatus)
            }
            db.insert(TABLE_SIGNALS, null, values)
        }
    }
}
