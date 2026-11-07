package com.cloudscope.moneyprinter.domain.signal

import com.cloudscope.moneyprinter.domain.normaloptions.NormalOptionsSignal
import com.cloudscope.moneyprinter.domain.normaloptions.NormalSignalAction
import com.cloudscope.moneyprinter.domain.smartmoney.SmartMoneySignal
import com.cloudscope.moneyprinter.domain.smartmoney.SmartMoneySignalAction

data class SignalConflictState(
    val hasConflict: Boolean = false,
    val normalAction: String = "NO TRADE",
    val smartMoneyAction: String = "NO ACTIVITY",
    val conflictReason: String = ""
)

object SignalConflictEngine {

    fun detectConflict(
        normalSignal: NormalOptionsSignal?,
        smartMoneySignal: SmartMoneySignal?
    ): SignalConflictState {
        val normalAction = normalSignal?.action ?: NormalSignalAction.NO_TRADE
        val smAction = smartMoneySignal?.action ?: SmartMoneySignalAction.NO_TRADE

        val isNormalBullish = normalAction == NormalSignalAction.BUY_CALL
        val isNormalBearish = normalAction == NormalSignalAction.BUY_PUT

        val isSmBullish = smAction == SmartMoneySignalAction.BUY_CE
        val isSmBearish = smAction == SmartMoneySignalAction.BUY_PE

        val hasConflict = (isNormalBullish && isSmBearish) || (isNormalBearish && isSmBullish)

        if (!hasConflict) {
            return SignalConflictState(
                hasConflict = false,
                normalAction = normalAction.name,
                smartMoneyAction = smAction.displayName,
                conflictReason = ""
            )
        }

        val reason = if (isNormalBullish && isSmBearish) {
            "SIGNAL CONFLICT: Normal Technicals suggest BUY CALL, but Smart Money Institutional Flow detects BUY PE / Short Buildup. Conflicting market forces detected—do not force a trade."
        } else {
            "SIGNAL CONFLICT: Normal Technicals suggest BUY PUT, but Smart Money Institutional Flow detects BUY CE / Long Accumulation. Conflicting market forces detected—do not force a trade."
        }

        return SignalConflictState(
            hasConflict = true,
            normalAction = normalAction.name,
            smartMoneyAction = smAction.displayName,
            conflictReason = reason
        )
    }
}
