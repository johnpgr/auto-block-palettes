---
name: Minecraft Configuration
description: Implement mod configuration systems across Fabric, Forge, and NeoForge using their respective config libraries. Use this skill when adding configurable options to your mod, creating config files, or handling config reload. Apply when working with config annotations, builders, or spec classes. Essential for providing users with customization options and server operators with control over mod behavior.
---

## Standards Reference

For detailed standards, refer to: [Configuration Standards](../../../agent-os/standards/infrastructure/configuration.md)

## When to use this skill:

- When adding user-configurable options to your mod
- When creating config files for server operators
- When implementing client-side settings (render distance, UI options)
- When implementing server-side settings (gameplay balance, features)
- When handling config file loading, saving, and reloading
- When validating config values and providing defaults
- When syncing server config to clients
- When integrating with mod menu/config screens

## Configuration Overview

Each mod loader has different config approaches:

| Loader | Library | Location |
|--------|---------|----------|
| Fabric | Cloth Config, YACL, or custom | config/{mod_id}.json |
| Forge | ForgeConfigSpec | config/{mod_id}-{type}.toml |
| NeoForge | NeoForge Config | config/{mod_id}-{type}.toml |

## Fabric Configuration

### Simple JSON Config

```java
// common/src/main/java/.../config/ModConfig.java
public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
        .getConfigDir().resolve(Constants.MOD_ID + ".json");

    // Config values with defaults
    public int processingTime = 200;
    public float energyMultiplier = 1.0f;
    public boolean enableAdvancedFeatures = false;
    public List<String> disabledBlocks = new ArrayList<>();

    private static ModConfig INSTANCE;

    public static ModConfig get() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public static ModConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                return GSON.fromJson(reader, ModConfig.class);
            } catch (IOException e) {
                Constants.LOG.error("Failed to load config", e);
            }
        }
        ModConfig config = new ModConfig();
        config.save();
        return config;
    }

    public void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            Constants.LOG.error("Failed to save config", e);
        }
    }

    public void reload() {
        ModConfig newConfig = load();
        this.processingTime = newConfig.processingTime;
        this.energyMultiplier = newConfig.energyMultiplier;
        this.enableAdvancedFeatures = newConfig.enableAdvancedFeatures;
        this.disabledBlocks = newConfig.disabledBlocks;
    }
}
```

### Fabric with Cloth Config (Config Screen)

```java
// fabric/build.gradle
dependencies {
    modApi "me.shedaniel.cloth:cloth-config-fabric:..."
}

// fabric/src/main/java/.../config/ClothConfigScreen.java
public class ClothConfigScreen {

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("config.examplemod.title"))
            .setSavingRunnable(ModConfig.get()::save);

        ConfigCategory general = builder.getOrCreateCategory(
            Component.translatable("config.examplemod.category.general"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        general.addEntry(entryBuilder
            .startIntField(
                Component.translatable("config.examplemod.processingTime"),
                ModConfig.get().processingTime)
            .setDefaultValue(200)
            .setMin(1)
            .setMax(6000)
            .setTooltip(Component.translatable("config.examplemod.processingTime.tooltip"))
            .setSaveConsumer(val -> ModConfig.get().processingTime = val)
            .build());

        general.addEntry(entryBuilder
            .startBooleanToggle(
                Component.translatable("config.examplemod.enableAdvanced"),
                ModConfig.get().enableAdvancedFeatures)
            .setDefaultValue(false)
            .setSaveConsumer(val -> ModConfig.get().enableAdvancedFeatures = val)
            .build());

        return builder.build();
    }
}

// Register with Mod Menu
// fabric.mod.json
{
  "entrypoints": {
    "modmenu": ["com.example.mod.ModMenuIntegration"]
  }
}

// ModMenuIntegration.java
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ClothConfigScreen::create;
    }
}
```

## NeoForge Configuration

```java
// neoforge/src/main/java/.../config/NeoForgeConfig.java
public class NeoForgeConfig {

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final CommonConfig COMMON;

    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ClientConfig CLIENT;

    static {
        ForgeConfigSpec.Builder commonBuilder = new ForgeConfigSpec.Builder();
        COMMON = new CommonConfig(commonBuilder);
        COMMON_SPEC = commonBuilder.build();

        ForgeConfigSpec.Builder clientBuilder = new ForgeConfigSpec.Builder();
        CLIENT = new ClientConfig(clientBuilder);
        CLIENT_SPEC = clientBuilder.build();
    }

    public static class CommonConfig {
        public final ForgeConfigSpec.IntValue processingTime;
        public final ForgeConfigSpec.DoubleValue energyMultiplier;
        public final ForgeConfigSpec.BooleanValue enableAdvancedFeatures;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> disabledBlocks;

        public CommonConfig(ForgeConfigSpec.Builder builder) {
            builder.comment("Common configuration for Example Mod")
                   .push("general");

            processingTime = builder
                .comment("Base processing time in ticks (20 ticks = 1 second)")
                .defineInRange("processingTime", 200, 1, 6000);

            energyMultiplier = builder
                .comment("Multiplier for energy consumption")
                .defineInRange("energyMultiplier", 1.0, 0.1, 10.0);

            enableAdvancedFeatures = builder
                .comment("Enable advanced features (requires restart)")
                .define("enableAdvancedFeatures", false);

            disabledBlocks = builder
                .comment("List of block IDs to disable")
                .defineList("disabledBlocks",
                    ArrayList::new,
                    obj -> obj instanceof String);

            builder.pop();
        }
    }

    public static class ClientConfig {
        public final ForgeConfigSpec.BooleanValue renderEffects;
        public final ForgeConfigSpec.IntValue particleCount;
        public final ForgeConfigSpec.EnumValue<RenderQuality> renderQuality;

        public ClientConfig(ForgeConfigSpec.Builder builder) {
            builder.comment("Client-side configuration")
                   .push("rendering");

            renderEffects = builder
                .comment("Enable visual effects")
                .define("renderEffects", true);

            particleCount = builder
                .comment("Maximum particle count")
                .defineInRange("particleCount", 100, 0, 1000);

            renderQuality = builder
                .comment("Render quality setting")
                .defineEnum("renderQuality", RenderQuality.MEDIUM);

            builder.pop();
        }
    }

    public enum RenderQuality {
        LOW, MEDIUM, HIGH, ULTRA
    }
}

// Register in mod constructor
@Mod(Constants.MOD_ID)
public class ExampleMod {
    public ExampleMod(IEventBus modEventBus) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, NeoForgeConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, NeoForgeConfig.CLIENT_SPEC);

        modEventBus.addListener(this::onConfigLoad);
        modEventBus.addListener(this::onConfigReload);
    }

    private void onConfigLoad(ModConfigEvent.Loading event) {
        Constants.LOG.info("Config loaded: {}", event.getConfig().getFileName());
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        Constants.LOG.info("Config reloaded: {}", event.getConfig().getFileName());
        // React to config changes
    }
}

// Usage
int time = NeoForgeConfig.COMMON.processingTime.get();
boolean effects = NeoForgeConfig.CLIENT.renderEffects.get();
```

## Forge Configuration (Similar to NeoForge)

```java
// forge/src/main/java/.../config/ForgeConfig.java
public class ForgeConfig {

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final CommonConfig COMMON;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        COMMON = new CommonConfig(builder);
        COMMON_SPEC = builder.build();
    }

    // Same structure as NeoForge
}

// Register
@Mod(Constants.MOD_ID)
public class ExampleMod {
    public ExampleMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ForgeConfig.COMMON_SPEC);
    }
}
```

## Cross-Loader Config Service

```java
// common/src/main/java/.../config/IModConfig.java
public interface IModConfig {
    int getProcessingTime();
    float getEnergyMultiplier();
    boolean isAdvancedFeaturesEnabled();
    List<String> getDisabledBlocks();

    void save();
    void reload();
}

// common/src/main/java/.../platform/services/IConfigProvider.java
public interface IConfigProvider {
    IModConfig getCommonConfig();
    IModConfig getClientConfig();
}

// Access via service
public class ConfigAccess {
    public static IModConfig common() {
        return Services.CONFIG.getCommonConfig();
    }

    public static IModConfig client() {
        return Services.CONFIG.getClientConfig();
    }
}

// Usage in common code
int time = ConfigAccess.common().getProcessingTime();
```

## Config Types

| Type | Purpose | Sync to Client |
|------|---------|----------------|
| COMMON | Gameplay settings, shared | Manually if needed |
| SERVER | Server-only settings | No |
| CLIENT | Rendering, UI settings | No (client-only) |

## Best Practices

1. **Use appropriate config type** - Client settings in CLIENT, gameplay in COMMON/SERVER
2. **Validate ranges** - Use `defineInRange` to prevent invalid values
3. **Provide defaults** - Always have sensible default values
4. **Document options** - Use comments to explain each setting
5. **Handle reload** - React to config changes without restart when possible
6. **Sync when needed** - Server configs that affect clients need manual sync
7. **Group related options** - Use `push/pop` to organize config sections

## Config File Locations

- **Fabric**: `config/{mod_id}.json` (or custom)
- **Forge/NeoForge**: `config/{mod_id}-common.toml`, `config/{mod_id}-client.toml`
- **World-specific**: `saves/{world}/serverconfig/{mod_id}-server.toml`
