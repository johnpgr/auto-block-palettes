---
name: Minecraft Networking
description: Implement client-server communication using packets and payloads in Minecraft mods across Fabric, Forge, and NeoForge. Use this skill when you need to synchronize data between client and server. Apply when creating custom packets, handling network events, or implementing block entity syncing. Essential when player actions on client need to trigger server-side logic, or when server state needs to update clients. Use when working with payload types, packet handlers, or network channels.
---

## Standards Reference

For detailed standards, refer to: [Networking Standards](../../../agent-os/standards/infrastructure/networking.md)

## When to use this skill:

- When sending data from client to server (player actions, GUI inputs)
- When sending data from server to client (state updates, custom data)
- When synchronizing block entity data with clients
- When implementing custom GUIs that need server communication
- When creating multiplayer-aware features
- When working with PayloadType, CustomPacketPayload, or network channels
- When handling packet serialization and deserialization
- When implementing network security and validation

## Networking Basics

Minecraft networking sends **packets** (payloads) between client and server:

```
CLIENT → SERVER: Player actions, GUI interactions, requests
SERVER → CLIENT: World state, entity data, responses
SERVER → ALL CLIENTS: Broadcasts (chat, effects, etc.)
```

## Modern Payload System (1.20.5+)

Minecraft 1.20.5+ uses a new payload-based networking system.

### Defining a Payload

```java
// common/src/main/java/.../network/ExamplePayload.java
public record ExamplePayload(BlockPos pos, int value) implements CustomPacketPayload {

    public static final Type<ExamplePayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "example"));

    public static final StreamCodec<FriendlyByteBuf, ExamplePayload> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, ExamplePayload::pos,
            ByteBufCodecs.INT, ExamplePayload::value,
            ExamplePayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
```

### Fabric Networking

```java
// fabric/src/main/java/.../network/FabricNetworking.java
public class FabricNetworking {

    public static void register() {
        // Register server-bound payload (client → server)
        PayloadTypeRegistry.playC2S().register(ExamplePayload.TYPE, ExamplePayload.STREAM_CODEC);

        // Register client-bound payload (server → client)
        PayloadTypeRegistry.playS2C().register(ExamplePayload.TYPE, ExamplePayload.STREAM_CODEC);

        // Handle server-bound packets
        ServerPlayNetworking.registerGlobalReceiver(ExamplePayload.TYPE,
            (payload, context) -> {
                ServerPlayer player = context.player();
                // Handle on server - already on server thread
                handleOnServer(player, payload.pos(), payload.value());
            });

        // Handle client-bound packets (in client init)
        ClientPlayNetworking.registerGlobalReceiver(ExamplePayload.TYPE,
            (payload, context) -> {
                // Handle on client - already on render thread
                handleOnClient(payload.pos(), payload.value());
            });
    }

    // Send from client to server
    public static void sendToServer(ExamplePayload payload) {
        ClientPlayNetworking.send(payload);
    }

    // Send from server to client
    public static void sendToClient(ServerPlayer player, ExamplePayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    // Send to all players tracking a position
    public static void sendToTracking(ServerLevel level, BlockPos pos, ExamplePayload payload) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 64 * 64) {
                sendToClient(player, payload);
            }
        }
    }
}
```

### NeoForge Networking

```java
// neoforge/src/main/java/.../network/NeoForgeNetworking.java
public class NeoForgeNetworking {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Constants.MOD_ID);

        // Register bidirectional payload
        registrar.playBidirectional(
            ExamplePayload.TYPE,
            ExamplePayload.STREAM_CODEC,
            NeoForgeNetworking::handlePayload
        );
    }

    private static void handlePayload(ExamplePayload payload, IPayloadContext context) {
        if (context.flow() == PacketFlow.SERVERBOUND) {
            // On server
            context.enqueueWork(() -> {
                ServerPlayer player = (ServerPlayer) context.player();
                handleOnServer(player, payload.pos(), payload.value());
            });
        } else {
            // On client
            context.enqueueWork(() -> {
                handleOnClient(payload.pos(), payload.value());
            });
        }
    }

    public static void sendToServer(ExamplePayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    public static void sendToClient(ServerPlayer player, ExamplePayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}

// Register in mod constructor
@Mod(Constants.MOD_ID)
public class ExampleMod {
    public ExampleMod(IEventBus eventBus) {
        eventBus.addListener(NeoForgeNetworking::register);
    }
}
```

### Forge Networking (Older SimpleChannel)

```java
// forge/src/main/java/.../network/ForgeNetworking.java
public class ForgeNetworking {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, ExamplePacket.class,
            ExamplePacket::encode,
            ExamplePacket::decode,
            ExamplePacket::handle);
    }

    public static void sendToServer(ExamplePacket packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToClient(ServerPlayer player, ExamplePacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
```

## Block Entity Syncing

For block entities that need client updates:

```java
public class ExampleBlockEntity extends BlockEntity {

    private int value;

    // Called when client loads chunk - initial sync
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    // Packet sent when block entity changes
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // Mark dirty and sync to clients
    public void markDirtyAndSync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
```

## Service Pattern for Cross-Loader

```java
// common - interface
public interface INetworkHelper {
    void sendToServer(CustomPacketPayload payload);
    void sendToClient(ServerPlayer player, CustomPacketPayload payload);
    void sendToAllTracking(ServerLevel level, BlockPos pos, CustomPacketPayload payload);
}

// Each loader implements this interface
// Access via: Services.NETWORK.sendToServer(payload);
```

## Security Best Practices

```java
// ALWAYS validate on server!
private static void handleOnServer(ServerPlayer player, BlockPos pos, int value) {
    // 1. Validate position is in loaded chunks
    if (!player.level().isLoaded(pos)) {
        return;
    }

    // 2. Validate player can reach/interact with position
    if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > 64) {
        return; // Too far away
    }

    // 3. Validate value is in expected range
    if (value < 0 || value > 100) {
        return; // Invalid value
    }

    // 4. Validate player has permission
    if (!canPlayerModify(player, pos)) {
        return;
    }

    // Now safe to process
    doServerAction(player, pos, value);
}
```

## Common Patterns

### Request-Response Pattern
```java
// Client sends request
Services.NETWORK.sendToServer(new RequestDataPayload(blockPos));

// Server handles and responds
void handleRequest(ServerPlayer player, RequestDataPayload payload) {
    Data data = getDataFor(payload.pos());
    Services.NETWORK.sendToClient(player, new ResponseDataPayload(payload.pos(), data));
}
```

### Broadcast Pattern
```java
// Server broadcasts to all nearby players
void broadcastChange(ServerLevel level, BlockPos pos, int newValue) {
    ChangePayload payload = new ChangePayload(pos, newValue);
    for (ServerPlayer player : level.players()) {
        if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 128 * 128) {
            Services.NETWORK.sendToClient(player, payload);
        }
    }
}
```
