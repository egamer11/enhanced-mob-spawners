package com.branders.spawnermod.registry;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class RegistryHandler {

	public static void init() {
		IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

		// Register blocks first
		BlockRegistry.register(bus);

		// Register block entities
		BlockEntityRegistry.register(bus);

		// Register items
		ItemRegistry items = new ItemRegistry();
		bus.register(items);
		items.register(bus);
	}
}
