# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## FN Selector Implementation: ✅ COMPLETED (September 9, 2025)

**Status**: Fully implemented, tested, and documented with production layouts ready for use.
- **Complete Guide**: [FN_SELECTOR_COMPLETE_GUIDE.md](./FN_SELECTOR_COMPLETE_GUIDE.md) - User guide and technical documentation
- **Design Doc**: [FNSEL.md](./FNSEL.md) - Original design and implementation plan
- **Branch**: `fn-selector` - Implementation complete with 3 production layouts
- **APK**: Available in Download folder as `HeliBoard_FN_3.3-debug.apk`

### What Was Implemented:
- ✅ FnSelector class for JSON-configurable FN mappings
- ✅ FN state tracking in KeyboardId and KeyboardSwitcher
- ✅ 3 production layouts: QWERTY+FN, Programmer+FN, Vim+FN
- ✅ 9/9 unit tests passing
- ✅ Full HeliBoard UI integration for layout selection
- ✅ Side-by-side installation with F-Droid version supported

### How to Use:
See [FN_SELECTOR_COMPLETE_GUIDE.md](./FN_SELECTOR_COMPLETE_GUIDE.md) for complete usage instructions.

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