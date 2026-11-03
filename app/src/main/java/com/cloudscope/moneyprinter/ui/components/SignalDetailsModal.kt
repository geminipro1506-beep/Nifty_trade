package com.cloudscope.moneyprinter.ui.components

import androidx.compose.runtime.Composable
import com.cloudscope.moneyprinter.data.model.HistoricalSignalRecord

/**
 * Backward compatibility alias for SignalDetailsDialog.
 */
@Composable
fun SignalDetailsModal(
    signal: HistoricalSignalRecord?,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onRetry: () -> Unit = {},
    onClosePaperTrade: ((HistoricalSignalRecord) -> Unit)? = null
) {
    SignalDetailsDialog(
        record = signal,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onDismiss = onDismiss,
        onRetry = onRetry,
        onClosePaperTrade = onClosePaperTrade
    )
}
