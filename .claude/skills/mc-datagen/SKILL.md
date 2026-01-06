---
name: Minecraft Data Generation
description: Use data generation to automatically create JSON files for recipes, loot tables, tags, advancements, models, and blockstates. Use this skill when adding new blocks or items that need associated data files. Apply when creating recipes, loot tables, or block/item tags programmatically. Essential when setting up data providers for your mod's assets. Use when running datagen tasks or troubleshooting generated file issues. Covers Fabric Data Generation API and Forge/NeoForge data providers.
---

## Standards Reference

For detailed standards, refer to: [Data Generation Standards](../../../agent-os/standards/development/datagen.md)

## When to use this skill:

- When adding new blocks that need blockstate and model JSONs
- When adding new items that need model JSONs
- When creating crafting recipes, smelting recipes, or custom recipes
- When defining loot tables for blocks, entities, or chests
- When creating or adding to tags (block tags, item tags, entity tags)
- When setting up advancement triggers and rewards
- When configuring data generation providers and run configurations
- When debugging generated JSON files or datagen failures
- When working with `*Provider` classes or `FabricDataGenerator`

## Data Generation Overview

Data generation creates JSON files automatically from Java code:

```
Java Code (DataProvider) → JSON Files
├── recipes/*.json
├── loot_tables/blocks/*.json
├── tags/blocks/*.json
├── tags/items/*.json
├── models/block/*.json
├── models/item/*.json
├── blockstates/*.json
└── advancements/*.json
```

## Project Structure

```
mod/
├── src/main/java/.../datagen/
│   ├── ModDataGenerator.java      # Main entry point
│   ├── ModRecipeProvider.java     # Recipes
│   ├── ModBlockLootProvider.java  # Block loot tables
│   ├── ModBlockTagProvider.java   # Block tags
│   ├── ModItemTagProvider.java    # Item tags
│   ├── ModModelProvider.java      # Block/item models
│   └── ModAdvancementProvider.java # Advancements
└── src/main/generated/            # Output directory
```

## Fabric Data Generation

### Entry Point

```java
// fabric/src/main/java/.../datagen/ModDataGenerator.java
public class ModDataGenerator implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        FabricDataGenerator.Pack pack = generator.createPack();

        // Register providers
        pack.addProvider(ModModelProvider::new);
        pack.addProvider(ModRecipeProvider::new);
        pack.addProvider(ModBlockLootTableProvider::new);
        BlockTagProvider blockTags = pack.addProvider(ModBlockTagProvider::new);
        pack.addProvider((output, registries) -> new ModItemTagProvider(output, registries, blockTags));
    }
}
```

Register in `fabric.mod.json`:
```json
{
  "entrypoints": {
    "fabric-datagen": ["com.example.mod.datagen.ModDataGenerator"]
  }
}
```

### Model Provider

```java
public class ModModelProvider extends FabricModelProvider {

    public ModModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator generator) {
        // Simple cube block
        generator.registerSimpleCubeAll(ModBlocks.EXAMPLE_BLOCK.get());

        // Block with different textures per side
        generator.registerCubeColumn(ModBlocks.PILLAR_BLOCK.get());

        // Slab, stairs, wall variants
        BlockStateModelGenerator.BlockTexturePool pool =
            generator.registerCubeAllModelTexturePool(ModBlocks.STONE_VARIANT.get());
        pool.slab(ModBlocks.STONE_VARIANT_SLAB.get());
        pool.stairs(ModBlocks.STONE_VARIANT_STAIRS.get());
        pool.wall(ModBlocks.STONE_VARIANT_WALL.get());

        // Door and trapdoor
        generator.registerDoor(ModBlocks.EXAMPLE_DOOR.get());
        generator.registerTrapdoor(ModBlocks.EXAMPLE_TRAPDOOR.get());
    }

    @Override
    public void generateItemModels(ItemModelGenerator generator) {
        // Simple item
        generator.register(ModItems.EXAMPLE_ITEM.get(), Models.GENERATED);

        // Handheld item (tools)
        generator.register(ModItems.EXAMPLE_TOOL.get(), Models.HANDHELD);

        // Block items are usually handled by block model
    }
}
```

### Recipe Provider

```java
public class ModRecipeProvider extends FabricRecipeProvider {

    public ModRecipeProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    public void buildRecipes(RecipeOutput output) {
        // Shaped recipe
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.EXAMPLE_BLOCK.get())
            .pattern("###")
            .pattern("#X#")
            .pattern("###")
            .define('#', Items.COBBLESTONE)
            .define('X', Items.DIAMOND)
            .unlockedBy("has_diamond", has(Items.DIAMOND))
            .save(output);

        // Shapeless recipe
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.EXAMPLE_ITEM.get(), 4)
            .requires(ModBlocks.EXAMPLE_BLOCK.get())
            .unlockedBy("has_example_block", has(ModBlocks.EXAMPLE_BLOCK.get()))
            .save(output);

        // Smelting recipe
        SimpleCookingRecipeBuilder.smelting(
                Ingredient.of(ModBlocks.EXAMPLE_ORE.get()),
                RecipeCategory.MISC,
                ModItems.EXAMPLE_INGOT.get(),
                0.7f,  // Experience
                200    // Cooking time (ticks)
            )
            .unlockedBy("has_ore", has(ModBlocks.EXAMPLE_ORE.get()))
            .save(output);

        // Stonecutting recipe
        SingleItemRecipeBuilder.stonecutting(
                Ingredient.of(ModBlocks.STONE_VARIANT.get()),
                RecipeCategory.BUILDING_BLOCKS,
                ModBlocks.STONE_VARIANT_SLAB.get(),
                2
            )
            .unlockedBy("has_stone", has(ModBlocks.STONE_VARIANT.get()))
            .save(output, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "stone_slab_stonecutting"));
    }
}
```

### Loot Table Provider

```java
public class ModBlockLootTableProvider extends FabricBlockLootTableProvider {

    public ModBlockLootTableProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    public void generate() {
        // Block drops itself
        dropSelf(ModBlocks.EXAMPLE_BLOCK.get());

        // Block drops different item
        dropOther(ModBlocks.EXAMPLE_ORE.get(), ModItems.EXAMPLE_GEM.get());

        // Ore with fortune
        add(ModBlocks.EXAMPLE_ORE.get(),
            createOreDrop(ModBlocks.EXAMPLE_ORE.get(), ModItems.EXAMPLE_GEM.get()));

        // Silk touch behavior
        add(ModBlocks.GLASS_VARIANT.get(),
            createSilkTouchOnlyTable(ModBlocks.GLASS_VARIANT.get()));

        // Slab (drops 2 when double)
        add(ModBlocks.EXAMPLE_SLAB.get(),
            createSlabItemTable(ModBlocks.EXAMPLE_SLAB.get()));

        // Door (drops 1 item for 2-block door)
        add(ModBlocks.EXAMPLE_DOOR.get(),
            createDoorTable(ModBlocks.EXAMPLE_DOOR.get()));
    }
}
```

### Tag Provider

```java
public class ModBlockTagProvider extends FabricTagProvider.BlockTagProvider {

    public ModBlockTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Add to vanilla tags
        getOrCreateTagBuilder(BlockTags.MINEABLE_WITH_PICKAXE)
            .add(ModBlocks.EXAMPLE_BLOCK.get())
            .add(ModBlocks.EXAMPLE_ORE.get());

        getOrCreateTagBuilder(BlockTags.NEEDS_IRON_TOOL)
            .add(ModBlocks.EXAMPLE_ORE.get());

        // Create custom tag
        getOrCreateTagBuilder(ModTags.Blocks.EXAMPLE_BLOCKS)
            .add(ModBlocks.EXAMPLE_BLOCK.get())
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("othermod", "their_blocks"));
    }
}

public class ModItemTagProvider extends FabricTagProvider.ItemTagProvider {

    public ModItemTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries, BlockTagProvider blockTags) {
        super(output, registries, blockTags);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Copy block tags to item tags
        copy(BlockTags.PLANKS, ItemTags.PLANKS);
        copy(ModTags.Blocks.EXAMPLE_BLOCKS, ModTags.Items.EXAMPLE_BLOCKS);

        // Add to item-specific tags
        getOrCreateTagBuilder(ItemTags.SWORDS)
            .add(ModItems.EXAMPLE_SWORD.get());
    }
}
```

## Running Data Generation

**Fabric:**
```bash
./gradlew :fabric:runDatagen
```

**Forge/NeoForge:**
```bash
./gradlew :forge:runData
./gradlew :neoforge:runData
```

Generated files appear in `src/main/generated/` and should be included in resources.

## Common Tags Reference

| Tag | Purpose |
|-----|---------|
| `BlockTags.MINEABLE_WITH_PICKAXE` | Pickaxe can mine |
| `BlockTags.MINEABLE_WITH_AXE` | Axe can mine |
| `BlockTags.MINEABLE_WITH_SHOVEL` | Shovel can mine |
| `BlockTags.MINEABLE_WITH_HOE` | Hoe can mine |
| `BlockTags.NEEDS_STONE_TOOL` | Requires stone+ tier |
| `BlockTags.NEEDS_IRON_TOOL` | Requires iron+ tier |
| `BlockTags.NEEDS_DIAMOND_TOOL` | Requires diamond+ tier |
| `ItemTags.COALS` | Coal and charcoal |
| `ItemTags.PLANKS` | All wood planks |
| `ItemTags.LOGS` | All log blocks |

## Best Practices

1. **Always use datagen** - Don't hand-write JSON files for standard content
2. **Test generated files** - Run datagen and verify output before committing
3. **Use existing helpers** - Fabric/Forge provide methods for common patterns
4. **Unlock recipes properly** - Use `unlockedBy` to gate recipes behind items
5. **Copy tags** - Use `copy()` to keep block/item tags in sync
