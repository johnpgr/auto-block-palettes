---
name: Minecraft Registry System
description: Register blocks, items, entities, block entities, sounds, particles, and other game content correctly across Fabric, Forge, and NeoForge mod loaders. Use this skill when adding new game content to your mod. Apply when working with DeferredRegister, Registry, or ResourceLocation/ResourceKey. Essential when creating custom blocks, items, entities, or any registrable game objects. Use when setting up registration events or understanding the registration lifecycle.
---

## Standards Reference

For detailed standards, refer to: [Registry System Standards](../../../agent-os/standards/development/registry.md)

## When to use this skill:

- When creating new blocks, items, entities, or block entities
- When registering sounds, particles, or other game content
- When working with DeferredRegister or platform-specific registration
- When using ResourceLocation or ResourceKey to identify game objects
- When setting up creative mode tabs for items
- When registering custom recipes, loot tables, or data-driven content
- When understanding registration order and lifecycle events
- When creating registries for custom game systems

## Registration Across Loaders

Each mod loader has a different registration API. In a multiloader project, use the service pattern:

### Common Interface

```java
// common/src/main/java/.../platform/services/IRegistryHelper.java
public interface IRegistryHelper {
    <T> Supplier<T> registerBlock(String name, Supplier<T> block);
    <T> Supplier<T> registerItem(String name, Supplier<T> item);
    <T> Supplier<T> registerBlockEntity(String name, Supplier<T> blockEntity);
    void registerCreativeTab(String name, Supplier<ItemStack> icon, Consumer<CreativeModeTab.Builder> configure);
}
```

### Fabric Implementation

```java
// fabric/src/main/java/.../platform/FabricRegistryHelper.java
public class FabricRegistryHelper implements IRegistryHelper {

    @Override
    public <T> Supplier<T> registerBlock(String name, Supplier<T> block) {
        T registered = Registry.register(
            BuiltInRegistries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, name),
            (Block) block.get()
        );
        return () -> (T) registered;
    }

    @Override
    public <T> Supplier<T> registerItem(String name, Supplier<T> item) {
        T registered = Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, name),
            (Item) item.get()
        );
        return () -> (T) registered;
    }
}
```

### NeoForge Implementation

```java
// neoforge/src/main/java/.../platform/NeoForgeRegistryHelper.java
public class NeoForgeRegistryHelper implements IRegistryHelper {

    private static final DeferredRegister.Blocks BLOCKS =
        DeferredRegister.createBlocks(Constants.MOD_ID);
    private static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(Constants.MOD_ID);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }

    @Override
    public <T> Supplier<T> registerBlock(String name, Supplier<T> block) {
        return (Supplier<T>) BLOCKS.register(name, () -> (Block) block.get());
    }

    @Override
    public <T> Supplier<T> registerItem(String name, Supplier<T> item) {
        return (Supplier<T>) ITEMS.register(name, () -> (Item) item.get());
    }
}
```

### Forge Implementation

```java
// forge/src/main/java/.../platform/ForgeRegistryHelper.java
public class ForgeRegistryHelper implements IRegistryHelper {

    private static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, Constants.MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, Constants.MOD_ID);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }

    @Override
    public <T> Supplier<T> registerBlock(String name, Supplier<T> block) {
        return (Supplier<T>) BLOCKS.register(name, () -> (Block) block.get());
    }
}
```

## Common Registration Pattern

```java
// common/src/main/java/.../ModBlocks.java
public class ModBlocks {

    public static Supplier<Block> EXAMPLE_BLOCK;

    public static void register() {
        IRegistryHelper registry = Services.REGISTRY;

        EXAMPLE_BLOCK = registry.registerBlock("example_block",
            () -> new Block(BlockBehaviour.Properties.of()
                .strength(3.0f)
                .requiresCorrectToolForDrops()
            ));
    }
}

// common/src/main/java/.../ModItems.java
public class ModItems {

    public static Supplier<Item> EXAMPLE_ITEM;
    public static Supplier<BlockItem> EXAMPLE_BLOCK_ITEM;

    public static void register() {
        IRegistryHelper registry = Services.REGISTRY;

        EXAMPLE_ITEM = registry.registerItem("example_item",
            () -> new Item(new Item.Properties()));

        // BlockItem for the block
        EXAMPLE_BLOCK_ITEM = registry.registerItem("example_block",
            () -> new BlockItem(ModBlocks.EXAMPLE_BLOCK.get(), new Item.Properties()));
    }
}
```

## Registry Types

Common registries in Minecraft:

| Registry | Content | Key Class |
|----------|---------|-----------|
| `BLOCK` | Blocks | `Block` |
| `ITEM` | Items | `Item` |
| `ENTITY_TYPE` | Entities | `EntityType<?>` |
| `BLOCK_ENTITY_TYPE` | Block Entities | `BlockEntityType<?>` |
| `MENU` | Container Menus | `MenuType<?>` |
| `SOUND_EVENT` | Sounds | `SoundEvent` |
| `PARTICLE_TYPE` | Particles | `ParticleType<?>` |
| `CREATIVE_MODE_TAB` | Creative Tabs | `CreativeModeTab` |
| `RECIPE_TYPE` | Recipe Types | `RecipeType<?>` |
| `RECIPE_SERIALIZER` | Recipe Serializers | `RecipeSerializer<?>` |

## ResourceLocation and ResourceKey

```java
// Creating a ResourceLocation (namespace:path)
ResourceLocation myBlock = ResourceLocation.fromNamespaceAndPath("mymod", "example_block");
// Result: "mymod:example_block"

// Using minecraft namespace
ResourceLocation vanillaStone = ResourceLocation.withDefaultNamespace("stone");
// Result: "minecraft:stone"

// ResourceKey for registry entries
ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, myBlock);

// Getting registered objects
Block block = BuiltInRegistries.BLOCK.get(myBlock);
Optional<Block> maybeBlock = BuiltInRegistries.BLOCK.getOptional(myBlock);
```

## Registration Order

Registration happens in a specific order:

1. **Blocks** - Must be registered first
2. **Items** - BlockItems need their blocks registered
3. **Block Entities** - Need their blocks registered
4. **Entities** - Can reference items/blocks
5. **Creative Tabs** - Need items registered

```java
// In your mod initialization
public static void init() {
    ModBlocks.register();    // First
    ModItems.register();     // Second (depends on blocks for BlockItems)
    ModBlockEntities.register(); // Third
    ModEntities.register();  // Fourth
}
```

## Block Entity Registration

```java
public class ModBlockEntities {

    public static Supplier<BlockEntityType<ExampleBlockEntity>> EXAMPLE_BLOCK_ENTITY;

    public static void register() {
        EXAMPLE_BLOCK_ENTITY = Services.REGISTRY.registerBlockEntity("example_block_entity",
            () -> BlockEntityType.Builder.of(
                ExampleBlockEntity::new,
                ModBlocks.EXAMPLE_BLOCK.get()
            ).build(null));
    }
}
```

## Entity Registration

```java
public class ModEntities {

    public static Supplier<EntityType<ExampleEntity>> EXAMPLE_ENTITY;

    public static void register() {
        EXAMPLE_ENTITY = Services.REGISTRY.registerEntity("example_entity",
            () -> EntityType.Builder.of(ExampleEntity::new, MobCategory.CREATURE)
                .sized(0.9f, 1.3f)
                .clientTrackingRange(10)
                .build("example_entity"));
    }
}
```

## Best Practices

1. **Use Supplier<T>** - Don't hold direct references to registered objects until after registration
2. **Static initialization** - Register in static methods called during mod init, not static initializers
3. **Consistent naming** - Use snake_case for registry names matching resource paths
4. **BlockItems** - Always create a BlockItem for blocks that should appear in inventory
5. **Deferred access** - Use `.get()` on suppliers only after registration completes
