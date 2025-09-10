# HeliBoard FN Key Feature

## Overview
The FN (Function) key feature adds a powerful modifier layer to HeliBoard, enabling access to arrow keys, function keys, and navigation controls without switching keyboard layouts.

## Quick Start

### Installation
1. Install the latest APK from the Download folder
2. Enable HeliBoard in Android Settings → System → Languages & Input
3. Select a layout with FN key support

### Basic Usage
**Toggle Mode:** Press FN to toggle on/off (see "FN ON/OFF" toast)
**Hold Mode:** Hold FN while pressing other keys (not yet implemented)

## Key Mappings

### Navigation (Vim-style)
| FN + | Result | Description |
|------|--------|-------------|
| h | ← | Move left |
| j | ↓ | Move down |
| k | ↑ | Move up |
| l | → | Move right |

### Function Keys
| FN + | Result |
|------|--------|
| 1-9 | F1-F9 |
| 0 | F10 |
| - | F11 |
| = | F12 |

### Extended Navigation
| FN + | Result | Description |
|------|--------|-------------|
| [ | Home | Beginning of line |
| ] | End | End of line |
| ; | Page Up | Scroll up |
| ' | Page Down | Scroll down |
| u | Word Left | Previous word |
| i | Word Right | Next word |

### Special Keys
| FN + | Result |
|------|--------|
| p | Insert |
| \ | Forward Delete |
| e | Escape |

## Terminal Support (Termux)
HeliBoard automatically detects terminal emulators and sends proper ANSI escape sequences instead of Android key events. This ensures arrow keys and function keys work correctly in:
- Termux
- Terminal emulators
- SSH clients
- Any app using TYPE_NULL input

## Visual Feedback
- **Toast notifications** show when FN is toggled ON/OFF
- **Key name toasts** display which special key was activated
- **Debug logging** to `/sdcard/Download/HeliBoard/FN_Debug.log`

## Customization

### Creating Custom Mappings
Edit the key mappings in `KeyboardActionListenerImpl.kt`:

```kotlin
when (primaryCode) {
    'h'.code -> KeyCode.ARROW_LEFT
    // Add your custom mappings here
}
```

### JSON Layout Configuration
FN key layouts are defined in JSON files:
- `functional_keys_with_fn.json` - FN key at top left
- `functional_keys_fn_bottom.json` - FN key in bottom row

## Troubleshooting

### Arrow Keys Not Working in Termux
- Check that you have the latest APK installed
- Verify FN state is ON (toast notification)
- Check debug log for "TerminalInputAdapter" messages

### FN Key Not Appearing
- Select a functional layout in keyboard settings
- Rebuild keyboard if necessary

### Debug Information
Enable debug logging to track FN key behavior:
- Log location: `/sdcard/Download/HeliBoard/FN_Debug.log`
- Shows key remapping, adapter selection, and event handling

## Technical Details

### Architecture
1. **FN State Management** - Tracks toggle state in KeyboardSwitcher
2. **Key Remapping** - Intercepts and remaps keys in KeyboardActionListenerImpl
3. **InputTypeAdapter** - Routes keys based on input field type
4. **Terminal Support** - Sends ANSI escape sequences for TYPE_NULL fields

### Key Code Reference
HeliBoard uses negative integers for special keys:
- Arrow keys: -21 to -24
- Function keys: -10028 to -10039
- Navigation: Various negative codes
- FN key itself: -5

## Known Issues
1. Visual labels don't update when FN is active (UI limitation)
2. Hold-to-activate mode not yet implemented
3. Some apps may not respond to all function keys

## Future Enhancements
- [ ] FN lock (double-tap to lock)
- [ ] Visual indicator for FN state
- [ ] Configurable timeout
- [ ] Multiple FN layers (FN1, FN2)
- [ ] Per-app custom mappings

## Contributing
To contribute to the FN key feature:
1. Test on various devices and apps
2. Report issues with specific app compatibility
3. Suggest new key mappings
4. Help with documentation

## Credits
FN key implementation based on caps-lock-as-function-key concept, adapted for mobile keyboards with terminal emulator support.