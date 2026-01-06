# Configuration Standards

This document defines standards for implementing mod configuration systems across Fabric, Forge, and NeoForge.

## Configuration Types

| Type | Purpose | Location |
|------|---------|----------|
| COMMON | Gameplay settings (shared) | `config/modid-common.toml` |
| CLIENT | Rendering, UI settings | `config/modid-client.toml` |
| SERVER | Server-only settings | `world/serverconfig/modid-server.toml` |

## Cross-Loader Config Interface

```java
// common/src/main/java/.../config/IModConfig.java
public interface IModConfig {
    int getProcessingTime();
    float getEnergyMultiplier();
    boolean isFeatureEnabled();
    List<String> getDisabledItems();

    void save();
    void reload();
}

// common/src/main/java/.../platform/services/IConfigProvider.java
public interface IConfigProvider {
    IModConfig getCommon();
    IModConfig getClient();
}

// Usage in common code
int time = Services.CONFIG.getCommon().getProcessingTime();
```

## Fabric Configuration (JSON)

### Simple JSON Config

```java
public class FabricModConfig implements IModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance()
        .getConfigDir().resolve(Constants.MOD_ID + ".json");

    // Fields with defaults
    public int processingTime = 200;
    public float energyMultiplier = 1.0f;
    public boolean featureEnabled = true;
    public List<String> disabledItems = new ArrayList<>();

    private static FabricModConfig INSTANCE;

    public static FabricModConfig get() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public static FabricModConfig load() {
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                FabricModConfig config = GSON.fromJson(reader, FabricModConfig.class);
                if (config != null) {
                    return config;
                }
            } catch (IOException e) {
                Constants.LOG.error("Failed to load config", e);
            }
        }
        FabricModConfig config = new FabricModConfig();
        config.save();
        return config;
    }

    @Override
    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            Constants.LOG.error("Failed to save config", e);
        }
    }

    @Override
    public void reload() {
        FabricModConfig loaded = load();
        this.processingTime = loaded.processingTime;
        this.energyMultiplier = loaded.energyMultiplier;
        this.featureEnabled = loaded.featureEnabled;
        this.disabledItems = loaded.disabledItems;
    }

    // IModConfig implementation
    @Override public int getProcessingTime() { return processingTime; }
    @Override public float getEnergyMultiplier() { return energyMultiplier; }
    @Override public boolean isFeatureEnabled() { return featureEnabled; }
    @Override public List<String> getDisabledItems() { return disabledItems; }
}
```

### With Cloth Config (GUI)

```java
public class ClothConfigScreen {

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("config.examplemod.title"))
            .setSavingRunnable(FabricModConfig.get()::save);

        ConfigCategory general = builder.getOrCreateCategory(
            Component.translatable("config.examplemod.general"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        general.addEntry(entryBuilder
            .startIntField(Component.translatable("config.examplemod.processingTime"),
                FabricModConfig.get().processingTime)
            .setDefaultValue(200)
            .setMin(1).setMax(6000)
            .setTooltip(Component.translatable("config.examplemod.processingTime.tooltip"))
            .setSaveConsumer(val -> FabricModConfig.get().processingTime = val)
            .build());

        general.addEntry(entryBuilder
            .startBooleanToggle(Component.translatable("config.examplemod.featureEnabled"),
                FabricModConfig.get().featureEnabled)
            .setDefaultValue(true)
            .setSaveConsumer(val -> FabricModConfig.get().featureEnabled = val)
            .build());

        return builder.build();
    }
}

// ModMenu integration
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ClothConfigScreen::create;
    }
}
```

## NeoForge/Forge Configuration (TOML)

### Config Spec

```java
public class NeoForgeModConfig {

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
        public final ForgeConfigSpec.BooleanValue featureEnabled;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> disabledItems;

        public CommonConfig(ForgeConfigSpec.Builder builder) {
            builder.comment("General Settings").push("general");

            processingTime = builder
                .comment("Base processing time in ticks")
                .defineInRange("processingTime", 200, 1, 6000);

            energyMultiplier = builder
                .comment("Energy consumption multiplier")
                .defineInRange("energyMultiplier", 1.0, 0.1, 10.0);

            featureEnabled = builder
                .comment("Enable advanced features")
                .define("featureEnabled", true);

            disabledItems = builder
                .comment("List of disabled item IDs")
                .defineList("disabledItems", ArrayList::new, obj -> obj instanceof String);

            builder.pop();
        }
    }

    public static class ClientConfig {
        public final ForgeConfigSpec.BooleanValue renderEffects;
        public final ForgeConfigSpec.IntValue particleCount;
        public final ForgeConfigSpec.EnumValue<Quality> renderQuality;

        public ClientConfig(ForgeConfigSpec.Builder builder) {
            builder.comment("Client Settings").push("client");

            renderEffects = builder
                .comment("Enable visual effects")
                .define("renderEffects", true);

            particleCount = builder
                .comment("Maximum particle count")
                .defineInRange("particleCount", 100, 0, 1000);

            renderQuality = builder
                .comment("Render quality level")
                .defineEnum("renderQuality", Quality.MEDIUM);

            builder.pop();
        }

        public enum Quality { LOW, MEDIUM, HIGH, ULTRA }
    }
}
```

### Registration

```java
@Mod(Constants.MOD_ID)
public class ExampleMod {

    public ExampleMod(IEventBus modEventBus) {
        // Register configs
        ModLoadingContext.get().registerConfig(
            ModConfig.Type.COMMON, NeoForgeModConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(
            ModConfig.Type.CLIENT, NeoForgeModConfig.CLIENT_SPEC);

        // Listen for config events
        modEventBus.addListener(this::onConfigLoad);
        modEventBus.addListener(this::onConfigReload);
    }

    private void onConfigLoad(ModConfigEvent.Loading event) {
        Constants.LOG.info("Loaded config: {}", event.getConfig().getFileName());
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        Constants.LOG.info("Reloaded config: {}", event.getConfig().getFileName());
        onConfigChanged();
    }
}
```

### Usage

```java
// Access values
int time = NeoForgeModConfig.COMMON.processingTime.get();
boolean effects = NeoForgeModConfig.CLIENT.renderEffects.get();

// Modify values (requires bake)
NeoForgeModConfig.COMMON.processingTime.set(300);
```

## Config Value Types

### ForgeConfigSpec Types

| Method | Java Type | TOML Type |
|--------|-----------|-----------|
| `define(String, boolean)` | BooleanValue | boolean |
| `defineInRange(String, int, min, max)` | IntValue | integer |
| `defineInRange(String, long, min, max)` | LongValue | integer |
| `defineInRange(String, double, min, max)` | DoubleValue | float |
| `define(String, String)` | ConfigValue<String> | string |
| `defineList(String, Supplier, Predicate)` | ConfigValue<List<?>> | array |
| `defineEnum(String, Enum)` | EnumValue<E> | string |

## Best Practices

### Validation
```java
// ForgeConfigSpec validates automatically
processingTime = builder.defineInRange("time", 200, 1, 6000);

// JSON needs manual validation
public void validate() {
    processingTime = Math.max(1, Math.min(6000, processingTime));
}
```

### Defaults
```java
// Always provide sensible defaults
public int processingTime = 200;  // Not 0

// Document default values
builder.comment("Default: 200 ticks (10 seconds)")
    .defineInRange("processingTime", 200, 1, 6000);
```

### Categories
```java
builder.comment("Machine Settings").push("machines");
// machine config values
builder.pop();

builder.comment("World Generation").push("worldgen");
// worldgen config values
builder.pop();
```

### Reload Handling
```java
// React to config changes
private void onConfigChanged() {
    // Recalculate cached values
    cachedMultiplier = config.getEnergyMultiplier();

    // Notify systems
    MachineManager.onConfigReload();
}
```

### Server Config Sync
```java
// For configs that affect gameplay
// Server must sync to clients
public void syncConfigToClient(ServerPlayer player) {
    Services.NETWORK.sendToClient(player,
        new ConfigSyncPayload(config.getProcessingTime()));
}
```

## File Locations

| Loader | Type | Location |
|--------|------|----------|
| Fabric | All | `config/{modid}.json` |
| Forge/NeoForge | COMMON | `config/{modid}-common.toml` |
| Forge/NeoForge | CLIENT | `config/{modid}-client.toml` |
| Forge/NeoForge | SERVER | `saves/{world}/serverconfig/{modid}-server.toml` |
