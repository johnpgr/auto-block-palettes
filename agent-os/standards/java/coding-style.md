# Java Coding Style Standards for Minecraft Mods

This document defines coding style standards for Java code in Minecraft mod development.

## Naming Conventions

### Classes
- **PascalCase** for all class names
- Descriptive, noun-based names
- Prefix mixins with `Mixin`: `MixinMinecraft`

```java
public class ProcessingMachine { }
public class ExampleBlockEntity { }
public abstract class MixinEntity { }
```

### Interfaces
- **PascalCase**, prefix with `I` for service interfaces

```java
public interface IPlatformHelper { }
public interface IEnergyStorage { }
```

### Methods
- **camelCase** for all methods
- Verb-based names for actions
- `get`/`set`/`is`/`has` prefixes for accessors

```java
public void processItem() { }
public ItemStack getOutput() { }
public boolean isProcessing() { }
public boolean hasEnergy() { }
```

### Variables and Fields
- **camelCase** for instance/local variables
- **SCREAMING_SNAKE_CASE** for constants
- **camelCase** for static non-final fields

```java
private int processingTime;
private static final int MAX_ENERGY = 10000;
private static int instanceCount;
public static final String MOD_ID = "examplemod";
```

### Packages
- **lowercase** with dots
- Follow structure: `com.example.modid.category`

```
com.example.examplemod.block
com.example.examplemod.item
com.example.examplemod.entity
com.example.examplemod.mixin
com.example.examplemod.platform.services
```

## Code Organization

### Class Structure Order
```java
public class ExampleClass {
    // 1. Static constants
    public static final int CONSTANT = 1;

    // 2. Static fields
    private static int counter;

    // 3. Instance fields
    private final String name;
    private int value;

    // 4. Constructors
    public ExampleClass(String name) {
        this.name = name;
    }

    // 5. Static methods
    public static ExampleClass create() {
        return new ExampleClass("default");
    }

    // 6. Public methods
    public void process() { }

    // 7. Protected methods
    protected void onProcess() { }

    // 8. Private methods
    private void doInternal() { }

    // 9. Inner classes/interfaces
    public interface Listener { }
}
```

### Import Organization
1. `java.*` imports
2. `javax.*` imports
3. Third-party imports (Mixin, etc.)
4. Minecraft imports (`net.minecraft.*`)
5. Mod loader imports (Fabric/Forge/NeoForge)
6. Project imports

```java
import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.api.ModInitializer;

import com.example.examplemod.Constants;
```

## Formatting

### Braces
- Opening brace on same line
- Closing brace on own line
- Always use braces, even for single statements

```java
// Good
if (condition) {
    doSomething();
}

// Bad
if (condition)
    doSomething();
```

### Indentation
- 4 spaces (not tabs)
- Continuation indent: 8 spaces

```java
public void method(String longParameter1,
        String longParameter2,
        String longParameter3) {
    // Body at normal indent
}
```

### Line Length
- Maximum 120 characters
- Break long lines at logical points

```java
// Good
ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
    Constants.MOD_ID, "example_block");

// Also good
ResourceLocation location =
    ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "example_block");
```

### Spacing
```java
// Operators
int result = a + b * c;

// Control structures
if (condition) {
    // body
}

for (int i = 0; i < 10; i++) {
    // body
}

// Method calls
method(arg1, arg2, arg3);

// No space inside parentheses
if (condition) { }  // Good
if ( condition ) { }  // Bad
```

## Documentation

### When to Document
- Public APIs
- Complex algorithms
- Non-obvious behavior
- Workarounds and hacks

### Javadoc Format
```java
/**
 * Processes the given item stack in the machine.
 *
 * @param stack The item stack to process
 * @param simulate If true, don't actually modify anything
 * @return The processing result, or empty if cannot process
 */
public ProcessingResult process(ItemStack stack, boolean simulate) {
    // Implementation
}
```

### Inline Comments
```java
// Calculate energy cost based on recipe complexity
int energyCost = recipe.getComplexity() * BASE_ENERGY;

// HACK: Workaround for MC-12345
if (level.isClientSide()) {
    // Client-side fix for rendering issue
}
```

## Minecraft-Specific Conventions

### Resource Locations
```java
// Always use fromNamespaceAndPath
ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "name");

// Never concatenate strings
ResourceLocation bad = new ResourceLocation(MOD_ID + ":name"); // Bad
```

### Registry Names
- Always snake_case
- Match the field name concept

```java
// Registration name matches concept
registerBlock("processing_machine", ...);
registerItem("energy_crystal", ...);
```

### Constants Class
```java
public class Constants {
    public static final String MOD_ID = "examplemod";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    private Constants() { } // Prevent instantiation
}
```

### Null Handling
```java
// Prefer empty collections over null
public List<ItemStack> getOutputs() {
    return outputs != null ? outputs : List.of();
}

// Use @Nullable/@NotNull annotations
public void process(@Nullable ItemStack input) {
    if (input == null || input.isEmpty()) {
        return;
    }
    // Process
}
```

## Error Handling

### Exceptions
```java
// Throw specific exceptions with context
if (pos == null) {
    throw new IllegalArgumentException("Position cannot be null");
}

// Log and handle gracefully in event handlers
try {
    processItem(stack);
} catch (Exception e) {
    Constants.LOG.error("Failed to process item: {}", stack, e);
}
```

### Logging
```java
// Use parameterized messages
Constants.LOG.info("Processing {} items", count);
Constants.LOG.debug("Item processed: {}", stack);
Constants.LOG.error("Failed to load config", exception);

// Don't log in hot paths (tick methods)
```

## Performance Considerations

### Avoid in Tick Methods
- Object allocation
- Stream operations on large collections
- Logging
- String concatenation

### Cache When Possible
```java
// Cache recipe lookups
private Recipe<?> cachedRecipe;
private ItemStack lastInput = ItemStack.EMPTY;

public Recipe<?> getRecipe(ItemStack input) {
    if (!ItemStack.matches(input, lastInput)) {
        cachedRecipe = findRecipe(input);
        lastInput = input.copy();
    }
    return cachedRecipe;
}
```

### Use Appropriate Collections
```java
// For iteration order
List<ItemStack> outputs = new ArrayList<>();

// For fast lookup
Set<ResourceLocation> validRecipes = new HashSet<>();

// For key-value with fast access
Map<BlockPos, BlockEntity> cache = new HashMap<>();
```
