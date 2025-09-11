// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard

import helium314.keyboard.keyboard.KeyboardSwitcher
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Simple tests for KeyboardSwitcher FN state management.
 */
class KeyboardSwitcherFnTest {
    
    @Test
    fun testFnStateInitiallyFalse() {
        val switcher = KeyboardSwitcher.getInstance()
        // Reset to known state
        switcher.setFnState(false)
        // Initial state should be false
        assertFalse(switcher.isFnActive())
    }
    
    @Test
    fun testSetFnStateTrue() {
        val switcher = KeyboardSwitcher.getInstance()
        switcher.setFnState(true)
        assertTrue(switcher.isFnActive())
    }
    
    @Test
    fun testSetFnStateFalse() {
        val switcher = KeyboardSwitcher.getInstance()
        switcher.setFnState(true)
        assertTrue(switcher.isFnActive())
        
        switcher.setFnState(false)
        assertFalse(switcher.isFnActive())
    }
    
    @Test
    fun testFnStateToggle() {
        val switcher = KeyboardSwitcher.getInstance()
        
        // Reset to known state
        switcher.setFnState(false)
        // Start false
        assertFalse(switcher.isFnActive())
        
        // Toggle to true
        switcher.setFnState(true)
        assertTrue(switcher.isFnActive())
        
        // Toggle to false
        switcher.setFnState(false)
        assertFalse(switcher.isFnActive())
        
        // Toggle to true again
        switcher.setFnState(true)
        assertTrue(switcher.isFnActive())
    }
    
    @Test
    fun testSettingSameValueMultipleTimes() {
        val switcher = KeyboardSwitcher.getInstance()
        
        // Set true multiple times
        switcher.setFnState(true)
        switcher.setFnState(true)
        switcher.setFnState(true)
        assertTrue(switcher.isFnActive())
        
        // Set false multiple times
        switcher.setFnState(false)
        switcher.setFnState(false)
        switcher.setFnState(false)
        assertFalse(switcher.isFnActive())
    }
}