package com.branders.spawnermod.client;

import com.branders.spawnermod.client.renderer.PreciseSpawnerRenderer;
import com.branders.spawnermod.registry.BlockEntityRegistry;
import com.branders.spawnermod.registry.BlockRegistry;

import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-side setup for renderers and other client-only functionality.
 *
 * @author Anders <Branders> Blomqvist
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Render type is now set in the block model JSON file (precise_spawner.json)
        // No additional client setup needed for transparency
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Register the precise spawner block entity renderer
        event.registerBlockEntityRenderer(BlockEntityRegistry.PRECISE_SPAWNER.get(), PreciseSpawnerRenderer::new);
    }
}