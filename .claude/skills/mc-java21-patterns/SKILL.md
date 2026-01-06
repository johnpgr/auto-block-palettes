---
name: Minecraft Java 21 Patterns
description: Apply modern Java 21 features and best practices in Minecraft mod development. Use this skill when writing new Java code, refactoring existing code, or deciding on data structures. Apply when using records, sealed classes, pattern matching, or other Java 17-21 features. Essential for writing clean, efficient, and maintainable mod code that leverages the latest Java capabilities.
---

## Standards Reference

For detailed standards, refer to: [Java 21 Patterns Standards](../../../agent-os/standards/java/java21-patterns.md)

## When to use this skill:

- When creating data classes or DTOs (consider records)
- When defining type hierarchies (consider sealed classes)
- When writing switch statements or instanceof checks (use pattern matching)
- When working with Optional values and null handling
- When using streams, lambdas, or functional interfaces
- When managing resources (try-with-resources)
- When writing text blocks for JSON/multiline strings
- When choosing between var and explicit types
- When refactoring older Java code to modern idioms

## Records for Data Classes

Records are perfect for immutable data carriers:

```java
// Network payloads
public record BlockUpdatePayload(BlockPos pos, BlockState state, int flags)
    implements CustomPacketPayload {

    public static final Type<BlockUpdatePayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "block_update")
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

// Configuration data
public record MachineConfig(int processingTime, float energyMultiplier, boolean requiresRedstone) {
    // Compact constructor for validation
    public MachineConfig {
        if (processingTime <= 0) throw new IllegalArgumentException("Processing time must be positive");
        if (energyMultiplier < 0) throw new IllegalArgumentException("Energy multiplier cannot be negative");
    }

    // Default config
    public static final MachineConfig DEFAULT = new MachineConfig(200, 1.0f, false);
}

// Event data
public record ProcessingResult(ItemStack output, int experience, boolean success) {}

// With additional methods
public record BlockPosition(int x, int y, int z) {
    public BlockPos toBlockPos() {
        return new BlockPos(x, y, z);
    }

    public static BlockPosition fromBlockPos(BlockPos pos) {
        return new BlockPosition(pos.getX(), pos.getY(), pos.getZ());
    }

    public BlockPosition offset(int dx, int dy, int dz) {
        return new BlockPosition(x + dx, y + dy, z + dz);
    }
}
```

## Sealed Classes for Type Hierarchies

Sealed classes restrict which classes can extend them:

```java
// Define a restricted hierarchy for machine states
public sealed interface MachineState
    permits MachineState.Idle, MachineState.Processing, MachineState.Outputting, MachineState.Error {

    record Idle() implements MachineState {}

    record Processing(int progress, int maxProgress, ItemStack input) implements MachineState {
        public float progressPercent() {
            return maxProgress > 0 ? (float) progress / maxProgress : 0;
        }
    }

    record Outputting(ItemStack result) implements MachineState {}

    record Error(String message) implements MachineState {}
}

// Usage with pattern matching
public void renderMachineState(MachineState state) {
    switch (state) {
        case MachineState.Idle() -> renderIdleState();
        case MachineState.Processing(int progress, int max, var input) -> renderProgress(progress, max);
        case MachineState.Outputting(var result) -> renderOutput(result);
        case MachineState.Error(var msg) -> renderError(msg);
    }
}

// Sealed class hierarchy for custom recipe types
public sealed abstract class ModRecipe extends Recipe<Container>
    permits ProcessingRecipe, InfusingRecipe, CombiningRecipe {

    protected final ResourceLocation id;
    protected final ItemStack result;

    protected ModRecipe(ResourceLocation id, ItemStack result) {
        this.id = id;
        this.result = result;
    }
}
```

## Pattern Matching

### Switch Expressions

```java
// Pattern matching with switch (Java 21)
public String getBlockCategory(Block block) {
    return switch (block) {
        case SlabBlock slab -> "slab";
        case StairBlock stairs -> "stairs";
        case WallBlock wall -> "wall";
        case FenceBlock fence -> "fence";
        case DoorBlock door -> "door";
        case null -> "unknown";
        default -> "block";
    };
}

// Guarded patterns
public int getHarvestLevel(BlockState state) {
    return switch (state.getBlock()) {
        case OreBlock ore when state.is(BlockTags.NEEDS_DIAMOND_TOOL) -> 3;
        case OreBlock ore when state.is(BlockTags.NEEDS_IRON_TOOL) -> 2;
        case OreBlock ore when state.is(BlockTags.NEEDS_STONE_TOOL) -> 1;
        case OreBlock ore -> 0;
        default -> -1;
    };
}

// Record patterns
public void handlePayload(CustomPacketPayload payload) {
    switch (payload) {
        case BlockUpdatePayload(var pos, var state, var flags) ->
            handleBlockUpdate(pos, state, flags);
        case EntityDataPayload(var entityId, var data) ->
            handleEntityData(entityId, data);
        default -> Constants.LOG.warn("Unknown payload type: {}", payload.type());
    }
}
```

### instanceof Pattern Matching

```java
// Old way
if (entity instanceof Player) {
    Player player = (Player) entity;
    player.sendMessage(...);
}

// Modern way
if (entity instanceof Player player) {
    player.sendMessage(...);
}

// With negation
if (!(entity instanceof Player player)) {
    return;
}
// player is in scope here

// Combined conditions
if (entity instanceof LivingEntity living && living.getHealth() < 10) {
    applyHealingEffect(living);
}
```

## Optional Best Practices

```java
// Avoid null, use Optional for "may not exist" values
public Optional<BlockEntity> getBlockEntityAt(Level level, BlockPos pos) {
    return Optional.ofNullable(level.getBlockEntity(pos));
}

// Chain operations
public Optional<ItemStack> getPlayerMainHandItem(Entity entity) {
    return Optional.of(entity)
        .filter(e -> e instanceof Player)
        .map(e -> (Player) e)
        .map(Player::getMainHandItem)
        .filter(stack -> !stack.isEmpty());
}

// Use orElse, orElseGet, orElseThrow appropriately
ItemStack stack = inventory.getItem(slot).orElse(ItemStack.EMPTY);
BlockEntity be = level.getBlockEntity(pos).orElseThrow(() ->
    new IllegalStateException("Expected block entity at " + pos));

// Don't use Optional for fields - use it for return types
public class Machine {
    private ItemStack output = ItemStack.EMPTY;  // Not Optional<ItemStack>

    public Optional<ItemStack> getOutputIfPresent() {
        return output.isEmpty() ? Optional.empty() : Optional.of(output);
    }
}
```

## Stream API for Collections

```java
// Filter and collect nearby entities
List<Monster> nearbyMonsters = level.getEntitiesOfClass(Monster.class, searchBox)
    .stream()
    .filter(m -> m.isAlive())
    .filter(m -> m.distanceToSqr(centerPos) < 100)
    .sorted(Comparator.comparingDouble(m -> m.distanceToSqr(centerPos)))
    .limit(10)
    .toList();

// Transform inventory contents
List<ItemStack> nonEmptyStacks = IntStream.range(0, inventory.getContainerSize())
    .mapToObj(inventory::getItem)
    .filter(stack -> !stack.isEmpty())
    .toList();

// Count items by type
Map<Item, Long> itemCounts = IntStream.range(0, inventory.getContainerSize())
    .mapToObj(inventory::getItem)
    .filter(stack -> !stack.isEmpty())
    .collect(Collectors.groupingBy(ItemStack::getItem, Collectors.summingLong(ItemStack::getCount)));

// Find first matching
Optional<BlockPos> foundOre = BlockPos.betweenClosedStream(searchArea)
    .filter(pos -> level.getBlockState(pos).is(BlockTags.COAL_ORES))
    .findFirst();
```

## Text Blocks for JSON/Resources

```java
// Useful for generating JSON or config templates
String modelJson = """
    {
      "parent": "minecraft:block/cube_all",
      "textures": {
        "all": "%s:block/%s"
      }
    }
    """.formatted(Constants.MOD_ID, blockName);

// Multiline messages
String helpText = """
    Welcome to Example Mod!

    Commands:
    - /examplemod help - Show this message
    - /examplemod config - Open configuration
    - /examplemod reload - Reload data
    """;
```

## Var for Local Variables

```java
// Use var when type is obvious from context
var player = (Player) entity;
var inventory = player.getInventory();
var stack = inventory.getItem(0);

// Use explicit types when clarity matters
ResourceLocation location = ResourceLocation.fromNamespaceAndPath(modId, path);
Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();

// Especially useful with generics
var entitiesByType = new HashMap<EntityType<?>, List<Entity>>();
// Instead of: HashMap<EntityType<?>, List<Entity>> entitiesByType = new HashMap<>();
```

## Try-With-Resources

```java
// Automatic resource management
public void saveData(Path path, CompoundTag data) throws IOException {
    try (var output = Files.newOutputStream(path)) {
        NbtIo.writeCompressed(data, output);
    }
}

// Multiple resources
public void convertFile(Path input, Path output) throws IOException {
    try (var reader = Files.newBufferedReader(input);
         var writer = Files.newBufferedWriter(output)) {
        reader.lines()
            .map(this::processLine)
            .forEach(line -> {
                try { writer.write(line); writer.newLine(); }
                catch (IOException e) { throw new UncheckedIOException(e); }
            });
    }
}
```

## Functional Interfaces

```java
// Use built-in functional interfaces
public void processItems(Container container, Predicate<ItemStack> filter, Consumer<ItemStack> action) {
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

// Supplier for lazy initialization
private final Supplier<ExpensiveObject> lazyObject = Suppliers.memoize(() -> {
    return new ExpensiveObject();
});
```
