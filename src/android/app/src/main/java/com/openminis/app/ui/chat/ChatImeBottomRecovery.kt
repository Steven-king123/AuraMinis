package com.openminis.app.ui.chat

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.flow.collect

/**
 * Observe IME pixels in a coroutine, not in the large ChatScreen composition scope.
 * Keep the existing T169/T170 rule: only clear a synthetic reader-away flag if
 * the list is actually near the bottom. This does not scroll or alter list layout.
 */
@Composable
internal fun ChatImeBottomRecoveryEffect(
    userScrolledAway: () -> Boolean,
    isNearBottom: State<Boolean>,
    onReset: () -> Unit,
) {
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    val currentUserScrolledAway by rememberUpdatedState(userScrolledAway)
    val currentReset by rememberUpdatedState(onReset)
    LaunchedEffect(ime, density, isNearBottom) {
        observeImeBottomRecovery(
            imeBottom = { ime.getBottom(density) },
            userScrolledAway = { currentUserScrolledAway() },
            isNearBottom = { isNearBottom.value },
            onReset = { currentReset() },
        )
    }
}

/** Only IME height is a flow dependency; flags are read fresh at each height edge. */
internal suspend fun observeImeBottomRecovery(
    imeBottom: () -> Int,
    userScrolledAway: () -> Boolean,
    isNearBottom: () -> Boolean,
    onReset: () -> Unit,
) {
    snapshotFlow { imeBottom() }.collect {
        if (userScrolledAway() && isNearBottom()) onReset()
    }
}
