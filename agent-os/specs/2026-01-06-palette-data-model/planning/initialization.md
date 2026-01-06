# Spec Initialization

## Feature

Palette Data Model

## Source

Roadmap Item #1

## Initial Description

Define the core data structures for block palettes including palette name, icon block, list of blocks with weights, and active state. Implement serialization for persistence.

## Size Estimate

S (Small)

## Context

This is the foundational feature - all other features in the Auto Block Palette mod depend on this data model being well-designed. The data structures defined here will be:

- Used by the Palette Manager Service for CRUD operations
- Serialized to JSON for persistence
- Displayed in GUI screens for management
- Referenced during block placement interception

## Technical Context

- Client-side only mod (no server code)
- Must support Fabric, Forge, and NeoForge via MultiLoader architecture
- Data stored as JSON in client config directory
- Must reference Minecraft blocks in a serializable way
