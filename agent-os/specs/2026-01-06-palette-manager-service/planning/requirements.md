# Spec Requirements: Palette Manager Service

## Initial Description

Create a service class to manage palette CRUD operations (create, read, update, delete) and handle the currently active palette state. This is a critical service layer component for the Auto Block Palettes Minecraft mod that builds on the existing Palette Data Model (BlockEntry.java, Palette.java, PaletteData.java).

The service must work within the MultiLoader architecture (Fabric/Forge/NeoForge) and integrate with the existing codebase patterns.

## Requirements Discussion

### First Round Questions

**Q1:** I assume the PaletteManager will live entirely in the `common` module since it only operates on the data model (no platform-specific APIs needed). Is that correct, or do you need platform-specific implementations for features like config directory access?
**Answer:** Yes, PaletteManager lives in common module.

**Q2:** I'm thinking we should use a singleton pattern with lazy initialization (similar to how `Services.PLATFORM` works) rather than dependency injection. Should the service be accessed via `PaletteManager.getInstance()`, or would you prefer it exposed through the `Services` class as `Services.PALETTE_MANAGER`?
**Answer:** Use Services pattern (Services.PALETTE_MANAGER) for future platform-specific extensibility - this allows loading different implementations per platform if needed later.

**Q3:** Since the data model uses immutable records (`Palette`, `PaletteData`), I assume CRUD operations should return new instances (functional style) - for example, `createPalette()` returns the new `PaletteData` containing the added palette. Is that correct, or should the service maintain mutable internal state and expose getters?
**Answer:** Immutability whenever possible - functional style returning new instances.

**Q4:** I'm planning for the service to maintain the in-memory `PaletteData` state, and later the Persistence layer (roadmap item 5) will call into this service to load/save. Should the service emit change notifications (observer pattern) when palettes change, so the persistence layer can auto-save, or will persistence be triggered explicitly?
**Answer:** Yes, implement observer pattern for state changes.

**Q5:** When the active palette is deleted, should the service automatically clear the active state (set to null), or should it select another palette (e.g., first in list)?
**Answer:** Clear the active palette (set to null).

**Q6:** Should toggling a palette on/off be a simple activate/deactivate, or do you want a "toggle mode" where activating an already-active palette deactivates it?
**Answer:** Yes, toggle mode - activating an already-active palette deactivates it.

**Q7:** I assume the service should validate palettes before adding (using existing `Palette.create()` validation) and throw `IllegalArgumentException` for invalid operations (like deleting a non-existent palette). Is that correct, or do you prefer returning `Optional`/result objects for error cases?
**Answer:** Prefer Optional values rather than exceptions, whenever possible.

**Q8:** Is there anything specifically **out of scope** for this service? For example: palette ordering/reordering, palette duplication, undo/redo, or limits on number of palettes?
**Answer:** Nothing out of scope - include all mentioned features (ordering, duplication, etc.). Note: This was later clarified to exclude undo/redo.

### Existing Code to Reference

**Similar Features Identified:**
- Existing data model: `common/src/main/java/com/autoblockpalette/data/Palette.java`
- Existing data model: `common/src/main/java/com/autoblockpalette/data/PaletteData.java`
- Existing data model: `common/src/main/java/com/autoblockpalette/data/BlockEntry.java`
- Service loader pattern: `common/src/main/java/com/autoblockpalette/platform/Services.java`
- Platform service interface: `common/src/main/java/com/autoblockpalette/platform/services/IPlatformHelper.java`
- Platform implementation example: `fabric/src/main/java/com/autoblockpalette/platform/FabricPlatformHelper.java`

No additional similar existing features identified by the user.

### Follow-up Questions

**Follow-up 1:** When you said "include all mentioned features" regarding scope, I want to confirm - should the PaletteManager service itself implement undo/redo functionality? This would add significant complexity (command pattern, history stack). Alternatively, we could design the service to be *compatible* with future undo/redo (by emitting events that a separate UndoManager could listen to) without implementing it directly. Which approach do you prefer?
**Answer:** No, do not include undo/redo functionality in this spec. Keep the service focused on CRUD operations.

**Follow-up 2:** For the change notification system, which events should be emitted? I'm thinking separate events (onPaletteCreated, onPaletteUpdated, onPaletteDeleted, onActivePaletteChanged, onPalettesReordered), or would you prefer a single `onPaletteDataChanged(PaletteData oldData, PaletteData newData)` event that listeners can diff themselves?
**Answer:** Option B - Single unified event `onPaletteDataChanged(PaletteData oldData, PaletteData newData)`. Listeners are responsible for diffing the changes themselves.

**Follow-up 3:** When duplicating a palette, should the new palette have a modified name (e.g., "Stone Bricks (Copy)" or "Stone Bricks 2")? And should it be inserted immediately after the original in the list, or at the end?
**Answer:** Add "(Copy)" suffix to the duplicated palette name. Position not specified - use reasonable default (end of list or after original).

## Visual Assets

### Files Provided:
No visual assets provided.

### Visual Insights:
N/A - No visual files were found in the visuals folder.

## Requirements Summary

### Functional Requirements

**Core CRUD Operations:**
- Create new palettes with validation
- Read/retrieve palettes by ID or name
- Update existing palettes (name, icon, blocks, weights)
- Delete palettes by ID
- List all palettes

**Active Palette Management:**
- Set a palette as active by ID
- Clear active palette (deactivate)
- Toggle mode: activating an already-active palette deactivates it
- Get currently active palette
- When active palette is deleted, automatically clear active state (set to null)

**Palette Organization:**
- Reorder palettes (change position in list)
- Duplicate palettes with "(Copy)" suffix appended to name
- No limits on number of palettes

**Observer Pattern:**
- Single unified change event: `onPaletteDataChanged(PaletteData oldData, PaletteData newData)`
- Listeners register/unregister with the service
- Listeners responsible for diffing changes themselves
- Events fired after any state mutation

**State Management:**
- Maintain in-memory PaletteData state
- All operations return new immutable instances (functional style)
- Service provides current state getter for other components

### Architecture Requirements

**Service Pattern:**
- Interface: `IPaletteManager` in `common/src/main/java/com/autoblockpalette/platform/services/`
- Default implementation in common module
- Exposed via `Services.PALETTE_MANAGER` using ServiceLoader pattern
- Allows future platform-specific implementations if needed

**Immutability:**
- All CRUD operations return new `PaletteData` instances
- Never mutate existing Palette or PaletteData objects
- Internal state updated by replacing references, not mutating

**Error Handling:**
- Return `Optional` values instead of throwing exceptions where possible
- Operations on non-existent palettes return `Optional.empty()` or unchanged state
- Validation errors during creation may still throw `IllegalArgumentException` (from Palette.create())

### Reusability Opportunities

- Follows existing `IPlatformHelper` / `Services` pattern exactly
- Builds directly on existing `Palette`, `PaletteData`, `BlockEntry` records
- Observer pattern compatible with future persistence layer (roadmap item 5)
- Observer pattern compatible with future GUI updates (roadmap items 6-7)

### Scope Boundaries

**In Scope:**
- CRUD operations (create, read, update, delete)
- Active palette state management with toggle behavior
- Palette reordering
- Palette duplication with "(Copy)" suffix
- Observer pattern with single unified change event
- Service interface and default implementation
- Integration with Services class

**Out of Scope:**
- Undo/redo functionality (explicitly excluded)
- Persistence to disk (roadmap item 5 - separate spec)
- Weighted random selection logic (roadmap item 3 - separate spec)
- GUI components (roadmap items 6-7 - separate specs)
- Keybind handling (roadmap item 8 - separate spec)

### Technical Considerations

**Integration Points:**
- Will be consumed by future Persistence layer for save/load operations
- Will be consumed by future GUI screens for palette management
- Will be consumed by future block placement interception for getting active palette
- Observer pattern enables loose coupling with these consumers

**Existing System Constraints:**
- Must work in common module (vanilla MC API only)
- Must follow MultiLoader ServiceLoader pattern
- Must maintain compatibility with existing immutable record data model
- Client-side only - no server communication needed

**Technology Patterns to Follow:**
- `Services.java` pattern for service loading
- `IPlatformHelper.java` pattern for interface definition
- `FabricPlatformHelper.java` pattern for implementation structure
- Immutable records pattern from `Palette.java` and `PaletteData.java`

### API Design Notes

**Expected Interface Methods (IPaletteManager):**
```java
// State access
PaletteData getData();
Optional<Palette> getActivePalette();
Optional<Palette> getPalette(UUID id);
Optional<Palette> getPaletteByName(String name);

// CRUD operations (return new state)
PaletteData createPalette(String name, String iconBlock, Map<String, Integer> blocks);
PaletteData updatePalette(UUID id, String name, String iconBlock, Map<String, Integer> blocks);
Optional<PaletteData> deletePalette(UUID id);
PaletteData duplicatePalette(UUID id); // Creates copy with "(Copy)" suffix

// Active palette management
PaletteData setActivePalette(UUID id); // Toggle behavior: if already active, deactivates
PaletteData clearActivePalette();

// Organization
PaletteData reorderPalette(UUID id, int newIndex);

// Observer pattern
void addChangeListener(PaletteChangeListener listener);
void removeChangeListener(PaletteChangeListener listener);

// State initialization (for persistence layer)
void loadState(PaletteData data);
```

**Expected Listener Interface:**
```java
@FunctionalInterface
interface PaletteChangeListener {
    void onPaletteDataChanged(PaletteData oldData, PaletteData newData);
}
```
