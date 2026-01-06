# Event System Standards

This document defines standards for handling game events across Fabric, Forge, and NeoForge mod loaders.

## Event System Comparison

| Loader | System | Registration |
|--------|--------|--------------|
| Fabric | Functional callbacks | `Event.register(lambda)` |
| Forge | Annotation-based | `@SubscribeEvent` |
| NeoForge | Annotation-based | `@SubscribeEvent` |

## Common Event Handler Pattern

Put shared logic in common module, call from loader events:

```java
// common/src/main/java/.../events/CommonEvents.java
public class CommonEvents {

    public static void onPlayerJoin(ServerPlayer player) {
        // Common logic
        syncPlayerData(player);
        sendWelcomeMessage(player);
    }

    public static boolean onBlockBreak(Level level, Player player, BlockPos pos, BlockState state) {
        // Return false to cancel
        if (state.is(ModTags.Blocks.PROTECTED)) {
            return false;
        }
        return true;
    }

    public static void onEntityDamage(LivingEntity entity, float amount, DamageSource source) {
        // Process damage
    }
}
```

## Fabric Events

### Registration

```java
public class FabricEvents {

    public static void register() {
        // Player events
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            CommonEvents.onPlayerJoin(handler.getPlayer());
        });

        // Block events
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> {
            return CommonEvents.onBlockBreak(world, player, pos, state);
        });

        // Entity events
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            CommonEvents.onEntityDamage(entity, amount, source);
            return true; // Allow damage
        });

        // Tick events
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            processWorldTick(world);
        });

        // Lifecycle
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            onServerStarted(server);
        });
    }
}
```

### Common Fabric Callbacks

| Callback | When |
|----------|------|
| `ServerPlayConnectionEvents.JOIN` | Player joins |
| `ServerPlayConnectionEvents.DISCONNECT` | Player leaves |
| `PlayerBlockBreakEvents.BEFORE/AFTER` | Block break |
| `AttackEntityCallback.EVENT` | Entity attacked |
| `UseBlockCallback.EVENT` | Right-click block |
| `UseItemCallback.EVENT` | Use item |
| `ServerTickEvents.END_SERVER_TICK` | Server tick |
| `ServerTickEvents.END_WORLD_TICK` | World tick |
| `ServerEntityEvents.ENTITY_LOAD` | Entity loads |
| `ServerLifecycleEvents.SERVER_STARTED` | Server starts |
| `ServerLifecycleEvents.SERVER_STOPPING` | Server stops |

### Client Fabric Events

```java
public class FabricClientEvents {

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            handleKeybinds();
        });

        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            renderCustomHud(graphics);
        });

        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof TitleScreen) {
                modifyTitleScreen(screen);
            }
        });
    }
}
```

## NeoForge Events

### Registration

```java
@Mod(Constants.MOD_ID)
public class ExampleMod {
    public ExampleMod(IEventBus modEventBus) {
        // MOD bus - registration, setup events
        modEventBus.addListener(this::onCommonSetup);

        // GAME bus - gameplay events
        NeoForge.EVENT_BUS.register(GameEvents.class);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CommonClass.init();
        });
    }
}

// Separate class for game events
public class GameEvents {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CommonEvents.onPlayerJoin(player);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!CommonEvents.onBlockBreak(event.getLevel(), event.getPlayer(),
                event.getPos(), event.getState())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        CommonEvents.onEntityDamage(event.getEntity(), event.getAmount(), event.getSource());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        processServerTick(event.getServer());
    }
}
```

### Common NeoForge/Forge Events

| Event | When |
|-------|------|
| `PlayerEvent.PlayerLoggedInEvent` | Player joins |
| `PlayerEvent.PlayerLoggedOutEvent` | Player leaves |
| `BlockEvent.BreakEvent` | Block broken |
| `BlockEvent.EntityPlaceEvent` | Block placed |
| `LivingDamageEvent` | Entity damaged |
| `LivingDeathEvent` | Entity dies |
| `EntityJoinLevelEvent` | Entity spawns |
| `PlayerInteractEvent.RightClickBlock` | Right-click block |
| `PlayerInteractEvent.RightClickItem` | Use item |
| `ServerTickEvent.Pre/Post` | Server tick |
| `LevelTickEvent.Pre/Post` | World tick |

### Client NeoForge Events

```java
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT, bus = Bus.GAME)
public class ClientGameEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        handleKeybinds();
    }

    @SubscribeEvent
    public static void onRenderHud(RenderGuiLayerEvent.Post event) {
        if (event.getName().equals(VanillaGuiLayers.HOTBAR)) {
            renderCustomHud(event.getGuiGraphics());
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS) {
            handleKeyPress(event.getKey());
        }
    }
}
```

## Event Cancellation

### Fabric
```java
// Return false to cancel
PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> {
    if (shouldCancel(state)) {
        return false;  // Cancels the break
    }
    return true;  // Allows the break
});
```

### NeoForge/Forge
```java
@SubscribeEvent
public static void onEvent(SomeCancellableEvent event) {
    if (shouldCancel()) {
        event.setCanceled(true);
    }
}
```

## Event Priority

### NeoForge/Forge
```java
@SubscribeEvent(priority = EventPriority.HIGH)
public static void onEvent(SomeEvent event) {
    // Runs before NORMAL priority
}

// Order: HIGHEST → HIGH → NORMAL → LOW → LOWEST
```

### Fabric
Register in order, or use phases where available.

## Events vs Mixins

| Use Events | Use Mixins |
|------------|------------|
| Hook exists for your need | No event available |
| Cross-mod compatibility | Internal modification |
| Modify parameters/results | Change control flow |
| Official API behavior | Private method access |

## Best Practices

1. **Prefer events over mixins** - Better compatibility
2. **Common handler logic** - Keep in common module
3. **Check event result** - Some events have results (ALLOW/DENY/PASS)
4. **Use appropriate bus** - MOD bus for registration, GAME bus for gameplay
5. **Client vs Server** - Use `@OnlyIn`/`@Environment` for client events
6. **Don't block tick events** - Keep tick handlers fast
