---
name: Minecraft Multiloader Architecture
description: Structure code correctly across common, Fabric, Forge, and NeoForge modules in a multiloader Minecraft mod project. Use this skill when creating new classes, deciding where code should live, implementing platform-specific features, or working with the service loader pattern. Apply when editing files in common/, fabric/, forge/, or neoforge/ directories. Essential when adding new game content (blocks, items, entities) that needs to work across all mod loaders. Use when implementing features that have different APIs on different loaders (events, networking, registries).
---

## Standards Reference

For detailed standards, refer to: [Multiloader Architecture Standards](../../../agent-os/standards/architecture/multiloader.md)

## When to use this skill:

- When creating new Java classes and deciding which module (common/fabric/forge/neoforge) they belong in
- When implementing features that need to work across all three mod loaders
- When using the service loader pattern to abstract platform-specific code
- When working with files in `common/src/`, `fabric/src/`, `forge/src/`, or `neoforge/src/`
- When adding new interfaces to `platform/services/` package
- When implementing platform-specific helpers in loader modules
- When registering game content that must be available on all platforms
- When deciding between using vanilla Minecraft APIs vs loader-specific APIs

## Multiloader Project Structure

This project uses the MultiLoader template pattern with four modules:

```
project/
├── common/          # Shared code - vanilla Minecraft + common abstractions
├── fabric/          # Fabric-specific implementation
├── forge/           # Forge-specific implementation
├── neoforge/        # NeoForge-specific implementation
└── buildSrc/        # Gradle build configuration
```

## Core Principles

### 1. Common Module First

**Always start in `common/`** - Put as much code as possible in the common module:

- Game logic, utilities, helpers
- Data classes, records, enums
- Vanilla Minecraft API usage (registries, components, etc.)
- Abstract interfaces for platform-specific features

```java
// common/src/main/java/.../
public class MyGameLogic {
    // This code works on ALL loaders
    public static void doSomething(Level level, BlockPos pos) {
        // Use vanilla Minecraft APIs only
    }
}
```

### 2. Service Loader Pattern

Use Java's ServiceLoader to abstract platform differences:

```java
// common - Define the interface
public interface IPlatformHelper {
    String getPlatformName();
    boolean isModLoaded(String modId);
    boolean isDevelopmentEnvironment();
    // Add methods for platform-specific operations
}

// common - Service loader
public class Services {
    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    public static <T> T load(Class<T> clazz) {
        return ServiceLoader.load(clazz)
            .findFirst()
            .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
    }
}
```

Each loader implements the interface:
```java
// fabric/src/main/java/.../platform/FabricPlatformHelper.java
public class FabricPlatformHelper implements IPlatformHelper {
    @Override
    public String getPlatformName() { return "Fabric"; }
    // ...
}
```

Register via META-INF/services:
```
// fabric/src/main/resources/META-INF/services/com.example.mod.platform.services.IPlatformHelper
com.example.mod.platform.FabricPlatformHelper
```

### 3. Loader-Specific Code

Only put code in loader modules when:
- Using loader-specific APIs (Forge events, Fabric API, NeoForge systems)
- Registration must use loader-specific mechanisms
- Implementing service interfaces
- Entry points differ between loaders

### 4. Entry Points

Each loader has its own entry point that bootstraps common code:

**Fabric:**
```java
public class ExampleMod implements ModInitializer {
    @Override
    public void onInitialize() {
        CommonClass.init();
    }
}
```

**Forge:**
```java
@Mod(Constants.MOD_ID)
public class ExampleMod {
    public ExampleMod() {
        CommonClass.init();
    }
}
```

**NeoForge:**
```java
@Mod(Constants.MOD_ID)
public class ExampleMod {
    public ExampleMod(IEventBus eventBus) {
        CommonClass.init();
    }
}
```

## Decision Guide: Where Does This Code Go?

| Code Type | Location | Reason |
|-----------|----------|--------|
| Game logic, math, utilities | `common/` | No loader dependencies |
| Data classes, records | `common/` | Pure Java |
| Vanilla registry access | `common/` | Vanilla API |
| Block/Item behavior logic | `common/` | Vanilla API |
| Event handling | loader-specific | Different event systems |
| Registration (DeferredRegister) | loader-specific | Different registration APIs |
| Networking packets | loader-specific | Different networking APIs |
| Config files | loader-specific | Different config libraries |
| Mod menu integration | loader-specific | Loader-specific APIs |

## Important Notes

1. **Never import loader classes in common** - The common module cannot see Fabric API, Forge, or NeoForge classes
2. **Use vanilla Minecraft APIs when possible** - They work everywhere
3. **Service loader for abstraction** - When you need loader-specific behavior, define an interface
4. **Test on all loaders** - Code compiling doesn't mean it works on all platforms
5. **Check API availability** - Some vanilla APIs behave differently or are unavailable in certain contexts
