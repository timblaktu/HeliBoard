# FN Selector Implementation Design

## Implementation Status (September 9, 2025)

✅ **COMPLETED** - Core FN selector functionality has been successfully implemented, compiles without errors, and has passing tests.

### Milestones Achieved:
1. ✅ **Implementation Complete** - All core components implemented
2. ✅ **Build Successful** - Code compiles and builds APK successfully  
3. ✅ **Tests Passing** - 9/9 unit tests passing for FN functionality

### What Was Implemented:
1. **FnSelector Class** - New selector type in KeyData.kt for JSON-based FN key mappings
2. **KeyboardId Updates** - Added mIsFnActive field to track FN state 
3. **KeyboardSwitcher Updates** - Added FN state management and keyboard refresh on FN toggle
4. **KeyboardLayoutSet Updates** - Added Builder method setFnActive() and Params field mIsFnActive
5. **LayoutParser Updates** - Registered FnSelector for JSON deserialization
6. **KeyboardActionListenerImpl Updates** - Added FN key press/release handling to update selector state

### Implementation Details:
- FN state is now tracked at the keyboard level similar to shift state
- When FN key is pressed/released, the keyboard rebuilds with the new FN state
- FN selector in JSON layouts can define different key behaviors for normal and FN states
- Hardcoded FN remapping has been disabled in favor of the selector system

## Overview

This document describes the design and implementation plan for adding FN key selector support to HeliBoard, making FN a first-class modifier/selector similar to shift state but with simpler binary behavior.

## Design Goals

1. **JSON Configuration**: Enable FN key mappings to be defined entirely in JSON layout files
2. **First-Class Support**: Make FN a proper selector type like `case_selector` and `shift_state_selector`
3. **User Customization**: Allow users to create custom FN layers without recompiling
4. **Composability**: Enable FN selector to work with other selectors for complex behaviors
5. **Visual Feedback**: Support dynamic key label updates when FN is active

## Architecture Design

### 1. FnSelector Class

Location: `app/src/main/java/helium314/keyboard/keyboard/internal/keyboard_parser/floris/KeyData.kt`

```kotlin
/**
 * Allows to select an [AbstractKeyData] based on the current FN key state. 
 * The FN key acts as a modifier similar to shift but with simpler binary behavior.
 * The JSON class identifier for this selector is `fn_selector`.
 *
 * Example usage in a layout JSON file:
 * ```
 * { "$": "fn_selector",
 *   "normal": { "label": "h" },
 *   "fn": { "code": -21, "label": "←" }
 * }
 * ```
 *
 * @property normal The key data to use when FN is not active.
 * @property fn The key data to use when FN is active.
 */
@Serializable
@SerialName("fn_selector")
class FnSelector(
    val normal: AbstractKeyData,
    val fn: AbstractKeyData,
) : AbstractKeyData {
    override fun compute(params: KeyboardParams): KeyData? {
        return (if (params.mId.isFnActive) { fn } else { normal }).compute(params)
    }

    override fun asString(isForDisplay: Boolean): String {
        return if (isForDisplay) {
            // Show both states for display purposes
            "${normal.asString(true)}/${fn.asString(true)}"
        } else {
            ""
        }
    }
}
```

### 2. KeyboardId Modifications

Location: `app/src/main/java/helium314/keyboard/keyboard/KeyboardId.java`

Add FN state tracking:
```java
public final class KeyboardId {
    // ... existing fields ...
    
    public final boolean mIsFnActive;
    
    // Update constructor to include FN state
    public KeyboardId(/* existing params */, boolean isFnActive) {
        // ... existing initialization ...
        mIsFnActive = isFnActive;
    }
    
    // Add getter for Kotlin compatibility
    public boolean isFnActive() {
        return mIsFnActive;
    }
    
    // Update equals() and hashCode() to include mIsFnActive
}
```

### 3. KeyboardParams Modifications

Location: `app/src/main/java/helium314/keyboard/keyboard/internal/KeyboardParams.java`

Ensure FN state is accessible:
```java
public class KeyboardParams {
    // The mId field already contains KeyboardId with FN state
    // No changes needed if KeyboardId is properly updated
}
```

### 4. LayoutParser Registration

Location: `app/src/main/java/helium314/keyboard/keyboard/internal/keyboard_parser/LayoutParser.kt`

Register the new selector:
```kotlin
// In the JSON parser module setup
module {
    // ... existing registrations ...
    polymorphic(AbstractKeyData::class) {
        // ... existing subclasses ...
        subclass(FnSelector::class)
    }
}
```

### 5. Keyboard State Management

#### KeyboardSwitcher Modifications

Location: `app/src/main/java/helium314/keyboard/keyboard/KeyboardSwitcher.java`

Track and propagate FN state:
```java
public class KeyboardSwitcher {
    private boolean mFnState = false;
    
    public void setFnState(boolean fnState) {
        if (mFnState != fnState) {
            mFnState = fnState;
            // Trigger keyboard recreation with new FN state
            setKeyboard(currentKeyboardId.withFnState(fnState));
        }
    }
    
    public boolean isFnActive() {
        return mFnState;
    }
}
```

#### KeyboardActionListenerImpl Updates

Location: `app/src/main/java/helium314/keyboard/keyboard/KeyboardActionListenerImpl.kt`

Update FN key handling to use selector system:
```kotlin
override fun onPressKey(code: Int, /* params */) {
    when (code) {
        KeyCode.FN -> {
            // Update FN state in KeyboardSwitcher
            keyboardSwitcher.setFnState(true)
            // Visual feedback will happen automatically via selector
        }
    }
}

override fun onReleaseKey(code: Int, /* params */) {
    when (code) {
        KeyCode.FN -> {
            keyboardSwitcher.setFnState(false)
        }
    }
}

override fun onCodeInput(primaryCode: Int, /* params */) {
    // Remove hardcoded FN remapping - now handled by selectors
    // Just process the code as received
}
```

## JSON Schema and Examples

### Basic FN Selector

```json
{ "$": "fn_selector",
  "normal": { "label": "h" },
  "fn": { "code": -21, "label": "←" }
}
```

### Complete Vim Navigation Row

```json
[
  { "$": "fn_selector",
    "normal": { "label": "h" },
    "fn": { "code": -21, "label": "←" }
  },
  { "$": "fn_selector",
    "normal": { "label": "j" },
    "fn": { "code": -24, "label": "↓" }
  },
  { "$": "fn_selector",
    "normal": { "label": "k" },
    "fn": { "code": -23, "label": "↑" }
  },
  { "$": "fn_selector",
    "normal": { "label": "l" },
    "fn": { "code": -22, "label": "→" }
  }
]
```

### Function Keys Row

```json
[
  { "$": "fn_selector",
    "normal": { "label": "1" },
    "fn": { "code": -10028, "label": "F1" }
  },
  { "$": "fn_selector",
    "normal": { "label": "2" },
    "fn": { "code": -10029, "label": "F2" }
  },
  { "$": "fn_selector",
    "normal": { "label": "3" },
    "fn": { "code": -10030, "label": "F3" }
  }
  // ... continue for F4-F12
]
```

### Nested with Shift Selector

```json
{ "$": "fn_selector",
  "normal": { 
    "$": "shift_state_selector",
    "unshifted": { "label": ";" },
    "shifted": { "label": ":" }
  },
  "fn": { "code": -10010, "label": "PgUp" }
}
```

### Nested with Variation Selector

```json
{ "$": "fn_selector",
  "normal": {
    "$": "variation_selector",
    "default": { "label": "." },
    "email": { "label": "@" },
    "uri": { "label": "/" }
  },
  "fn": { "code": -10016, "label": "End" }
}
```

## Implementation Steps

1. **Phase 1: Core Infrastructure**
   - [x] Create feature branch `fn-selector`
   - [x] Add `mIsFnActive` field to KeyboardId
   - [x] Implement FnSelector class in KeyData.kt
   - [x] Register FnSelector in LayoutParser

2. **Phase 2: State Management**
   - [x] Update KeyboardSwitcher to track FN state
   - [x] Modify keyboard creation pipeline to pass FN state
   - [x] Update KeyboardActionListenerImpl to set FN state (not remap)
   - [x] Ensure keyboard refreshes when FN state changes

3. **Phase 3: Testing** ✅ **COMPLETED**
   - [x] Created unit tests for FN selector functionality
   - [x] Tested basic FN selector state switching
   - [x] Tested KeyboardSwitcher FN state management
   - [x] Verified selector display string generation
   - [x] All 9 tests passing successfully

4. **Phase 4: JSON Layout Creation** (NEXT STEP)
   - [ ] Create test JSON layouts with FN selectors
   - [ ] Convert hardcoded mappings to JSON layouts
   - [ ] Create default FN-enabled layouts (qwerty_fn.json, etc.)
   - [ ] Test JSON parsing and loading
   - [ ] Document usage for users

5. **Phase 5: Integration Testing**
   - [ ] Test visual feedback (label changes)
   - [ ] Test sliding input from FN key
   - [ ] Test with different keyboard layouts
   - [ ] Test performance impact
   - [ ] Test on actual device

## Key Code Reference

Standard HeliBoard key codes that can be used in FN mappings:

### Navigation Keys
- Arrow Left: `-21`
- Arrow Right: `-22`
- Arrow Up: `-23`
- Arrow Down: `-24`
- Home: `-10015`
- End: `-10016`
- Page Up: `-10010`
- Page Down: `-10011`

### Word Movement
- Word Left: `-10020`
- Word Right: `-10021`

### Function Keys
- F1: `-10028`
- F2: `-10029`
- F3: `-10030`
- F4: `-10031`
- F5: `-10032`
- F6: `-10033`
- F7: `-10034`
- F8: `-10035`
- F9: `-10036`
- F10: `-10037`
- F11: `-10038`
- F12: `-10039`

### Edit Keys
- Delete (forward): `-10009`
- Insert: `-10013`
- Escape: `-10014`
- Tab: `-10008`

### Modifiers
- FN: `-5`
- Shift: `-1`
- Ctrl: `-6`
- Alt: `-7`

## Benefits Over Hardcoded Implementation

1. **User Customization**: Users can create their own FN mappings without recompiling
2. **Layout Flexibility**: Different layouts can have different FN behaviors
3. **Maintainability**: Changes don't require code modifications
4. **Composability**: Can combine with other selectors for complex behaviors
5. **Visual Feedback**: Keys can show different labels when FN is active
6. **Standard Pattern**: Follows existing HeliBoard selector patterns

## Migration Path

1. Keep `fn-hard` branch as reference implementation
2. Implement FN selector on `fn-selector` branch
3. Test thoroughly with sample layouts
4. Create migration guide for users
5. Consider making FN selector the default in future releases

## Testing Checklist

### Unit Tests ✅ COMPLETED
- [x] FN key press/release updates keyboard state
- [x] Keys with fn_selector show correct output  
- [x] FN selector display strings work correctly
- [x] KeyboardSwitcher FN state management works
- [x] Edge cases handled (null keys, empty labels)

### Integration Tests (TODO - Phase 4)
- [ ] Visual labels update when FN is pressed
- [ ] Sliding from FN key works correctly
- [ ] Nested selectors work with FN
- [ ] No performance regression
- [ ] Backwards compatibility maintained
- [ ] JSON validation catches malformed fn_selectors

## Current Limitations & Known Issues

1. **Visual Feedback**: Key labels don't update visually yet when FN is pressed (requires UI integration)
2. **Sliding Input**: FN sliding (hold FN + tap) not yet tested on device
3. **Performance**: Keyboard rebuild on FN toggle may have performance impact (needs measurement)
4. **JSON Layouts**: No production JSON layouts created yet with fn_selector

## Future Enhancements

1. **FN Lock**: Double-tap FN to lock (similar to caps lock)
2. **FN Indicators**: Visual indicator when FN is active
3. **Per-Layout FN**: Different FN behaviors per language/layout
4. **FN Gestures**: Swipe gestures on FN key for special functions
5. **Multi-FN**: Support for FN1, FN2 layers (like gaming keyboards)
6. **FN Timeout**: Auto-release FN after timeout with no input
7. **FN Combinations**: Support for FN+Shift, FN+Ctrl combinations

## Conclusion

The FN selector implementation makes HeliBoard's FN key a proper first-class citizen in the layout system, enabling full customization through JSON configuration without requiring code changes or recompilation. This aligns with HeliBoard's philosophy of user customization and flexibility.