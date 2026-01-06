# Data Generation Standards

This document defines standards for using data generation to create JSON files for recipes, loot tables, tags, models, and blockstates.

## Data Generation Overview

Data generation creates JSON files automatically from Java code:

```
Java Provider Classes → JSON Files
├── data/{mod_id}/recipe/
├── data/{mod_id}/loot_table/
├── data/{mod_id}/tags/
├── data/{mod_id}/advancement/
├── assets/{mod_id}/blockstates/
├── assets/{mod_id}/models/block/
└── assets/{mod_id}/models/item/
```

## Project Structure

```
mod/
├── src/main/java/.../datagen/
│   ├── ModDataGenerator.java         # Entry point
│   ├── ModModelProvider.java         # Block/item models
│   ├── ModRecipeProvider.java        # Recipes
│   ├── ModBlockLootProvider.java     # Block drops
│   ├── ModBlockTagProvider.java      # Block tags
│   ├── ModItemTagProvider.java       # Item tags
│   └── ModAdvancementProvider.java   # Advancements
└── src/main/generated/               # Output
```

## Fabric Data Generator Entry Point

```java
public class ModDataGenerator implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        FabricDataGenerator.Pack pack = generator.createPack();

        // Register providers in dependency order
        pack.addProvider(ModModelProvider::new);
        pack.addProvider(ModRecipeProvider::new);
        pack.addProvider(ModBlockLootProvider::new);

        // Tags with dependencies
        BlockTagProvider blockTags = pack.addProvider(ModBlockTagProvider::new);
        pack.addProvider((out, reg) -> new ModItemTagProvider(out, reg, blockTags));
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

## Model Provider

```java
public class ModModelProvider extends FabricModelProvider {

    public ModModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator gen) {
        // Simple cube (all sides same)
        gen.registerSimpleCubeAll(ModBlocks.EXAMPLE_BLOCK.get());

        // Cube column (pillar-style)
        gen.registerCubeColumn(ModBlocks.PILLAR.get());

        // Orientable (furnace-style)
        gen.registerNorthDefaultHorizontalRotation(ModBlocks.MACHINE.get());

        // Variants (slab, stairs, wall)
        BlockStateModelGenerator.BlockTexturePool pool =
            gen.registerCubeAllModelTexturePool(ModBlocks.STONE_VARIANT.get());
        pool.slab(ModBlocks.STONE_SLAB.get());
        pool.stairs(ModBlocks.STONE_STAIRS.get());
        pool.wall(ModBlocks.STONE_WALL.get());

        // Special blocks
        gen.registerDoor(ModBlocks.CUSTOM_DOOR.get());
        gen.registerTrapdoor(ModBlocks.CUSTOM_TRAPDOOR.get());
    }

    @Override
    public void generateItemModels(ItemModelGenerator gen) {
        // Flat items
        gen.register(ModItems.EXAMPLE_ITEM.get(), Models.GENERATED);

        // Handheld (tools, weapons)
        gen.register(ModItems.EXAMPLE_SWORD.get(), Models.HANDHELD);
    }
}
```

## Recipe Provider

```java
public class ModRecipeProvider extends FabricRecipeProvider {

    public ModRecipeProvider(FabricDataOutput out, CompletableFuture<HolderLookup.Provider> reg) {
        super(out, reg);
    }

    @Override
    public void buildRecipes(RecipeOutput output) {
        // Shaped recipe
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.EXAMPLE.get())
            .pattern("###")
            .pattern("#X#")
            .pattern("###")
            .define('#', Items.COBBLESTONE)
            .define('X', Items.DIAMOND)
            .unlockedBy("has_diamond", has(Items.DIAMOND))
            .save(output);

        // Shapeless recipe
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.DUST.get(), 4)
            .requires(ModBlocks.EXAMPLE.get())
            .unlockedBy("has_block", has(ModBlocks.EXAMPLE.get()))
            .save(output);

        // Smelting
        SimpleCookingRecipeBuilder.smelting(
                Ingredient.of(ModBlocks.ORE.get()),
                RecipeCategory.MISC,
                ModItems.INGOT.get(),
                0.7f,  // XP
                200)   // Ticks
            .unlockedBy("has_ore", has(ModBlocks.ORE.get()))
            .save(output);

        // Blasting (faster smelting)
        SimpleCookingRecipeBuilder.blasting(
                Ingredient.of(ModBlocks.ORE.get()),
                RecipeCategory.MISC,
                ModItems.INGOT.get(),
                0.7f,
                100)
            .unlockedBy("has_ore", has(ModBlocks.ORE.get()))
            .save(output, modLoc("ingot_from_blasting"));

        // Stonecutting
        SingleItemRecipeBuilder.stonecutting(
                Ingredient.of(ModBlocks.STONE_VARIANT.get()),
                RecipeCategory.BUILDING_BLOCKS,
                ModBlocks.STONE_SLAB.get(),
                2)
            .unlockedBy("has_stone", has(ModBlocks.STONE_VARIANT.get()))
            .save(output, modLoc("stone_slab_stonecutting"));

        // Smithing
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItems.DIAMOND_TOOL.get()),
                Ingredient.of(Items.NETHERITE_INGOT),
                RecipeCategory.TOOLS,
                ModItems.NETHERITE_TOOL.get())
            .unlocks("has_netherite", has(Items.NETHERITE_INGOT))
            .save(output, modLoc("netherite_tool_smithing"));
    }

    private ResourceLocation modLoc(String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
    }
}
```

## Loot Table Provider

```java
public class ModBlockLootProvider extends FabricBlockLootTableProvider {

    public ModBlockLootProvider(FabricDataOutput out, CompletableFuture<HolderLookup.Provider> reg) {
        super(out, reg);
    }

    @Override
    public void generate() {
        // Drop itself
        dropSelf(ModBlocks.EXAMPLE_BLOCK.get());

        // Drop different item
        dropOther(ModBlocks.INFESTED.get(), Items.STONE);

        // Ore drops (with fortune)
        add(ModBlocks.ORE.get(),
            createOreDrop(ModBlocks.ORE.get(), ModItems.GEM.get()));

        // Silk touch only
        add(ModBlocks.GLASS_VARIANT.get(),
            createSilkTouchOnlyTable(ModBlocks.GLASS_VARIANT.get()));

        // Silk touch or else
        add(ModBlocks.SPAWNER.get(),
            createSilkTouchOrShearsDispatchTable(
                ModBlocks.SPAWNER.get(),
                applyExplosionCondition(ModBlocks.SPAWNER.get(),
                    LootItem.lootTableItem(Items.EXPERIENCE_BOTTLE))));

        // Slab (drops 2 when double)
        add(ModBlocks.SLAB.get(), createSlabItemTable(ModBlocks.SLAB.get()));

        // Door (only one item for two blocks)
        add(ModBlocks.DOOR.get(), createDoorTable(ModBlocks.DOOR.get()));
    }
}
```

## Tag Provider

```java
public class ModBlockTagProvider extends FabricTagProvider.BlockTagProvider {

    public ModBlockTagProvider(FabricDataOutput out, CompletableFuture<HolderLookup.Provider> reg) {
        super(out, reg);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Add to vanilla tags
        getOrCreateTagBuilder(BlockTags.MINEABLE_WITH_PICKAXE)
            .add(ModBlocks.EXAMPLE_BLOCK.get())
            .add(ModBlocks.ORE.get())
            .add(ModBlocks.MACHINE.get());

        getOrCreateTagBuilder(BlockTags.NEEDS_IRON_TOOL)
            .add(ModBlocks.ORE.get());

        getOrCreateTagBuilder(BlockTags.NEEDS_DIAMOND_TOOL)
            .add(ModBlocks.MACHINE.get());

        // Custom mod tags
        getOrCreateTagBuilder(ModTags.Blocks.MACHINES)
            .add(ModBlocks.MACHINE.get());

        // Add other mod's blocks (optional)
        getOrCreateTagBuilder(ModTags.Blocks.MACHINES)
            .addOptional(ResourceLocation.fromNamespaceAndPath("othermod", "their_machine"));
    }
}

public class ModItemTagProvider extends FabricTagProvider.ItemTagProvider {

    public ModItemTagProvider(FabricDataOutput out, CompletableFuture<HolderLookup.Provider> reg,
                              BlockTagProvider blockTags) {
        super(out, reg, blockTags);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Copy block tags to items
        copy(BlockTags.PLANKS, ItemTags.PLANKS);
        copy(ModTags.Blocks.MACHINES, ModTags.Items.MACHINES);

        // Item-only tags
        getOrCreateTagBuilder(ItemTags.SWORDS)
            .add(ModItems.CUSTOM_SWORD.get());

        getOrCreateTagBuilder(ModTags.Items.FUELS)
            .add(ModItems.CUSTOM_FUEL.get());
    }
}
```

## Custom Tags Definition

```java
public class ModTags {

    public static class Blocks {
        public static final TagKey<Block> MACHINES = create("machines");
        public static final TagKey<Block> ORES = create("ores");

        private static TagKey<Block> create(String name) {
            return TagKey.create(Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, name));
        }
    }

    public static class Items {
        public static final TagKey<Item> MACHINES = create("machines");
        public static final TagKey<Item> FUELS = create("fuels");

        private static TagKey<Item> create(String name) {
            return TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, name));
        }
    }
}
```

## Running Data Generation

```bash
# Fabric
./gradlew :fabric:runDatagen

# Forge
./gradlew :forge:runData

# NeoForge
./gradlew :neoforge:runData
```

Output goes to `src/main/generated/` - include in resources.

## Common Tool Tags

| Tag | Purpose |
|-----|---------|
| `MINEABLE_WITH_PICKAXE` | Pickaxe efficient |
| `MINEABLE_WITH_AXE` | Axe efficient |
| `MINEABLE_WITH_SHOVEL` | Shovel efficient |
| `MINEABLE_WITH_HOE` | Hoe efficient |
| `NEEDS_STONE_TOOL` | Stone tier minimum |
| `NEEDS_IRON_TOOL` | Iron tier minimum |
| `NEEDS_DIAMOND_TOOL` | Diamond tier minimum |

## Best Practices

1. **Always use datagen** - Don't hand-write JSON for standard content
2. **Run before building** - Ensure generated files are up-to-date
3. **Use `unlockedBy`** - Gate recipes behind obtainable items
4. **Copy tags** - Keep block/item tags synchronized
5. **Test output** - Verify generated JSON is correct
6. **Commit generated files** - Include in version control
