# FN Key Layout Examples

This directory contains example layouts demonstrating the FN key functionality. These layouts are not included in the default layout list but can be imported by users who want to use the FN key feature.

## How to Use These Layouts

1. Open HeliBoard settings
2. Go to Languages & Layouts
3. Add a new layout with "No language" selected
4. Choose "Load from file" 
5. Navigate to these example files and import them

## Available Example Layouts

### qwerty_fn.json
Standard QWERTY layout with FN key support. Includes:
- Vim-style navigation (FN+hjkl for arrows)
- Function keys F1-F12 (FN+1-0,-,=)
- Navigation keys (Home/End, Page Up/Down)

### programmer_fn.json
Programming-optimized layout with FN key for:
- Quick access to brackets and symbols
- Function keys for IDE shortcuts
- Enhanced navigation for code editing

### vim_fn.json
Vim-user optimized layout featuring:
- Full vim navigation keys
- Word movement (FN+w,b,e)
- Line operations (FN+0,$)
- Escape key access (FN+q)

## Creating Your Own FN Layout

These examples use the `fn_selector` feature to define keys that change when FN is active:

```json
{
  "$": "fn_selector",
  "normal": { "label": "h" },
  "fn": { "code": -21, "label": "←" }
}
```

The FN key itself is defined with code `-5`:
```json
{ "code": -5, "label": "FN" }
```

## Key Code Reference

When creating your own mappings, use these codes:
- Arrow keys: LEFT=-21, UP=-23, RIGHT=-22, DOWN=-24
- Function keys: F1=-10028 through F12=-10039
- Navigation: HOME=-27, END=-28, PAGE_UP=-10010, PAGE_DOWN=-10011
- Word movement: WORD_LEFT=-10015, WORD_RIGHT=-10016
- Special: INSERT=-10018, ESCAPE=-10017

## Note

These are example layouts to demonstrate the FN key functionality. Users should customize them according to their needs and preferences.