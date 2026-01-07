# Specification: Palette Manager Service

## Goal
Create a service layer component that manages palette CRUD operations, active palette state with toggle behavior, and observer notifications, following the existing MultiLoader ServiceLoader architecture pattern.

## User Stories
- As a mod developer, I want a centralized service to manage palettes so that all components can share consistent palette state
- As a future persistence layer, I want to receive change notifications so that I can auto-save palette data when modifications occur

## Specific Requirements

**IPaletteManager Interface Definition**
- Define interface in `common/src/main/java/com/autoblockpalette/platform/services/IPaletteManager.java`
- Include state access methods: `getData()`, `getActivePalette()`, `getPalette(UUID)`, `getPaletteByName(String)`
- Include CRUD methods: `createPalette()`, `updatePalette()`, `deletePalette()`, `duplicatePalette()`
- Include active palette methods: `setActivePalette()`, `clearActivePalette()`
- Include organization method: `reorderPalette(UUID, int)`
- Include observer methods: `addChangeListener()`, `removeChangeListener()`
- Include state initialization method: `loadState(PaletteData)` for persistence layer integration

**PaletteChangeListener Functional Interface**
- Define as `@FunctionalInterface` in same package or nested in IPaletteManager
- Single method signature: `void onPaletteDataChanged(PaletteData oldData, PaletteData newData)`
- Listeners are responsible for diffing changes themselves
- No separate event types - single unified change notification

**Default Implementation Class**
- Create `DefaultPaletteManager` in `common/src/main/java/com/autoblockpalette/platform/services/`
- Maintain internal `PaletteData` state initialized to `PaletteData.empty()`
- Use `CopyOnWriteArrayList` for thread-safe listener collection
- Fire change notifications after every state mutation
- All operations return new immutable `PaletteData` instances

**Services Class Integration**
- Add `public static final IPaletteManager PALETTE_MANAGER = load(IPaletteManager.class);` to Services.java
- Create service registration files in each platform's `META-INF/services/` directory
- Service file named `com.autoblockpalette.platform.services.IPaletteManager`
- All platforms point to `com.autoblockpalette.platform.services.DefaultPaletteManager`

**Create Palette Operation**
- Method signature: `PaletteData createPalette(String name, String iconBlock, Map<String, Integer> blocks)`
- Delegate to `Palette.create()` for validation (may throw `IllegalArgumentException`)
- Add new palette to end of list
- Return new `PaletteData` with updated palettes list

**Update Palette Operation**
- Method signature: `Optional<PaletteData> updatePalette(UUID id, String name, String iconBlock, Map<String, Integer> blocks)`
- Return `Optional.empty()` if palette with given ID does not exist
- Create new `Palette` instance with same UUID but updated fields
- Replace palette in list at same index position
- Return `Optional.of(newPaletteData)` on success

**Delete Palette Operation**
- Method signature: `Optional<PaletteData> deletePalette(UUID id)`
- Return `Optional.empty()` if palette with given ID does not exist
- If deleted palette was active, automatically clear `activePaletteId` to null
- Remove palette from list and return new `PaletteData`

**Duplicate Palette Operation**
- Method signature: `Optional<PaletteData> duplicatePalette(UUID id)`
- Return `Optional.empty()` if source palette does not exist
- Create new palette with generated UUID and name suffixed with " (Copy)"
- Copy all blocks and weights from source palette, use same iconBlock
- Insert duplicate at end of palettes list

**Active Palette Toggle Behavior**
- `setActivePalette(UUID id)` implements toggle: if id matches current active, deactivate instead
- Return new `PaletteData` with updated `activePaletteId`
- If palette ID does not exist, return unchanged state (no error)
- `clearActivePalette()` sets `activePaletteId` to null

**Reorder Palette Operation**
- Method signature: `Optional<PaletteData> reorderPalette(UUID id, int newIndex)`
- Return `Optional.empty()` if palette does not exist
- Clamp `newIndex` to valid range [0, palettes.size()-1]
- Remove palette from current position and insert at new position
- Preserve active palette state (same UUID remains active)

## Visual Design
No visual assets provided.

## Existing Code to Leverage

**Services.java ServiceLoader Pattern**
- Use exact same `load(Class<T>)` utility method for loading IPaletteManager
- Follow same `public static final` field pattern for service exposure
- Maintains consistency with existing platform abstraction approach

**IPlatformHelper Interface Pattern**
- Model IPaletteManager interface structure similarly
- Use same package location: `com.autoblockpalette.platform.services`
- Include Javadoc documentation for all methods

**PaletteData Immutable Record**
- Leverage existing `findById()`, `findByName()`, `getActivePalette()` methods for lookups
- Use `PaletteData.empty()` for initial state
- All CRUD operations must construct new `PaletteData` instances with `List.copyOf()`

**Palette.create() Validation**
- Reuse existing validation logic in `Palette.create(UUID, String, String, Map)`
- Allows validation errors to propagate as `IllegalArgumentException` for create operations
- For update operations, use same validation approach

**META-INF/services Registration**
- Create service files in fabric, forge, and neoforge `src/main/resources/META-INF/services/`
- File name: `com.autoblockpalette.platform.services.IPaletteManager`
- Content: fully qualified class name of DefaultPaletteManager implementation

## Out of Scope
- Undo/redo functionality (explicitly excluded per requirements)
- Persistence to disk / file I/O (separate spec - roadmap item 5)
- Weighted random block selection algorithm (separate spec - roadmap item 3)
- GUI screens or visual components (separate specs - roadmap items 6-7)
- Keybind handling and input events (separate spec - roadmap item 8)
- Server-side synchronization or network packets (client-side only mod)
- Palette import/export functionality
- Palette search or filtering capabilities
- Maximum palette count limits or warnings
- Palette name uniqueness enforcement (names are not required to be unique per data model)
