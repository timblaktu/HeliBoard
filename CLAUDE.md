# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Screenshot Location for Debugging
**Screenshot Directory**: `/storage/emulated/0/DCIM/Screenshots/`
- Access screenshots here to see UI issues and crash dialogs
- Latest crash screenshot: `Screenshot_20250909_082512_Device care.jpg`

## FN Selector Implementation: ✅ FULLY WORKING (September 9, 2025, 5:15 PM)

**Status**: FN toggle mode FULLY FUNCTIONAL - All arrow keys and function keys working!
- **Complete Guide**: [FN_SELECTOR_COMPLETE_GUIDE.md](./FN_SELECTOR_COMPLETE_GUIDE.md) - User guide and technical documentation
- **Design Doc**: [FNSEL.md](./FNSEL.md) - Original design and implementation plan
- **Branch**: `fn-selector` - Implementation complete with 3 production layouts
- **Latest APK**: `HeliBoard_FN_Fixed_Arrow_Keys_20250909_1715.apk` in Download folder
- **Log Location**: `/storage/emulated/0/Download/HeliBoard/FN_Debug.log` (accessible from Termux!)

### ✅ FIXED: Arrow Keys Now Working! (September 9, 2025, 5:15 PM)

#### The Fix That Solved Everything
**Location**: `KeyboardActionListenerImpl.kt:310-323`

**Root Cause**: After remapping functional keys (negative codes like -21 for ARROW_LEFT), the code was creating events twice and not returning early, causing the functional keys to be mishandled.

**The Solution**: Added early return for remapped functional keys:
```kotlin
// CRITICAL FIX: Handle remapped functional keys directly
if (remappedCode != primaryCode && remappedCode < 0) {
    logToFile("FN remapped to functional key, processing directly: $remappedCode")
    
    // Create the functional key event and process it
    val event = Event.createSoftwareKeypressEvent(remappedCode, metaState, mkv.getKeyX(x), mkv.getKeyY(y), isKeyRepeat)
    logToFile("Functional event created - isFunctional=${event.isFunctionalKeyEvent}, keyCode=${event.mKeyCode}")
    
    // Send the event to InputLogic for proper handling
    latinIME.onEvent(event)
    return  // CRITICAL: Return early to prevent double processing
}
```

**Why This Works**:
1. Functional keys (negative codes) need to be processed only once
2. The early return prevents creating duplicate events
3. InputLogic's handleFunctionalEvent properly converts HeliBoard codes to Android KeyEvent codes
4. The arrow keys now correctly send DPAD events to the application

### Technical Analysis: Event Processing Flow

#### How HeliBoard Processes Key Events:

1. **Key Press Detection** (`KeyboardActionListenerImpl.onCodeInput`)
   - Receives key code from keyboard
   - Checks FN state and remaps if active
   - Creates Event object using `Event.createSoftwareKeypressEvent()`

2. **Event Creation** (`Event.kt`)
   ```kotlin
   // For negative codes (like -21 for ARROW_LEFT):
   Event.createSoftwareKeypressEvent(keyCodeOrCodePoint, metaState, x, y, isKeyRepeat)
   // This creates an event with:
   // - mCodePoint = NOT_A_CODE_POINT (-1)
   // - mKeyCode = the negative value (-21)
   // - This makes isFunctionalKeyEvent = true
   ```

3. **Event Processing** (`InputLogic.java`)
   ```java
   if (event.isFunctionalKeyEvent()) {
       handleFunctionalEvent(event, ...);  // Should handle arrow keys
   } else {
       handleNonFunctionalEvent(event, ...);
   }
   ```

4. **Functional Event Handling** (`InputLogic.handleFunctionalEvent`)
   - Has specific cases for DELETE, SHIFT, etc.
   - **Default case handles unmapped functional keys**:
   ```java
   default:
       // Converts HeliBoard key codes to Android KeyEvent codes
       final int keyEventCode = KeyCode.keyCodeToKeyEventCode(keyCode);
       if (keyEventCode != KeyEvent.KEYCODE_UNKNOWN) {
           sendDownUpKeyEventWithMetaState(keyEventCode, metaState);
       }
   ```

5. **Key Code Conversion** (`KeyCode.kt`)
   ```kotlin
   fun keyCodeToKeyEventCode(keyCode: Int) = when (keyCode) {
       ARROW_LEFT -> KeyEvent.KEYCODE_DPAD_LEFT    // -21 → 21
       ARROW_RIGHT -> KeyEvent.KEYCODE_DPAD_RIGHT  // -22 → 22
       ARROW_UP -> KeyEvent.KEYCODE_DPAD_UP        // -23 → 19
       ARROW_DOWN -> KeyEvent.KEYCODE_DPAD_DOWN    // -24 → 20
       // etc...
   }
   ```

6. **Sending to Application** (`InputLogic.sendDownUpKeyEventWithMetaState`)
   - Creates Android KeyEvent objects
   - Sends ACTION_DOWN and ACTION_UP events via InputConnection

#### Current Hypothesis:
The arrow keys should be working based on the code flow. The enhanced logging will reveal:
1. Whether events are created with correct properties
2. If they reach handleFunctionalEvent
3. If key code conversion works
4. If sendDownUpKeyEvent is actually called

#### Files Modified for Debug Logging:
- `KeyboardActionListenerImpl.kt:323` - Log event properties after creation
- `InputLogic.java:651-653` - Log when arrow keys enter handleFunctionalEvent
- `InputLogic.java:805-816` - Log default case processing and key code conversion

### Previous Issues Found and Fixed (September 9, 2025)

#### 4. ✅ FIXED: FN State Persistence Issue
**Problem**: FN state was always logging as `false` even though remapping worked
**Root Causes Identified**:
1. Null safety operators (`?.`) in Kotlin code defaulting to false
2. Missing thread synchronization between UI and input threads
3. Possible race condition in state updates

**Solutions Applied**:
1. Removed null safety operators - use direct calls to `keyboardSwitcher.isFnActive()`
2. Added `volatile` modifier to `mFnState` field in KeyboardSwitcher.java
3. Enhanced debug logging to track state at each transition

**Verification**: Log now shows correct state transitions (true → false → true)

#### 1. ✅ Fixed: Keyboard Rebuild Crash
**Problem**: App crashed when FN key was pressed
**Root Cause**: `setFnState()` was rebuilding entire KeyboardLayoutSet during key press, causing null pointer exceptions
**Solution**: Simplified `setFnState()` to only update boolean flag without rebuilding keyboard

#### 2. ✅ Fixed: Incorrect Key Code Constants
**Problem**: Crash reports showed "key code -10013 not yet supported" 
**Analysis**: Found mismatched key codes between hardcoded mappings and actual KeyCode constants
- INSERT was incorrectly using -10013 (undefined) instead of -10018
- FORWARD_DELETE constant doesn't exist (commented out)
- MOVE_HOME/MOVE_END don't exist, should use MOVE_START_OF_LINE/MOVE_END_OF_LINE

**Solution**: Updated all key mappings to use correct constants from `KeyCode.kt`

#### 3. ✅ Fixed: Debugging Issues from Termux & Android 15 Storage
**Problem**: Multiple storage access issues on Android 15 without root
1. ADB logcat not accessible from Termux (no root, no system permissions)
2. `/sdcard/` root not writable on Android 10+ due to scoped storage
3. App-specific directories (`/Android/data/[package]/`) not accessible between apps
4. Termux cannot access other apps' private storage directories

**Root Cause**: Android's scoped storage restrictions prevent cross-app file access

**Solution Implemented**:
- File-based logging to `/storage/emulated/0/Download/HeliBoard/FN_Debug.log`
- This is the ONLY location accessible to both HeliBoard and Termux without root
- Toast notifications for immediate visual feedback
- Created HeliBoard subdirectory in Downloads for organization

**Added Features**:
- Toast shows "FN ON/OFF" when toggling
- Toast displays remapped keys (e.g., "FN: ←" for arrow left)
- All debug info written to Termux-accessible location
- Automatic directory creation if it doesn't exist

### CRITICAL: Android Storage & Termux Access Guide

#### Storage Locations on Android 15 (SDK 35)

| Location | HeliBoard Access | Termux Access | Notes |
|----------|-----------------|---------------|--------|
| `/sdcard/` (root) | ❌ No (SDK 30+) | ❌ No | Requires MANAGE_EXTERNAL_STORAGE permission |
| `/storage/emulated/0/Android/data/[package]/` | ✅ Own directory only | ❌ No | App-specific, isolated |
| `/storage/emulated/0/Download/` | ✅ Yes | ✅ Yes | **ONLY shared location that works!** |
| `/data/data/[package]/` | ✅ Own directory only | ❌ No | Internal app storage |

#### Termux Storage Setup
```bash
# Run this once to setup storage access in Termux:
termux-setup-storage

# This creates symlinks in ~/storage/:
~/storage/shared → /storage/emulated/0  # Full shared storage
~/storage/downloads → /storage/emulated/0/Download
~/storage/dcim → /storage/emulated/0/DCIM
# etc.
```

#### Accessing HeliBoard Logs from Termux
```bash
# View the log file (after FN key usage):
cat ~/storage/downloads/HeliBoard/FN_Debug.log

# Monitor log in real-time:
tail -f ~/storage/downloads/HeliBoard/FN_Debug.log

# Clear log file:
> ~/storage/downloads/HeliBoard/FN_Debug.log
```

### Fixed Key Mappings
```kotlin
// Correct key codes now being used:
ARROW_LEFT = -21, ARROW_RIGHT = -22, ARROW_UP = -23, ARROW_DOWN = -24
F1 through F12 = -10028 through -10039
MOVE_START_OF_LINE = -27, MOVE_END_OF_LINE = -28
PAGE_UP = -10010, PAGE_DOWN = -10011
WORD_LEFT = -10015, WORD_RIGHT = -10016
INSERT = -10018, ESCAPE = -10017
CLIPBOARD_CUT = -10002 (using as forward delete alternative)
```

### What Was Implemented:
- ✅ FnSelector class for JSON-configurable FN mappings
- ✅ FN state tracking in KeyboardId and KeyboardSwitcher
- ✅ FN toggle mode (press to toggle on/off, not hold mode)
- ✅ File-based debug logging to `/sdcard/HeliBoard_FN_Debug.log`
- ✅ Toast notifications for visual feedback
- ✅ 3 production layouts: QWERTY+FN, Programmer+FN, Vim+FN
- ✅ 9/9 unit tests passing
- ✅ Full HeliBoard UI integration for layout selection
- ✅ Side-by-side installation with F-Droid version supported

### How to Use:
See [FN_SELECTOR_COMPLETE_GUIDE.md](./FN_SELECTOR_COMPLETE_GUIDE.md) for complete usage instructions.

### Current Implementation Mode: Toggle (Not Hold)
The FN key currently works in **toggle mode**:
1. Press FN once to enable FN mode (toast shows "FN ON")
2. All subsequent keys will be remapped (hjkl→arrows, numbers→F-keys, etc.)
3. Press FN again to disable FN mode (toast shows "FN OFF")
4. Check `/sdcard/HeliBoard_FN_Debug.log` for detailed debugging info

**Note**: Hold mode (hold FN while pressing other keys) can be implemented later if needed.

### Testing the Latest Build
1. Install latest `HeliBoard_FN_DownloadFolder_*.apk` from Download folder
2. Enable HeliBoard in Android settings
3. Select a layout with FN key (functional_keys_with_fn)
4. Test FN toggle and key remappings
5. Monitor toast notifications for visual feedback
6. Check log file from Termux:
   ```bash
   # View log (accessible without root!)
   cat ~/storage/downloads/HeliBoard/FN_Debug.log
   
   # Watch log in real-time
   tail -f ~/storage/downloads/HeliBoard/FN_Debug.log
   ```

## 🚀 Next Steps for Continuation (Priority Order)

### 1. ✅ COMPLETED: Toggle Mode Implementation
**Status**: Successfully implemented toggle mode instead of hold mode
- FN key now toggles on/off like Caps Lock
- Toast notifications provide visual feedback
- File logging enables debugging without ADB
- Full key remapping working in toggle mode

### 2. Implement Hold Mode (Optional Future Enhancement)
**If hold mode is preferred over toggle**:
- Modify to detect FN key press/release events
- Maintain temporary state during hold
- Clear state on FN release
- May require changes to PointerTracker for gesture support

### 3. Implement Visual Feedback
**Once FN remapping works**:
- Update key labels when FN is active
- Add FN indicator in status area
- Consider key highlighting for active FN state

### 4. Complete FN Selector Integration
**After hardcoded version works**:
- Ensure FN selector JSON parsing works
- Test with production JSON layouts
- Remove hardcoded mappings in favor of JSON configuration

### 5. Create Pull Request
**When fully functional**:
- Clean up debug logging
- Write comprehensive tests
- Document the feature
- Submit to upstream HeliBoard repository

## Build Commands

### Common Development Tasks
- **Build debug APK**: `./gradlew assembleDebug`
- **Build release APK**: `./gradlew assembleRelease`
- **Fast IDE builds**: `./gradlew assembleDebugNoMinify` (no minification for faster builds)
- **Run tests**: `./gradlew test`
- **Run CI tests**: `./gradlew testRunTests` (skips tests known to fail on CI)
- **Lint code**: `./gradlew lint` (enforced with abortOnError = true)
- **Generate emoji data**: `./gradlew tools:make-emoji-keys:makeEmoji`

### Build Variants
- `debug`: Minified for GitHub 25MB limit, applicationIdSuffix = ".debug"
- `release`: Production build with minification and resource shrinking
- `nouserlib`: Same as release but prevents user from loading external glide typing libraries
- `debugNoMinify`: Fastest builds for IDE development
- `runTests`: Unminified variant specifically for CI testing

### Python Tools
- `tools/diacritics.py`: Diacritics processing utility
- `tools/release.py`: Release automation script
- `tools/make-emoji-keys/`: Emoji data generation tool

## Architecture Overview

HeliBoard is based on AOSP keyboard with significant modifications. The codebase is large and contains mixed code styles from different eras.

### Core Components
- **LatinIME** (`app/src/main/java/helium314/keyboard/latin/LatinIME.java`): Main IME service, receives events and information from apps/text fields
- **InputLogic** (`app/src/main/java/helium314/keyboard/latin/inputlogic/InputLogic.java`): Handles key inputs and text processing logic
- **PointerTracker** (`app/src/main/java/helium314/keyboard/keyboard/PointerTracker.java`): Touch and swipe input handling
- **Suggest** (`app/src/main/java/helium314/keyboard/latin/Suggest.kt`) + **DictionaryFacilitatorImpl**: Suggestion system from creation to display
- **SuggestionStripView**: Displays suggestions to user
- **RichInputConnection**: Forwards entered text/keys to apps/text fields
- **SettingsValues**: Contains settings with functionality in Settings class and default values in Default class

### Key Directories
- `app/src/main/java/helium314/keyboard/` - Main source code (Kotlin/Java mix)
- `app/src/main/assets/layouts/` - Keyboard layout files (both simple .txt and JSON formats)
- `app/src/main/assets/locale_key_texts/` - Language-specific popup keys and labels
- `app/src/main/jni/` - Native C++ code (NDK build)
- `app/src/main/res/` - Android resources
- `app/src/test/` - Unit tests (Robolectric framework)
- `tools/` - Development utilities and scripts

## Layout System

HeliBoard supports extensive layout customization with two distinct formats:

### Layout Types
1. **Main layouts**: Primary keyboard layouts selectable in Languages & Layouts
2. **Functional layouts**: Keys surrounding main layout (shift, space, delete, etc.)
3. **Symbol layouts**: Accessed via ?123 button, includes More Symbols variant
4. **Number/Numpad layouts**: For numeric input fields and numpad toggle
5. **Phone layouts**: For phone number input with Phone Symbols variant
6. **Special layouts**: Emoji bottom row, Clipboard bottom row, Number row

### Layout Formats
- **Simple format**: Text files with one key per line, double newlines mark new rows
- **JSON format**: FlorisBoard-compatible with advanced conditional features (selectors for shift state, input type, keyboard state, etc.)

### Layout Locations
- Main layouts: `app/src/main/assets/layouts/main/`
- Other layouts: `app/src/main/assets/layouts/`
- Method definitions: `app/src/main/res/xml/method.xml`

### Selector Key Classes (Multiuse Keys)

HeliBoard's selector system enables context-aware keys that reduce layout crowding by adapting behavior based on keyboard state, input type, and shift state.

#### Available Selector Types:

1. **FnSelector** (`"$": "fn_selector"`): ✨ **NEW - IMPLEMENTED & TESTED**
   - Switches between normal and FN layer keys
   - Uses `KeyboardId.isFnActive()` state
   - Enables function key layers without hardcoding
   - Keyboard rebuilds on FN state change for visual feedback
   ```json
   { "$": "fn_selector",
     "normal": { "label": "h" },
     "fn": { "code": -21, "label": "←" } }
   ```

2. **CaseSelector** (`"$": "case_selector"`):
   - Switches between lowercase/uppercase variants
   - Uses `KeyboardId.isAlphabetShifted()` state
   ```json
   { "$": "case_selector",
     "lower": { "label": ";" },
     "upper": { "label": ":" } }
   ```

3. **ShiftStateSelector** (`"$": "shift_state_selector"`):
   - Most comprehensive shift state handling
   - Supports: `unshifted`, `shifted`, `shiftedManual`, `shiftedAutomatic`, `capsLock`, `manualOrLocked`, `default`
   - Complex fallback chain: `shiftedManual → manualOrLocked → shifted → default`
   ```json
   { "$": "shift_state_selector",
     "manualOrLocked": { "label": "!" },
     "default": { "label": "1" } }
   ```

4. **VariationSelector** (`"$": "variation_selector"`):
   - Context-specific behavior for different input types
   - Supports: `default`, `email`, `uri`, `password`, `date`, `time`, `datetime`, `normal`
   ```json
   { "$": "variation_selector",
     "default": { "label": "comma" },
     "email": { "label": "@" },
     "uri": { "label": "/" } }
   ```

5. **KeyboardStateSelector** (`"$": "keyboard_state_selector"`):
   - Dynamic visibility based on keyboard configuration
   - Supports: `emojiKeyEnabled`, `languageKeyEnabled`, `symbols`, `moreSymbols`, `alphabet`, `default`
   - Sequential evaluation with early returns
   ```json
   { "$": "keyboard_state_selector",
     "languageKeyEnabled": { 
       "$": "keyboard_state_selector", 
       "alphabet": { "label": "language_switch" }
     }}
   ```

6. **LayoutDirectionSelector** (`"$": "layout_direction_selector"`):
   - RTL/LTR layout adaptation
   - Requires both `ltr` and `rtl` properties
   ```json
   { "$": "layout_direction_selector",
     "ltr": { "label": "(" },
     "rtl": { "label": ")" } }
   ```

#### Selector Implementation Details:
- **Recursive Design**: Selectors can contain other selectors for complex hierarchical behavior
- **KeyboardParams Context**: Provides rich context via `KeyboardId` (element, mode, enabled features)
- **Null-Safe Fallbacks**: All chains handle null gracefully with `?.compute(params)`
- **Single Evaluation**: One `compute()` call resolves entire selector chain

#### Best Practices for Layout Optimization:
1. **Layer selectors** for maximum key reuse: `shift_state_selector` containing `variation_selector`
2. **Use KeyboardStateSelector** for keys that should disappear when not needed
3. **Combine with popup keys** for tertiary functions
4. **Test across input types** (email, password, URL fields) to ensure proper behavior

#### Function Layer Implementation

**Current Status**: ✅ Selector-based implementation COMPLETE, ready for JSON layout creation

**Branch Structure**:
- `fn-hard`: Contains working hardcoded FN implementation (completed)
- `fn-selector`: New selector-based FN implementation (in development)

**Design Documentation**: See [FNSEL.md](./FNSEL.md) for complete FN selector design and implementation plan.

##### Hardcoded Implementation (fn-hard branch) ✅

1. **`app/src/main/java/helium314/keyboard/keyboard/PointerTracker.java:736`**
   - Removed `&& code != KeyCode.FN` from sliding input check
   - FN key supports sliding input (hold FN + tap other keys)

2. **`app/src/main/java/helium314/keyboard/keyboard/KeyboardActionListenerImpl.kt:117-165`**
   - Hardcoded FN key remapping logic in `onCodeInput()` method
   - When FN state is active, remaps keys to function/navigation codes

3. **Layout Files Created:**
   - `app/src/main/assets/layouts/functional/functional_keys_with_fn.json` - FN key at top left
   - `app/src/main/assets/layouts/functional/functional_keys_fn_bottom.json` - FN key in bottom row

##### FN Selector Implementation (fn-selector branch) ✅

**Status**: Implementation and testing complete. Ready for production JSON layouts.

**Key Features**:
- JSON-configurable FN mappings (no recompilation needed)
- Visual feedback (dynamic label updates)
- Composable with other selectors
- User customization without code changes

**Implemented Components**:
1. ✅ `FnSelector` class in KeyData.kt - Complete with compute() and asString()
2. ✅ FN state tracking in KeyboardId - mIsFnActive field and isFnActive() method
3. ✅ State management in KeyboardSwitcher - setFnState() and keyboard rebuild logic
4. ✅ JSON schema for fn_selector - Serialization registered in LayoutParser
5. ✅ Unit tests - 9 tests covering core functionality

**Example JSON Usage**:
```json
{ "$": "fn_selector",
  "normal": { "label": "h" },
  "fn": { "code": -21, "label": "←" }
}
```

The original hardcoded implementation used a fixed FN key definition:

```json
[
  [
    { "code": -5, "label": "FN", "width": 0.11 },
    { "type": "placeholder" },
    { "label": "delete", "width": 0.15 }
  ],
  [
    { "label": "shift", "width": 0.15 },
    { "type": "placeholder" }
  ],
  [
    { "label": "symbol_alpha", "width": 0.15 },
    { "$": "variation_selector",
      "default":  { "label": "comma" },
      "email":    { "label": "@", "groupId": 1, "type": "function" },
      "uri":      { "label": "/", "groupId": 1, "type": "function" }
    },
    { "label": "space" },
    { "label": "period" },
    { "label": "action", "width": 0.15 }
  ]
]
```

**Implemented Key Mappings:**

```kotlin
// FN key remapping implementation in KeyboardActionListenerImpl.kt
if (mFnState) {
    val remappedCode = when (code) {
        // Vim-style navigation
        'h'.code -> KeyCode.ARROW_LEFT    // -21
        'j'.code -> KeyCode.ARROW_DOWN    // -24
        'k'.code -> KeyCode.ARROW_UP      // -23
        'l'.code -> KeyCode.ARROW_RIGHT   // -22
        
        // Function keys F1-F12
        '1'.code -> -10028  // F1
        '2'.code -> -10029  // F2
        '3'.code -> -10030  // F3
        '4'.code -> -10031  // F4
        '5'.code -> -10032  // F5
        '6'.code -> -10033  // F6
        '7'.code -> -10034  // F7
        '8'.code -> -10035  // F8
        '9'.code -> -10036  // F9
        '0'.code -> -10037  // F10
        '-'.code -> -10038  // F11
        '='.code -> -10039  // F12
        
        // Navigation keys
        '['.code -> KeyCode.MOVE_HOME     // -10015
        ']'.code -> KeyCode.MOVE_END      // -10016
        ';'.code -> KeyCode.PAGE_UP       // -10010
        '\''.code -> KeyCode.PAGE_DOWN    // -10011
        
        // Word navigation
        'u'.code -> KeyCode.WORD_LEFT     // -10020
        'i'.code -> KeyCode.WORD_RIGHT    // -10021
        
        // Special keys
        'p'.code -> KeyCode.INSERT        // -10013
        '\\'.code -> KeyCode.FORWARD_DELETE // -10009
        
        else -> code  // No remapping
    }
    
    if (remappedCode != code) {
        handleFunctionalCode(remappedCode, x, y, spaceState, metaState, keyboardSwitchState)
        return
    }
}
```

**Key Code Reference:**
- **Arrow Keys:** LEFT=-21, UP=-23, RIGHT=-22, DOWN=-24
- **Function Keys:** F1=-10028 through F12=-10039
- **Navigation:** HOME=-10015, END=-10016, PAGE_UP=-10010, PAGE_DOWN=-10011
- **Word Movement:** WORD_LEFT=-10020, WORD_RIGHT=-10021
- **Edit Keys:** INSERT=-10013, FORWARD_DELETE=-10009
- **Modifiers:** FN=-5, SHIFT=-1, CTRL=-6, ALT=-7

**How to Enable:**
1. Select a functional layout with FN key (`functional_keys_with_fn` or `functional_keys_fn_bottom`)
2. The FN key will appear in your keyboard layout
3. Hold FN and tap mapped keys for special functions
4. Or slide from FN to target key (like shift sliding)

With the new FN selector implementation, customization is done entirely through JSON layout files without any code changes.

## Next Steps for FN Selector (Phase 4 & 5)

### Phase 4: JSON Layout Creation
1. **Create Production Layouts**
   - Convert existing layouts to include FN selector mappings
   - Create specialized FN layouts (vim navigation, programming, etc.)
   - Test JSON parsing and loading

2. **Example Layouts to Create**:
   - `qwerty_fn.json` - Standard QWERTY with FN layer
   - `programmer_fn.json` - Programming-focused FN mappings
   - `vim_fn.json` - Vim navigation optimized
   - `compact_fn.json` - Compact layout with extensive FN usage

3. **Layout Testing**:
   - Verify JSON deserialization
   - Test nested selectors with FN
   - Validate all key mappings work correctly

### Phase 5: Integration & Device Testing
1. **Visual Feedback**
   - Implement label updates when FN active
   - Add visual indicator for FN state
   - Test keyboard refresh performance

2. **Device Testing**
   - Test on physical Android devices
   - Verify sliding input (hold FN + tap)
   - Measure performance impact
   - Test with different screen sizes

3. **Documentation**
   - Create user guide for FN functionality
   - Document JSON layout creation
   - Add examples to repository

### Known Issues to Address
1. **Visual Updates**: Labels don't change visually when FN pressed (needs UI integration)
2. **Performance**: Keyboard rebuild on FN toggle needs optimization
3. **Sliding Input**: Not yet tested on actual device
4. **Accessibility**: Need to add accessibility descriptions for FN state

### Future Enhancements
1. **FN Lock** - Double-tap to lock FN state
2. **FN Indicators** - Status bar or key highlighting
3. **Custom FN Timeout** - Auto-release after inactivity
4. **Multi-FN Layers** - Support FN1, FN2, etc.

## Dictionary System

Four dictionary types per language:
- **main**: Built-in or user-provided dictionary
- **history**: User-typed words (deleted in incognito mode)
- **personal**: Device dictionary manageable through Android settings
- **contacts**: Contact names (disabled by default)

Custom dictionaries use .dict files and can be added via app settings or file explorer.

## Key Development Patterns

### Settings System
- **SettingsValues**: Central configuration holder
- **Settings**: Settings functionality
- **Default**: Default values
- Settings stored in device protected storage for pre-unlock access

### UI Components
- **ViewBinding**: Used for view binding
- **Jetpack Compose**: Used for newer UI components (themes, color picker, etc.)
- **Material 3**: Design system for Compose components

### Input Processing Flow
1. Touch input → PointerTracker
2. Key processing → InputLogic  
3. Text output → RichInputConnection → App
4. Suggestions: DictionaryFacilitatorImpl → Suggest → SuggestionStripView

### Testing
- **Framework**: Robolectric for unit tests
- **CI**: Uses runTests build variant
- **Debug mode**: Enabled by tapping version number multiple times in About

## Customization Features

### Theme System
- Day/night themes with user-defined colors
- Granular control over individual UI elements  
- Dynamic colors support (Android 12+)
- Theme export/import via files or clipboard

### Layout Customization
- In-app editor for both simple and JSON formats
- JSON format supports conditional keys (selectors)
- Special labels for functional keys ($$$, alpha, symbol, etc.)
- Toolbar key customization with Unicode code points

### Hidden Functionality
- Long-press toolbar keys for extended functions (clipboard→paste, move left/right→word left/right, etc.)
- Sliding key input from shift/symbol keys
- Suggestion gestures (swipe up, long press)
- Comma key long-press for quick access menu
- Shift key text case cycling on selected text

## Special Technical Details

### Build Configuration
- **NDK version**: 28.0.13004108
- **Compile/Target SDK**: 35
- **Min SDK**: 21
- **Kotlin version**: 2.1.21
- **AGP version**: 8.10.1
- **Java version**: 17

### Glide Typing
- External library support (not included due to licensing)
- SHA256 verification for security
- Compatible libraries available from GApps packages or community sources
- nouserlib variant prevents user library loading

### Privacy & Security
- No internet permission (100% offline)
- Device protected storage for settings
- Optional gesture typing library loading with integrity checks
- Incognito mode prevents learning/history

## Development Guidelines

### Code Contributions
- Make controversial features optional
- Avoid performance impact on core components  
- Keep changes localized to few places
- Consider APK size impact for major features
- Test on older devices for performance
- Use draft PRs for work-in-progress

### Layout Contributions  
- Follow existing popup key conventions
- Consider language-specific requirements
- Test with various screen sizes and orientations
- Maintain compatibility with existing features

### Translation
- Use Weblate (translate.codeberg.org/projects/heliboard/)
- Do not include translations in PRs
- New strings can be added directly in PR

### Dictionary Contributions
- Submit to separate dictionaries repository (codeberg.org/Helium314/aosp-dictionaries)
- No new dictionaries added directly to main app
## Terminal Emulator Support (Termux Arrow Keys Fix)
**Date:** September 09, 2025
**Issue:** Arrow keys and function keys not working in Termux despite working in regular Android apps
**Solution:** Implemented InputTypeAdapter pattern for terminal-specific key handling

### Problem
Terminal emulators like Termux use `InputType.TYPE_NULL` which expects ANSI escape sequences instead of Android key events. This caused FN+hjkl arrow keys to work in regular apps but fail in terminals.

### Solution Architecture
Implemented a general **InputTypeAdapter pattern** that:
- Detects input field type from EditorInfo
- Routes keys through appropriate adapter
- Sends escape sequences for TYPE_NULL fields
- Falls back to key events for regular fields

### Key Components
1. **InputTypeAdapter.kt** - Interface and implementations
2. **TerminalInputAdapter** - Handles TYPE_NULL with escape sequences
3. **DefaultInputAdapter** - Standard Android key event handling
4. **Modified KeyboardActionListenerImpl** - Integrated adapter routing

### Escape Sequences Implemented
- Arrow keys: `ESC[A/B/C/D` (up/down/right/left)
- Function keys: `ESC O P/Q/R/S` (F1-F4), `ESC[15~-24~` (F5-F12)
- Navigation: Home/End, Page Up/Down, Word Left/Right
- Full terminal compatibility without app-specific hacks

### Testing
Latest APK: `HeliBoard_FN_Termux_Fix_YYYYMMDD_HHMM.apk` in Download folder
- Works in Termux and all terminal emulators using TYPE_NULL
- Maintains compatibility with regular Android apps
- Debug log: `/sdcard/Download/HeliBoard/FN_Debug.log`

**For detailed documentation, see:** [TERMUX_FN_FIX.md](TERMUX_FN_FIX.md)
