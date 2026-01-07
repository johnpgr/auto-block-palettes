# Task Breakdown: Palette Manager Service

## Overview
Total Tasks: 21

This spec implements a service layer component that manages palette CRUD operations, active palette state with toggle behavior, and observer notifications. The implementation follows the existing MultiLoader ServiceLoader architecture pattern established in `Services.java`.

## Task List

### Service Layer

#### Task Group 1: Interface and Listener Definitions
**Dependencies:** None

- [ ] 1.0 Complete service interface definitions
  - [ ] 1.1 Write 4-6 focused tests for IPaletteManager interface contract
    - Test that service can be loaded via Services.PALETTE_MANAGER
    - Test getData() returns PaletteData instance
    - Test listener registration and removal
    - Test listener notification on state change
  - [ ] 1.2 Create PaletteChangeListener functional interface
    - Location: `common/src/main/java/com/autoblockpalette/platform/services/PaletteChangeListener.java`
    - Add `@FunctionalInterface` annotation
    - Single method: `void onPaletteDataChanged(PaletteData oldData, PaletteData newData)`
    - Include Javadoc documentation
  - [ ] 1.3 Create IPaletteManager interface
    - Location: `common/src/main/java/com/autoblockpalette/platform/services/IPaletteManager.java`
    - Follow pattern from `IPlatformHelper.java`
    - State access methods: `getData()`, `getActivePalette()`, `getPalette(UUID)`, `getPaletteByName(String)`
    - CRUD methods: `createPalette()`, `updatePalette()`, `deletePalette()`, `duplicatePalette()`
    - Active palette methods: `setActivePalette()`, `clearActivePalette()`
    - Organization method: `reorderPalette(UUID, int)`
    - Observer methods: `addChangeListener()`, `removeChangeListener()`
    - State initialization: `loadState(PaletteData)`
    - Include comprehensive Javadoc for all methods
  - [ ] 1.4 Ensure interface definition tests pass
    - Run ONLY the tests written in 1.1
    - Verify interfaces compile correctly

**Acceptance Criteria:**
- The 4-6 tests written in 1.1 pass
- IPaletteManager interface defines all required methods per spec
- PaletteChangeListener is properly annotated as @FunctionalInterface
- All methods have Javadoc documentation
- Interface follows existing IPlatformHelper pattern

---

#### Task Group 2: Default Implementation - Core State Management
**Dependencies:** Task Group 1

- [ ] 2.0 Complete core implementation with state management
  - [ ] 2.1 Write 4-6 focused tests for core state operations
    - Test initial state is PaletteData.empty()
    - Test loadState() replaces internal state
    - Test getData() returns current state
    - Test state changes fire listener notifications with old and new data
    - Test multiple listeners all receive notifications
  - [ ] 2.2 Create DefaultPaletteManager class skeleton
    - Location: `common/src/main/java/com/autoblockpalette/platform/services/DefaultPaletteManager.java`
    - Implement IPaletteManager interface
    - Initialize internal state to `PaletteData.empty()`
    - Use `CopyOnWriteArrayList` for thread-safe listener collection
  - [ ] 2.3 Implement observer pattern methods
    - `addChangeListener(PaletteChangeListener)` - add to listener list
    - `removeChangeListener(PaletteChangeListener)` - remove from listener list
    - Private `notifyListeners(PaletteData oldData, PaletteData newData)` method
  - [ ] 2.4 Implement state access methods
    - `getData()` - return current PaletteData
    - `getActivePalette()` - delegate to PaletteData.getActivePalette()
    - `getPalette(UUID)` - delegate to PaletteData.findById()
    - `getPaletteByName(String)` - delegate to PaletteData.findByName()
    - `loadState(PaletteData)` - replace internal state and notify listeners
  - [ ] 2.5 Ensure core state management tests pass
    - Run ONLY the tests written in 2.1
    - Verify state initialization works correctly
    - Verify observer notifications fire correctly

**Acceptance Criteria:**
- The 4-6 tests written in 2.1 pass
- DefaultPaletteManager initializes with empty state
- Observer pattern correctly notifies all registered listeners
- State access methods delegate properly to PaletteData

---

#### Task Group 3: CRUD Operations Implementation
**Dependencies:** Task Group 2

- [ ] 3.0 Complete CRUD operations
  - [ ] 3.1 Write 6-8 focused tests for CRUD operations
    - Test createPalette() adds palette to list and returns new state
    - Test createPalette() validation errors propagate as IllegalArgumentException
    - Test updatePalette() returns Optional.of(newState) on success
    - Test updatePalette() returns Optional.empty() for non-existent ID
    - Test deletePalette() removes palette and returns Optional.of(newState)
    - Test deletePalette() returns Optional.empty() for non-existent ID
    - Test deletePalette() clears activePaletteId when deleting active palette
    - Test duplicatePalette() creates copy with "(Copy)" suffix
  - [ ] 3.2 Implement createPalette operation
    - Signature: `PaletteData createPalette(String name, String iconBlock, Map<String, Integer> blocks)`
    - Delegate to `Palette.create()` for validation
    - Add new palette to end of list
    - Construct new PaletteData with `List.copyOf()`
    - Fire change notification
    - Return new PaletteData
  - [ ] 3.3 Implement updatePalette operation
    - Signature: `Optional<PaletteData> updatePalette(UUID id, String name, String iconBlock, Map<String, Integer> blocks)`
    - Return `Optional.empty()` if palette does not exist
    - Create new Palette with same UUID but updated fields via `Palette.create(UUID, ...)`
    - Replace palette in list at same index position
    - Fire change notification
    - Return `Optional.of(newPaletteData)`
  - [ ] 3.4 Implement deletePalette operation
    - Signature: `Optional<PaletteData> deletePalette(UUID id)`
    - Return `Optional.empty()` if palette does not exist
    - If deleted palette was active, set activePaletteId to null
    - Remove palette from list
    - Construct new PaletteData
    - Fire change notification
    - Return `Optional.of(newPaletteData)`
  - [ ] 3.5 Implement duplicatePalette operation
    - Signature: `Optional<PaletteData> duplicatePalette(UUID id)`
    - Return `Optional.empty()` if source palette does not exist
    - Create new palette with generated UUID
    - Name: original name + " (Copy)"
    - Copy iconBlock and blocks from source
    - Insert at end of palettes list
    - Fire change notification
    - Return `Optional.of(newPaletteData)`
  - [ ] 3.6 Ensure CRUD operation tests pass
    - Run ONLY the tests written in 3.1
    - Verify all CRUD operations work correctly
    - Verify notifications fire on each operation

**Acceptance Criteria:**
- The 6-8 tests written in 3.1 pass
- Create operation validates and adds palettes correctly
- Update operation replaces palettes at same index
- Delete operation removes palettes and clears active if needed
- Duplicate operation creates proper copy with "(Copy)" suffix
- All operations return new immutable PaletteData instances

---

#### Task Group 4: Active Palette and Reorder Operations
**Dependencies:** Task Group 3

- [ ] 4.0 Complete active palette and reorder operations
  - [ ] 4.1 Write 4-6 focused tests for active palette and reorder
    - Test setActivePalette() activates palette when not already active
    - Test setActivePalette() toggle behavior - deactivates when already active
    - Test setActivePalette() returns unchanged state for non-existent ID
    - Test clearActivePalette() sets activePaletteId to null
    - Test reorderPalette() moves palette to new position
    - Test reorderPalette() clamps index to valid range
  - [ ] 4.2 Implement setActivePalette with toggle behavior
    - Signature: `PaletteData setActivePalette(UUID id)`
    - If palette ID does not exist, return unchanged state (no notification)
    - If id matches current activePaletteId, deactivate (set to null)
    - Otherwise, set activePaletteId to id
    - Fire change notification only if state changed
    - Return new PaletteData
  - [ ] 4.3 Implement clearActivePalette
    - Signature: `PaletteData clearActivePalette()`
    - Set activePaletteId to null
    - Fire change notification if state changed
    - Return new PaletteData
  - [ ] 4.4 Implement reorderPalette operation
    - Signature: `Optional<PaletteData> reorderPalette(UUID id, int newIndex)`
    - Return `Optional.empty()` if palette does not exist
    - Clamp newIndex to valid range [0, palettes.size()-1]
    - Remove palette from current position
    - Insert at new position
    - Preserve activePaletteId (same UUID remains active)
    - Fire change notification
    - Return `Optional.of(newPaletteData)`
  - [ ] 4.5 Ensure active palette and reorder tests pass
    - Run ONLY the tests written in 4.1
    - Verify toggle behavior works correctly
    - Verify reorder preserves active state

**Acceptance Criteria:**
- The 4-6 tests written in 4.1 pass
- Toggle behavior works: activating already-active palette deactivates it
- Non-existent palette IDs do not cause errors
- Reorder clamps indices and preserves active palette state
- All operations maintain immutability

---

### Platform Integration

#### Task Group 5: Services Integration and Platform Registration
**Dependencies:** Task Group 4

- [ ] 5.0 Complete platform integration
  - [ ] 5.1 Write 2-4 focused tests for service loading
    - Test Services.PALETTE_MANAGER loads successfully
    - Test loaded service is instance of DefaultPaletteManager
    - Test service is singleton (same instance returned)
  - [ ] 5.2 Update Services.java with PALETTE_MANAGER field
    - Location: `common/src/main/java/com/autoblockpalette/platform/Services.java`
    - Add: `public static final IPaletteManager PALETTE_MANAGER = load(IPaletteManager.class);`
    - Add import for IPaletteManager
  - [ ] 5.3 Create Fabric service registration file
    - Location: `fabric/src/main/resources/META-INF/services/com.autoblockpalette.platform.services.IPaletteManager`
    - Content: `com.autoblockpalette.platform.services.DefaultPaletteManager`
  - [ ] 5.4 Create Forge service registration file
    - Location: `forge/src/main/resources/META-INF/services/com.autoblockpalette.platform.services.IPaletteManager`
    - Content: `com.autoblockpalette.platform.services.DefaultPaletteManager`
  - [ ] 5.5 Create NeoForge service registration file
    - Location: `neoforge/src/main/resources/META-INF/services/com.autoblockpalette.platform.services.IPaletteManager`
    - Content: `com.autoblockpalette.platform.services.DefaultPaletteManager`
  - [ ] 5.6 Ensure platform integration tests pass
    - Run ONLY the tests written in 5.1
    - Verify ServiceLoader discovers implementation
    - Verify service is accessible via Services.PALETTE_MANAGER

**Acceptance Criteria:**
- The 2-4 tests written in 5.1 pass
- Services.PALETTE_MANAGER loads DefaultPaletteManager
- All three platforms (Fabric, Forge, NeoForge) have service registration files
- Service follows existing IPlatformHelper registration pattern exactly

---

### Testing

#### Task Group 6: Test Review and Gap Analysis
**Dependencies:** Task Groups 1-5

- [ ] 6.0 Review existing tests and fill critical gaps only
  - [ ] 6.1 Review tests from Task Groups 1-5
    - Review the 4-6 tests written in Task 1.1 (interface contract)
    - Review the 4-6 tests written in Task 2.1 (core state management)
    - Review the 6-8 tests written in Task 3.1 (CRUD operations)
    - Review the 4-6 tests written in Task 4.1 (active palette and reorder)
    - Review the 2-4 tests written in Task 5.1 (service loading)
    - Total existing tests: approximately 20-30 tests
  - [ ] 6.2 Analyze test coverage gaps for this feature only
    - Identify critical integration scenarios that lack coverage
    - Focus on edge cases in CRUD + active palette interactions
    - Check observer notification consistency across all operations
    - Verify thread safety with concurrent listener modifications
  - [ ] 6.3 Write up to 10 additional strategic tests maximum
    - Add tests for complex interaction scenarios if needed
    - Focus on integration between CRUD and active state
    - Test listener notification ordering and consistency
    - Test edge cases: empty state operations, boundary conditions
  - [ ] 6.4 Run feature-specific tests only
    - Run ONLY tests related to Palette Manager Service
    - Expected total: approximately 30-40 tests maximum
    - Verify all critical workflows pass
    - Do NOT run entire application test suite

**Acceptance Criteria:**
- All feature-specific tests pass (approximately 30-40 tests total)
- Critical integration scenarios are covered
- No more than 10 additional tests added
- Testing focused exclusively on Palette Manager Service

---

## Execution Order

Recommended implementation sequence:

1. **Task Group 1: Interface and Listener Definitions**
   - Establishes the API contract that all subsequent work depends on
   - No dependencies, can start immediately

2. **Task Group 2: Default Implementation - Core State Management**
   - Builds foundation for state and observer pattern
   - Depends on Task Group 1 interfaces

3. **Task Group 3: CRUD Operations Implementation**
   - Implements core business logic
   - Depends on Task Group 2 state management

4. **Task Group 4: Active Palette and Reorder Operations**
   - Completes remaining business logic
   - Depends on Task Group 3 for list manipulation patterns

5. **Task Group 5: Services Integration and Platform Registration**
   - Wires up the service for use by other components
   - Depends on Task Group 4 (complete implementation)

6. **Task Group 6: Test Review and Gap Analysis**
   - Final validation and coverage improvement
   - Depends on all previous groups

---

## File Inventory

### Files to Create
| File | Task |
|------|------|
| `common/src/main/java/com/autoblockpalette/platform/services/PaletteChangeListener.java` | 1.2 |
| `common/src/main/java/com/autoblockpalette/platform/services/IPaletteManager.java` | 1.3 |
| `common/src/main/java/com/autoblockpalette/platform/services/DefaultPaletteManager.java` | 2.2 |
| `fabric/src/main/resources/META-INF/services/com.autoblockpalette.platform.services.IPaletteManager` | 5.3 |
| `forge/src/main/resources/META-INF/services/com.autoblockpalette.platform.services.IPaletteManager` | 5.4 |
| `neoforge/src/main/resources/META-INF/services/com.autoblockpalette.platform.services.IPaletteManager` | 5.5 |

### Files to Modify
| File | Task |
|------|------|
| `common/src/main/java/com/autoblockpalette/platform/Services.java` | 5.2 |

### Test Files to Create
| File | Task |
|------|------|
| `common/src/test/java/com/autoblockpalette/platform/services/IPaletteManagerTest.java` | 1.1 |
| `common/src/test/java/com/autoblockpalette/platform/services/DefaultPaletteManagerStateTest.java` | 2.1 |
| `common/src/test/java/com/autoblockpalette/platform/services/DefaultPaletteManagerCrudTest.java` | 3.1 |
| `common/src/test/java/com/autoblockpalette/platform/services/DefaultPaletteManagerActiveTest.java` | 4.1 |
| `common/src/test/java/com/autoblockpalette/platform/services/ServicesIntegrationTest.java` | 5.1 |

---

## Technical Notes

### Existing Patterns to Follow
- **Services.java**: Use `load(Class<T>)` method pattern for service loading
- **IPlatformHelper.java**: Model interface structure with Javadoc documentation
- **PaletteData**: Use `List.copyOf()` for immutable list construction
- **Palette.create()**: Delegate validation to existing factory method

### Thread Safety Considerations
- Use `CopyOnWriteArrayList` for listener collection to allow concurrent iteration and modification
- All public methods should be thread-safe through immutable return values
- Internal state updates should be atomic (single reference assignment)

### Immutability Guidelines
- Never mutate existing `Palette` or `PaletteData` instances
- All CRUD operations return new `PaletteData` instances
- Use `List.copyOf()` and `Map.copyOf()` for defensive copies
