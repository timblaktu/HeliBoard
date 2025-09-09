# HeliBoard FN Selector - Complete Implementation Guide

## Overview
The FN selector feature enables JSON-configurable function key layers in HeliBoard, allowing users to access extended functionality (arrows, function keys, navigation) through an FN modifier key without recompiling the app.

## Implementation Status: ✅ COMPLETE (September 9, 2025)
- Core implementation: **Done**
- Unit tests: **9/9 passing**
- Production layouts: **3 created**
- Build: **Successful**
- APK: **Available in Download folder**

## How to Use FN Selector in HeliBoard

### Installation

#### If you have F-Droid HeliBoard installed:
**Good news!** The debug APK uses a different package name, so both can coexist:
- F-Droid: `helium314.keyboard` 
- Debug: `helium314.keyboard.debug`

1. **Keep F-Droid version installed**
2. **Install debug APK** from Download folder (`HeliBoard_FN_3.3-debug.apk`)
3. **Enable HeliBoard Debug**: Settings → System → Languages & Input → Virtual Keyboard → Enable "HeliBoard Debug"

### Configuring FN Layouts in HeliBoard UI

1. **Open HeliBoard Debug Settings**
   - Long press comma key → Settings icon
   - Or: Settings app → System → Languages & Input → Virtual Keyboard → HeliBoard Debug

2. **Navigate to Languages & Layouts**
   - In HeliBoard settings, tap "Languages & Layouts"
   - You'll see a list of available languages with toggle switches

3. **Select English (US) Layout Options**
   - Tap on "English (US)" (not the toggle switch)
   - This opens a submenu showing all available layout variants
   - The FN layouts appear with their JSON filenames:
     - **qwerty** (standard)
     - **qwerty_fn** - Standard layout with vim navigation and FN key
     - **programmer_fn** - Developer-focused with symbols on FN layer
     - **vim_fn** - Vim-optimized navigation with FN key
     - QWERTZ, AZERTY, Dvorak, Colemak, etc.

4. **Choose Your FN Layout**
   - Tap the pencil icon next to your preferred FN layout
   - The layout will be selected for English (US)

5. **Enable the Language**
   - Back in Languages & Layouts screen
   - Toggle the switch next to "English (US)" to enable it

6. **Start Using FN Key**
   - Switch to HeliBoard Debug when typing
   - FN key appears (location varies by layout)
   - Hold FN and press other keys for special functions

### Using the FN Key

#### Three Ways to Use FN:
1. **Tap FN then key**: Temporarily activates FN for next keypress
2. **Hold FN + tap keys**: Keep FN active while held
3. **Slide from FN**: Touch FN and slide to target key

#### Key Mappings by Layout

##### QWERTY + FN
- **Navigation**: FN + hjkl → Arrow keys (←↓↑→)
- **Function Keys**: FN + 1234567890-= → F1-F12
- **Word Jump**: FN + u/i → Word left/right
- **Page**: FN + ;/' → Page Up/Down
- **Document**: FN + [/] → Home/End
- **Special**: FN + w → Escape, FN + e → Tab, FN + p → Insert

##### Programmer + FN  
- **Symbols**: FN + qwertyuiop → `~!@#$%^&*
- **Brackets**: FN + asdf → []{}
- **Operators**: FN + zxcvbnm → <>=+-/\|
- **Navigation**: FN + hjkl → Arrows (preserved)
- **Parentheses**: FN + g → (

##### Vim + FN
- **Movement**: FN + hjkl → Arrows, FN + w/b → Word forward/back
- **Line**: FN + 0 → Line start, FN + $ → Line end
- **Document**: FN + g → Top, FN + G → Bottom
- **Page**: FN + u/d → Page Up/Down
- **Edit**: FN + x → Delete, FN + i → Insert
- **Escape**: FN + symbol key → ESC

## Technical Implementation

### Core Components

#### 1. FnSelector Class (`KeyData.kt`)
```kotlin
class FnSelector(
    val normal: AbstractKeyData,
    val fn: AbstractKeyData
) : AbstractKeyData {
    override fun compute(params: KeyboardParams): KeyData? {
        return (if (params.mId.isFnActive) fn else normal).compute(params)
    }
}
```

#### 2. State Management
- **KeyboardId**: Tracks FN state via `mIsFnActive` field
- **KeyboardSwitcher**: Manages FN toggle and keyboard rebuild
- **KeyboardLayoutSet**: Passes FN state through builder pattern

#### 3. JSON Format
```json
{
  "$": "fn_selector",
  "normal": { "label": "h" },
  "fn": { "code": -21, "label": "←" }
}
```

#### 4. Nested Selectors
FN selector can contain other selectors:
```json
{
  "$": "fn_selector",
  "normal": { 
    "$": "case_selector",
    "lower": { "label": "h" },
    "upper": { "label": "H" }
  },
  "fn": { "code": -21, "label": "←" }
}
```

### Key Code Reference
- **Arrows**: LEFT=-21, RIGHT=-22, UP=-23, DOWN=-24
- **Functions**: F1=-10028 to F12=-10039
- **Navigation**: HOME=-10015, END=-10016, PAGE_UP=-10010, PAGE_DOWN=-10011
- **Word**: WORD_LEFT=-10020, WORD_RIGHT=-10021
- **Special**: TAB=-10008, ESC=-10014, INSERT=-10013, DELETE=-10009
- **Modifiers**: FN=-5, SHIFT=-1, CTRL=-6, ALT=-7

## Creating Custom FN Layouts

### Step 1: Create Main Layout
Create in `app/src/main/assets/layouts/main/custom_fn.json`:
```json
[
  [
    {
      "$": "fn_selector",
      "normal": { "label": "q" },
      "fn": { "label": "custom_action" }
    }
    // ... more keys
  ]
]
```

### Step 2: Create Functional Layout
Create in `app/src/main/assets/layouts/functional/functional_keys_custom_fn.json`:
```json
[
  [
    { "code": -5, "label": "FN", "width": 0.15 },
    { "type": "placeholder" },
    { "label": "delete" }
  ]
  // ... more rows
]
```

### Step 3: Register in method.xml
Add to `app/src/main/res/xml/method.xml`:
```xml
<subtype android:icon="@drawable/ic_ime_switcher"
    android:label="Custom + FN"
    android:subtypeId="0xf0000004"
    android:imeSubtypeLocale="en_US"
    android:imeSubtypeExtraValue="KeyboardLayoutSet=MAIN:custom_fn+FUNCTIONAL:functional_keys_custom_fn,AsciiCapable,EmojiCapable"
/>
```

### Step 4: Build and Install
```bash
./gradlew assembleDebug
cp app/build/outputs/apk/debug/*.apk ~/storage/shared/Download/
```

## Test Results
- **Unit Tests**: 9/9 passing
- **Build**: Successful, APK generated
- **Layouts**: 3 production layouts created and registered

## Known Limitations
1. **Visual Feedback**: Labels don't update visually when FN pressed (requires UI integration)
2. **Performance**: Keyboard rebuilds on each FN toggle
3. **Discovery**: Users need documentation to know mappings

## Future Enhancements
1. **FN Lock**: Double-tap to lock FN state
2. **Visual Indicators**: Show FN state in UI
3. **Dynamic Labels**: Update key labels when FN active
4. **More Layouts**: Colemak+FN, Dvorak+FN, etc.

## Troubleshooting

### FN Key Not Appearing
- Make sure you selected an FN-enabled layout (ends with "+FN")
- Check Languages & Layouts settings

### FN Combinations Not Working
- Verify you're holding FN while pressing other keys
- Try sliding from FN to target key
- Check if the layout supports the combination

### Can't Install APK
- Enable "Install unknown apps" for your file manager
- If you have F-Droid version, both can coexist (different packages)

## Files Modified in Implementation

### Core Implementation
- `KeyboardId.java` - Added FN state tracking
- `KeyboardSwitcher.java` - FN state management
- `KeyboardLayoutSet.java` - FN state in builder
- `KeyData.kt` - FnSelector class
- `LayoutParser.kt` - FN selector registration
- `KeyboardActionListenerImpl.kt` - FN key handling

### Layouts Created
- `qwerty_fn.json`, `programmer_fn.json`, `vim_fn.json` (main)
- `functional_keys_qwerty_fn.json`, etc. (functional)
- `method.xml` - Layout registration

### Tests Added
- `FnSelectorBasicTest.kt` - Core logic tests
- `KeyboardSwitcherFnTest.kt` - State management tests

## Summary
The FN selector system is fully implemented, tested, and ready for use. It provides a clean, JSON-configurable way to add function key layers to any HeliBoard layout, following the existing selector pattern for consistency and maintainability.