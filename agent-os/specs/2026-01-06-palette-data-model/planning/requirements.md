# Spec Requirements: Palette Data Model

## Initial Description

Define the core data structures for block palettes including palette name, icon block, list of blocks with weights, and active state. Implement serialization for persistence.

## Requirements Discussion

### First Round Questions

**Q1:** I assume weights should be stored as relative integers (e.g., 60/20/15/5) rather than strict percentages that must sum to 100, since relative weights are easier to edit without rebalancing. Is that correct, or would you prefer enforced percentages?
**Answer:** Relative integers - confirmed.

**Q2:** I'm thinking a weight of 0 should effectively disable a block from selection (useful for temporarily removing a block without deleting it). Should we support this, or should all blocks in a palette always participate in selection?
**Answer:** Weight of 0 means never place this block (effectively disabled).

**Q3:** I assume we only need to store the block's ResourceLocation (e.g., `minecraft:stone_bricks`) and not specific block states (like stair orientation or slab type), since placement orientation is determined at placement time. Is that correct, or do you need state-specific entries?
**Answer:** Confirmed ResourceLocation approach. Clarification: This is a multiloader project. Fabric uses `Identifier`, Forge/NeoForge uses `ResourceLocation`. Both represent the same concept. The common module should store the string representation.

**Q4:** Should the data model support modded blocks (e.g., `biomesoplenty:willow_planks`), or is vanilla Minecraft block support sufficient for the initial implementation?
**Answer:** Modded blocks should be supported (any mod's blocks can be added to palettes).

**Q5:** I'm assuming each palette needs: name, icon block, and block list with weights. Should we also include any additional metadata like description, creation timestamp, or category/tags for organization?
**Answer:** Just basic: name, block list with weights. No extra metadata needed.

**Q6:** For the "active" state - I assume only one palette can be active at a time globally. Is that correct, or should users be able to have multiple palettes active simultaneously (perhaps mapped to different hotbar slots)?
**Answer:** One active palette at a time (global state).

**Q7:** I assume a palette must have at least one block to be valid, and palette names should be unique. Are there any other validation rules - like maximum blocks per palette, maximum palettes total, or name length limits?
**Answer:** At least 1 block per palette. Initial max limit of 9 blocks per palette, but user can increase this limit at any time (configurable).

**Q8:** Is there anything you explicitly do NOT want included in this data model that I should avoid (features to defer to later versions)?
**Answer:** Nothing to exclude/defer.

### Existing Code to Reference

No similar existing features identified for reference.

### Follow-up Questions

**Follow-up 1:** You mentioned a configurable max of 9 blocks per palette. Should this limit be stored in a separate mod configuration file (alongside other future settings), or should it be hardcoded for now with a TODO to make it configurable later?
**Answer:** 9 blocks is the DEFAULT display limit in the palette, but not a hard constraint. The palette creation/editing GUI will have a button to add more blocks beyond 9. So the data model should NOT enforce a maximum - it's a UI concern for the GUI spec later.

**Follow-up 2:** For the common module, I assume we should store the string representation (e.g., `"minecraft:stone_bricks"`) in the data model and JSON, then convert to the platform-specific type (`Identifier` or `ResourceLocation`) only when needed at runtime. Is that the approach you want?
**Answer:** Confirmed. Store as strings like `"minecraft:stone_bricks"`, convert to Identifier/ResourceLocation at runtime.

## Visual Assets

### Files Provided:

No visual assets provided.

### Visual Insights:

N/A

## Requirements Summary

### Functional Requirements

- Define a Palette data structure containing:
    - Name (string, required, should be unique across all palettes)
    - Icon block (string block ID for visual representation in GUI/HUD)
    - Blocks map (block ID string to weight integer)
- Weight behavior:
    - Positive integer = relative frequency (higher = more likely to be selected)
    - Zero = block is disabled/never placed (allows temporary exclusion without removal)
- Global active palette state:
    - Only one palette can be active at a time
    - Active palette can be null/none (no palette active)
- Support for any block from any mod (full namespace support, not just vanilla)
- Block IDs stored as platform-agnostic strings, converted to Identifier/ResourceLocation at runtime

### Reusability Opportunities

- No existing similar code patterns identified in the codebase
- Data model should be defined in the `common` module for cross-loader compatibility
- String-based block IDs enable platform-agnostic serialization

### Scope Boundaries

**In Scope:**

- Palette data class/record definition
- JSON serialization structure for persistence
- Validation logic (at least 1 block required)
- Active palette state tracking

**Out of Scope:**

- CRUD operations (Palette Manager Service - Roadmap Item #2)
- File I/O and persistence implementation (Palette Persistence - Roadmap Item #5)
- Weighted random selection algorithm (Roadmap Item #3)
- GUI display constraints (9-block default is a UI concern - Roadmap Item #6)
- Block state properties (orientation, slab type, etc.)
- Additional metadata (description, timestamps, tags, categories)

### Technical Considerations

- MultiLoader architecture: code goes in `common` module
- Block IDs as strings avoid Fabric `Identifier` vs Forge/NeoForge `ResourceLocation` differences
- JSON format for persistence (implementation in separate spec)
- Client-side only - no server interaction
- Data model must be serializable to JSON
- Expected JSON structure (optimized format):

```json
{
  "palettes": [
    {
      "name": "Stone Brick Mix",
      "iconBlock": "minecraft:stone_bricks",
      "blocks": {
        "minecraft:stone_bricks": 60,
        "minecraft:cracked_stone_bricks": 20,
        "minecraft:mossy_stone_bricks": 15,
        "minecraft:cobblestone": 5
      }
    }
  ],
  "activePalette": "Stone Brick Mix"
}
```
