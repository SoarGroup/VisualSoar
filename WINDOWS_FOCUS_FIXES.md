# Windows Focus Issues - Fixed

## Problem Description
Users on Windows were reporting issues with dialog boxes and windows not getting proper focus when they open. This was causing problems such as:

1. Dialog boxes appearing but not being active/focused
2. Typing not working in dialog text fields immediately
3. Keyboard navigation not working properly
4. Inconsistent focus behavior across different dialogs

## Root Causes Identified

### 1. Inconsistent Event Handling Patterns
- Some dialogs used `windowOpened` events for focus management
- Others used `windowActivated` events
- Mixed approaches created unpredictable behavior, especially on Windows

### 2. Improper Focus Methods
- Most dialogs used `requestFocus()` instead of `requestFocusInWindow()`
- `requestFocus()` can fail on Windows due to focus stealing prevention policies
- `requestFocusInWindow()` is more reliable across platforms

### 3. Race Conditions
- Dialog positioning (`setLocationRelativeTo()`) and focus requests happened simultaneously
- Windows requires proper sequencing of these operations

### 4. Missing `toFront()` Calls
- Dialogs weren't explicitly brought to front on Windows
- Internal frames had similar issues

## Solutions Implemented

### 1. Enhanced DialogUtils Class
**File: `DialogUtils.java`**

- **Improved `closeOnEscapeKey()` method:**
  - Added `toFront()` call for better Windows compatibility
  - Changed from `requestFocus()` to `requestFocusInWindow()`
  - Added `SwingUtilities.invokeLater()` for proper sequencing

- **New `closeOnEscapeKeyWithFocus()` method:**
  - Allows specifying a component to receive focus
  - Handles focus management centrally
  - Uses Windows-compatible focus techniques

### 2. Updated Dialog Classes
**Files: `NameDialog.java`, `SaveProjectAsDialog.java`, `NewAgentDialog.java`, `FindDialog.java`, `ReplaceInProjectDialog.java`**

- **Removed inconsistent focus listeners:**
  - Eliminated custom `windowActivated` and `windowOpened` handlers
  - Centralized focus management through `DialogUtils`

- **Simplified focus handling:**
  - Use `DialogUtils.closeOnEscapeKeyWithFocus()` consistently
  - Pass the specific component that should receive focus

### 3. Updated Panel Components
**Files: `NamePanel.java`, `AgentNamePanel.java`, `RangePanel.java`, `EnumPanel.java`, `FindPanel.java`**

- **Changed focus methods:**
  - Replaced `requestFocus()` with `requestFocusInWindow()`
  - More reliable on Windows platforms

### 4. Enhanced MainFrame Internal Frame Handling
**File: `MainFrame.java`**

- **Updated `WindowMenuListener.actionPerformed()`:**
  - Added `SwingUtilities.invokeLater()` for proper sequencing
  - Added `requestFocusInWindow()` call for internal frames

- **Enhanced `selectNewInternalFrame()` method:**
  - Added `SwingUtilities.invokeLater()` wrapper
  - Improved focus handling for internal frames

## Key Improvements

### 1. Platform Consistency
- Unified focus management approach across all dialogs
- Windows-specific focus handling patterns implemented
- Proper event sequencing for cross-platform compatibility

### 2. Better User Experience
- Dialogs now properly receive focus when opened
- Text fields are immediately ready for typing
- Keyboard navigation works consistently
- ESC key handling is more reliable

### 3. Reduced Code Duplication
- Centralized focus management in `DialogUtils`
- Consistent patterns across all dialog classes
- Easier maintenance and debugging

### 4. Improved Reliability
- Use of `requestFocusInWindow()` instead of `requestFocus()`
- Proper use of `SwingUtilities.invokeLater()` for event sequencing
- Added `toFront()` calls where needed

## Windows-Specific Considerations

1. **Focus Stealing Prevention:**
   - Windows has aggressive focus stealing prevention
   - `requestFocusInWindow()` respects these policies better than `requestFocus()`

2. **Event Timing:**
   - Windows requires proper sequencing of positioning and focus operations
   - `SwingUtilities.invokeLater()` ensures operations happen in correct order

3. **Z-Order Management:**
   - Explicit `toFront()` calls ensure dialogs appear above other windows
   - Important for proper focus behavior on Windows

## Testing Recommendations

### Manual Testing
1. **Dialog Focus:**
   - Open various dialogs (New Agent, Save As, Find, etc.)
   - Verify text fields are immediately ready for typing
   - Test that cursor appears in the correct field

2. **Keyboard Navigation:**
   - Test ESC key to close dialogs
   - Test Tab navigation between fields
   - Test Enter key for default actions

3. **Window Management:**
   - Test with multiple internal frames open
   - Verify proper focus when switching between frames
   - Test minimizing/restoring windows

### Platform Testing
- Test specifically on Windows 10/11
- Test with different window managers
- Test with multiple monitors
- Test with accessibility software

## Future Considerations

1. **Additional Improvements:**
   - Consider implementing platform-specific focus delays if needed
   - Monitor for any remaining focus issues in complex dialogs
   - Consider adding focus indicators for better accessibility

2. **Monitoring:**
   - Watch for user feedback on focus behavior
   - Consider adding telemetry for focus-related issues
   - Monitor performance impact of changes

## Files Modified

1. `DialogUtils.java` - Enhanced focus management utilities
2. `NameDialog.java` - Updated focus handling
3. `SaveProjectAsDialog.java` - Updated focus handling
4. `NewAgentDialog.java` - Updated focus handling
5. `FindDialog.java` - Updated focus handling
6. `ReplaceInProjectDialog.java` - Updated focus handling
7. `NamePanel.java` - Updated focus method
8. `AgentNamePanel.java` - Updated focus method
9. `RangePanel.java` - Updated focus method
10. `EnumPanel.java` - Updated focus method
11. `FindPanel.java` - Updated focus method
12. `MainFrame.java` - Enhanced internal frame focus handling

These changes should significantly improve the focus behavior for Windows users while maintaining compatibility with other platforms.
