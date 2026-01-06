# Registry System Standards

This document defines standards for registering blocks, items, entities, and other game content in multiloader Minecraft mods.

## Registration Philosophy

1. **Define content in common** - Block/Item classes in common module
2. **Register per-loader** - Use loader-specific registration APIs
3. **Use Supplier<T>** - Don't hold direct references until registered
4. **Order matters** - Register blocks before items, items before entities

## Registration Order

```
1. Blocks          (must be first)
2. Items           (BlockItems need blocks)
3. Block Entities  (need their blocks)
4. Entities        (can reference items/blocks)
5. Sounds          (can be anytime)
6. Creative Tabs   (need items registered)
7. Recipes         (need items/blocks)
```

## Content Definition (Common Module)

### Blocks

```java
// common/src/main/java/.../block/ModBlocks.java
public class ModBlocks {
    public static Supplier<Block> EXAMPLE_BLOCK;
    public static Supplier<Block> MACHINE_BLOCK;

    public static void init() {
        // Called after registration to validate
    }
}
```

### Items

```java
// common/src/main/java/.../item/ModItems.java
public class ModItems {
    public static Supplier<Item> EXAMPLE_ITEM;
    public static Supplier<BlockItem> EXAMPLE_BLOCK_ITEM;

    public static void init() {
        // Called after registration
    }
}
```

## Fabric Registration

```java
public class FabricRegistration {

    public static void register() {
        // Blocks
        ModBlocks.EXAMPLE_BLOCK = registerBlock("example_block",
            () -> new Block(BlockBehaviour.Properties.of()
                .strength(3.0f)
                .requiresCorrectToolForDrops()));

        // Items
        ModItems.EXAMPLE_ITEM = registerItem("example_item",
            () -> new Item(new Item.Properties()));

        ModItems.EXAMPLE_BLOCK_ITEM = registerItem("example_block",
            () -> new BlockItem(ModBlocks.EXAMPLE_BLOCK.get(),
                new Item.Properties()));
    }

    private static <T extends Block> Supplier<T> registerBlock(String name, Supplier<T> block) {
        T registered = Registry.register(
            BuiltInRegistries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, name),
            block.get()
        );
        return () -> registered;
    }

    private static <T extends Item> Supplier<T> registerItem(String name, Supplier<T> item) {
        T registered = Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, name),
            item.get()
        );
        return () -> registered;
    }
}
```

## NeoForge Registration

```java
public class NeoForgeRegistration {

    public static final DeferredRegister.Blocks BLOCKS =
        DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(Constants.MOD_ID);

    public static void register(IEventBus eventBus) {
        // Register deferred registers
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);

        // Blocks
        ModBlocks.EXAMPLE_BLOCK = BLOCKS.register("example_block",
            () -> new Block(BlockBehaviour.Properties.of()
                .strength(3.0f)
                .requiresCorrectToolForDrops()));

        // Items
        ModItems.EXAMPLE_ITEM = ITEMS.register("example_item",
            () -> new Item(new Item.Properties()));

        ModItems.EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block",
            () -> new BlockItem(ModBlocks.EXAMPLE_BLOCK.get(),
                new Item.Properties()));
    }
}
```

## Forge Registration

```java
public class ForgeRegistration {

    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, Constants.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, Constants.MOD_ID);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);

        ModBlocks.EXAMPLE_BLOCK = BLOCKS.register("example_block",
            () -> new Block(BlockBehaviour.Properties.of()
                .strength(3.0f)));

        ModItems.EXAMPLE_ITEM = ITEMS.register("example_item",
            () -> new Item(new Item.Properties()));
    }
}
```

## Registry Types Reference

| Registry | Content Type |
|----------|--------------|
| `BLOCK` | Block |
| `ITEM` | Item |
| `ENTITY_TYPE` | EntityType<?> |
| `BLOCK_ENTITY_TYPE` | BlockEntityType<?> |
| `MENU` | MenuType<?> |
| `SOUND_EVENT` | SoundEvent |
| `PARTICLE_TYPE` | ParticleType<?> |
| `CREATIVE_MODE_TAB` | CreativeModeTab |
| `RECIPE_TYPE` | RecipeType<?> |
| `RECIPE_SERIALIZER` | RecipeSerializer<?> |
| `MOB_EFFECT` | MobEffect |
| `POTION` | Potion |
| `ENCHANTMENT` | Enchantment |

## ResourceLocation and ResourceKey

```java
// Creating ResourceLocation
ResourceLocation id = ResourceLocation.fromNamespaceAndPath("modid", "example");
// Result: "modid:example"

// Minecraft namespace
ResourceLocation vanilla = ResourceLocation.withDefaultNamespace("stone");
// Result: "minecraft:stone"

// ResourceKey for registry entries
ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);

// Looking up registered objects
Block block = BuiltInRegistries.BLOCK.get(id);
Optional<Block> maybe = BuiltInRegistries.BLOCK.getOptional(id);
```

## Block Entity Registration

```java
// Common
public class ModBlockEntities {
    public static Supplier<BlockEntityType<MachineBlockEntity>> MACHINE;
}

// Registration
ModBlockEntities.MACHINE = registerBlockEntity("machine",
    () -> BlockEntityType.Builder.of(
        MachineBlockEntity::new,
        ModBlocks.MACHINE_BLOCK.get()
    ).build(null));
```

## Entity Registration

```java
// Common
public class ModEntities {
    public static Supplier<EntityType<CustomEntity>> CUSTOM_ENTITY;
}

// Registration
ModEntities.CUSTOM_ENTITY = registerEntity("custom_entity",
    () -> EntityType.Builder.of(CustomEntity::new, MobCategory.CREATURE)
        .sized(0.9f, 1.3f)
        .clientTrackingRange(10)
        .build("custom_entity"));
```

## Creative Tab Registration

```java
// NeoForge/Forge
public static final DeferredRegister<CreativeModeTab> TABS =
    DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Constants.MOD_ID);

public static final Supplier<CreativeModeTab> EXAMPLE_TAB = TABS.register("main",
    () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup." + Constants.MOD_ID + ".main"))
        .icon(() -> new ItemStack(ModItems.EXAMPLE_ITEM.get()))
        .displayItems((params, output) -> {
            output.accept(ModItems.EXAMPLE_ITEM.get());
            output.accept(ModBlocks.EXAMPLE_BLOCK.get());
        })
        .build());
```

## Best Practices

1. **Use Supplier<T>** - Never store direct Block/Item references as static fields
2. **Snake_case names** - Registry names should match resource paths
3. **Create BlockItems** - Every placeable block needs a BlockItem
4. **Consistent mod ID** - Always use Constants.MOD_ID
5. **Validate after registration** - Call init() methods to verify suppliers work
6. **Group related content** - Keep ModBlocks, ModItems, etc. organized
