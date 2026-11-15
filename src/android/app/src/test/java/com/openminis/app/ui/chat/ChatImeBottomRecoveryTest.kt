package com.openminis.app.ui.chat

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatImeBottomRecoveryTest {
    @Test fun openingAndClosingImePreserveReadersAwayFromBottom() = runTest {
        val height = mutableStateOf(0)
        var away = true
        var resets = 0
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeImeBottomRecovery({ height.value }, { away }, { false }) {
                resets++; away = false
            }
        }
        for (px in listOf(40, 140, 400, 700, 500, 0)) {
            Snapshot.withMutableSnapshot { height.value = px }
            runCurrent()
        }
        assertEquals(0, resets)
        assertTrue(away)
        job.cancelAndJoin()
    }

    @Test fun eachImeEdgeUsesLatestFlagsInsteadOfCapturedInitialFlags() = runTest {
        val height = mutableStateOf(0)
        var away = false
        var near = true
        var resets = 0
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeImeBottomRecovery({ height.value }, { away }, { near }) {
                resets++; away = false
            }
        }
        assertEquals(0, resets)
        away = true
        near = false
        Snapshot.withMutableSnapshot { height.value = 200 }
        runCurrent()
        assertTrue(away)
        near = true
        Snapshot.withMutableSnapshot { height.value = 400 }
        runCurrent()
        assertEquals(1, resets)
        assertFalse(away)
        // Simulate another synthetic reader-away flag during keyboard closing.
        away = true
        Snapshot.withMutableSnapshot { height.value = 100 }
        runCurrent()
        assertEquals(2, resets)
        job.cancelAndJoin()
    }

    @Test fun flagsAreNotAdditionalFlowDependenciesAndDisposeStopsCollection() = runTest {
        val height = mutableStateOf(0)
        val away = mutableStateOf(false)
        val near = mutableStateOf(true)
        var resets = 0
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeImeBottomRecovery({ height.value }, { away.value }, { near.value }) {
                resets++
            }
        }
        Snapshot.withMutableSnapshot { away.value = true }
        runCurrent()
        assertEquals(0, resets)
        Snapshot.withMutableSnapshot { height.value = 1 }
        runCurrent()
        assertEquals(1, resets)
        job.cancelAndJoin()
        Snapshot.withMutableSnapshot { height.value = 2 }
        runCurrent()
        assertEquals(1, resets)
    }

    @Test fun initialCollectionRepairsOnlyAtBottomAndUsesUpdatedCallback() = runTest {
        val height = mutableStateOf(0)
        val away = mutableStateOf(true)
        val near = mutableStateOf(true)
        var oldCalls = 0
        var newCalls = 0
        val callback = mutableStateOf<() -> Unit>({ oldCalls++ })
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeImeBottomRecovery({ height.value }, { away.value }, { near.value }) {
                callback.value()
            }
        }
        assertEquals(1, oldCalls)
        Snapshot.withMutableSnapshot {
            near.value = false
            callback.value = { newCalls++ }
        }
        runCurrent()
        assertEquals(0, newCalls)
        // Flags and final IME height change in one snapshot, with no composition.
        Snapshot.withMutableSnapshot {
            near.value = true
            away.value = true
            height.value = 700
        }
        runCurrent()
        assertEquals(1, oldCalls)
        assertEquals(1, newCalls)
        job.cancelAndJoin()
    }

    @Test fun screenDelegatesImePixelsToTheIsolatedEffect() {
        val screen = File("src/main/java/com/openminis/app/ui/chat/ChatScreen.kt").readText()
        assertTrue(screen.contains("ChatImeBottomRecoveryEffect("))
        assertTrue(screen.contains("userScrolledAway = { userScrolledAway }"))
        assertFalse(screen.contains("WindowInsets.ime.getBottom("))
        assertFalse(screen.contains("LaunchedEffect(imeBottomPx)"))
        val effect = File("src/main/java/com/openminis/app/ui/chat/ChatImeBottomRecovery.kt").readText()
        assertTrue(effect.contains("snapshotFlow { imeBottom() }"))
        assertTrue(effect.contains("rememberUpdatedState(onReset)"))
    }
}
