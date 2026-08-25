package iblis.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import iblis.IblisMod;
import iblis.entity.ThrowingKnifeEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Ports the original low-poly blade instead of displaying a flat inventory sprite. */
public final class ThrowingKnifeRenderer extends EntityRenderer<ThrowingKnifeEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            IblisMod.MOD_ID, "textures/particle/particles.png");
    private static final float TEXEL = 1.0F / 256.0F;

    public ThrowingKnifeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ThrowingKnifeEntity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(
                Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 90.0F));
        if (!entity.onHardSurface) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(
                    Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        }
        poseStack.scale(0.05625F, 0.05625F, 0.05625F);
        poseStack.translate(-4.0F, 0.0F, 0.0F);

        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutout(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        drawBlade(consumer, pose, 0.0F, -0.6F, -1.0F, 8.0F, 0.2F, 1.0F,
                0.0F, 28.0F * TEXEL, 16.0F * TEXEL, 23.0F * TEXEL);
        drawBox(consumer, pose, -8.0F, -0.6F, -1.0F, 0.0F, 0.2F, 1.0F,
                0.0F, 10.0F * TEXEL, 23.0F * TEXEL, 33.0F * TEXEL);
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
    }

    private static void drawBlade(VertexConsumer output, PoseStack.Pose pose,
                                  float x1, float y1, float z1, float x2, float y2, float z2,
                                  float u1, float u2, float v1, float v2) {
        float xm = (x1 + x2) * 0.5F;
        float ym = (y1 + y2) * 0.5F;
        float zm = (z1 + z2) * 0.5F;
        float vm = (v1 + v2) * 0.5F;
        quad(output, pose, 0.0F, 1.0F, 0.2F,
                x1, ym, z2, u1, v2, xm, ym, z2, u2, v2,
                x2, ym, zm, u2, vm, x1, y2, zm, u1, vm);
        quad(output, pose, 0.0F, 1.0F, -0.2F,
                x1, y2, zm, u1, vm, x2, ym, zm, u2, vm,
                xm, ym, z1, u2, v1, x1, ym, z1, u1, v1);
        quad(output, pose, 0.0F, -1.0F, 0.2F,
                x1, y1, zm, u1, vm, x2, ym, zm, u2, vm,
                xm, ym, z2, u2, v1, x1, ym, z2, u1, v1);
        quad(output, pose, 0.05625F, 0.0F, 0.0F,
                x1, ym, z1, u1, v2, xm, ym, z1, u2, v2,
                x2, ym, zm, u2, vm, x1, y1, zm, u1, vm);
    }

    private static void drawBox(VertexConsumer output, PoseStack.Pose pose,
                                float x1, float y1, float z1, float x2, float y2, float z2,
                                float u1, float u2, float v1, float v2) {
        quad(output, pose, 0.0F, 1.0F, 0.0F,
                x2, y1, z1, u1, v1, x2, y1, z2, u1, v2,
                x1, y1, z2, u2, v2, x1, y1, z1, u2, v1);
        quad(output, pose, 0.0F, -1.0F, 0.0F,
                x2, y2, z2, u1, v1, x2, y2, z1, u1, v2,
                x1, y2, z1, u2, v2, x1, y2, z2, u2, v1);
        quad(output, pose, 0.0F, 0.0F, 1.0F,
                x1, y2, z1, u1, v1, x2, y2, z1, u1, v2,
                x2, y1, z1, u2, v2, x1, y1, z1, u2, v1);
        quad(output, pose, 0.0F, 0.0F, -1.0F,
                x2, y2, z2, u1, v1, x1, y2, z2, u1, v2,
                x1, y1, z2, u2, v2, x2, y1, z2, u2, v1);
        quad(output, pose, 1.0F, 0.0F, 0.0F,
                x1, y2, z2, u1, v1, x1, y2, z1, u1, v2,
                x1, y1, z1, u2, v2, x1, y1, z2, u2, v1);
        quad(output, pose, -1.0F, 0.0F, 0.0F,
                x2, y2, z1, u1, v1, x2, y2, z2, u1, v2,
                x2, y1, z2, u2, v2, x2, y1, z1, u2, v1);
    }

    private static void quad(VertexConsumer output, PoseStack.Pose pose,
                             float nx, float ny, float nz,
                             float x1, float y1, float z1, float u1, float v1,
                             float x2, float y2, float z2, float u2, float v2,
                             float x3, float y3, float z3, float u3, float v3,
                             float x4, float y4, float z4, float u4, float v4) {
        vertex(output, pose, x1, y1, z1, u1, v1, nx, ny, nz);
        vertex(output, pose, x2, y2, z2, u2, v2, nx, ny, nz);
        vertex(output, pose, x3, y3, z3, u3, v3, nx, ny, nz);
        vertex(output, pose, x4, y4, z4, u4, v4, nx, ny, nz);
    }

    private static void vertex(VertexConsumer output, PoseStack.Pose pose,
                               float x, float y, float z, float u, float v,
                               float nx, float ny, float nz) {
        output.vertex(pose.pose(), x, y, z).color(255, 255, 255, 255).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), nx, ny, nz).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(ThrowingKnifeEntity entity) {
        return TEXTURE;
    }
}
