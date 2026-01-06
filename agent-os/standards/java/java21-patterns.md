# Java 21 Patterns for Minecraft Modding

This document defines standards for using modern Java 21 features effectively in Minecraft mod development.

## Records

Use records for immutable data carriers:

### Network Payloads
```java
public record BlockUpdatePayload(BlockPos pos, BlockState state, int flags)
    implements CustomPacketPayload {

    public static final Type<BlockUpdatePayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "block_update"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
```

### Configuration Data
```java
public record MachineConfig(int processingTime, float energyMultiplier, boolean requiresRedstone) {
    // Compact constructor for validation
    public MachineConfig {
        if (processingTime <= 0)
            throw new IllegalArgumentException("Processing time must be positive");
    }

    public static final MachineConfig DEFAULT = new MachineConfig(200, 1.0f, false);
}
```

### Result Types
```java
public record ProcessingResult(ItemStack output, int experience, boolean success) {
    public static ProcessingResult failure() {
        return new ProcessingResult(ItemStack.EMPTY, 0, false);
    }
}
```

### When NOT to use records
- Mutable state (block entities, entities)
- Complex inheritance hierarchies
- Classes requiring custom equals/hashCode

## Sealed Classes

Restrict inheritance for type hierarchies:

```java
public sealed interface MachineState
    permits MachineState.Idle, MachineState.Processing, MachineState.Error {

    record Idle() implements MachineState {}
    record Processing(int progress, int maxProgress) implements MachineState {}
    record Error(String message) implements MachineState {}
}
```

### Benefits
- Exhaustive switch expressions
- Clear API boundaries
- Better pattern matching support

## Pattern Matching

### Switch Expressions
```java
public String describeBlock(Block block) {
    return switch (block) {
        case SlabBlock s -> "slab";
        case StairBlock s -> "stairs";
        case WallBlock w -> "wall";
        case FenceBlock f -> "fence";
        case null -> "unknown";
        default -> "block";
    };
}
```

### Guarded Patterns
```java
public int getHarvestLevel(BlockState state) {
    return switch (state.getBlock()) {
        case OreBlock ore when state.is(BlockTags.NEEDS_DIAMOND_TOOL) -> 3;
        case OreBlock ore when state.is(BlockTags.NEEDS_IRON_TOOL) -> 2;
        case OreBlock ore -> 0;
        default -> -1;
    };
}
```

### Record Patterns
```java
public void handleState(MachineState state) {
    switch (state) {
        case MachineState.Idle() -> handleIdle();
        case MachineState.Processing(var progress, var max) -> handleProcessing(progress, max);
        case MachineState.Error(var msg) -> handleError(msg);
    }
}
```

### instanceof Pattern Matching
```java
// Old
if (entity instanceof Player) {
    Player player = (Player) entity;
    player.sendMessage(...);
}

// Modern
if (entity instanceof Player player) {
    player.sendMessage(...);
}

// With conditions
if (entity instanceof LivingEntity living && living.getHealth() < 10) {
    applyHealing(living);
}
```

## Optional Usage

```java
// Return Optional for "may not exist"
public Optional<BlockEntity> getBlockEntityAt(Level level, BlockPos pos) {
    return Optional.ofNullable(level.getBlockEntity(pos));
}

// Chain operations
public Optional<ItemStack> getPlayerTool(Entity entity) {
    return Optional.of(entity)
        .filter(Player.class::isInstance)
        .map(Player.class::cast)
        .map(Player::getMainHandItem)
        .filter(stack -> !stack.isEmpty());
}

// Terminal operations
ItemStack stack = getItem().orElse(ItemStack.EMPTY);
BlockEntity be = getBlockEntity().orElseThrow(() -> new IllegalStateException("Missing"));

// DON'T use Optional for fields
public class Machine {
    private ItemStack output = ItemStack.EMPTY;  // Not Optional<ItemStack>

    public Optional<ItemStack> getOutput() {
        return output.isEmpty() ? Optional.empty() : Optional.of(output);
    }
}
```

## Stream API

```java
// Filter entities
List<Monster> nearbyMonsters = level.getEntitiesOfClass(Monster.class, box)
    .stream()
    .filter(Entity::isAlive)
    .filter(m -> m.distanceToSqr(pos) < 100)
    .sorted(Comparator.comparingDouble(m -> m.distanceToSqr(pos)))
    .limit(10)
    .toList();

// Process inventory
Map<Item, Long> itemCounts = IntStream.range(0, container.getContainerSize())
    .mapToObj(container::getItem)
    .filter(stack -> !stack.isEmpty())
    .collect(Collectors.groupingBy(
        ItemStack::getItem,
        Collectors.summingLong(ItemStack::getCount)));

// Find first matching
Optional<BlockPos> ore = BlockPos.betweenClosedStream(area)
    .filter(p -> level.getBlockState(p).is(BlockTags.COAL_ORES))
    .findFirst();
```

## Text Blocks

```java
// JSON templates
String modelJson = """
    {
      "parent": "minecraft:block/cube_all",
      "textures": {
        "all": "%s:block/%s"
      }
    }
    """.formatted(Constants.MOD_ID, blockName);

// Multiline messages
String help = """
    Example Mod Commands:
    - /examplemod help
    - /examplemod config
    - /examplemod reload
    """;
```

## Var for Local Variables

```java
// Use when type is obvious
var player = (Player) entity;
var inventory = player.getInventory();
var stack = inventory.getItem(0);

// Use for complex generics
var entitiesByType = new HashMap<EntityType<?>, List<Entity>>();

// DON'T use when type matters for understanding
ResourceLocation location = ResourceLocation.fromNamespaceAndPath(modId, path);
```

## Try-With-Resources

```java
public void saveData(Path path, CompoundTag data) throws IOException {
    try (var output = Files.newOutputStream(path)) {
        NbtIo.writeCompressed(data, output);
    }
}

// Multiple resources
try (var reader = Files.newBufferedReader(input);
     var writer = Files.newBufferedWriter(output)) {
    // Process
}
```

## Functional Interfaces

```java
// Use built-in interfaces
public void processItems(Container container,
                         Predicate<ItemStack> filter,
                         Consumer<ItemStack> action) {
    for (int i = 0; i < container.getContainerSize(); i++) {
        ItemStack stack = container.getItem(i);
        if (filter.test(stack)) {
            action.accept(stack);
        }
    }
}

// Usage
processItems(inventory,
    stack -> stack.is(Items.DIAMOND),
    stack -> stack.shrink(1));
```

## Best Practices Summary

| Feature | Use For |
|---------|---------|
| Records | DTOs, payloads, configs |
| Sealed Classes | Type hierarchies, states |
| Pattern Matching | Type checking, destructuring |
| Optional | Return types for "maybe" |
| Streams | Collection transformations |
| Text Blocks | JSON, multiline strings |
| Var | Obvious types, complex generics |
