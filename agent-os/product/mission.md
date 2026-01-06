# Product Mission

## Pitch

Auto Block Palette is a client-side Minecraft mod that helps builders create textured, varied constructions effortlessly by automatically swapping between blocks in user-defined palettes based on configurable weights - eliminating tedious manual hotbar scrolling and inventory management.

## Users

### Primary Customers

- **Creative Builders**: Players focused on large-scale building projects who want professional-looking textured walls, floors, and structures without the repetitive manual effort
- **Survival Builders**: Players who want efficient palette-based building while managing limited resources
- **Content Creators**: Streamers and YouTubers who need smooth, uninterrupted building footage without constant inventory fiddling

### User Personas

**Alex the Creative Builder** (18-35)
- **Role:** Dedicated Minecraft builder spending 10+ hours weekly on builds
- **Context:** Works on large creative mode projects like castles, cities, and custom terrain
- **Pain Points:** Manually switching between 4-6 similar blocks (stone variants, wood types) is tedious and breaks creative flow; builds look flat and repetitive without texture variation
- **Goals:** Create professional-looking builds with natural texture variation; maintain building momentum without interruption

**Sam the Survival Builder** (16-30)
- **Role:** Survival player who enjoys building aesthetically pleasing bases
- **Context:** Limited block resources, needs efficient building workflow
- **Pain Points:** Wants varied textures but managing inventory slots for multiple similar blocks is cumbersome in survival
- **Goals:** Achieve textured builds without wasting inventory space or time on block management

**Jordan the Content Creator** (20-40)
- **Role:** Minecraft YouTuber/streamer showcasing building techniques
- **Context:** Recording gameplay where smooth, professional building demonstrations matter
- **Pain Points:** Constant hotbar scrolling and inventory management looks unprofessional on camera and interrupts commentary flow
- **Goals:** Demonstrate advanced building techniques seamlessly; show viewers how to achieve textured builds efficiently

## The Problem

### Manual Block Variation is Tedious

Experienced builders know that mixing similar blocks (stone bricks with cracked and mossy variants, different wood types, various stone textures) creates more natural, visually appealing constructions. However, achieving this manually requires:

1. Dedicating multiple hotbar slots to similar blocks
2. Constantly scrolling or pressing number keys to switch between blocks
3. Mentally tracking which block to place next for "random" distribution
4. Breaking building flow and creative momentum

This friction discourages texture variation, resulting in flat, monotonous builds - or exhausted builders who burn out on tedious mechanical tasks.

**Our Solution:** Automate block variation by defining weighted palettes that automatically swap the active block on each placement, letting builders focus on design and placement rather than inventory management.

## Differentiators

### Weighted Probability System

Unlike simple random block selectors, Auto Block Palette uses configurable weights/percentages for each block in a palette. This allows builders to control the distribution - for example, 60% stone bricks, 20% cracked, 15% mossy, 5% cobblestone - creating intentional, reproducible texture patterns rather than chaotic randomness.

### Seamless Creative Mode Integration

The mod handles creative inventory specially, swapping blocks without cluttering the player's inventory or requiring blocks to be pre-loaded in the hotbar. This results in a frictionless experience for creative builders who represent the primary user base.

### Non-Invasive Client-Side Design

As a purely client-side mod, Auto Block Palette works on any server without requiring server-side installation. Builders can use it anywhere - singleplayer, multiplayer survival servers, or creative servers - without administrative approval or compatibility concerns.

## Key Features

### Core Features

- **Block Palette System:** Define named palettes containing multiple blocks with individual weight/percentage values that control placement frequency
- **Automatic Block Swapping:** When a palette is active, right-clicking to place a block automatically selects the next block based on weighted random selection
- **Palette Persistence:** Palettes are saved and persist across game sessions

### Management Features

- **Interactive GUI Screen:** Full-featured interface for creating, editing, and managing palettes - including block selection, naming, icon choice, and weight configuration
- **Palette Activation:** Quick keybind or GUI toggle to activate/deactivate palettes and switch between them

### Advanced Features

- **Creative Mode Support:** Seamlessly swap blocks from creative inventory without affecting player inventory state
- **Visual Highlighting:** Active palette blocks are highlighted in inventory screens for easy identification
- **Import/Export:** Share palette configurations with other players

## Success Criteria

- Builders can create a new palette in under 30 seconds
- Block swapping feels instantaneous with no perceptible lag
- Palettes work identically across Fabric, Forge, and NeoForge
- Zero impact on server compatibility (pure client-side)
- Intuitive enough that builders can use core features without documentation
