---
name: Minecraft Events and Hooks
description: Handle game events across Fabric, Forge, and NeoForge mod loaders using their respective event systems. Use this skill when you need to react to game events like player actions, world ticks, entity spawning, or block interactions. Apply when registering event listeners, creating custom events, or understanding event priority and cancellation. Essential for integrating mod behavior with Minecraft's lifecycle without using mixins.
---

## Standards Reference

For detailed standards, refer to: [Event System Standards](../../../agent-os/standards/development/events.md)

## When to use this skill:

- When reacting to player events (join, leave, respawn, death, attack, interact)
- When handling world/level events (load, save, tick, weather)
- When intercepting entity events (spawn, death, damage, AI)
- When responding to block events (break, place, interact, update)
- When handling item events (use, pickup, drop, craft)
- When registering event listeners with proper priority
- When creating custom events for your mod
- When deciding between events vs mixins for a feature
- When cancelling or modifying vanilla behavior

## Event System Overview

Each loader has a different event system:

| Loader | System | Key Classes |
|--------|--------|-------------|
| Fabric | Callbacks | `*Callback`, `*Events` interfaces |
| Forge | Event Bus | `@SubscribeEvent`, `MinecraftForge.EVENT_BUS` |
| NeoForge | Event Bus | `@SubscribeEvent`, `IEventBus` |

## Fabric Events

Fabric uses functional callback interfaces:

```java
// fabric/src/main/java/.../FabricEvents.java
public class FabricEvents {

    public static void register() {
        // Player events
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            Constants.LOG.info("Player joined: {}", player.getName().getString());
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            Constants.LOG.info("Player left: {}", player.getName().getString());
        });

        // Entity events
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof Monster) {
                // Handle monster spawn
            }
        });

        // Block events
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            // Return false to cancel block break
            if (state.is(ModTags.Blocks.UNBREAKABLE)) {
                return false;
            }
            return true;
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            // After block is broken
            grantExperience(player, state);
        });

        // Item use events
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(ModItems.SPECIAL_ITEM.get())) {
                // Handle special item use
                return InteractionResultHolder.success(stack);
            }
            return InteractionResultHolder.pass(stack);
        });

        // World tick
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            // Called every tick for each dimension
            processWorldTick(world);
        });

        // Server lifecycle
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Constants.LOG.info("Server started!");
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            Constants.LOG.info("Server stopping...");
            saveModData();
        });

        // Attack entity
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity instanceof Villager) {
                // Prevent attacking villagers
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }
}
```

### Client-Side Fabric Events

```java
// In client initializer
public class FabricClientEvents {

    public static void register() {
        // Client tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (ModKeybinds.EXAMPLE_KEY.consumeClick()) {
                // Handle keybind press
            }
        });

        // HUD render
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            renderCustomHud(graphics);
        });

        // Screen events
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen) {
                // Add widgets to title screen
            }
        });
    }
}
```

## NeoForge Events

NeoForge uses an event bus with annotations:

```java
// neoforge/src/main/java/.../NeoForgeEvents.java
public class NeoForgeEvents {

    // Register to MOD event bus (for registration events)
    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Thread-safe initialization
            CommonClass.init();
        });
    }

    // Register to GAME event bus (for gameplay events)
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Constants.LOG.info("Player joined: {}", player.getName().getString());
        }
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        Constants.LOG.info("Player left: {}", event.getEntity().getName().getString());
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        BlockState state = event.getState();

        if (state.is(ModTags.Blocks.UNBREAKABLE)) {
            event.setCanceled(true);  // Cancel the break
            return;
        }

        // Modify experience dropped
        event.setExpToDrop(event.getExpToDrop() * 2);
    }

    @SubscribeEvent
    public static void onEntitySpawn(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Monster monster) {
            // Modify or cancel spawn
            if (shouldPreventSpawn(monster)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (player.hasEffect(ModEffects.PROTECTION.get())) {
                event.setAmount(event.getAmount() * 0.5f);  // Half damage
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // Called every server tick
        processServerTick(event.getServer());
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getItemStack().is(ModItems.WRENCH.get())) {
            // Handle wrench interaction
            if (handleWrench(event)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        }
    }
}

// Registration in mod class
@Mod(Constants.MOD_ID)
public class ExampleMod {
    public ExampleMod(IEventBus modEventBus) {
        // MOD bus for registration events
        modEventBus.register(NeoForgeEvents.class);

        // GAME bus for gameplay events
        NeoForge.EVENT_BUS.register(NeoForgeEvents.class);
    }
}
```

### Client-Side NeoForge Events

```java
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.GAME)
public class NeoForgeClientEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (ModKeybinds.EXAMPLE_KEY.consumeClick()) {
            // Handle keybind
        }
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

## Forge Events (Similar to NeoForge)

```java
// forge/src/main/java/.../ForgeEvents.java
@Mod.EventBusSubscriber(modid = Constants.MOD_ID)
public class ForgeEvents {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        // Same as NeoForge
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        // Same as NeoForge
    }
}
```

## Service Pattern for Cross-Loader Events

```java
// common/src/main/java/.../events/ModEventHandler.java
public class ModEventHandler {

    public static void onPlayerJoin(ServerPlayer player) {
        // Common logic for all loaders
        syncPlayerData(player);
        sendWelcomeMessage(player);
    }

    public static boolean onBlockBreak(Level level, Player player, BlockPos pos, BlockState state) {
        // Return false to cancel
        if (state.is(ModTags.Blocks.UNBREAKABLE)) {
            return false;
        }
        return true;
    }
}

// Each loader calls these methods from their event handlers
// Fabric: PlayerBlockBreakEvents.BEFORE.register((w, p, pos, state, be) -> ModEventHandler.onBlockBreak(w, p, pos, state));
// NeoForge: @SubscribeEvent void onBreak(BreakEvent e) { if (!ModEventHandler.onBlockBreak(...)) e.setCanceled(true); }
```

## Event Priority

**NeoForge/Forge:**
```java
@SubscribeEvent(priority = EventPriority.HIGH)
public static void onEvent(SomeEvent event) {
    // Runs before NORMAL priority handlers
}
// Priority order: HIGHEST → HIGH → NORMAL → LOW → LOWEST
```

**Fabric:**
Fabric callbacks are invoked in registration order. Use phases for ordering:
```java
ServerTickEvents.END_SERVER_TICK.register(EventPriority.HIGHEST, server -> {
    // Runs first
});
```

## When to Use Events vs Mixins

| Use Events When | Use Mixins When |
|-----------------|-----------------|
| Hook exists for your need | No event covers your case |
| Modifying parameters/results | Need to change control flow |
| Adding behavior | Modifying private methods |
| Cross-mod compatibility matters | Accessing internal state |
| Event provides cancellation | Core behavior modification |
