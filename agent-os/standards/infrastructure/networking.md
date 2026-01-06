# Networking Standards

This document defines standards for implementing client-server communication in Minecraft mods across all mod loaders.

## Networking Overview

```
CLIENT → SERVER: Player actions, GUI inputs, requests
SERVER → CLIENT: State updates, responses, broadcasts
SERVER → ALL: Global events (chat, weather, etc.)
```

## Modern Payload System (1.20.5+)

### Payload Definition

```java
public record BlockUpdatePayload(BlockPos pos, int value) implements CustomPacketPayload {

    public static final Type<BlockUpdatePayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "block_update"));

    public static final StreamCodec<FriendlyByteBuf, BlockUpdatePayload> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, BlockUpdatePayload::pos,
            ByteBufCodecs.INT, BlockUpdatePayload::value,
            BlockUpdatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
```

### Common StreamCodecs

| Type | Codec |
|------|-------|
| int | `ByteBufCodecs.INT` |
| long | `ByteBufCodecs.LONG` |
| float | `ByteBufCodecs.FLOAT` |
| double | `ByteBufCodecs.DOUBLE` |
| boolean | `ByteBufCodecs.BOOL` |
| String | `ByteBufCodecs.STRING_UTF8` |
| UUID | `UUIDUtil.STREAM_CODEC` |
| BlockPos | `BlockPos.STREAM_CODEC` |
| ItemStack | `ItemStack.STREAM_CODEC` |
| CompoundTag | `ByteBufCodecs.COMPOUND_TAG` |
| ResourceLocation | `ResourceLocation.STREAM_CODEC` |

### Complex Payloads

```java
public record ComplexPayload(
    BlockPos pos,
    List<ItemStack> items,
    Optional<String> message
) implements CustomPacketPayload {

    public static final StreamCodec<FriendlyByteBuf, ComplexPayload> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, ComplexPayload::pos,
            ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), ComplexPayload::items,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs::optional), ComplexPayload::message,
            ComplexPayload::new);

    // ... type() method
}
```

## Fabric Networking

### Registration

```java
public class FabricNetworking {

    public static void register() {
        // Client → Server
        PayloadTypeRegistry.playC2S().register(
            BlockUpdatePayload.TYPE, BlockUpdatePayload.STREAM_CODEC);

        // Server → Client
        PayloadTypeRegistry.playS2C().register(
            BlockUpdatePayload.TYPE, BlockUpdatePayload.STREAM_CODEC);
    }

    public static void registerServerHandlers() {
        ServerPlayNetworking.registerGlobalReceiver(
            BlockUpdatePayload.TYPE,
            (payload, context) -> {
                ServerPlayer player = context.player();
                handleServerPayload(player, payload);
            });
    }

    public static void registerClientHandlers() {
        ClientPlayNetworking.registerGlobalReceiver(
            BlockUpdatePayload.TYPE,
            (payload, context) -> {
                handleClientPayload(payload);
            });
    }
}
```

### Sending Packets

```java
// Client → Server
public static void sendToServer(CustomPacketPayload payload) {
    ClientPlayNetworking.send(payload);
}

// Server → Client
public static void sendToClient(ServerPlayer player, CustomPacketPayload payload) {
    ServerPlayNetworking.send(player, payload);
}

// Server → All tracking
public static void sendToTracking(ServerLevel level, BlockPos pos, CustomPacketPayload payload) {
    for (ServerPlayer player : level.players()) {
        if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 64 * 64) {
            sendToClient(player, payload);
        }
    }
}
```

## NeoForge Networking

### Registration

```java
public class NeoForgeNetworking {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Constants.MOD_ID);

        registrar.playBidirectional(
            BlockUpdatePayload.TYPE,
            BlockUpdatePayload.STREAM_CODEC,
            NeoForgeNetworking::handlePayload);
    }

    private static void handlePayload(BlockUpdatePayload payload, IPayloadContext context) {
        if (context.flow() == PacketFlow.SERVERBOUND) {
            context.enqueueWork(() -> {
                ServerPlayer player = (ServerPlayer) context.player();
                handleServerPayload(player, payload);
            });
        } else {
            context.enqueueWork(() -> handleClientPayload(payload));
        }
    }
}

// Register in mod constructor
@Mod(Constants.MOD_ID)
public class ExampleMod {
    public ExampleMod(IEventBus bus) {
        bus.addListener(NeoForgeNetworking::register);
    }
}
```

### Sending Packets

```java
// Client → Server
PacketDistributor.sendToServer(payload);

// Server → Client
PacketDistributor.sendToPlayer(player, payload);

// Server → All tracking chunk
PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, payload);
```

## Cross-Loader Service Pattern

```java
// common/src/main/java/.../platform/services/INetworkHelper.java
public interface INetworkHelper {
    void sendToServer(CustomPacketPayload payload);
    void sendToClient(ServerPlayer player, CustomPacketPayload payload);
    void sendToAllTracking(ServerLevel level, BlockPos pos, CustomPacketPayload payload);
}

// Usage
Services.NETWORK.sendToServer(new BlockUpdatePayload(pos, value));
```

## Block Entity Syncing

```java
public class SyncedBlockEntity extends BlockEntity {

    private int value;

    // Initial sync when chunk loads
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    // Sync packet when block updates
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // Call when data changes
    public void syncToClients() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
```

## Security Best Practices

**ALWAYS validate on server:**

```java
private static void handleServerPayload(ServerPlayer player, BlockUpdatePayload payload) {
    BlockPos pos = payload.pos();

    // 1. Check position is loaded
    if (!player.level().isLoaded(pos)) {
        return;
    }

    // 2. Check player distance
    if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > 64) {
        return;
    }

    // 3. Check player permission
    if (!canPlayerModify(player, pos)) {
        return;
    }

    // 4. Validate payload data
    if (payload.value() < 0 || payload.value() > 100) {
        return;
    }

    // 5. Now safe to process
    processUpdate(player.level(), pos, payload.value());
}
```

## Common Patterns

### Request-Response
```java
// Client sends request
Services.NETWORK.sendToServer(new DataRequestPayload(pos));

// Server handles and responds
void handleRequest(ServerPlayer player, DataRequestPayload request) {
    Data data = getData(request.pos());
    Services.NETWORK.sendToClient(player, new DataResponsePayload(request.pos(), data));
}
```

### State Sync
```java
// Server broadcasts on change
public void onValueChanged(ServerLevel level, BlockPos pos, int newValue) {
    Services.NETWORK.sendToAllTracking(level, pos, new ValueUpdatePayload(pos, newValue));
}
```

## Thread Safety

```java
// NeoForge/Forge - use enqueueWork for thread safety
context.enqueueWork(() -> {
    // Now on main thread
    processPayload(payload);
});

// Fabric - already on correct thread in handler
ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> {
    // Already on server thread
});
```
