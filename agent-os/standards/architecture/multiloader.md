# Multiloader Architecture Standards

This document defines the standards for structuring code in a multiloader Minecraft mod project that supports Fabric, Forge, and NeoForge simultaneously.

## Project Structure

```
project/
├── common/                 # Shared code (vanilla MC + abstractions)
│   └── src/main/java/
│       └── com/example/mod/
│           ├── CommonClass.java      # Shared initialization
│           ├── Constants.java        # Mod constants (MOD_ID, LOG)
│           ├── platform/
│           │   ├── Services.java     # Service loader access
│           │   └── services/         # Service interfaces
│           ├── block/                # Block classes
│           ├── item/                 # Item classes
│           ├── entity/               # Entity classes
│           └── mixin/                # Common mixins
├── fabric/                 # Fabric-specific code
│   └── src/main/java/
│       └── com/example/mod/
│           ├── ExampleMod.java       # Fabric entry point
│           ├── ExampleModClient.java # Client entry point
│           ├── platform/             # Service implementations
│           └── mixin/                # Fabric-only mixins
├── forge/                  # Forge-specific code
│   └── src/main/java/
│       └── com/example/mod/
│           ├── ExampleMod.java       # @Mod entry point
│           ├── platform/             # Service implementations
│           └── mixin/                # Forge-only mixins
├── neoforge/               # NeoForge-specific code
│   └── src/main/java/
│       └── com/example/mod/
│           ├── ExampleMod.java       # @Mod entry point
│           ├── platform/             # Service implementations
│           └── mixin/                # NeoForge-only mixins
└── buildSrc/               # Gradle build configuration
```

## Module Responsibilities

### Common Module

The common module contains:
- **All game logic** that doesn't require loader-specific APIs
- **Data classes, records, enums** (pure Java)
- **Vanilla Minecraft API usage** (registries, components, etc.)
- **Service interfaces** for platform-specific operations
- **Block, Item, Entity behavior** (not registration)
- **Utility classes and helpers**

**Rules:**
1. NEVER import Fabric API, Forge, or NeoForge classes
2. Only use `net.minecraft.*` and standard Java classes
3. Define interfaces in `platform/services/` for anything platform-specific

### Loader Modules (fabric/, forge/, neoforge/)

Loader modules contain:
- **Entry points** that bootstrap common code
- **Service implementations** for platform interfaces
- **Event handlers** using loader-specific event systems
- **Registration code** using loader-specific registries
- **Platform-specific mixins**
- **Networking implementations**
- **Config implementations**

## Service Loader Pattern

### Defining Services

```java
// common/src/main/java/.../platform/services/IPlatformHelper.java
public interface IPlatformHelper {
    String getPlatformName();
    boolean isModLoaded(String modId);
    boolean isDevelopmentEnvironment();
}

// common/src/main/java/.../platform/Services.java
public class Services {
    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    public static <T> T load(Class<T> clazz) {
        return ServiceLoader.load(clazz)
            .findFirst()
            .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
    }
}
```

### Implementing Services

```java
// fabric/src/main/java/.../platform/FabricPlatformHelper.java
public class FabricPlatformHelper implements IPlatformHelper {
    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}
```

### Registering Services

Create a file at:
`fabric/src/main/resources/META-INF/services/com.example.mod.platform.services.IPlatformHelper`

Contents:
```
com.example.mod.platform.FabricPlatformHelper
```

## Entry Point Patterns

### Fabric

```java
// Main initialization
public class ExampleMod implements ModInitializer {
    @Override
    public void onInitialize() {
        CommonClass.init();
    }
}

// Client initialization (separate entrypoint)
public class ExampleModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientClass.initClient();
    }
}
```

### Forge

```java
@Mod(Constants.MOD_ID)
public class ExampleMod {
    public ExampleMod() {
        CommonClass.init();
    }
}
```

### NeoForge

```java
@Mod(Constants.MOD_ID)
public class ExampleMod {
    public ExampleMod(IEventBus modEventBus) {
        CommonClass.init();
        // Register to event bus if needed
    }
}
```

## Decision Matrix: Where Does Code Go?

| Code Type | Module | Reason |
|-----------|--------|--------|
| Game logic, math | `common/` | No loader dependencies |
| Block/Item behavior | `common/` | Vanilla API |
| Data classes, records | `common/` | Pure Java |
| Service interfaces | `common/platform/services/` | Abstraction layer |
| Event handling | loader-specific | Different event systems |
| Registration | loader-specific | Different registration APIs |
| Networking | loader-specific | Different networking APIs |
| Config | loader-specific | Different config libraries |
| Client rendering hooks | loader-specific | Different client APIs |

## Best Practices

1. **Maximize common code** - If it can use vanilla APIs, put it in common
2. **Use services for abstraction** - Don't duplicate logic across loaders
3. **Test on all platforms** - Compilation success doesn't guarantee runtime success
4. **Keep loader modules thin** - They should mainly delegate to common code
5. **Document platform differences** - Comment when behavior differs across loaders
