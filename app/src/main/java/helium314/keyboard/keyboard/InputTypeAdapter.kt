/*
 * Copyright (C) 2025 Helium314
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package helium314.keyboard.keyboard

import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.RichInputConnection

/**
 * Interface for adapting keyboard input to different input field types.
 * This allows special handling for terminal emulators and other apps that
 * require specific input formats.
 */
interface InputTypeAdapter {
    /**
     * Check if this adapter can handle the given input type
     */
    fun canHandle(inputType: Int): Boolean
    
    /**
     * Send a navigation key (arrow keys, page up/down, etc.) to the input field
     */
    fun sendNavigationKey(connection: RichInputConnection?, keyCode: Int, metaState: Int): Boolean
    
    /**
     * Send a function key (F1-F12) to the input field
     */
    fun sendFunctionKey(connection: RichInputConnection?, functionKeyNumber: Int): Boolean
}

/**
 * Adapter for terminal emulators that use TYPE_NULL input type.
 * Sends ANSI/VT100 escape sequences instead of Android key events.
 */
class TerminalInputAdapter : InputTypeAdapter {
    
    override fun canHandle(inputType: Int): Boolean {
        // Terminal emulators typically use TYPE_NULL or sometimes TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        return inputType == InputType.TYPE_NULL ||
               (inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT &&
               (inputType and InputType.TYPE_MASK_VARIATION) == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD &&
               (inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0
    }
    
    override fun sendNavigationKey(connection: RichInputConnection?, keyCode: Int, metaState: Int): Boolean {
        if (connection == null) return false
        
        // Convert HeliBoard key codes to ANSI escape sequences
        val escapeSequence = when (keyCode) {
            KeyCode.ARROW_UP -> "\u001b[A"      // ESC[A
            KeyCode.ARROW_DOWN -> "\u001b[B"    // ESC[B
            KeyCode.ARROW_RIGHT -> "\u001b[C"   // ESC[C
            KeyCode.ARROW_LEFT -> "\u001b[D"    // ESC[D
            
            // Extended navigation keys
            KeyCode.MOVE_START_OF_LINE -> "\u001b[H"     // ESC[H (Home)
            KeyCode.MOVE_END_OF_LINE -> "\u001b[F"        // ESC[F (End)
            KeyCode.MOVE_START_OF_PAGE -> "\u001b[1~"    // ESC[1~ (Page Home)
            KeyCode.MOVE_END_OF_PAGE -> "\u001b[4~"      // ESC[4~ (Page End)
            KeyCode.PAGE_UP -> "\u001b[5~"      // ESC[5~
            KeyCode.PAGE_DOWN -> "\u001b[6~"    // ESC[6~
            KeyCode.INSERT -> "\u001b[2~"       // ESC[2~
            KeyCode.CLIPBOARD_CUT -> "\u001b[3~" // ESC[3~ (Delete key - using CUT as forward delete)
            
            // Word navigation (using Ctrl+Arrow sequences)
            KeyCode.WORD_LEFT -> "\u001b[1;5D"  // Ctrl+Left
            KeyCode.WORD_RIGHT -> "\u001b[1;5C" // Ctrl+Right
            
            // Special keys
            KeyCode.ESCAPE -> "\u001b"          // ESC
            KeyCode.TAB -> "\t"                 // Tab character
            
            else -> null
        }
        
        return if (escapeSequence != null) {
            connection.commitText(escapeSequence, 1)
            true
        } else {
            false
        }
    }
    
    override fun sendFunctionKey(connection: RichInputConnection?, functionKeyNumber: Int): Boolean {
        if (connection == null || functionKeyNumber !in 1..12) return false
        
        // F1-F12 escape sequences (standard VT100/xterm)
        val escapeSequence = when (functionKeyNumber) {
            1 -> "\u001bOP"      // F1
            2 -> "\u001bOQ"      // F2
            3 -> "\u001bOR"      // F3
            4 -> "\u001bOS"      // F4
            5 -> "\u001b[15~"    // F5
            6 -> "\u001b[17~"    // F6
            7 -> "\u001b[18~"    // F7
            8 -> "\u001b[19~"    // F8
            9 -> "\u001b[20~"    // F9
            10 -> "\u001b[21~"   // F10
            11 -> "\u001b[23~"   // F11
            12 -> "\u001b[24~"   // F12
            else -> null
        }
        
        return if (escapeSequence != null) {
            connection.commitText(escapeSequence, 1)
            true
        } else {
            false
        }
    }
}

/**
 * Default adapter for regular Android text input fields.
 * Uses standard Android key events.
 */
class DefaultInputAdapter : InputTypeAdapter {
    
    override fun canHandle(inputType: Int): Boolean = true
    
    override fun sendNavigationKey(connection: RichInputConnection?, keyCode: Int, metaState: Int): Boolean {
        // For regular apps, return false to use the default key event handling
        // The caller should use sendDownUpKeyEventWithMetaState when this returns false
        return false
    }
    
    override fun sendFunctionKey(connection: RichInputConnection?, functionKeyNumber: Int): Boolean {
        // Regular apps typically don't handle function keys specially
        // Return false to let the default handling occur
        return false
    }
}

/**
 * Factory for creating the appropriate InputTypeAdapter based on the current input field
 */
object InputTypeAdapterFactory {
    private val terminalAdapter = TerminalInputAdapter()
    private val defaultAdapter = DefaultInputAdapter()
    
    fun getAdapter(inputType: Int): InputTypeAdapter {
        return when {
            terminalAdapter.canHandle(inputType) -> terminalAdapter
            else -> defaultAdapter
        }
    }
}