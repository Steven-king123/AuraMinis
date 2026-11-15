package com.openminis.app.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedMemoryUiPolicyTest {
    @Test fun `disabled idle page hides routine ready and done status`() {
        assertFalse(AdvancedMemoryUiPolicy.showStatus(false, false, false, false))
    }

    @Test fun `collapse never hides busy error or cancellation feedback`() {
        for (busy in listOf(false, true)) for (failed in listOf(false, true)) {
            for (cancelled in listOf(false, true)) {
                if (busy || failed || cancelled) {
                    assertTrue(AdvancedMemoryUiPolicy.showStatus(false, busy, failed, cancelled))
                }
                assertTrue(AdvancedMemoryUiPolicy.showStatus(true, busy, failed, cancelled))
            }
        }
    }

    @Test fun `stale advanced callbacks cannot launch after disabling`() {
        assertTrue(AdvancedMemoryUiPolicy.canStartOperation(true, false, true))
        // Evaluate the current store flag, not the flag captured by the old dialog.
        assertFalse(AdvancedMemoryUiPolicy.canStartOperation(false, false, true))
    }

    @Test fun `main switch is allowed while disabled but all work respects busy`() {
        assertTrue(AdvancedMemoryUiPolicy.canStartOperation(false, false, false))
        assertTrue(AdvancedMemoryUiPolicy.canStartOperation(true, false, false))
        for (enabled in listOf(false, true)) for (requireEnabled in listOf(false, true)) {
            assertFalse(AdvancedMemoryUiPolicy.canStartOperation(enabled, true, requireEnabled))
        }
    }
}
