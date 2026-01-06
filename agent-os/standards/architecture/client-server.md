# Client/Server Separation Standards

This document defines standards for properly separating client-side and server-side code in Minecraft mods to prevent crashes and ensure correct behavior.

## The Two Sides

Minecraft operates on a logical client-server model:

```
SINGLEPLAYER:
┌─────────────┐         ┌─────────────────────┐
│   CLIENT    │ ←─────→ │  INTEGRATED SERVER  │
│ (Rendering) │         │   (World Logic)     │
└─────────────┘         └─────────────────────┘

MULTIPLAYER:
┌─────────────┐         ┌─────────────────────┐
│   CLIENT    │ ←─NET─→ │  DEDICATED SERVER   │
│ (Has GUI)   │         │  (NO GUI CLASSES!)  │
└─────────────┘         └─────────────────────┘
```

**Critical:** Dedicated servers DO NOT have client classes. Any reference causes `ClassNotFoundException`.

## Client-Only Classes (NEVER reference from server code)

These classes exist ONLY on the client JAR:

- `Minecraft` - Game instance
- `Screen`, `GuiComponent` - All GUI classes
- `KeyMapping` - Keybindings
- `SoundManager`, `SoundEngine`
- All `*Renderer` classes
- `ParticleEngine`, particle classes
- `TextureManager`, `ModelManager`
- `ClientLevel` (use `Level` interface)
- `LocalPlayer` (use `Player` interface)
- `GameRenderer`, `LevelRenderer`
- `Font`, `FontManager`

## Runtime Side Checking

### In Regular Code

```java
public void handleInteraction(Level level, BlockPos pos) {
    if (level.isClientSide()) {
        // CLIENT - spawn particles, play local sounds
        spawnParticles(level, pos);
    } else {
        // SERVER - modify world state, save data
        processServerLogic(level, pos);
    }
}
```

### In Block/Item Methods

```java
@Override
public InteractionResult use(BlockState state, Level level, BlockPos pos,
                             Player player, InteractionHand hand, BlockHitResult hit) {
    if (!level.isClientSide()) {
        // Server-side processing only
        doServerAction(player);
    }
    return InteractionResult.sidedSuccess(level.isClientSide());
}
```

## Mixin Configuration

### Correct Separation

```json
{
  "package": "com.example.mod.mixin",
  "mixins": [
    "MixinLevel",
    "MixinEntity",
    "MixinBlockEntity"
  ],
  "client": [
    "MixinMinecraft",
    "MixinGameRenderer",
    "MixinScreen"
  ],
  "server": [
    "MixinDedicatedServer",
    "MixinServerLevel"
  ]
}
```

**RULE:** Never put client-targeting mixins in the `"mixins"` array.

## Safe Patterns

### Pattern 1: Separate Client Class

```java
// CommonLogic.java - safe everywhere
public class CommonLogic {
    public static void process(Level level, BlockPos pos) {
        // Common processing
    }
}

// ClientRenderer.java - client-only
@Environment(EnvType.CLIENT)  // Fabric
// or @OnlyIn(Dist.CLIENT)    // Forge/NeoForge
public class ClientRenderer {
    public static void render(PoseStack poseStack) {
        Minecraft mc = Minecraft.getInstance();
        // Safe to use client classes
    }
}
```

### Pattern 2: Entry Point Separation

**Fabric fabric.mod.json:**
```json
{
  "entrypoints": {
    "main": ["com.example.mod.ExampleMod"],
    "client": ["com.example.mod.ExampleModClient"]
  }
}
```

**Forge/NeoForge:**
```java
@Mod(Constants.MOD_ID)
public class ExampleMod {
    public ExampleMod() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.init();
        }
    }
}
```

### Pattern 3: Lambda Isolation

```java
// Safe - lambda body only evaluated on client
public void showGui(Level level) {
    if (level.isClientSide()) {
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().setScreen(new MyScreen());
        });
    }
}
```

### Pattern 4: Service for Client Operations

```java
// common - interface
public interface IClientHelper {
    void showScreen(Object screen);
    void playLocalSound(ResourceLocation sound);
}

// fabric client - implementation
public class FabricClientHelper implements IClientHelper {
    @Override
    public void showScreen(Object screen) {
        Minecraft.getInstance().setScreen((Screen) screen);
    }
}
```

## Environment Annotations

### Fabric
```java
@Environment(EnvType.CLIENT)
public class ClientOnlyClass { }
```

### Forge/NeoForge
```java
@OnlyIn(Dist.CLIENT)
public class ClientOnlyClass { }
```

**Warning:** These annotations strip code at compile time but don't prevent `ClassNotFoundException` if referenced from common code. They are documentation, not runtime safety.

## Common Mistakes

### Wrong: Client class in common code
```java
// CRASH ON DEDICATED SERVER!
public class CommonClass {
    public void sendMessage() {
        Minecraft.getInstance().player.sendMessage(...);
    }
}
```

### Right: Use parameters instead
```java
public class CommonClass {
    public void sendMessage(Player player, Component message) {
        player.sendSystemMessage(message);
    }
}
```

### Wrong: Client mixin in common array
```json
{
  "mixins": ["MixinMinecraft"]  // CRASH!
}
```

### Right: Client mixin in client array
```json
{
  "client": ["MixinMinecraft"]  // Safe
}
```

## Testing Requirements

Always test on:
1. **Singleplayer** - Integrated server + client
2. **Dedicated Server** - `./gradlew :fabric:runServer`
3. **Multiplayer Client** - Connect to dedicated server

A mod working in singleplayer can still crash a dedicated server!

## Quick Reference

| Action | Client | Server |
|--------|--------|--------|
| Spawn particles | Yes | No |
| Play sounds | Local only | Broadcast to clients |
| Modify world | Visual only | Authoritative |
| Open GUI | Yes | No (send packet) |
| Save data | No | Yes |
| Process input | Yes | No |
| Validate actions | No | Always |
