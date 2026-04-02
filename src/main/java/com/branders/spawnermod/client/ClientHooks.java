package com.branders.spawnermod.client;

import com.branders.spawnermod.block.entity.PreciseSpawnerBlockEntity;
import com.branders.spawnermod.gui.PreciseSpawnerConfigGui;
import com.branders.spawnermod.gui.SpawnerConfigGui;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.BaseSpawner;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientHooks {

    public static void openSpawnerGui(BaseSpawner logic, BlockPos pos) {
        Minecraft.getInstance().setScreen(new SpawnerConfigGui(Component.translatable("gui.spawnermod.spawner_config_screen_title"), logic, pos));
    }

    public static void openPreciseSpawnerGui(PreciseSpawnerBlockEntity preciseSpawner, BlockPos pos) {
        Minecraft.getInstance().setScreen(new PreciseSpawnerConfigGui(Component.translatable("gui.spawnermod.precise_spawner_config_screen_title"), preciseSpawner, pos));
    }
}


