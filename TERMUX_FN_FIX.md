# Terminal Emulator Support - InputTypeAdapter Pattern

## Problem Analysis
Terminal emulators like Termux use `InputType.TYPE_NULL` for their input fields, which requires special handling compared to regular text fields. Arrow keys and function keys that work in normal Android apps fail in terminal emulators because they expect ANSI/VT100 escape sequences instead of Android key events.

## Solution: InputTypeAdapter Pattern

### Architecture
Created a flexible adapter pattern that handles different input types appropriately:

1. **InputTypeAdapter Interface** (`InputTypeAdapter.kt`)
   - Provides abstraction for handling different input field types
   - Methods for sending navigation keys and function keys
   - Extensible for future input type requirements

2. **TerminalInputAdapter**
   - Handles TYPE_NULL input fields (terminal emulators)
   - Sends ANSI escape sequences via `commitText()`
   - Supports full range of terminal navigation commands

3. **DefaultInputAdapter** 
   - Fallback for regular Android text fields
   - Uses standard key event handling

### Implementation Details

#### Files Created/Modified:
1. **New File:** `app/src/main/java/helium314/keyboard/keyboard/InputTypeAdapter.kt`
   - Contains adapter interface and implementations
   - Factory pattern for adapter selection

2. **Modified:** `KeyboardActionListenerImpl.kt`
   - Integrated adapter pattern in FN key handling
   - Detects input type from EditorInfo
   - Routes keys through appropriate adapter

### ANSI Escape Sequences Mapping

| Key | HeliBoard Code | ANSI Sequence | Description |
|-----|---------------|---------------|-------------|
| Arrow Up | -23 | ESC[A | Cursor up |
| Arrow Down | -24 | ESC[B | Cursor down |
| Arrow Right | -22 | ESC[C | Cursor right |
| Arrow Left | -21 | ESC[D | Cursor left |
| Home | -27 | ESC[H | Beginning of line |
| End | -28 | ESC[F | End of line |
| Page Up | -10010 | ESC[5~ | Page up |
| Page Down | -10011 | ESC[6~ | Page down |
| Insert | -10018 | ESC[2~ | Insert mode |
| Delete | CUT | ESC[3~ | Forward delete |
| F1-F4 | -10028 to -10031 | ESC O P/Q/R/S | Function keys |
| F5-F12 | -10032 to -10039 | ESC[15~-24~ | Function keys |
| Word Left | -10015 | ESC[1;5D | Ctrl+Left |
| Word Right | -10016 | ESC[1;5C | Ctrl+Right |

### How It Works

1. **Detection Phase**
   - When FN+key is pressed, check current EditorInfo
   - Determine input type (TYPE_NULL for terminals)
   - Select appropriate adapter via factory

2. **Routing Phase**
   - Navigation keys routed through adapter
   - Terminal adapter sends escape sequences
   - Default adapter falls back to key events

3. **Fallback Mechanism**
   - If adapter doesn't handle key, use default event system
   - Ensures compatibility with all apps

### Testing
- **Build:** `./gradlew assembleDebug`
- **APK Location:** `app/build/outputs/apk/debug/HeliBoard_3.3-debug.apk`
- **Test in Termux:**
  1. Install APK
  2. Enable HeliBoard in Android settings
  3. Open Termux
  4. Press FN+hjkl for arrow navigation
  5. Check debug log: `/sdcard/Download/HeliBoard/FN_Debug.log`

### Benefits of This Approach

1. **Not Application-Specific**
   - Works for ANY terminal emulator using TYPE_NULL
   - Not hardcoded for Termux

2. **Extensible Design**
   - Easy to add adapters for other input types
   - Clean separation of concerns

3. **Maintains Compatibility**
   - Regular apps continue working unchanged
   - Terminal apps get proper escape sequences

4. **Future-Proof**
   - Can add more terminal sequences as needed
   - Supports different terminal standards

### Debug Logging
The implementation includes comprehensive logging:
- Input type detection
- Adapter selection
- Key handling success/failure
- Log location: `/sdcard/Download/HeliBoard/FN_Debug.log`

### Known Limitations
1. Some terminal emulators may expect different escape sequences
2. Meta key combinations (Ctrl/Alt) not fully implemented
3. Terminal bell and other special sequences not handled

### Future Enhancements
1. Support for more terminal control sequences
2. Configurable escape sequence mappings
3. Terminal type detection (xterm, vt100, etc.)
4. Meta key modifier support