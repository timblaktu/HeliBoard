# FN Key Feature Documentation

## Overview

This PR introduces comprehensive FN key support to HeliBoard, enabling power users to access navigation keys, function keys, and special controls without leaving the main keyboard layout. The implementation follows HeliBoard's existing patterns and provides both hardcoded and JSON-configurable approaches.

## Features

### Core FN Key Functionality
- **Toggle Mode**: Press FN to enable/disable the function layer (like Caps Lock)
- **Key Remapping**: When FN is active, regular keys are remapped to function/navigation keys
- **Visual Feedback**: Keyboard rebuilds to show updated key labels when FN is toggled
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

## Implementation Details

### Architecture Changes

1. **FN State Tracking** (`KeyboardId.java`)
   - Added `mIsFnActive` field to track FN state
   - State persists across keyboard rebuilds
   - Integrated with keyboard identity system

2. **Key Remapping** (`KeyboardActionListenerImpl.kt`)
   - Intercepts key codes when FN is active
   - Remaps to appropriate function/navigation codes
   - Handles both lowercase and uppercase variants

3. **FN Selector** (`KeyData.kt`)
   - New selector type for JSON layouts
   - Allows per-key FN layer definitions
   - Composable with other selectors

4. **Layout Parser** (`LayoutParser.kt`)
   - Registered `fn_selector` for JSON deserialization
   - Supports nested selector configurations

### JSON Layout Support

The FN selector enables JSON-configurable FN layers:

```json
{
  "$": "fn_selector",
  "normal": { "label": "h" },
  "fn": { "code": -21, "label": "←" }
}
```

The FN key itself is defined as:
```json
{ "code": -5, "label": "FN" }
```

### Example Layouts

Three example layouts are provided in `app/src/main/assets/layouts/examples/`:
- `qwerty_fn.json` - Standard QWERTY with FN layer
- `programmer_fn.json` - Programming-optimized FN mappings
- `vim_fn.json` - Vim navigation focused layout

Users can import these through the "No language" layout option in settings.

## Terminal Emulator Support

A separate `InputTypeAdapter` system handles terminal emulators that expect ANSI escape sequences instead of Android key events. This ensures FN key combinations work correctly in apps like Termux.

## Testing

### Unit Tests
- `FnSelectorBasicTest.kt` - Tests FN selector functionality
- `KeyboardSwitcherFnTest.kt` - Tests FN state management
- All tests passing

### Manual Testing
- Tested on Android 14/15 devices
- Verified in standard text fields and terminal emulators
- Confirmed visual feedback and state persistence

## Compatibility

- **Min SDK**: 21 (unchanged)
- **Target SDK**: 35 (unchanged)
- **Backward Compatible**: Yes, FN feature is optional
- **Side-by-side Installation**: Works with existing HeliBoard installations

## User Guide

### Enabling FN Key

1. Open HeliBoard Settings
2. Go to Languages & Layouts
3. Add a new layout with "No language"
4. Import an example FN layout from the examples folder
5. Or create your own layout with FN key support

### Using FN Key

1. Press FN to toggle the function layer on/off
2. When FN is active, key labels update to show remapped functions
3. Press mapped keys to send function/navigation commands
4. Press FN again to return to normal typing

### Creating Custom FN Mappings

Users can create custom FN mappings by:
1. Copying an example layout
2. Modifying the `fn_selector` definitions
3. Importing the custom layout

## Performance Considerations

- **Keyboard Rebuild**: FN toggle triggers keyboard rebuild for visual updates
- **Memory Impact**: Minimal - only boolean state tracking
- **CPU Impact**: Negligible - simple key code remapping
- **APK Size**: ~10KB increase for FN functionality

## Future Enhancements

Potential improvements not included in this PR:
- Hold mode (hold FN while pressing other keys)
- FN lock (double-tap to lock)
- Multiple FN layers (FN1, FN2, etc.)
- Per-app FN configurations

## Code Quality

- Follows HeliBoard's existing patterns
- No external dependencies added
- Comprehensive error handling
- Self-documenting code with clear comments
- Removed debug logging for production