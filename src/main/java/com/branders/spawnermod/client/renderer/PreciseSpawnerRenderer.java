package com.branders.spawnermod.client.renderer;

import com.branders.spawnermod.block.entity.PreciseSpawnerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Renderer for the Precise Spawner that shows the spinning mob inside like vanilla spawners.
 *
 * @author Anders <Branders> Blomqvist
 */
@OnlyIn(Dist.CLIENT)
public class PreciseSpawnerRenderer implements BlockEntityRenderer<PreciseSpawnerBlockEntity> {

    private final EntityRenderDispatcher entityRenderer;

    public PreciseSpawnerRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.getEntityRenderer();
    }

    @Override
    public void render(PreciseSpawnerBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        // Get the display entity from the spawner
        Entity entity = blockEntity.getDisplayEntity();
        if (entity != null) {
            renderFloatingItem(blockEntity, partialTick, poseStack, bufferSource, packedLight, entity);
        }
    }

    /**
     * Renders the spinning mob inside the spawner, similar to vanilla spawner behavior.
     */
    private void renderFloatingItem(PreciseSpawnerBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, Entity entity) {
        poseStack.pushPose();

        // Center the entity in the spawner block
        poseStack.translate(0.5D, 0.0D, 0.5D);

        // Calculate smooth rotation based on spin values from block entity (like vanilla)
        float spinTime = Mth.lerp(partialTick, (float) blockEntity.getOSpin(), (float) blockEntity.getSpin());
        float rotation = spinTime * 10.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        // Position entity in center of spawner (no bobbing, just spinning)
        poseStack.translate(0.0D, 0.20D, 0.0D);

        // Scale the entity to be smaller and fit nicely in the spawner
        float scale = 0.33125F; // Same scale as vanilla spawner
        poseStack.scale(scale, scale, scale);

        // Set entity rotation
        entity.setYRot(rotation);

        try {
            // Render the entity
            this.entityRenderer.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, partialTick, poseStack, bufferSource, packedLight);
        } catch (Exception e) {
            // If rendering fails for any reason, just skip it to prevent crashes
            // This can happen with some modded entities
        }

        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return 80; // Same as vanilla spawner
    }
}