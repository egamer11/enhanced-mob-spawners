package com.branders.spawnermod.networking.packet;

import java.util.function.Supplier;

import com.branders.spawnermod.SpawnerMod;
import com.branders.spawnermod.block.entity.PreciseSpawnerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

/**
 * Packet for syncing precise spawner configuration data from client to server.
 *
 * @author Anders <Branders> Blomqvist
 */
public class SyncPreciseSpawnerMessage {

    private final BlockPos pos;
    private final int customDelay;
    private final int minSpawnDelay;
    private final int maxSpawnDelay;
    private final int requiredPlayerRange;
    private final boolean disabled;
    private final int mobLimit;

    public SyncPreciseSpawnerMessage(BlockPos pos, int customDelay, int minSpawnDelay, int maxSpawnDelay, int requiredPlayerRange, boolean disabled, int mobLimit) {
        this.pos = pos;
        this.customDelay = customDelay;
        this.minSpawnDelay = minSpawnDelay;
        this.maxSpawnDelay = maxSpawnDelay;
        this.requiredPlayerRange = requiredPlayerRange;
        this.disabled = disabled;
        this.mobLimit = mobLimit;
    }

    public static void encode(SyncPreciseSpawnerMessage message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeInt(message.customDelay);
        buffer.writeInt(message.minSpawnDelay);
        buffer.writeInt(message.maxSpawnDelay);
        buffer.writeInt(message.requiredPlayerRange);
        buffer.writeBoolean(message.disabled);
        buffer.writeInt(message.mobLimit);
    }

    public static SyncPreciseSpawnerMessage decode(FriendlyByteBuf buffer) {
        return new SyncPreciseSpawnerMessage(
            buffer.readBlockPos(),
            buffer.readInt(),
            buffer.readInt(),
            buffer.readInt(),
            buffer.readInt(),
            buffer.readBoolean(),
            buffer.readInt()
        );
    }

    public static void handle(SyncPreciseSpawnerMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                Level level = player.level();
                BlockPos pos = message.pos;

                if (level.isLoaded(pos)) {
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity instanceof PreciseSpawnerBlockEntity preciseSpawner) {
                        preciseSpawner.configure(
                            message.customDelay,
                            message.minSpawnDelay,
                            message.maxSpawnDelay,
                            message.requiredPlayerRange,
                            message.disabled,
                            message.mobLimit
                        );

                        SpawnerMod.LOGGER.info("Updated precise spawner configuration at {}", pos);
                    } else {
                        SpawnerMod.LOGGER.warn("Block entity at {} is not a precise spawner", pos);
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}