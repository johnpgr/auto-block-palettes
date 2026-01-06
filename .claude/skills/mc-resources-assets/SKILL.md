---
name: Minecraft Resources and Assets
description: Structure and create resource files including textures, models, blockstates, lang files, sounds, and data files for Minecraft mods. Use this skill when adding visual assets, localization, or data-driven content to your mod. Apply when creating or editing JSON files in resources/assets/ or resources/data/ directories. Essential when troubleshooting missing textures, broken models, or localization issues.
---

## Standards Reference

For detailed standards, refer to: [Resources and Assets Standards](../../../agent-os/standards/development/resources.md)

## When to use this skill:

- When creating texture files (.png) for blocks, items, or entities
- When writing blockstate JSON files for block rendering
- When creating block and item model JSON files
- When adding or editing language files (en_us.json, etc.)
- When setting up sound events and sound files
- When creating data-driven content (recipes, loot tables, tags, advancements)
- When organizing resources in the correct directory structure
- When troubleshooting "missing texture" or "missing model" errors
- When working with resource packs or data packs within your mod

## Resource Directory Structure

```
src/main/resources/
├── assets/
│   └── {mod_id}/
│       ├── blockstates/          # Block rendering states
│       │   └── example_block.json
│       ├── models/
│       │   ├── block/            # Block models
│       │   │   └── example_block.json
│       │   └── item/             # Item models
│       │       ├── example_block.json
│       │       └── example_item.json
│       ├── textures/
│       │   ├── block/            # Block textures
│       │   │   └── example_block.png
│       │   ├── item/             # Item textures
│       │   │   └── example_item.png
│       │   └── entity/           # Entity textures
│       │       └── example_entity.png
│       ├── lang/                 # Localization
│       │   └── en_us.json
│       └── sounds.json           # Sound definitions
│
└── data/
    └── {mod_id}/
        ├── recipe/              # Crafting recipes
        ├── loot_table/
        │   ├── blocks/          # Block drops
        │   └── entities/        # Entity drops
        ├── tags/
        │   ├── block/           # Block tags
        │   └── item/            # Item tags
        └── advancement/         # Advancements
```

## Blockstates

Defines how a block renders based on its state properties:

```json
// assets/{mod_id}/blockstates/example_block.json
{
  "variants": {
    "": { "model": "examplemod:block/example_block" }
  }
}

// Block with facing property
// assets/{mod_id}/blockstates/machine.json
{
  "variants": {
    "facing=north": { "model": "examplemod:block/machine" },
    "facing=south": { "model": "examplemod:block/machine", "y": 180 },
    "facing=west": { "model": "examplemod:block/machine", "y": 270 },
    "facing=east": { "model": "examplemod:block/machine", "y": 90 }
  }
}

// Block with multiple properties
// assets/{mod_id}/blockstates/lamp.json
{
  "variants": {
    "lit=false,facing=north": { "model": "examplemod:block/lamp_off" },
    "lit=true,facing=north": { "model": "examplemod:block/lamp_on" },
    "lit=false,facing=south": { "model": "examplemod:block/lamp_off", "y": 180 },
    "lit=true,facing=south": { "model": "examplemod:block/lamp_on", "y": 180 }
  }
}

// Multipart (for fences, walls, pipes)
// assets/{mod_id}/blockstates/pipe.json
{
  "multipart": [
    { "apply": { "model": "examplemod:block/pipe_core" } },
    { "when": { "north": "true" }, "apply": { "model": "examplemod:block/pipe_side" } },
    { "when": { "south": "true" }, "apply": { "model": "examplemod:block/pipe_side", "y": 180 } },
    { "when": { "west": "true" }, "apply": { "model": "examplemod:block/pipe_side", "y": 270 } },
    { "when": { "east": "true" }, "apply": { "model": "examplemod:block/pipe_side", "y": 90 } }
  ]
}
```

## Block Models

```json
// Simple cube - all sides same texture
// models/block/example_block.json
{
  "parent": "minecraft:block/cube_all",
  "textures": {
    "all": "examplemod:block/example_block"
  }
}

// Different top/bottom/sides
// models/block/pillar.json
{
  "parent": "minecraft:block/cube_column",
  "textures": {
    "end": "examplemod:block/pillar_top",
    "side": "examplemod:block/pillar_side"
  }
}

// Oriented block (furnace-style)
// models/block/machine.json
{
  "parent": "minecraft:block/orientable",
  "textures": {
    "top": "examplemod:block/machine_top",
    "front": "examplemod:block/machine_front",
    "side": "examplemod:block/machine_side"
  }
}

// Custom model
// models/block/custom.json
{
  "parent": "minecraft:block/block",
  "textures": {
    "particle": "examplemod:block/custom",
    "texture": "examplemod:block/custom"
  },
  "elements": [
    {
      "from": [4, 0, 4],
      "to": [12, 16, 12],
      "faces": {
        "north": { "texture": "#texture", "uv": [4, 0, 12, 16] },
        "south": { "texture": "#texture", "uv": [4, 0, 12, 16] },
        "west": { "texture": "#texture", "uv": [4, 0, 12, 16] },
        "east": { "texture": "#texture", "uv": [4, 0, 12, 16] },
        "up": { "texture": "#texture", "uv": [4, 4, 12, 12] },
        "down": { "texture": "#texture", "uv": [4, 4, 12, 12] }
      }
    }
  ]
}
```

## Item Models

```json
// Simple flat item
// models/item/example_item.json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "examplemod:item/example_item"
  }
}

// Handheld item (sword, tool)
// models/item/example_sword.json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "layer0": "examplemod:item/example_sword"
  }
}

// Block item (inherits block model)
// models/item/example_block.json
{
  "parent": "examplemod:block/example_block"
}

// Item with multiple layers
// models/item/layered_item.json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "examplemod:item/base",
    "layer1": "examplemod:item/overlay"
  }
}
```

## Language Files

```json
// lang/en_us.json
{
  "block.examplemod.example_block": "Example Block",
  "block.examplemod.machine": "Processing Machine",

  "item.examplemod.example_item": "Example Item",
  "item.examplemod.example_sword": "Example Sword",

  "itemGroup.examplemod.main": "Example Mod",

  "entity.examplemod.example_entity": "Example Entity",

  "effect.examplemod.example_effect": "Example Effect",

  "enchantment.examplemod.example_enchant": "Example Enchant",

  "container.examplemod.machine": "Processing Machine",

  "tooltip.examplemod.example_item": "A mysterious item",
  "tooltip.examplemod.shift_info": "Hold SHIFT for more info",

  "message.examplemod.welcome": "Welcome to the server!",
  "message.examplemod.not_enough_fuel": "Not enough fuel!"
}
```

### Adding Translations

```java
// In code, use translatable components
Component name = Component.translatable("block.examplemod.example_block");
Component message = Component.translatable("message.examplemod.welcome");

// With arguments
Component formatted = Component.translatable("message.examplemod.player_joined", playerName);
// lang: "message.examplemod.player_joined": "%s has joined the game"
```

## Sounds

```json
// sounds.json
{
  "block.example_block.break": {
    "subtitle": "subtitles.examplemod.block.break",
    "sounds": [
      "examplemod:block/example_break1",
      "examplemod:block/example_break2"
    ]
  },
  "entity.example_entity.ambient": {
    "subtitle": "subtitles.examplemod.entity.ambient",
    "sounds": [
      {
        "name": "examplemod:entity/example_ambient",
        "volume": 0.8,
        "pitch": 1.0
      }
    ]
  },
  "item.example_item.use": {
    "sounds": ["examplemod:item/example_use"]
  }
}
```

Sound files go in: `assets/{mod_id}/sounds/`

```java
// Register and use sounds
public class ModSounds {
    public static SoundEvent EXAMPLE_SOUND = SoundEvent.createVariableRangeEvent(
        ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "block.example_block.break")
    );

    // Play sound
    level.playSound(null, pos, ModSounds.EXAMPLE_SOUND, SoundSource.BLOCKS, 1.0f, 1.0f);
}
```

## Texture Guidelines

- **Size**: 16x16 pixels (or multiples: 32x32, 64x64 for high-res)
- **Format**: PNG with transparency support
- **Naming**: lowercase with underscores (example_block.png)
- **Animation**: Use .mcmeta files for animated textures

```json
// textures/block/animated_block.png.mcmeta
{
  "animation": {
    "frametime": 2,
    "frames": [0, 1, 2, 3, 2, 1]
  }
}
```

## Common Issues

| Problem | Solution |
|---------|----------|
| Purple/black missing texture | Check texture path and filename |
| Block invisible | Verify blockstates JSON exists |
| Item has no model | Check models/item/{name}.json |
| Lang key shows raw | Verify lang file JSON syntax |
| Sound not playing | Check sounds.json and file paths |

## Pack Format

In `pack.mcmeta`:
```json
{
  "pack": {
    "description": "Example Mod Resources",
    "pack_format": 34
  }
}
```

Pack format changes with Minecraft versions - check the wiki for your target version.
