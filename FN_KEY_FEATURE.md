# FN Key Feature Documentation

## Overview

This feature adds comprehensive FN (Function) key support to HeliBoard, enabling power users to access navigation keys, function keys, and special controls without leaving the keyboard. The implementation follows HeliBoard's existing modifier key patterns (Shift, Ctrl, Alt) and integrates seamlessly with the keyboard's architecture.

## Problem Statement

Mobile keyboards lack efficient access to navigation and function keys that are essential for:
- Terminal emulator usage (Termux, ConnectBot)
- Remote desktop applications
- Code editors and IDEs
- Power user workflows requiring arrow keys, Home/End, Page Up/Down
- Vim-style navigation preferences

## Features

### Core FN Key Functionality
- **Toggle Mode**: Press FN to enable/disable the function layer (like Caps Lock)
- **Key Remapping**: When FN is active, regular keys are remapped to function/navigation keys
- **Visual Feedback**: Toast notifications ("FN ON"/"FN OFF") provide clear state indication
- **Sliding Input**: FN key supports sliding input like Shift
- **JSON Configuration**: FN mappings can be defined in layout files using the `fn_selector`

### Default Key Mappings

When FN is active, the following remappings are applied:

#### Navigation (Vim-style)
- `h` → Left Arrow
- `j` → Down Arrow  
- `k` → Up Arrow
- `l` → Right Arrow

#### Function Keys
- `1-9` → F1-F9
- `0` → F10
- `-` → F11
- `=` → F12

#### Extended Navigation
- `[` → Home
- `]` → End
- `;` → Page Up
- `'` → Page Down
- `u` → Word Left (Ctrl+Left)
- `i` → Word Right (Ctrl+Right)

#### Special Keys
- `p` → Insert
- `\` → Forward Delete
- `e` → Escape

## Toast Notifications Rationale

Toast notifications for FN state changes follow HeliBoard's existing patterns:
- **Precedent**: `KeyboardSwitcher.showToast()` is used for user feedback throughout the codebase
- **Examples in codebase**:
  - Clipboard operations: "Text copied to clipboard" (RichInputConnection.java:1072)
  - Keyboard errors: "error loading the keyboard" (KeyboardSwitcher.java:176)
- **Purpose**: Provides visual confirmation of FN toggle state without disrupting typing flow
- **Implementation**: Uses existing `showToast()` method with brief duration for minimal intrusion

## Implementation Details

### Architecture Changes

1. **FN State Tracking** (`KeyboardId.java`)
   - Added `mIsFnActive` field to track FN state
   - State persists across keyboard rebuilds
   - Integrated with keyboard identity hashing

2. **State Management** (`KeyboardSwitcher.java`)
   - `setFnState(boolean)` - Updates FN state without keyboard rebuild
   - `isFnActive()` - Returns current FN state
   - `toggleFnState()` - Convenience method for toggling
   - Toast notifications via existing `showToast()` method

3. **Key Remapping** (`KeyboardActionListenerImpl.kt`)
   - FN toggle handling in `onCodeInput()`
   - Conditional remapping based on FN state
   - Proper logging with HeliBoard's Log utility
   - Exception handling with try-catch blocks

4. **Sliding Input** (`PointerTracker.java`)
   - Line 736: Removed FN exclusion from sliding input
   - Allows FN+key sliding gestures

5. **JSON Layout Support** (`KeyData.kt`, `LayoutParser.kt`)
   - `FnSelector` class for dynamic key configuration
   - Registered with layout parser for JSON deserialization
   - Enables per-layout FN customization

### Layout Examples

Example layouts in `app/src/main/assets/layouts/examples/`:
- `qwerty_fn.json` - Standard QWERTY with FN key
- `programmer_fn.json` - Programmer-friendly layout  
- `vim_fn.json` - Vim-optimized navigation
- `README.md` - Documentation for creating custom FN layouts

## Testing

### Manual Testing
1. Build and install the APK
2. Enable HeliBoard in Android settings
3. Open any text input field
4. Test FN key combinations:
   - FN toggle shows toast notifications
   - FN + hjkl for arrow navigation
   - FN + numbers for function keys
   - FN + other mapped keys
5. Test sliding from FN to target keys

### Unit Tests
- `FnSelectorBasicTest.kt` - Tests FN selector JSON parsing
- `KeyboardSwitcherFnTest.kt` - Tests FN state management

### Automated Testing
CI workflows run on push to verify:
- Compilation success
- Unit test passage
- APK generation

## Contributing

This feature is designed to be contributed upstream to the HeliBoard project. The implementation:
- Follows HeliBoard coding standards
- Uses existing patterns and utilities (Log utility, showToast, exception handling)
- Maintains backward compatibility
- Includes comprehensive documentation
- Makes the feature optional (users can choose layouts with or without FN key)

## Issue Template

### Title
`[Feature Request] Add FN key support with configurable layers`

### Description

**Is your feature request related to a problem? Please describe.**

Yes, mobile keyboards lack efficient access to navigation and function keys that are essential for terminal emulators, remote desktop applications, and code editors. Currently, users must switch between multiple keyboard layouts or use cumbersome workarounds to access arrow keys, function keys (F1-F12), and navigation controls (Home/End, Page Up/Down).

**Describe the solution you'd like**

Add comprehensive FN key support that:
- Provides a toggle-mode FN key (press to activate/deactivate)
- Enables vim-style navigation (FN+hjkl for arrows)
- Maps number keys to function keys (FN+1-9,0,-,= for F1-F12)
- Includes navigation controls (Home/End, Page Up/Down, word movement)
- Shows visual feedback via toast notifications
- Works with sliding input (like shift sliding)
- Integrates with the FN selector system for JSON layouts

**Use case**

Primary use cases:
1. **Terminal emulators**: Navigate command history, use function keys in vim/emacs
2. **Remote desktop**: Access full keyboard functionality without switching apps
3. **Code editors**: Efficient text navigation and editing
4. **Power users**: Keyboard-driven workflows without lifting fingers

**Describe alternatives you've considered**

- External keyboard: Not portable, requires additional hardware
- Multiple keyboard apps: Context switching disrupts workflow
- Existing terminal keyboards: Limited functionality, poor integration
- On-screen buttons: Take up valuable screen space

## Pull Request Template

### Title
`feat: Add FN key support with configurable layers`

### Description

## Summary

This PR adds comprehensive FN (Function) key support to HeliBoard, enabling power users to access navigation keys, function keys, and special controls without leaving the keyboard.

## Features Added

- ✅ FN key toggle mode (press to activate/deactivate)
- ✅ Vim-style navigation (hjkl → arrow keys)
- ✅ Function keys F1-F12 (number row mapping)
- ✅ Navigation controls (Home/End, Page Up/Down)
- ✅ Word navigation (Ctrl+Left/Right equivalents)
- ✅ Visual feedback via toast notifications
- ✅ Sliding input support (like shift sliding)
- ✅ FN selector system for JSON layouts
- ✅ Example layouts demonstrating usage

## Implementation Details

### Core Changes
1. **KeyboardActionListenerImpl.kt**
   - FN key toggle handler with try-catch protection
   - Key remapping logic for FN+key combinations
   - Toast notifications for state changes
   - Proper logging with HeliBoard's Log utility

2. **KeyboardSwitcher.java**
   - FN state management (`mFnState`, `setFnState()`, `isFnActive()`)
   - Integration with keyboard building
   - Reuses existing `showToast()` method

3. **KeyboardId.java & KeyboardLayoutSet.java**
   - FN state tracking in keyboard identity
   - Builder pattern integration

4. **PointerTracker.java**
   - Enabled FN sliding input (line 736)

5. **KeyData.kt & LayoutParser.kt**
   - FN selector implementation for JSON layouts
   - Dynamic key mapping based on FN state

### Layout Examples
Added example layouts in `app/src/main/assets/layouts/examples/`:
- `qwerty_fn.json` - Standard QWERTY with FN key
- `programmer_fn.json` - Programmer-friendly layout
- `vim_fn.json` - Vim-optimized navigation

## Testing

- ✅ Manual testing on Android 11+ devices
- ✅ Unit tests for FN selector and state management
- ✅ No regression in existing functionality
- ✅ CI build passing

## Compatibility

- Android API 21+ (existing HeliBoard requirement)
- Backward compatible - existing layouts work unchanged
- Optional feature - users choose layouts with/without FN key

## Screenshots

[Would include screenshots of:
1. Keyboard with FN key visible
2. Toast notification showing "FN ON"
3. Example of using FN+hjkl for navigation]

## Checklist

- [x] Code follows HeliBoard style guidelines
- [x] Tests added/updated
- [x] Documentation updated
- [x] No hardcoded strings (uses existing patterns)
- [x] Feature is optional/configurable
- [x] Tested on physical device
- [x] No memory leaks or performance issues

Closes #[issue-number]

## Future Improvements

- Visual indicator for FN state on keyboard
- Customizable FN key mappings via settings
- Long-press FN for sticky mode
- Double-tap FN for caps-lock style toggle
- Settings UI for enabling/disabling FN features
- Per-app FN key behavior customization

## Technical Notes

### Key Code Reference
```kotlin
// Navigation
const val ARROW_LEFT = -21
const val ARROW_RIGHT = -22
const val ARROW_UP = -23
const val ARROW_DOWN = -24

// Function Keys
const val F1 = -10028
const val F2 = -10029
// ... through F12 = -10039

// Special Keys
const val FN = -5
const val ESCAPE = -10017
const val INSERT = -10018
```

### JSON Layout Example
```json
{
  "$": "fn_selector",
  "fn": { "code": -21, "label": "←" },
  "default": { "label": "h" }
}
```

This allows a key to show "h" normally but send arrow left when FN is active.