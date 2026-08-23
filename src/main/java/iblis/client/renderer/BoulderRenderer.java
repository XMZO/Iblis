package iblis.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import iblis.entity.BoulderEntity;
import iblis.registry.IblisItems;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** Preserves the small, motion-axis spin used by the 1.12.2 boulder renderer. */
public final class BoulderRenderer extends EntityRenderer<BoulderEntity> {
    private final ItemRenderer itemRenderer;
    private final ItemStack boulder = new ItemStack(IblisItems.BOULDER.get());
    private int frame;

    public BoulderRenderer(EntityRendererProvider.Context context) {
        super(context);
        itemRenderer = context.getItemRenderer();
        shadowRadius = 0.15F;
        shadowStrength = 0.75F;
    }

    @Override
    public void render(BoulderEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(0.25F, 0.25F, 0.25F);
        Vec3 movement = entity.getDeltaMovement();
        double length = movement.length();
        if (length > 1.0E-7) {
            poseStack.mulPose(new Quaternionf().rotationAxis(
                    (float) Math.toRadians(++frame),
                    (float) (movement.x / length),
                    (float) (movement.y / length),
                    (float) (movement.z / length)));
        } else {
            frame++;
        }
        itemRenderer.renderStatic(boulder, ItemDisplayContext.GROUND, packedLight,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                poseStack, buffers, entity.level(), entity.getId());
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(BoulderEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
