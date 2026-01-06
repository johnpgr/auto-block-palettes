# Product Roadmap

1. [ ] Palette Data Model — Define the core data structures for block palettes including palette name, icon block, list of blocks with weights, and active state. Implement serialization for persistence. `S`

2. [ ] Palette Manager Service — Create a service class to manage palette CRUD operations (create, read, update, delete) and handle the currently active palette state. `S`

3. [ ] Weighted Random Block Selection — Implement the weighted random selection algorithm that picks the next block from the active palette based on configured percentages/weights. `S`

4. [ ] Block Placement Interception — Use Mixin to intercept right-click block placement events and swap the placed block with the weighted selection from the active palette when active. `M`

5. [ ] Palette Persistence — Implement saving and loading of palettes to/from disk using JSON, ensuring palettes persist across game sessions. `S`

6. [ ] Palette Management GUI Screen — Create an interactive GUI screen for creating and editing palettes, including block selection from inventory, weight sliders/inputs, and name/icon configuration. `L`

7. [ ] Palette Selection GUI — Build a quick-access GUI for viewing all palettes and activating/deactivating them with a single click or keybind. `M`

8. [ ] Keybind Registration — Register configurable keybinds for opening the palette GUI, toggling the active palette on/off, and cycling between palettes. `S`

9. [ ] Creative Mode Block Swapping — Implement special handling for creative mode that pulls blocks from creative inventory without requiring them in the hotbar or affecting player inventory state. `M`

10. [ ] Inventory Highlighting — Add visual highlighting overlay to inventory screens that marks blocks belonging to the currently active palette. `M`

11. [ ] HUD Indicator — Display a small HUD element showing the currently active palette name and icon, with visual feedback when block swapping occurs. `S`

12. [ ] Palette Import/Export — Allow users to export palettes to shareable JSON files and import palettes from files or clipboard. `S`

> Notes
> - Order items by technical dependencies and product architecture
> - Each item should represent an end-to-end (frontend + backend) functional and testable feature
> - Items 1-5 establish core functionality for survival mode building
> - Items 6-8 provide user-facing palette management
> - Items 9-12 add polish and advanced features
