package com.branders.spawnermod.registry;

import com.branders.spawnermod.SpawnerMod;
import com.branders.spawnermod.block.PreciseSpawnerBlock;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for blocks in the SpawnerMod.
 *
 * @author Anders <Branders> Blomqvist
 */
public class BlockRegistry {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, SpawnerMod.MOD_ID);

    public static final RegistryObject<Block> PRECISE_SPAWNER =
            BLOCKS.register("precise_spawner", PreciseSpawnerBlock::new);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}