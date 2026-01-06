# Resources and Assets Standards

This document defines standards for organizing and creating resource files including textures, models, blockstates, lang files, and sounds.

## Directory Structure

```
src/main/resources/
├── assets/{mod_id}/
│   ├── blockstates/           # Block rendering rules
│   │   └── example_block.json
│   ├── models/
│   │   ├── block/             # Block models
│   │   │   └── example_block.json
│   │   └── item/              # Item models
│   │       ├── example_block.json
│   │       └── example_item.json
│   ├── textures/
│   │   ├── block/             # Block textures (16x16 PNG)
│   │   │   └── example_block.png
│   │   ├── item/              # Item textures (16x16 PNG)
│   │   │   └── example_item.png
│   │   ├── entity/            # Entity textures
│   │   ├── gui/               # GUI textures
│   │   └── particle/          # Particle textures
│   ├── lang/                  # Translations
│   │   ├── en_us.json
│   │   └── pt_br.json
│   ├── sounds/                # Sound files (.ogg)
│   └── sounds.json            # Sound definitions
│
├── data/{mod_id}/
│   ├── recipe/                # Crafting recipes
│   ├── loot_table/
│   │   ├── blocks/            # Block drops
│   │   └── entities/          # Entity drops
│   ├── tags/
│   │   ├── block/             # Block tags
│   │   └── item/              # Item tags
│   └── advancement/           # Advancements
│
└── pack.mcmeta                # Pack metadata
```

## Naming Conventions

- **All lowercase** with underscores: `example_block.json`
- **Match registry name**: Block `example_block` → `blockstates/example_block.json`
- **Consistent prefixes** for variants: `example_block.png`, `example_block_top.png`

## Blockstates

### Simple Block (all sides same)
```json
{
  "variants": {
    "": { "model": "examplemod:block/example_block" }
  }
}
```

### Directional Block (furnace-style)
```json
{
  "variants": {
    "facing=north": { "model": "examplemod:block/machine" },
    "facing=south": { "model": "examplemod:block/machine", "y": 180 },
    "facing=west": { "model": "examplemod:block/machine", "y": 270 },
    "facing=east": { "model": "examplemod:block/machine", "y": 90 }
  }
}
```

### Multi-Property Block
```json
{
  "variants": {
    "lit=false,facing=north": { "model": "examplemod:block/lamp_off" },
    "lit=true,facing=north": { "model": "examplemod:block/lamp_on" },
    "lit=false,facing=south": { "model": "examplemod:block/lamp_off", "y": 180 },
    "lit=true,facing=south": { "model": "examplemod:block/lamp_on", "y": 180 }
  }
}
```

### Multipart (fences, pipes)
```json
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

### Cube All (same texture all sides)
```json
{
  "parent": "minecraft:block/cube_all",
  "textures": {
    "all": "examplemod:block/example_block"
  }
}
```

### Cube Column (pillar)
```json
{
  "parent": "minecraft:block/cube_column",
  "textures": {
    "end": "examplemod:block/pillar_top",
    "side": "examplemod:block/pillar_side"
  }
}
```

### Orientable (directional)
```json
{
  "parent": "minecraft:block/orientable",
  "textures": {
    "top": "examplemod:block/machine_top",
    "front": "examplemod:block/machine_front",
    "side": "examplemod:block/machine_side"
  }
}
```

### Custom Geometry
```json
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

### Generated (flat sprite)
```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "examplemod:item/example_item"
  }
}
```

### Handheld (tools)
```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "layer0": "examplemod:item/example_sword"
  }
}
```

### Block Item
```json
{
  "parent": "examplemod:block/example_block"
}
```

### Layered Item
```json
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
{
  "block.examplemod.example_block": "Example Block",
  "block.examplemod.machine": "Processing Machine",

  "item.examplemod.example_item": "Example Item",
  "item.examplemod.example_sword": "Example Sword",

  "itemGroup.examplemod.main": "Example Mod",

  "entity.examplemod.custom_entity": "Custom Entity",

  "effect.examplemod.custom_effect": "Custom Effect",

  "enchantment.examplemod.custom_enchant": "Custom Enchantment",

  "container.examplemod.machine": "Processing Machine",

  "tooltip.examplemod.example_item": "A mysterious item",
  "tooltip.examplemod.shift_info": "Hold SHIFT for details",

  "key.examplemod.open_menu": "Open Menu",
  "key.categories.examplemod": "Example Mod",

  "message.examplemod.welcome": "Welcome!",
  "message.examplemod.error": "An error occurred: %s"
}
```

### Translation Key Patterns

| Type | Pattern |
|------|---------|
| Block | `block.{modid}.{name}` |
| Item | `item.{modid}.{name}` |
| Entity | `entity.{modid}.{name}` |
| Effect | `effect.{modid}.{name}` |
| Enchantment | `enchantment.{modid}.{name}` |
| Creative Tab | `itemGroup.{modid}.{name}` |
| Container | `container.{modid}.{name}` |
| Key Binding | `key.{modid}.{name}` |
| Key Category | `key.categories.{modid}` |
| Advancement | `advancement.{modid}.{name}` |
| Custom | `{modid}.{category}.{name}` |

## Sounds

### sounds.json
```json
{
  "block.example.break": {
    "subtitle": "subtitles.examplemod.block.break",
    "sounds": [
      "examplemod:block/example_break1",
      "examplemod:block/example_break2"
    ]
  },
  "entity.custom.ambient": {
    "subtitle": "subtitles.examplemod.entity.ambient",
    "sounds": [
      {
        "name": "examplemod:entity/custom_ambient",
        "volume": 0.8,
        "pitch": 1.0
      }
    ]
  }
}
```

Sound files: OGG format in `assets/{modid}/sounds/`

## Textures

### Requirements
- **Format:** PNG
- **Size:** 16x16 pixels (or power-of-2 multiples)
- **Transparency:** Supported
- **Color depth:** 32-bit RGBA

### Animated Textures
Create `.mcmeta` file alongside texture:

```json
// example_block.png.mcmeta
{
  "animation": {
    "frametime": 2,
    "frames": [0, 1, 2, 3, 2, 1]
  }
}
```

## pack.mcmeta

```json
{
  "pack": {
    "description": "Example Mod Resources",
    "pack_format": 34
  }
}
```

Pack format varies by Minecraft version - check wiki.

## Common Issues

| Problem | Solution |
|---------|----------|
| Missing texture (purple/black) | Verify texture path |
| Block invisible | Check blockstates JSON |
| Item no model | Check models/item/{name}.json |
| Lang key raw | Verify JSON syntax |
| Sound silent | Check sounds.json paths |

## Best Practices

1. **Use datagen** - Generate JSON automatically when possible
2. **Consistent naming** - Follow vanilla conventions
3. **Optimize textures** - Keep at 16x16 unless necessary
4. **Test all variants** - Check every blockstate combination
5. **Localize everything** - Add lang entries for all content
6. **Include subtitles** - For accessibility
