package com.branders.spawnermod.registry;

import com.branders.spawnermod.SpawnerMod;
import com.branders.spawnermod.block.entity.PreciseSpawnerBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for block entities in the SpawnerMod.
 *
 * @author Anders <Branders> Blomqvist
 */
public class BlockEntityRegistry {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SpawnerMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<PreciseSpawnerBlockEntity>> PRECISE_SPAWNER =
            BLOCK_ENTITIES.register("precise_spawner",
                    () -> BlockEntityType.Builder.of(PreciseSpawnerBlockEntity::new,
                            BlockRegistry.PRECISE_SPAWNER.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}