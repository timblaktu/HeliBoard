package helium314.keyboard.keyboard

import android.text.InputType
import android.util.SparseArray
import android.view.KeyEvent
import android.view.inputmethod.InputMethodSubtype
import helium314.keyboard.event.Event
import helium314.keyboard.event.HangulEventDecoder
import helium314.keyboard.event.HardwareEventDecoder
import helium314.keyboard.event.HardwareKeyboardEventDecoder
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.EmojiAltPhysicalKeyDetector
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.RichInputMethodManager
import helium314.keyboard.latin.common.Constants
import helium314.keyboard.latin.common.InputPointers
import helium314.keyboard.latin.common.StringUtils
import helium314.keyboard.latin.common.combiningRange
import helium314.keyboard.latin.common.loopOverCodePoints
import helium314.keyboard.latin.common.loopOverCodePointsBackwards
import helium314.keyboard.latin.define.ProductionFlags
import helium314.keyboard.latin.inputlogic.InputLogic
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.Log
import kotlin.math.abs
import kotlin.math.min

class KeyboardActionListenerImpl(private val latinIME: LatinIME, private val inputLogic: InputLogic) : KeyboardActionListener {

    companion object {
        private val TAG = KeyboardActionListenerImpl::class.java.simpleName
    }

    private val connection = inputLogic.mConnection
    private val emojiAltPhysicalKeyDetector by lazy { EmojiAltPhysicalKeyDetector(latinIME.resources) }

    // We expect to have only one decoder in almost all cases, hence the default capacity of 1.
    // If it turns out we need several, it will get grown seamlessly.
    private val hardwareEventDecoders: SparseArray<HardwareEventDecoder> = SparseArray(1)

    private val keyboardSwitcher = KeyboardSwitcher.getInstance()
    private val settings = Settings.getInstance()
    private var metaState = 0 // is this enough, or are there threading issues with the different PointerTrackers?

    // language slide state
    private var initialSubtype: InputMethodSubtype? = null
    private var subtypeSwitchCount = 0

    // todo: maybe keep meta state presses to KeyboardActionListenerImpl, and avoid calls to press/release key
    private fun adjustMetaState(code: Int, remove: Boolean) {
        val metaCode = when (code) {
            KeyCode.CTRL -> KeyEvent.META_CTRL_ON
            KeyCode.CTRL_LEFT -> KeyEvent.META_CTRL_LEFT_ON
            KeyCode.CTRL_RIGHT -> KeyEvent.META_CTRL_RIGHT_ON
            KeyCode.ALT -> KeyEvent.META_ALT_ON
            KeyCode.ALT_LEFT -> KeyEvent.META_ALT_LEFT_ON
            KeyCode.ALT_RIGHT -> KeyEvent.META_ALT_RIGHT_ON
            KeyCode.FN -> KeyEvent.META_FUNCTION_ON
            KeyCode.META -> KeyEvent.META_META_ON
            KeyCode.META_LEFT -> KeyEvent.META_META_LEFT_ON
            KeyCode.META_RIGHT -> KeyEvent.META_META_RIGHT_ON
            else -> return
        }
        metaState = if (remove) metaState and metaCode.inv()
            else metaState or metaCode
    }

    override fun onPressKey(primaryCode: Int, repeatCount: Int, isSinglePointer: Boolean) {
        adjustMetaState(primaryCode, false)
        // Update FN state in KeyboardSwitcher for selector system
        if (primaryCode == KeyCode.FN) {
            keyboardSwitcher.setFnState(true)
        }
        keyboardSwitcher.onPressKey(primaryCode, isSinglePointer, latinIME.currentAutoCapsState, latinIME.currentRecapitalizeState)
        latinIME.hapticAndAudioFeedback(primaryCode, repeatCount)
    }

    override fun onReleaseKey(primaryCode: Int, withSliding: Boolean) {
        adjustMetaState(primaryCode, true)
        // Update FN state in KeyboardSwitcher for selector system
        if (primaryCode == KeyCode.FN) {
            keyboardSwitcher.setFnState(false)
        }
        keyboardSwitcher.onReleaseKey(primaryCode, withSliding, latinIME.currentAutoCapsState, latinIME.currentRecapitalizeState)
    }

    override fun onKeyUp(keyCode: Int, keyEvent: KeyEvent): Boolean {
        emojiAltPhysicalKeyDetector.onKeyUp(keyEvent)
        if (!ProductionFlags.IS_HARDWARE_KEYBOARD_SUPPORTED)
            return false

        val keyIdentifier = keyEvent.deviceId.toLong() shl 32 + keyEvent.keyCode
        return inputLogic.mCurrentlyPressedHardwareKeys.remove(keyIdentifier)
    }

    override fun onKeyDown(keyCode: Int, keyEvent: KeyEvent): Boolean {
        emojiAltPhysicalKeyDetector.onKeyDown(keyEvent)
        if (!ProductionFlags.IS_HARDWARE_KEYBOARD_SUPPORTED)
            return false

        val event: Event
        if (settings.current.mLocale.language == "ko") { // todo: this does not appear to be the right place
            val subtype = keyboardSwitcher.keyboard?.mId?.mSubtype ?: RichInputMethodManager.getInstance().currentSubtype
            event = HangulEventDecoder.decodeHardwareKeyEvent(subtype, keyEvent) {
                getHardwareKeyEventDecoder(keyEvent.deviceId).decodeHardwareKey(keyEvent)
            }
        } else {
            event = getHardwareKeyEventDecoder(keyEvent.deviceId).decodeHardwareKey(keyEvent)
        }

        if (event.isHandled) {
            inputLogic.onCodeInput(
                settings.current, event,
                keyboardSwitcher.getKeyboardShiftMode(), // TODO: this is not necessarily correct for a hardware keyboard right now
                keyboardSwitcher.getCurrentKeyboardScript(),
                latinIME.mHandler
            )
            return true
        }
        return false
    }


    override fun onCodeInput(primaryCode: Int, x: Int, y: Int, isKeyRepeat: Boolean) {
        when (primaryCode) {
            KeyCode.TOGGLE_AUTOCORRECT -> return Settings.getInstance().toggleAutoCorrect()
            KeyCode.TOGGLE_INCOGNITO_MODE -> return Settings.getInstance().toggleAlwaysIncognitoMode()
            KeyCode.FN -> {
                // Toggle FN state
                try {
                    val currentFnState = keyboardSwitcher.isFnActive()
                    val newFnState = !currentFnState
                    keyboardSwitcher.setFnState(newFnState)
                    // Show toast notification for FN state change
                    val message = if (newFnState) "FN ON" else "FN OFF"
                    keyboardSwitcher.showToast(message, true)
                    Log.d(TAG, "FN state toggled: $newFnState")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to toggle FN state", e)
                }
                return
            }
        }
        val mkv = keyboardSwitcher.mainKeyboardView

        // FN Key Remapping System
        // When FN is active, remap regular keys to function/navigation keys
        // This allows a compact keyboard to provide full desktop-style navigation
        // without requiring additional rows or modifier keys
        val fnActive = try {
            keyboardSwitcher.isFnActive()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get FN state", e)
            false
        }
        
        val remappedCode = if (fnActive) {
            // Map standard keys to function keys when FN is active
            // The mappings are inspired by vim navigation and common desktop shortcuts
            val mapped = when (primaryCode) {
                // hjkl = arrow keys (vim-style navigation)
                'h'.code -> KeyCode.ARROW_LEFT      // -21
                'j'.code -> KeyCode.ARROW_DOWN      // -24
                'k'.code -> KeyCode.ARROW_UP        // -23
                'l'.code -> KeyCode.ARROW_RIGHT     // -22
                'H'.code -> KeyCode.ARROW_LEFT      // Capital H
                'J'.code -> KeyCode.ARROW_DOWN      // Capital J
                'K'.code -> KeyCode.ARROW_UP        // Capital K
                'L'.code -> KeyCode.ARROW_RIGHT     // Capital L
                
                // Numbers = F1-F12
                '1'.code -> KeyCode.F1              // -10028
                '2'.code -> KeyCode.F2              // -10029
                '3'.code -> KeyCode.F3              // -10030
                '4'.code -> KeyCode.F4              // -10031
                '5'.code -> KeyCode.F5              // -10032
                '6'.code -> KeyCode.F6              // -10033
                '7'.code -> KeyCode.F7              // -10034
                '8'.code -> KeyCode.F8              // -10035
                '9'.code -> KeyCode.F9              // -10036
                '0'.code -> KeyCode.F10             // -10037
                '-'.code -> KeyCode.F11             // -10038
                '='.code -> KeyCode.F12             // -10039
                
                // Navigation keys
                '['.code -> KeyCode.MOVE_START_OF_LINE  // -27 (Home)
                ']'.code -> KeyCode.MOVE_END_OF_LINE    // -28 (End)
                ';'.code -> KeyCode.PAGE_UP             // -10010
                '\''.code -> KeyCode.PAGE_DOWN          // -10011
                
                // Word navigation
                'u'.code -> KeyCode.WORD_LEFT           // -10015
                'i'.code -> KeyCode.WORD_RIGHT          // -10016
                'U'.code -> KeyCode.WORD_LEFT           // Capital U
                'I'.code -> KeyCode.WORD_RIGHT          // Capital I
                
                // Other useful mappings
                'p'.code -> KeyCode.INSERT              // -10018
                'P'.code -> KeyCode.INSERT              // Capital P
                '\\'.code -> KeyCode.CLIPBOARD_CUT      // Use CUT as forward delete alternative
                
                // Escape key
                'e'.code -> KeyCode.ESCAPE              // -10017
                'E'.code -> KeyCode.ESCAPE              // Capital E
                
                else -> primaryCode // Keep original code if no mapping
            }
            if (mapped != primaryCode) {
                Log.d(TAG, "FN remapping: $primaryCode -> $mapped")
            }
            mapped
        } else {
            primaryCode
        }

        // Special Handling for Functional Keys
        // Functional keys (negative codes) need special processing to work correctly
        // This section ensures proper event generation and routing for navigation,
        // function keys, and other special keys
        if (remappedCode != primaryCode && remappedCode < 0) {
            
            // Get the current input type to determine how to send the key
            val editorInfo = latinIME.currentInputEditorInfo
            val inputType = editorInfo?.inputType ?: InputType.TYPE_CLASS_TEXT
            val adapter = InputTypeAdapterFactory.getAdapter(inputType)
            
            
            // Try to handle navigation keys through the adapter first
            var handled = false
            try {
                if (isNavigationKey(remappedCode)) {
                    handled = adapter.sendNavigationKey(connection, remappedCode, metaState)
                    if (handled) {
                        Log.d(TAG, "Navigation key handled by adapter: $remappedCode")
                    }
                } else if (isFunctionKey(remappedCode)) {
                    val fnNumber = getFunctionKeyNumber(remappedCode)
                    if (fnNumber != null) {
                        handled = adapter.sendFunctionKey(connection, fnNumber)
                        if (handled) {
                            Log.d(TAG, "Function key F$fnNumber handled by adapter")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling key through adapter", e)
                handled = false
            }
            
            // If the adapter handled it, we're done
            if (handled) {
                return
            }
            
            // Otherwise, fall back to the default event handling
            val event = Event.createSoftwareKeypressEvent(remappedCode, metaState, mkv.getKeyX(x), mkv.getKeyY(y), isKeyRepeat)
            
            // Send the event to InputLogic for proper handling
            latinIME.onEvent(event)
            return  // CRITICAL: Return early to prevent double processing
        }

        // checking if the character is a combining accent
        val event = if (remappedCode in combiningRange) { // todo: should this be done later, maybe in inputLogic?
            Event.createSoftwareDeadEvent(remappedCode, 0, metaState, mkv.getKeyX(x), mkv.getKeyY(y), null)
        } else {
            // todo:
            //  setting meta shift should only be done for arrow and similar cursor movement keys
            //  should only be enabled once it works more reliably (currently depends on app for some reason)
//            if (mkv.keyboard?.mId?.isAlphabetShiftedManually == true)
//                Event.createSoftwareKeypressEvent(remappedCode, metaState or KeyEvent.META_SHIFT_ON, mkv.getKeyX(x), mkv.getKeyY(y), isKeyRepeat)
//            else Event.createSoftwareKeypressEvent(remappedCode, metaState, mkv.getKeyX(x), mkv.getKeyY(y), isKeyRepeat)
            Event.createSoftwareKeypressEvent(remappedCode, metaState, mkv.getKeyX(x), mkv.getKeyY(y), isKeyRepeat)
        }
        
        latinIME.onEvent(event)
    }

    override fun onTextInput(text: String?) = latinIME.onTextInput(text)

    override fun onStartBatchInput() = latinIME.onStartBatchInput()

    override fun onUpdateBatchInput(batchPointers: InputPointers?) = latinIME.onUpdateBatchInput(batchPointers)

    override fun onEndBatchInput(batchPointers: InputPointers?) = latinIME.onEndBatchInput(batchPointers)

    override fun onCancelBatchInput() = latinIME.onCancelBatchInput()

    // User released a finger outside any key
    override fun onCancelInput() { }

    override fun onFinishSlidingInput() =
        keyboardSwitcher.onFinishSlidingInput(latinIME.currentAutoCapsState, latinIME.currentRecapitalizeState)

    override fun onCustomRequest(requestCode: Int): Boolean {
        if (requestCode == Constants.CUSTOM_CODE_SHOW_INPUT_METHOD_PICKER) {
            return latinIME.showInputPickerDialog()
        }
        return false
    }

    override fun onHorizontalSpaceSwipe(steps: Int): Boolean = when (Settings.getValues().mSpaceSwipeHorizontal) {
        KeyboardActionListener.SWIPE_MOVE_CURSOR -> onMoveCursorHorizontally(steps)
        KeyboardActionListener.SWIPE_SWITCH_LANGUAGE -> onLanguageSlide(steps)
        KeyboardActionListener.SWIPE_TOGGLE_NUMPAD -> toggleNumpad(false, false)
        else -> false
    }

    override fun onVerticalSpaceSwipe(steps: Int): Boolean = when (Settings.getValues().mSpaceSwipeVertical) {
        KeyboardActionListener.SWIPE_MOVE_CURSOR -> onMoveCursorVertically(steps)
        KeyboardActionListener.SWIPE_SWITCH_LANGUAGE -> onLanguageSlide(steps)
        KeyboardActionListener.SWIPE_TOGGLE_NUMPAD -> toggleNumpad(false, false)
        KeyboardActionListener.SWIPE_HIDE_KEYBOARD -> {
            latinIME.requestHideSelf(0)
            true
        }
        else -> false
    }

    override fun onEndSpaceSwipe(){
        initialSubtype = null
        subtypeSwitchCount = 0
    }

    override fun toggleNumpad(withSliding: Boolean, forceReturnToAlpha: Boolean): Boolean {
        KeyboardSwitcher.getInstance().toggleNumpad(withSliding, latinIME.currentAutoCapsState, latinIME.currentRecapitalizeState, forceReturnToAlpha)
        return true
    }

    override fun onMoveDeletePointer(steps: Int) {
        inputLogic.finishInput()
        val end = connection.expectedSelectionEnd
        val actualSteps = actualSteps(steps)
        val start = connection.expectedSelectionStart + actualSteps
        if (start > end) return
        connection.setSelection(start, end)
    }

    private fun actualSteps(steps: Int): Int {
        var actualSteps = 0
        // corrected steps to avoid splitting chars belonging to the same codepoint
        if (steps > 0) {
            val text = connection.getSelectedText(0) ?: return steps
            loopOverCodePoints(text) { cp, charCount ->
                actualSteps += charCount
                actualSteps >= steps
            }
        } else {
            val text = connection.getTextBeforeCursor(-steps * 4, 0) ?: return steps
            loopOverCodePointsBackwards(text) { cp, charCount ->
                actualSteps -= charCount
                actualSteps <= steps
            }
        }
        return actualSteps
    }

    override fun onUpWithDeletePointerActive() {
        if (!connection.hasSelection()) return
        inputLogic.finishInput()
        onCodeInput(KeyCode.DELETE, Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE, false)
    }

    override fun resetMetaState() {
        metaState = 0
    }

    private fun onLanguageSlide(steps: Int): Boolean {
        if (abs(steps) < settings.current.mLanguageSwipeDistance) return false
        val subtypes = RichInputMethodManager.getInstance().getMyEnabledInputMethodSubtypes(true)
        if (subtypes.size <= 1) { // only allow if we have more than one subtype
            return false
        }
        // decide next or previous dependent on up or down
        val current = RichInputMethodManager.getInstance().currentSubtype.rawSubtype
        var wantedIndex = subtypes.indexOf(current) + if (steps > 0) 1 else -1
        wantedIndex %= subtypes.size
        if (wantedIndex < 0) {
            wantedIndex += subtypes.size
        }
        val newSubtype = subtypes[wantedIndex]

        // do not switch if we would switch to the initial subtype after cycling all other subtypes
        if (initialSubtype == null) initialSubtype = current
        if (initialSubtype == newSubtype) {
            if ((subtypeSwitchCount > 0 && steps > 0) || (subtypeSwitchCount < 0 && steps < 0)) {
                return true
            }
        }
        if (steps > 0) subtypeSwitchCount++ else subtypeSwitchCount--

        KeyboardSwitcher.getInstance().switchToSubtype(newSubtype)
        return true
    }

    private fun onMoveCursorVertically(steps: Int): Boolean {
        if (steps == 0) return false
        val code = if (steps < 0) KeyCode.ARROW_UP else KeyCode.ARROW_DOWN
        onCodeInput(code, Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE, false)
        return true
    }

    private fun onMoveCursorHorizontally(rawSteps: Int): Boolean {
        if (rawSteps == 0) return false
        // for RTL languages we want to invert pointer movement
        val steps = if (RichInputMethodManager.getInstance().currentSubtype.isRtlSubtype) -rawSteps else rawSteps
        val moveSteps: Int
        if (steps < 0) {
            val text = connection.getTextBeforeCursor(-steps * 4, 0) ?: return false
            moveSteps = negativeMoveSteps(text, steps)
            if (moveSteps == 0) {
                // some apps don't return any text via input connection, and the cursor can't be moved
                // we fall back to virtually pressing the left/right key one or more times instead
                repeat(-steps) {
                    onCodeInput(KeyCode.ARROW_LEFT, Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE, false)
                }
                return true
            }
        } else {
            val text = connection.getTextAfterCursor(steps * 4, 0) ?: return false
            moveSteps = positiveMoveSteps(text, steps)
            if (moveSteps == 0) {
                // some apps don't return any text via input connection, and the cursor can't be moved
                // we fall back to virtually pressing the left/right key one or more times instead
                repeat(steps) {
                    onCodeInput(KeyCode.ARROW_RIGHT, Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE, false)
                }
                return true
            }
        }

        // the shortcut below causes issues due to horrible handling of text fields by Firefox and forks
        // issues:
        //  * setSelection "will cause the editor to call onUpdateSelection", see: https://developer.android.com/reference/android/view/inputmethod/InputConnection#setSelection(int,%20int)
        //     but Firefox is simply not doing this within the same word... WTF?
        //     https://github.com/Helium314/HeliBoard/issues/1139#issuecomment-2588169384
        //  * inputType is NOT if variant InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT (variant appears to always be 0)
        //     so we can't even only do it for browsers (identifying by app name will break for forks)
        // best "solution" is not doing this for InputType variation 0 but this applies to the majority of text fields...
        val variation = InputType.TYPE_MASK_VARIATION and Settings.getValues().mInputAttributes.mInputType
        if (variation != 0 && inputLogic.moveCursorByAndReturnIfInsideComposingWord(moveSteps)) {
            // no need to finish input and restart suggestions if we're still in the word
            // this is a noticeable performance improvement when moving through long words
            val newPosition = connection.expectedSelectionStart + moveSteps
            connection.setSelection(newPosition, newPosition)
            return true
        }

        inputLogic.finishInput()
        val newPosition = connection.expectedSelectionStart + moveSteps
        connection.setSelection(newPosition, newPosition)
        inputLogic.restartSuggestionsOnWordTouchedByCursor(settings.current, keyboardSwitcher.currentKeyboardScript)
        return true
    }

    private fun positiveMoveSteps(text: CharSequence, steps: Int): Int {
        var actualSteps = 0
        // corrected steps to avoid splitting chars belonging to the same codepoint
        loopOverCodePoints(text) { cp, charCount ->
            if (StringUtils.mightBeEmoji(cp)) return 0
            actualSteps += charCount
            actualSteps >= steps
        }
        return min(actualSteps, text.length)
    }

    private fun negativeMoveSteps(text: CharSequence, steps: Int): Int {
        var actualSteps = 0
        // corrected steps to avoid splitting chars belonging to the same codepoint
        loopOverCodePointsBackwards(text) { cp, charCount ->
            if (StringUtils.mightBeEmoji(cp)) return 0
            actualSteps -= charCount
            actualSteps <= steps
        }
        return -min(-actualSteps, text.length)
    }

    private fun getHardwareKeyEventDecoder(deviceId: Int): HardwareEventDecoder {
        hardwareEventDecoders.get(deviceId)?.let { return it }

        // TODO: create the decoder according to the specification
        val newDecoder = HardwareKeyboardEventDecoder(deviceId)
        hardwareEventDecoders.put(deviceId, newDecoder)
        return newDecoder
    }
    
    // Helper functions for InputTypeAdapter integration
    private fun isNavigationKey(keyCode: Int): Boolean {
        return keyCode in listOf(
            KeyCode.ARROW_UP, KeyCode.ARROW_DOWN, KeyCode.ARROW_LEFT, KeyCode.ARROW_RIGHT,
            KeyCode.MOVE_START_OF_LINE, KeyCode.MOVE_END_OF_LINE,
            KeyCode.MOVE_START_OF_PAGE, KeyCode.MOVE_END_OF_PAGE,
            KeyCode.PAGE_UP, KeyCode.PAGE_DOWN,
            KeyCode.WORD_LEFT, KeyCode.WORD_RIGHT,
            KeyCode.INSERT, KeyCode.CLIPBOARD_CUT,  // Using CUT as forward delete
            KeyCode.ESCAPE, KeyCode.TAB
        )
    }
    
    private fun isFunctionKey(keyCode: Int): Boolean {
        return keyCode in -10039..-10028 // F1 to F12
    }
    
    private fun getFunctionKeyNumber(keyCode: Int): Int? {
        return when (keyCode) {
            -10028 -> 1   // F1
            -10029 -> 2   // F2
            -10030 -> 3   // F3
            -10031 -> 4   // F4
            -10032 -> 5   // F5
            -10033 -> 6   // F6
            -10034 -> 7   // F7
            -10035 -> 8   // F8
            -10036 -> 9   // F9
            -10037 -> 10  // F10
            -10038 -> 11  // F11
            -10039 -> 12  // F12
            else -> null
        }
    }
}
