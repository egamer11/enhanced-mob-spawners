package com.branders.spawnermod.networking.packet;

import java.util.function.Supplier;

import com.branders.spawnermod.SpawnerMod;
import com.branders.spawnermod.block.entity.PreciseSpawnerBlockEntity;
import com.branders.spawnermod.registry.BlockRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

/**
 * Packet to handle upgrading a regular spawner to a precise spawner.
 * Preserves the entity type from the original spawner.
 *
 * @author Anders <Branders> Blomqvist
 */
public class UpgradeSpawnerMessage {

    private final BlockPos pos;

    public UpgradeSpawnerMessage(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(UpgradeSpawnerMessage message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
    }

    public static UpgradeSpawnerMessage decode(FriendlyByteBuf buffer) {
        return new UpgradeSpawnerMessage(buffer.readBlockPos());
    }

    public static void handle(UpgradeSpawnerMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                Level level = player.level();
                BlockPos pos = message.pos;

                if (level.isLoaded(pos)) {
                    BlockState currentState = level.getBlockState(pos);

                    if (currentState.getBlock() == Blocks.SPAWNER) {
                        // Get the existing spawner data
                        SpawnerBlockEntity spawnerEntity = (SpawnerBlockEntity) level.getBlockEntity(pos);
                        if (spawnerEntity == null) {
                            SpawnerMod.LOGGER.warn("Could not find spawner entity at position {}", pos);
                            return;
                        }

                        // Save spawner data
                        CompoundTag spawnerNbt = new CompoundTag();
                        spawnerNbt = spawnerEntity.getSpawner().save(spawnerNbt);

                        // Get entity type from spawner using the same method as the egg drop system
                        String entityString = "";
                        try {
                            String spawnDataString = spawnerNbt.get("SpawnData").toString();
                            if (spawnDataString.contains("\"")) {
                                // Extract entity ID from SpawnData NBT string
                                // Example: {id: "minecraft:pig"} -> minecraft:pig
                                entityString = spawnDataString.substring(spawnDataString.indexOf("\"") + 1);
                                entityString = entityString.substring(0, entityString.indexOf("\""));
                            }
                        } catch (Exception e) {
                            SpawnerMod.LOGGER.warn("Failed to extract entity type from spawner: {}", e.getMessage());
                        }

                        // Replace with precise spawner
                        level.setBlock(pos, BlockRegistry.PRECISE_SPAWNER.get().defaultBlockState(), 3);

                        // Get the new precise spawner entity
                        PreciseSpawnerBlockEntity preciseSpawnerEntity = (PreciseSpawnerBlockEntity) level.getBlockEntity(pos);
                        if (preciseSpawnerEntity != null) {
                            // Set the entity type if we found one
                            if (!entityString.isEmpty()) {
                                try {
                                    EntityType<?> entityType = EntityType.byString(entityString).orElse(EntityType.PIG);
                                    preciseSpawnerEntity.setEntityId(entityType, level, level.getRandom(), pos);

                                    // Set spawn data
                                    CompoundTag entityNbt = new CompoundTag();
                                    entityNbt.putString("id", entityString);
                                    preciseSpawnerEntity.setSpawnData(entityNbt);
                                } catch (Exception e) {
                                    SpawnerMod.LOGGER.warn("Failed to set entity type for precise spawner: {}", e.getMessage());
                                }
                            }

                            // Copy other relevant settings
                            int requiredPlayerRange = spawnerNbt.getShort("RequiredPlayerRange");
                            int minSpawnDelay = spawnerNbt.getShort("MinSpawnDelay");
                            int maxSpawnDelay = spawnerNbt.getShort("MaxSpawnDelay");

                            preciseSpawnerEntity.configure(
                                100, // Default custom delay for precise spawner
                                minSpawnDelay,
                                maxSpawnDelay,
                                requiredPlayerRange,
                                false, // Not disabled by default
                                1 // Default mob limit of 1
                            );

                            SpawnerMod.LOGGER.info("Successfully upgraded spawner to precise spawner at {}", pos);
                        }
                    } else {
                        SpawnerMod.LOGGER.warn("Block at position {} is not a spawner", pos);
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}