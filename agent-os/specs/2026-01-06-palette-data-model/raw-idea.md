# Raw Idea: Palette Data Model

## Feature Summary

Define the core data structures for block palettes that will serve as the foundation for the entire Auto Block Palette mod.

## Description

A palette is a named collection of Minecraft blocks with associated weights that control how frequently each block is selected during automatic block swapping. When a builder places a block while a palette is active, the mod selects which block to actually place based on the weighted random selection from the palette.

## Required Data Elements

Based on the roadmap and mission documents:

- **Palette Name**: Human-readable identifier for the palette (e.g., "Stone Brick Mix")
- **Icon Block**: A single block used to visually represent the palette in GUIs and HUD
- **Block List**: Collection of blocks included in the palette
- **Weights**: Each block has an associated weight controlling selection probability
- **Active State**: Whether this palette is currently being used for block swapping

## Technical Requirements

- Must serialize to JSON for persistence across game sessions
- Must work with Minecraft's block registry system
- Must be compatible with the MultiLoader architecture (Fabric/Forge/NeoForge)
- Client-side only - no server interaction needed

## Dependencies

None - this is the foundational feature.

## Dependents

All other features depend on this data model:

- Palette Manager Service (CRUD operations)
- Weighted Random Block Selection (reads weights)
- Block Placement Interception (reads active palette)
- Palette Persistence (serialization)
- All GUI features (display and edit palettes)
