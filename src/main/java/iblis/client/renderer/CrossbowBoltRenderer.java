package iblis.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import iblis.entity.CrossbowBoltEntity;
import iblis.registry.IblisItems;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Renders the OBJ bolt with the same orientation and scale as the 1.12.2 renderer. */
public final class CrossbowBoltRenderer extends EntityRenderer<CrossbowBoltEntity> {
    private final ItemRenderer itemRenderer;
    private final ItemStack bolt = new ItemStack(IblisItems.CROSSBOW_BOLT.get());

    public CrossbowBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
        itemRenderer = context.getItemRenderer();
        shadowRadius = 0.15F;
        shadowStrength = 0.75F;
    }

    @Override
    public void render(CrossbowBoltEntity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(
                Mth.lerp(partialTick, entity.yRotO, entity.getYRot())));
        poseStack.mulPose(Axis.XN.rotationDegrees(
                Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        itemRenderer.renderStatic(bolt, ItemDisplayContext.NONE, packedLight,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                poseStack, buffers, entity.level(), entity.getId());
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(CrossbowBoltEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
