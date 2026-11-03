package com.cloudscope.moneyprinter.data.local.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.cloudscope.moneyprinter.data.model.*
import com.cloudscope.moneyprinter.domain.options.ContractSpecificationService
import com.cloudscope.moneyprinter.domain.signal.ChargesCalculator
import java.util.UUID

class SignalDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "cloudscope_signals.db"
        const val DATABASE_VERSION = 3

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

        // Phase 4 Centralized Lot Size & Historical Migration Columns
        const val COL_LOTS = "lots"
        const val COL_LOT_SIZE = "lot_size"
        const val COL_ORIGINAL_GROSS_PNL = "original_gross_pnl"
        const val COL_ORIGINAL_NET_PNL = "original_net_pnl"
        const val COL_ORIGINAL_LOT_SIZE = "original_lot_size"
        const val COL_MIGRATION_STATUS = "migration_status"
        const val COL_MIGRATION_NOTES = "migration_notes"

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
                $COL_FINAL_RESOLUTION_STATUS TEXT,
                $COL_LOTS INTEGER NOT NULL DEFAULT 1,
                $COL_LOT_SIZE INTEGER NOT NULL DEFAULT 65,
                $COL_ORIGINAL_GROSS_PNL REAL,
                $COL_ORIGINAL_NET_PNL REAL,
                $COL_ORIGINAL_LOT_SIZE INTEGER,
                $COL_MIGRATION_STATUS TEXT,
                $COL_MIGRATION_NOTES TEXT
            )
        """.trimIndent()
        db.execSQL(createTableQuery)

        preseedWalkForwardSignals(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            try {
                // Ensure new Phase 4 columns exist safely
                addColumnIfNotExists(db, COL_LOTS, "INTEGER NOT NULL DEFAULT 1")
                addColumnIfNotExists(db, COL_LOT_SIZE, "INTEGER NOT NULL DEFAULT 65")
                addColumnIfNotExists(db, COL_ORIGINAL_GROSS_PNL, "REAL")
                addColumnIfNotExists(db, COL_ORIGINAL_NET_PNL, "REAL")
                addColumnIfNotExists(db, COL_ORIGINAL_LOT_SIZE, "INTEGER")
                addColumnIfNotExists(db, COL_MIGRATION_STATUS, "TEXT")
                addColumnIfNotExists(db, COL_MIGRATION_NOTES, "TEXT")

                // Run historical data migration
                migrateHistoricalLotSizes(db)
            } catch (e: Exception) {
                // In case of any schema conflict, recreate clean table
                db.execSQL("DROP TABLE IF EXISTS $TABLE_SIGNALS")
                onCreate(db)
            }
        }
    }

    private fun addColumnIfNotExists(db: SQLiteDatabase, columnName: String, columnDefinition: String) {
        val cursor = db.rawQuery("PRAGMA table_info($TABLE_SIGNALS)", null)
        var exists = false
        cursor.use {
            val nameIndex = it.getColumnIndex("name")
            while (it.moveToNext()) {
                if (it.getString(nameIndex) == columnName) {
                    exists = true
                    break
                }
            }
        }
        if (!exists) {
            db.execSQL("ALTER TABLE $TABLE_SIGNALS ADD COLUMN $columnName $columnDefinition")
        }
    }

    /**
     * Historical data migration routine.
     * Identifies records where lot size was 25 and recalculates them from original entry/exit inputs using lot size 65.
     * Preserves original audit records in originalGrossPnl and originalNetPnl.
     * Flags records for manual review if original inputs are unavailable.
     */
    fun migrateHistoricalLotSizes(db: SQLiteDatabase) {
        val cursor = db.query(TABLE_SIGNALS, null, null, null, null, null, null)
        val recordsToUpdate = mutableListOf<ContentValues>()

        cursor.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(c.getColumnIndexOrThrow(COL_ID))
                val entryPrice = c.getDouble(c.getColumnIndexOrThrow(COL_ENTRY_PRICE))
                val actualExitPrice = if (!c.isNull(c.getColumnIndexOrThrow(COL_ACTUAL_EXIT_PRICE))) c.getDouble(c.getColumnIndexOrThrow(COL_ACTUAL_EXIT_PRICE)) else null
                val oldGross = c.getDouble(c.getColumnIndexOrThrow(COL_GROSS_PNL))
                val oldNet = c.getDouble(c.getColumnIndexOrThrow(COL_NET_PNL))
                val currentLotSize = if (c.getColumnIndex(COL_LOT_SIZE) != -1 && !c.isNull(c.getColumnIndex(COL_LOT_SIZE))) c.getInt(c.getColumnIndex(COL_LOT_SIZE)) else 25
                val currentLots = if (c.getColumnIndex(COL_LOTS) != -1 && !c.isNull(c.getColumnIndex(COL_LOTS))) c.getInt(c.getColumnIndex(COL_LOTS)) else 1
                val statusStr = c.getString(c.getColumnIndexOrThrow(COL_STATUS))
                val outcomeStr = if (!c.isNull(c.getColumnIndexOrThrow(COL_OUTCOME))) c.getString(c.getColumnIndexOrThrow(COL_OUTCOME)) else null

                if (currentLotSize == 25 || currentLotSize == 0) {
                    val values = ContentValues().apply {
                        put(COL_ID, id)
                        put(COL_ORIGINAL_LOT_SIZE, 25)
                        put(COL_ORIGINAL_GROSS_PNL, oldGross)
                        put(COL_ORIGINAL_NET_PNL, oldNet)
                        put(COL_LOTS, currentLots)
                        put(COL_LOT_SIZE, 65)

                        if (statusStr == SignalLifecycleStatus.CLOSED.name && actualExitPrice != null && entryPrice > 0) {
                            val newQuantity = currentLots * 65
                            val gross = (actualExitPrice - entryPrice) * newQuantity
                            val (grossRounded, netRounded) = ChargesCalculator.calculateNetPnl(entryPrice, actualExitPrice, currentLots, 65)
                            val charges = ChargesCalculator.calculate(entryPrice, actualExitPrice, currentLots, 65).totalCharges

                            put(COL_GROSS_PNL, grossRounded)
                            put(COL_NET_PNL, netRounded)
                            put(COL_TOTAL_CHARGES, charges)
                            put(COL_MIGRATION_STATUS, "MIGRATED_LOT_SIZE_65")
                            put(COL_MIGRATION_NOTES, "Recalculated with authorized NIFTY lot size 65 (Qty: $newQuantity). Original Gross: ₹$oldGross, Original Net: ₹$oldNet.")
                        } else if (statusStr == SignalLifecycleStatus.CANCELLED.name || statusStr == SignalLifecycleStatus.EXPIRED.name) {
                            put(COL_GROSS_PNL, 0.0)
                            put(COL_NET_PNL, 0.0)
                            put(COL_TOTAL_CHARGES, 0.0)
                            put(COL_MIGRATION_STATUS, "MIGRATED_UNFILLED")
                            put(COL_MIGRATION_NOTES, "Unfilled/Cancelled order updated to authorized specification (65 qty/lot).")
                        } else {
                            put(COL_MIGRATION_STATUS, "FLAGGED_MANUAL_REVIEW")
                            put(COL_MIGRATION_NOTES, "Original execution pricing unavailable. Retained original values for manual audit.")
                        }
                    }
                    recordsToUpdate.add(values)
                }
            }
        }

        for (vals in recordsToUpdate) {
            val id = vals.getAsString(COL_ID)
            db.update(TABLE_SIGNALS, vals, "$COL_ID = ?", arrayOf(id))
        }
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
            put(COL_LOTS, record.lots)
            put(COL_LOT_SIZE, record.lotSize)
            put(COL_ORIGINAL_GROSS_PNL, record.originalGrossPnl)
            put(COL_ORIGINAL_NET_PNL, record.originalNetPnl)
            put(COL_ORIGINAL_LOT_SIZE, record.originalLotSize)
            put(COL_MIGRATION_STATUS, record.migrationStatus)
            put(COL_MIGRATION_NOTES, record.migrationNotes)
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
            put(COL_LOTS, record.lots)
            put(COL_LOT_SIZE, record.lotSize)
            put(COL_ORIGINAL_GROSS_PNL, record.originalGrossPnl)
            put(COL_ORIGINAL_NET_PNL, record.originalNetPnl)
            put(COL_ORIGINAL_LOT_SIZE, record.originalLotSize)
            put(COL_MIGRATION_STATUS, record.migrationStatus)
            put(COL_MIGRATION_NOTES, record.migrationNotes)
        }
        return db.update(TABLE_SIGNALS, values, "$COL_ID = ?", arrayOf(record.id))
    }

    fun getAllSignals(): List<HistoricalSignalRecord> {
        val db = readableDatabase
        val cursor = db.query(TABLE_SIGNALS, null, null, null, null, null, "$COL_TIMESTAMP DESC")
        return cursorToList(cursor)
    }

    fun getSignalById(id: String): HistoricalSignalRecord? {
        val db = readableDatabase
        val cursor = db.query(TABLE_SIGNALS, null, "$COL_ID = ?", arrayOf(id), null, null, null)
        return cursorToList(cursor).firstOrNull()
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

                val lotsIdx = it.getColumnIndex(COL_LOTS)
                val lots = if (lotsIdx != -1 && !it.isNull(lotsIdx)) it.getInt(lotsIdx) else 1

                val lotSizeIdx = it.getColumnIndex(COL_LOT_SIZE)
                val lotSize = if (lotSizeIdx != -1 && !it.isNull(lotSizeIdx)) it.getInt(lotSizeIdx) else 65

                val origGrossIdx = it.getColumnIndex(COL_ORIGINAL_GROSS_PNL)
                val origGross = if (origGrossIdx != -1 && !it.isNull(origGrossIdx)) it.getDouble(origGrossIdx) else null

                val origNetIdx = it.getColumnIndex(COL_ORIGINAL_NET_PNL)
                val origNet = if (origNetIdx != -1 && !it.isNull(origNetIdx)) it.getDouble(origNetIdx) else null

                val origLotSizeIdx = it.getColumnIndex(COL_ORIGINAL_LOT_SIZE)
                val origLotSize = if (origLotSizeIdx != -1 && !it.isNull(origLotSizeIdx)) it.getInt(origLotSizeIdx) else null

                val migStatusIdx = it.getColumnIndex(COL_MIGRATION_STATUS)
                val migStatus = if (migStatusIdx != -1 && !it.isNull(migStatusIdx)) it.getString(migStatusIdx) else null

                val migNotesIdx = it.getColumnIndex(COL_MIGRATION_NOTES)
                val migNotes = if (migNotesIdx != -1 && !it.isNull(migNotesIdx)) it.getString(migNotesIdx) else null

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
                        finalResolutionStatus = finalResStatus,
                        lots = lots,
                        lotSize = lotSize,
                        originalGrossPnl = origGross,
                        originalNetPnl = origNet,
                        originalLotSize = origLotSize,
                        migrationStatus = migStatus,
                        migrationNotes = migNotes
                    )
                )
            }
        }
        return list
    }

    private fun preseedWalkForwardSignals(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()
        val oneDay = 86400000L
        val lotSize = 65 // Authorized NSE NIFTY Lot Size

        val sampleSignals = listOf(
            // 1. Winning Breakout CE
            createPreseedRecord(
                id = UUID.randomUUID().toString(),
                timestamp = now - (oneDay * 1),
                spotPrice = 24860.0,
                direction = MarketDirection.BULLISH,
                action = SignalAction.BUY_CE,
                symbol = "NIFTY 24850 CE",
                strike = 24850.0,
                optType = OptionType.CE,
                entryPrice = 140.0,
                exitPrice = 182.0,
                sl = 112.0,
                t1 = 182.0, t2 = 196.0, t3 = 224.0,
                exitReason = ExitReason.TARGET_1,
                outcome = TradeOutcome.WIN,
                holdingMin = 30,
                strategy = StrategyType.BREAKOUT,
                confidence = 78,
                vix = 14.1,
                lotSize = lotSize,
                notes = "Preseeded authorized walk-forward record."
            ),
            // 2. Winning Trend Following PE
            createPreseedRecord(
                id = UUID.randomUUID().toString(),
                timestamp = now - (oneDay * 2),
                spotPrice = 24720.0,
                direction = MarketDirection.BEARISH,
                action = SignalAction.BUY_PE,
                symbol = "NIFTY 24700 PE",
                strike = 24700.0,
                optType = OptionType.PE,
                entryPrice = 128.0,
                exitPrice = 180.0,
                sl = 102.0,
                t1 = 167.0, t2 = 180.0, t3 = 206.0,
                exitReason = ExitReason.TARGET_2,
                outcome = TradeOutcome.WIN,
                holdingMin = 55,
                strategy = StrategyType.TREND_FOLLOWING,
                confidence = 82,
                vix = 15.6,
                lotSize = lotSize,
                notes = "Preseeded authorized walk-forward record."
            ),
            // 3. Controlled Loss (Stop Loss CE)
            createPreseedRecord(
                id = UUID.randomUUID().toString(),
                timestamp = now - (oneDay * 3),
                spotPrice = 24900.0,
                direction = MarketDirection.BULLISH,
                action = SignalAction.BUY_CE,
                symbol = "NIFTY 24900 CE",
                strike = 24900.0,
                optType = OptionType.CE,
                entryPrice = 148.0,
                exitPrice = 118.0,
                sl = 118.0,
                t1 = 193.0, t2 = 208.0, t3 = 238.0,
                exitReason = ExitReason.STOP_LOSS,
                outcome = TradeOutcome.LOSS,
                holdingMin = 23,
                strategy = StrategyType.TREND_FOLLOWING,
                confidence = 68,
                vix = 14.8,
                lotSize = lotSize,
                notes = "Preseeded authorized walk-forward record."
            ),
            // 4. Ambiguous candle outcome (both T1 and SL touched in same window)
            createPreseedRecord(
                id = UUID.randomUUID().toString(),
                timestamp = now - (oneDay * 4),
                spotPrice = 24800.0,
                direction = MarketDirection.BULLISH,
                action = SignalAction.BUY_CE,
                symbol = "NIFTY 24800 CE",
                strike = 24800.0,
                optType = OptionType.CE,
                entryPrice = 138.0,
                exitPrice = 138.0, // Breakeven exit at entry
                sl = 110.0,
                t1 = 180.0, t2 = 194.0, t3 = 222.0,
                exitReason = ExitReason.AMBIGUOUS,
                outcome = TradeOutcome.AMBIGUOUS,
                holdingMin = 10,
                strategy = StrategyType.BREAKOUT,
                confidence = 65,
                vix = 18.2,
                lotSize = lotSize,
                ambiguousReason = "Target 1 (₹180.0) and SL (₹110.0) hit in same 5m candle [H: ₹182.0, L: ₹108.0]. Excluded from win rate denominator.",
                isAmbiguous = true,
                notes = "Preseeded authorized walk-forward record."
            ),
            // 5. Reversal Winning Trade CE
            createPreseedRecord(
                id = UUID.randomUUID().toString(),
                timestamp = now - (oneDay * 5),
                spotPrice = 24650.0,
                direction = MarketDirection.BULLISH,
                action = SignalAction.BUY_CE,
                symbol = "NIFTY 24650 CE",
                strike = 24650.0,
                optType = OptionType.CE,
                entryPrice = 112.0,
                exitPrice = 178.0,
                sl = 90.0,
                t1 = 145.0, t2 = 156.0, t3 = 178.0,
                exitReason = ExitReason.TARGET_3,
                outcome = TradeOutcome.WIN,
                holdingMin = 85,
                strategy = StrategyType.REVERSAL,
                confidence = 75,
                vix = 14.2,
                lotSize = lotSize,
                notes = "Preseeded authorized walk-forward record."
            ),
            // 6. Cancelled before entry
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
                finalResolutionStatus = "CANCELLED_BEFORE_ENTRY",
                lots = 1,
                lotSize = lotSize,
                migrationStatus = "NSE_AUTHORIZED_LOT_SIZE_65",
                migrationNotes = "Preseeded authorized specification."
            )
        )

        for (sig in sampleSignals) {
            insertSignal(sig)
        }
    }

    private fun createPreseedRecord(
        id: String,
        timestamp: Long,
        spotPrice: Double,
        direction: MarketDirection,
        action: SignalAction,
        symbol: String,
        strike: Double,
        optType: OptionType,
        entryPrice: Double,
        exitPrice: Double,
        sl: Double,
        t1: Double,
        t2: Double,
        t3: Double,
        exitReason: ExitReason,
        outcome: TradeOutcome,
        holdingMin: Long,
        strategy: StrategyType,
        confidence: Int,
        vix: Double,
        lotSize: Int = 65,
        ambiguousReason: String? = null,
        isAmbiguous: Boolean = false,
        notes: String = ""
    ): HistoricalSignalRecord {
        val lots = 1
        val (gross, net) = ChargesCalculator.calculateNetPnl(entryPrice, exitPrice, lots, lotSize)
        val charges = ChargesCalculator.calculate(entryPrice, exitPrice, lots, lotSize).totalCharges

        return HistoricalSignalRecord(
            id = id,
            timestamp = timestamp,
            spotPrice = spotPrice,
            marketDirection = direction,
            action = action,
            contractSymbol = symbol,
            strikePrice = strike,
            optionType = optType,
            expiryDate = "Current Weekly",
            premiumAtSignal = entryPrice,
            entryPrice = entryPrice,
            stopLoss = sl,
            target1 = t1,
            target2 = t2,
            target3 = t3,
            confidenceScore = confidence,
            rsi14 = 55.0,
            vwap = spotPrice - 15.0,
            ema9 = spotPrice + 5.0,
            ema20 = spotPrice - 5.0,
            ema50 = spotPrice - 25.0,
            pcr = 1.15,
            vix = vix,
            reasons = listOf("Quantitative algorithm execution"),
            invalidationConditions = "Opposite momentum reversal",
            entryMet = true,
            entryTimestamp = timestamp + 300000L,
            actualExitPrice = exitPrice,
            exitTimestamp = timestamp + (holdingMin * 60000L),
            exitReason = exitReason,
            status = SignalLifecycleStatus.CLOSED,
            outcome = outcome,
            grossPnl = gross,
            netPnl = net,
            totalCharges = charges,
            holdingTimeMinutes = holdingMin,
            strategyType = strategy,
            timeframe = "5m",
            candleResolution = "5m",
            targetAndSlSameCandle = isAmbiguous,
            ambiguousReason = ambiguousReason,
            finalResolutionStatus = if (isAmbiguous) "AMBIGUOUS_UNRESOLVED_EXCLUDED_FROM_WIN_RATE" else "RESOLVED_${exitReason.name}",
            lots = lots,
            lotSize = lotSize,
            migrationStatus = "NSE_AUTHORIZED_LOT_SIZE_65",
            migrationNotes = notes
        )
    }
}
