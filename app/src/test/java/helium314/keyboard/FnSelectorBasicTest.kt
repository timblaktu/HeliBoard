// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard

import helium314.keyboard.keyboard.internal.keyboard_parser.floris.FnSelector
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.TextKeyData
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.AbstractKeyData
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Basic tests for FnSelector functionality without mocking.
 * Tests core selector logic.
 */
class FnSelectorBasicTest {
    
    @Test
    fun testFnSelectorDisplayString() {
        val selector = FnSelector(
            normal = TextKeyData(code = 'h'.code, label = "h"),
            fn = TextKeyData(code = KeyCode.ARROW_LEFT, label = "←")
        )
        
        // Display string shows both states
        assertEquals("h/←", selector.asString(isForDisplay = true))
        
        // Non-display string is empty
        assertEquals("", selector.asString(isForDisplay = false))
    }
    
    @Test
    fun testFnSelectorWithDifferentKeys() {
        // Test various key combinations
        val combinations = listOf(
            Triple('h', KeyCode.ARROW_LEFT, "←"),
            Triple('j', KeyCode.ARROW_DOWN, "↓"),
            Triple('k', KeyCode.ARROW_UP, "↑"),
            Triple('l', KeyCode.ARROW_RIGHT, "→"),
            Triple('1', KeyCode.F1, "F1"),
            Triple('2', KeyCode.F2, "F2")
        )
        
        for ((normalChar, fnCode, fnLabel) in combinations) {
            val selector = FnSelector(
                normal = TextKeyData(code = normalChar.code, label = normalChar.toString()),
                fn = TextKeyData(code = fnCode, label = fnLabel)
            )
            
            // Just verify the selector is created properly
            assertNotNull(selector)
            val displayString = selector.asString(isForDisplay = true)
            assertEquals("$normalChar/$fnLabel", displayString)
        }
    }
    
    @Test
    fun testFnSelectorWithComplexKeys() {
        // Test with special navigation keys
        val selector = FnSelector(
            normal = TextKeyData(code = '['.code, label = "["),
            fn = TextKeyData(code = -10015, label = "Home")  // MOVE_HOME
        )
        
        assertNotNull(selector)
        assertEquals("[/Home", selector.asString(isForDisplay = true))
    }
    
    @Test
    fun testFnSelectorWithEmptyLabels() {
        val selector = FnSelector(
            normal = TextKeyData(code = 32, label = ""),  // Space with empty label
            fn = TextKeyData(code = -1, label = "")       // Special code with empty label
        )
        
        assertNotNull(selector)
        assertEquals("/", selector.asString(isForDisplay = true))
    }
}