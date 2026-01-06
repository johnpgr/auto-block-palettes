---
name: Minecraft Client/Server Separation
description: Properly separate client-side and server-side code in Minecraft mods to prevent crashes and ensure correct behavior. Use this skill when writing code that accesses rendering, GUI, input handling, or any client-only Minecraft classes. Apply when creating mixins that target client classes like Minecraft.class, Screen, or renderers. Essential when working with Level objects to ensure server-side code doesn't call client methods. Use when adding visual effects, particles, sounds, or any player-facing features. Critical for avoiding ClassNotFoundException on dedicated servers.
---

## Standards Reference

For detailed standards, refer to: [Client/Server Separation Standards](../../../agent-os/standards/architecture/client-server.md)

## When to use this skill:

- When writing code that uses `Minecraft.getInstance()` or client-only classes
- When creating mixins that target client-side classes (Minecraft, Screen, Renderer, etc.)
- When working with rendering, particles, sounds, or visual effects
- When handling keyboard/mouse input or GUI screens
- When checking `level.isClientSide()` for side-specific logic
- When configuring mixin JSON files with client/server/common sections
- When using `@OnlyIn(Dist.CLIENT)` or `@Environment(EnvType.CLIENT)` annotations
- When code needs to behave differently on integrated vs dedicated servers

## The Two Sides of Minecraft

Minecraft runs as two logical sides, even in singleplayer:

```
┌─────────────────────────────────────────────────────────┐
│                    SINGLEPLAYER                         │
│  ┌─────────────────┐       ┌─────────────────────────┐  │
│  │     CLIENT      │ ←───→ │   INTEGRATED SERVER     │  │
│  │  - Rendering    │       │   - World logic         │  │
│  │  - Input        │       │   - Entities            │  │
│  │  - GUI          │       │   - Block ticks         │  │
│  └─────────────────┘       └─────────────────────────┘  │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                    MULTIPLAYER                          │
│  ┌─────────────────┐       ┌─────────────────────────┐  │
│  │     CLIENT      │ ←───→ │   DEDICATED SERVER      │  │
│  │  JAR has client │  NET  │   JAR has NO client     │  │
│  │  classes        │       │   classes at all!       │  │
│  └─────────────────┘       └─────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

**Critical:** Dedicated servers don't have client classes. Any reference to them causes `ClassNotFoundException`.

## Client-Only Classes (NEVER use on server)

These classes exist ONLY on the client:
- `Minecraft` - The game instance
- `Screen`, `GuiComponent`, all GUI classes
- `KeyMapping` - Keybindings
- `SoundManager`, `SoundEngine`
- All `*Renderer` classes
- `ParticleEngine`, particle classes
- `TextureManager`, `ModelManager`
- `ClientLevel` (use `Level` interface instead)
- `LocalPlayer` (use `Player` interface instead)

## Checking Sides at Runtime

```java
// In code that receives a Level
public void doSomething(Level level, BlockPos pos) {
    if (level.isClientSide()) {
        // CLIENT SIDE - safe to spawn particles, play sounds locally
        spawnParticles(level, pos);
    } else {
        // SERVER SIDE - modify world state, save data
        modifyBlockState(level, pos);
    }
}

// In block/entity code
@Override
public InteractionResult use(BlockState state, Level level, BlockPos pos,
                             Player player, InteractionHand hand, BlockHitResult hit) {
    if (!level.isClientSide()) {
        // Server-side logic only
        doServerAction();
    }
    return InteractionResult.sidedSuccess(level.isClientSide());
}
```

## Mixin Configuration (Critical!)

In your mixin JSON, separate client and common mixins:

```json
{
  "required": true,
  "package": "com.example.mod.mixin",
  "compatibilityLevel": "JAVA_21",
  "mixins": [
    "MixinServerLevel",
    "MixinEntity"
  ],
  "client": [
    "MixinMinecraft",
    "MixinScreen",
    "MixinGameRenderer"
  ],
  "server": [
    "MixinDedicatedServer"
  ]
}
```

**Never put client-targeting mixins in the `"mixins"` array** - they'll crash dedicated servers!

## Safe Patterns

### Pattern 1: Dedicated Client Class
```java
// CommonClass.java - safe everywhere
public class CommonClass {
    public static void init() {
        // Common initialization
    }
}

// ClientClass.java - only loaded on client
public class ClientClass {
    public static void initClient() {
        // Client-only initialization
        Minecraft mc = Minecraft.getInstance();
        // Register keybindings, renderers, etc.
    }
}

// Fabric entry point - fabric.mod.json
{
  "entrypoints": {
    "main": ["com.example.mod.ExampleMod"],
    "client": ["com.example.mod.ExampleModClient"]
  }
}
```

### Pattern 2: Side-Specific Service
```java
// common - interface
public interface IClientHelper {
    void showScreen(Screen screen);
    void playLocalSound(SoundEvent sound);
}

// client module - implementation (only exists in client JAR)
public class ClientHelper implements IClientHelper {
    @Override
    public void showScreen(Screen screen) {
        Minecraft.getInstance().setScreen(screen);
    }
}
```

### Pattern 3: Lambda Isolation
```java
// Safe - lambda only evaluated on client
public void openGui(Level level) {
    if (level.isClientSide()) {
        // Lambda captures Minecraft.class but only runs client-side
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().setScreen(new MyScreen());
        });
    }
}
```

## Environment Annotations

**Fabric:**
```java
@Environment(EnvType.CLIENT)
public class ClientOnlyClass { }
```

**Forge/NeoForge:**
```java
@OnlyIn(Dist.CLIENT)
public class ClientOnlyClass { }
```

**Warning:** These annotations strip the class/method from the JAR but don't prevent ClassNotFound if referenced from common code. Use them as documentation, not as a safety mechanism.

## Common Mistakes to Avoid

```java
// WRONG - Will crash dedicated server!
public class CommonClass {
    public void doThing() {
        Minecraft.getInstance().player.sendMessage(...); // NO!
    }
}

// RIGHT - Check side first or use abstraction
public class CommonClass {
    public void doThing(Level level, Player player) {
        if (level.isClientSide()) {
            // Use player parameter, not Minecraft.getInstance()
        }
    }
}

// WRONG - Client class in common mixin list
// mixins.json: "mixins": ["MixinMinecraft"]  // CRASH!

// RIGHT - Client class in client mixin list
// mixins.json: "client": ["MixinMinecraft"]  // Safe
```

## Testing

Always test your mod on:
1. **Singleplayer** - Integrated server + client
2. **Dedicated server** - Run `./gradlew :fabric:runServer` or equivalent
3. **Multiplayer client** - Connect to dedicated server

A mod that works in singleplayer can still crash a dedicated server!
