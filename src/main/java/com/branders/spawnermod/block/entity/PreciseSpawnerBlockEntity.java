package com.branders.spawnermod.block.entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.branders.spawnermod.SpawnerMod;
import com.branders.spawnermod.config.ConfigValues;
import com.branders.spawnermod.registry.BlockEntityRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Block entity for the Precise Spawner that tracks spawned mobs and only allows 1 at a time.
 * Uses configurable delay between spawns and precise mob tracking.
 *
 * @author Anders <Branders> Blomqvist
 */
public class PreciseSpawnerBlockEntity extends BlockEntity {

    private int spawnDelay = 20;
    private CompoundTag spawnData = new CompoundTag();
    private double spin;
    private double oSpin;
    private int minSpawnDelay = 200;
    private int maxSpawnDelay = 800;
    private int spawnCount = 1; // Always 1 for precise spawner
    private @Nullable Entity displayEntity;
    private int maxNearbyEntities = 1; // Always 1 for precise spawner
    private int requiredPlayerRange = 16;
    private int spawnRange = 4;

    // Precise spawner specific fields
    private List<UUID> spawnedMobIds = new ArrayList<>();
    private boolean disabled = false;
    private int customDelay = 100; // Default 5 second delay (100 ticks)
    private int mobLimit = 1; // Maximum number of mobs that can be alive at once
    private boolean needsValidation = false; // Flag to validate on first server tick

    public PreciseSpawnerBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.PRECISE_SPAWNER.get(), pos, blockState);
        // Don't set a default entity - spawner should be empty until configured
        // Set default mob limit if not set
        if (this.mobLimit <= 0) {
            this.mobLimit = 1;
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PreciseSpawnerBlockEntity blockEntity) {
        blockEntity.serverTick((ServerLevel) level, pos);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, PreciseSpawnerBlockEntity blockEntity) {
        blockEntity.clientTick();
    }

    public void clientTick() {
        // Update display entity rotation on client side for smooth spinning
        this.oSpin = this.spin;
        this.spin += 1.0D; // Consistent rotation speed like vanilla
    }

    public void serverTick(ServerLevel level, BlockPos pos) {
        // Perform validation on first tick after world load
        if (needsValidation) {
            validateAndRebuildMobTracking();
            needsValidation = false;
        }

        if (disabled) return;

        // Check if we have redstone power (disable if powered)
        if (level.hasNeighborSignal(pos)) return;

        // Update display entity rotation
        this.oSpin = this.spin;
        this.spin += 1.0D; // Consistent rotation speed like vanilla

        // Check if our spawned mobs are still alive and clean up dead ones
        cleanupDeadMobs(level);

        // Don't spawn if we've reached the mob limit
        if (spawnedMobIds.size() >= mobLimit) return;

        // Handle spawn delay
        if (spawnDelay > 0) {
            --spawnDelay;
            return;
        }

        // Check if player is in range
        if (!level.hasNearbyAlivePlayer((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, (double)this.requiredPlayerRange)) {
            return;
        }

        // Check difficulty
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }

        // Try to spawn
        boolean flag = this.spawnMob(level, pos);

        if (flag) {
            spawnDelay = customDelay; // Reset to custom delay after spawn
        }
    }

    private boolean spawnMob(ServerLevel level, BlockPos pos) {
        RandomSource randomSource = level.getRandom();

        if (this.spawnData.isEmpty()) {
            this.delay(level, pos);
            return false;
        }

        CompoundTag compoundTag = this.spawnData;
        Optional<EntityType<?>> optional = EntityType.by(compoundTag);

        if (optional.isEmpty()) {
            this.delay(level, pos);
            return false;
        }

        EntityType<?> entityType = optional.get();

        // Calculate spawn position
        double d0 = (double)pos.getX() + (randomSource.nextDouble() - randomSource.nextDouble()) * (double)this.spawnRange + 0.5D;
        double d1 = (double)(pos.getY() + randomSource.nextInt(3) - 1);
        double d2 = (double)pos.getZ() + (randomSource.nextDouble() - randomSource.nextDouble()) * (double)this.spawnRange + 0.5D;

        if (level.noCollision(entityType.getAABB(d0, d1, d2))) {
            BlockPos blockpos = BlockPos.containing(d0, d1, d2);

            // Check spawn placement rules
            if (!SpawnPlacements.checkSpawnRules(entityType, level, MobSpawnType.SPAWNER, blockpos, level.getRandom())) {
                return false;
            }

            // Check for peaceful difficulty with hostile mobs
            if (!entityType.getCategory().isFriendly() && level.getDifficulty() == Difficulty.PEACEFUL) {
                return false;
            }

            Entity entity = EntityType.loadEntityRecursive(compoundTag, level, (entityToSpawn) -> {
                entityToSpawn.moveTo(d0, d1, d2, entityToSpawn.getYRot(), entityToSpawn.getXRot());
                return entityToSpawn;
            });

            if (entity == null) {
                this.delay(level, pos);
                return false;
            }

            // Add entity to world and track it
            if (level.tryAddFreshEntityWithPassengers(entity)) {
                spawnedMobIds.add(entity.getUUID());
                level.levelEvent(2004, pos, 0);
                level.gameEvent(entity, net.minecraft.world.level.gameevent.GameEvent.ENTITY_PLACE, blockpos);

                if (entity instanceof net.minecraft.world.entity.Mob) {
                    ((net.minecraft.world.entity.Mob)entity).spawnAnim();
                }

                // Spawn soul fire flame particles when mob spawns
                spawnParticles(level);

                return true;
            }
        }

        return false;
    }

    /**
     * Clean up dead mobs from the tracking list
     */
    private void cleanupDeadMobs(ServerLevel level) {
        Iterator<UUID> iterator = spawnedMobIds.iterator();
        boolean removedAny = false;

        while (iterator.hasNext()) {
            UUID mobId = iterator.next();
            Entity entity = level.getEntity(mobId);

            // Check if entity exists and is alive
            if (entity == null || !entity.isAlive()) {
                iterator.remove();
                removedAny = true;
            }
            // Also check if entity is too far away (may have wandered off or been teleported)
            else if (entity.distanceToSqr(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5) > 64 * 64) {
                // If mob is more than 64 blocks away, stop tracking it
                iterator.remove();
                removedAny = true;
            }
        }

        // Reset spawn delay when any mobs were removed so we can spawn another soon
        if (removedAny) {
            spawnDelay = Math.min(spawnDelay, customDelay);
        }
    }

    private void delay(ServerLevel level, BlockPos pos) {
        if (this.maxSpawnDelay <= this.minSpawnDelay) {
            this.spawnDelay = this.minSpawnDelay;
        } else {
            this.spawnDelay = this.minSpawnDelay + level.getRandom().nextInt(this.maxSpawnDelay - this.minSpawnDelay);
        }

        if (!this.spawnData.isEmpty()) {
            this.broadcastEvent(level, pos, 1);
        }

        this.setChanged();
    }

    public void broadcastEvent(Level level, BlockPos pos, int id) {
        level.blockEvent(pos, this.getBlockState().getBlock(), id, 0);
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        if (id == 1) {
            if (this.level.isClientSide) {
                this.spawnDelay = this.minSpawnDelay;
                return true;
            }
        }
        return false;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.spawnDelay = tag.getShort("Delay");
        this.spawnData = new CompoundTag();

        if (tag.contains("SpawnData", 10)) {
            this.spawnData = tag.getCompound("SpawnData");
        }

        if (tag.contains("MinSpawnDelay", 99)) {
            this.minSpawnDelay = tag.getShort("MinSpawnDelay");
            this.maxSpawnDelay = tag.getShort("MaxSpawnDelay");
            this.spawnCount = tag.getShort("SpawnCount");
        }

        if (tag.contains("MaxNearbyEntities", 99)) {
            this.maxNearbyEntities = tag.getShort("MaxNearbyEntities");
            this.requiredPlayerRange = tag.getShort("RequiredPlayerRange");
        }

        if (tag.contains("SpawnRange", 99)) {
            this.spawnRange = tag.getShort("SpawnRange");
        }

        // Precise spawner specific data
        this.spawnedMobIds.clear();
        if (tag.contains("SpawnedMobIds", 9)) {
            var mobIdsList = tag.getList("SpawnedMobIds", 11); // 11 = IntArrayTag for UUIDs
            for (int i = 0; i < mobIdsList.size(); i++) {
                var uuidArray = mobIdsList.getIntArray(i);
                if (uuidArray.length == 4) {
                    UUID uuid = new UUID((long)uuidArray[0] << 32 | (long)uuidArray[1] & 0xFFFFFFFFL,
                                        (long)uuidArray[2] << 32 | (long)uuidArray[3] & 0xFFFFFFFFL);
                    this.spawnedMobIds.add(uuid);
                }
            }
        }
        this.disabled = tag.getBoolean("Disabled");
        this.customDelay = tag.getInt("CustomDelay");
        this.mobLimit = tag.getInt("MobLimit");

        this.displayEntity = null;

        // Schedule validation for first server tick when world is fully loaded
        // This prevents mob limit bypass when rejoining worlds
        this.needsValidation = true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putShort("Delay", (short)this.spawnDelay);
        tag.putShort("MinSpawnDelay", (short)this.minSpawnDelay);
        tag.putShort("MaxSpawnDelay", (short)this.maxSpawnDelay);
        tag.putShort("SpawnCount", (short)this.spawnCount);
        tag.putShort("MaxNearbyEntities", (short)this.maxNearbyEntities);
        tag.putShort("RequiredPlayerRange", (short)this.requiredPlayerRange);
        tag.putShort("SpawnRange", (short)this.spawnRange);
        tag.put("SpawnData", this.spawnData);

        // Precise spawner specific data
        var mobIdsList = new net.minecraft.nbt.ListTag();
        for (UUID mobId : this.spawnedMobIds) {
            int[] uuidArray = new int[4];
            long mostSig = mobId.getMostSignificantBits();
            long leastSig = mobId.getLeastSignificantBits();
            uuidArray[0] = (int)(mostSig >> 32);
            uuidArray[1] = (int)mostSig;
            uuidArray[2] = (int)(leastSig >> 32);
            uuidArray[3] = (int)leastSig;
            mobIdsList.add(new net.minecraft.nbt.IntArrayTag(uuidArray));
        }
        tag.put("SpawnedMobIds", mobIdsList);
        tag.putBoolean("Disabled", this.disabled);
        tag.putInt("CustomDelay", this.customDelay);
        tag.putInt("MobLimit", this.mobLimit);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = this.saveWithoutMetadata();
        tag.remove("SpawnPotentials");
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // Getters and setters for GUI integration
    public void setEntityId(EntityType<?> entityType, Level level, RandomSource random, BlockPos pos) {
        // Validate entity type
        if (entityType == null || ForgeRegistries.ENTITY_TYPES.getKey(entityType) == null) {
            SpawnerMod.LOGGER.warn("Invalid entity type provided to precise spawner at {}", pos);
            return;
        }

        this.spawnData = new CompoundTag();
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(entityType).toString();
        this.spawnData.putString("id", entityId);

        // Reset mob tracking when entity type changes
        this.spawnedMobIds.clear();

        this.displayEntity = null;
        this.setChanged();

        SpawnerMod.LOGGER.info("Precise spawner at {} set to spawn: {}", pos, entityId);

        // Sync to client
        if (this.level != null && !this.level.isClientSide) {
            var blockState = this.level.getBlockState(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, blockState, blockState, 3);
        }
    }

    public CompoundTag getSpawnData() {
        return this.spawnData;
    }

    public void setSpawnData(CompoundTag spawnData) {
        this.spawnData = spawnData;
    }

    public double getSpin() {
        return this.spin;
    }

    public double getOSpin() {
        return this.oSpin;
    }

    public Entity getDisplayEntity() {
        if (this.displayEntity == null && !this.spawnData.isEmpty()) {
            this.displayEntity = EntityType.loadEntityRecursive(this.spawnData, this.level, (entity) -> entity);
        }
        return this.displayEntity;
    }

    // Configuration methods for GUI
    public void configure(int delay, int minDelay, int maxDelay, int playerRange, boolean disabled, int mobLimit) {
        this.customDelay = delay;
        this.minSpawnDelay = minDelay;
        this.maxSpawnDelay = maxDelay;
        this.requiredPlayerRange = playerRange;
        this.disabled = disabled;
        this.mobLimit = Math.max(1, Math.min(10, mobLimit)); // Limit between 1 and 10

        if (!disabled && this.requiredPlayerRange == 0) {
            this.disabled = true;
        } else if (disabled) {
            this.spawnRange = this.requiredPlayerRange;
            this.requiredPlayerRange = 0;
        } else {
            this.spawnRange = 4; // Default spawn range
        }

        this.setChanged();

        if (this.level != null) {
            BlockState blockstate = this.level.getBlockState(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, blockstate, blockstate, 3);
        }
    }

    // Get current configuration values for GUI
    public int getDelay() { return this.spawnDelay; }
    public int getMinSpawnDelay() { return this.minSpawnDelay; }
    public int getMaxSpawnDelay() { return this.maxSpawnDelay; }
    public int getRequiredPlayerRange() { return this.requiredPlayerRange; }
    public int getSpawnRange() { return this.spawnRange; }
    public boolean isDisabled() { return this.disabled; }
    public int getCustomDelay() { return this.customDelay; }

    public int getMobLimit() { return this.mobLimit; }
    public int getCurrentMobCount() { return this.spawnedMobIds.size(); }
    public List<UUID> getSpawnedMobIds() { return new ArrayList<>(this.spawnedMobIds); }

    // Method to manually reset mob tracking (for debugging or admin use)
    public void resetMobTracking() {
        this.spawnedMobIds.clear();
        this.setChanged();
    }

    // Get the current entity type name for display
    public String getCurrentEntityTypeName() {
        if (this.spawnData.contains("id")) {
            return this.spawnData.getString("id");
        }
        return null; // No entity configured
    }

    // Get user-friendly entity name
    public String getCurrentEntityDisplayName() {
        String entityId = getCurrentEntityTypeName();
        if (entityId == null) {
            return "None"; // No entity configured
        }
        // Convert "minecraft:pig" to "Pig"
        if (entityId.contains(":")) {
            String[] parts = entityId.split(":");
            String name = parts[parts.length - 1]; // Get the last part
            return name.substring(0, 1).toUpperCase() + name.substring(1).replace("_", " ");
        }
        return entityId;
    }

    /**
     * Spawn soul fire flame particles around the precise spawner when mob spawns
     */
    private void spawnParticles(ServerLevel level) {
        RandomSource random = level.getRandom();

        // Spawn more particles for a dramatic spawn effect
        for (int i = 0; i < 10; ++i) {
            double offsetX = (double) this.worldPosition.getX() + random.nextDouble();
            double offsetY = (double) this.worldPosition.getY() + random.nextDouble() * 0.5D + 0.2D;
            double offsetZ = (double) this.worldPosition.getZ() + random.nextDouble();

            // Add upward velocity and random horizontal movement for spawn burst
            double velocityX = (random.nextDouble() - 0.5D) * 0.2D;
            double velocityY = random.nextDouble() * 0.15D + 0.1D;
            double velocityZ = (random.nextDouble() - 0.5D) * 0.2D;

            // Send particles to all nearby players
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                offsetX, offsetY, offsetZ,
                1, velocityX, velocityY, velocityZ, 0.0D);
        }
    }

    /**
     * Validates existing mob tracking and rebuilds it by scanning the area.
     * This prevents mob limit bypass when rejoining worlds.
     */
    private void validateAndRebuildMobTracking() {
        if (this.level == null || this.level.isClientSide) {
            return; // Only run on server
        }

        SpawnerMod.LOGGER.info("Validating mob tracking for precise spawner at {}", this.worldPosition);

        // If no entity type is configured, clear tracking and return
        if (this.spawnData.isEmpty() || !this.spawnData.contains("id")) {
            this.spawnedMobIds.clear();
            SpawnerMod.LOGGER.info("Precise spawner at {} has no entity configured, clearing tracking", this.worldPosition);
            return;
        }

        // Get the expected entity type
        String expectedEntityId = this.spawnData.getString("id");
        Optional<EntityType<?>> expectedType = EntityType.byString(expectedEntityId);

        if (expectedType.isEmpty()) {
            this.spawnedMobIds.clear();
            SpawnerMod.LOGGER.warn("Precise spawner at {} has invalid entity type: {}", this.worldPosition, expectedEntityId);
            return;
        }

        // Scan a large area around spawner for mobs of the expected type
        double searchRadius = 32; // Large search area to catch all nearby mobs
        AABB searchArea = new AABB(
            this.worldPosition.getX() - searchRadius,
            this.worldPosition.getY() - 8,
            this.worldPosition.getZ() - searchRadius,
            this.worldPosition.getX() + searchRadius + 1,
            this.worldPosition.getY() + 8,
            this.worldPosition.getZ() + searchRadius + 1
        );

        // Find ALL entities of the expected type in the area
        var nearbyEntities = this.level.getEntitiesOfClass(Entity.class, searchArea,
            entity -> entity.getType().equals(expectedType.get()) && entity.isAlive());

        SpawnerMod.LOGGER.info("Precise spawner at {} found {} nearby entities of type {}",
            this.worldPosition, nearbyEntities.size(), expectedEntityId);

        // Store old count for comparison
        int oldCount = this.spawnedMobIds.size();

        // Rebuild tracking list with nearby mobs (up to mob limit)
        this.spawnedMobIds.clear();
        int trackedCount = 0;

        for (Entity entity : nearbyEntities) {
            if (trackedCount >= this.mobLimit) {
                break; // Don't track more than the limit
            }

            this.spawnedMobIds.add(entity.getUUID());
            trackedCount++;
            SpawnerMod.LOGGER.debug("Tracking mob UUID: {} at distance: {}",
                entity.getUUID(), entity.distanceToSqr(this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ()));
        }

        // If we found mobs, reset spawn delay to prevent immediate spawning
        if (!this.spawnedMobIds.isEmpty()) {
            this.spawnDelay = this.customDelay;
        }

        SpawnerMod.LOGGER.info("Precise spawner at {} tracking updated: {} -> {} mobs of type {} (limit: {})",
            this.worldPosition, oldCount, this.spawnedMobIds.size(), expectedEntityId, this.mobLimit);

        // Force save the changes
        this.setChanged();
    }

    /**
     * Clear entity data (reset spawner to empty state)
     */
    public void clearEntityData() {
        this.spawnData.remove("id");
        this.spawnedMobIds.clear();
        this.displayEntity = null;
        this.setChanged();
    }
}