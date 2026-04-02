package com.branders.spawnermod.networking.packet;

import java.util.function.Supplier;

import com.branders.spawnermod.block.entity.PreciseSpawnerBlockEntity;
import com.branders.spawnermod.config.ConfigValues;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 	Do monster egg drop when player right clicks a spawner.
 * 	Needs to be done on server which requires a network message
 * 
 * 	@author Anders <Branders> Blomqvist
 */
public class SyncSpawnerEggDrop 
{
	private final BlockPos pos;
	
	public SyncSpawnerEggDrop(BlockPos pos) {
		this.pos = pos;
	}
	
	public static void encode(SyncSpawnerEggDrop msg, FriendlyByteBuf buf) {
		buf.writeBlockPos(msg.pos);
	}
	
	public static SyncSpawnerEggDrop decode(FriendlyByteBuf buf) {
		BlockPos pos = new BlockPos(buf.readBlockPos());
		
		return new SyncSpawnerEggDrop(pos);
	}
	
	public static void handle(SyncSpawnerEggDrop msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			
			Level level = ctx.get().getSender().level();
			
			if(level != null) {
		    	// Leave if disabled in config
		    	if(ConfigValues.get("disable_egg_removal_from_spawner") == 1)
		    		return;

				BlockState blockstate = level.getBlockState(msg.pos);
				var blockEntity = level.getBlockEntity(msg.pos);

				String entity_string = null;

				// Handle both regular and precise spawners
				if (blockEntity instanceof SpawnerBlockEntity) {
					// Regular spawner
					SpawnerBlockEntity spawner = (SpawnerBlockEntity)blockEntity;
			    	BaseSpawner logic = spawner.getSpawner();

			    	// Get entity ResourceLocation string from spawner by creating a empty compound which we make our
			    	// spawner logic write to. We can then access what type of entity id the spawner has inside
			    	CompoundTag nbt = new CompoundTag();
			    	nbt = logic.save(nbt);
			    	String nbtString = nbt.get("SpawnData").toString();

			    	// Leave if the spawner does not contain an entity
			        if(nbtString.indexOf("\"") == -1)
			            return;

			    	// Strips the string
			    	// Example: {id: "minecraft:xxx_xx"} --> minecraft:xxx_xx
			    	entity_string = nbtString.substring(nbtString.indexOf("\"") + 1);
			    	entity_string = entity_string.substring(0, entity_string.indexOf("\""));

			    	// Replace the entity inside the spawner with default entity
					logic.setEntityId(EntityType.AREA_EFFECT_CLOUD, level, level.random, msg.pos);
					spawner.setChanged();
				} else if (blockEntity instanceof PreciseSpawnerBlockEntity) {
					// Precise spawner
					PreciseSpawnerBlockEntity preciseSpawner = (PreciseSpawnerBlockEntity)blockEntity;

					// Get entity type from precise spawner
					entity_string = preciseSpawner.getCurrentEntityTypeName();

					// Leave if no entity configured
					if (entity_string == null)
						return;

					// Clear the entity from precise spawner (reset to empty)
					preciseSpawner.clearEntityData();
				} else {
					return; // Not a spawner block entity
				}

				// Leave if the spawner does not contain an egg
				if(entity_string == null || entity_string.equalsIgnoreCase(EntityType.AREA_EFFECT_CLOUD.toString()))
					return;

		    	// Get the entity mob egg and put in an ItemStack
				ItemStack itemStack = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(entity_string + "_spawn_egg")));

				// Get random fly-out position offsets
				double d0 = (double)(level.random.nextFloat() * 0.7F) + (double)0.15F;
		        double d1 = (double)(level.random.nextFloat() * 0.7F) + (double)0.06F + 0.6D;
		        double d2 = (double)(level.random.nextFloat() * 0.7F) + (double)0.15F;

		        // Create entity item
		        ItemEntity entityItem = new ItemEntity(level, (double)msg.pos.getX() + d0, (double)msg.pos.getY() + d1, (double)msg.pos.getZ() + d2, itemStack);
				entityItem.setDefaultPickUpDelay();

				// Spawn entity item (egg)
				level.addFreshEntity(entityItem);

				level.sendBlockUpdated(msg.pos, blockstate, blockstate, 3);
			}
		});
		
		ctx.get().setPacketHandled(true);
	}
}
