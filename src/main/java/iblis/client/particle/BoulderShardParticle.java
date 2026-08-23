package iblis.client.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import iblis.registry.IblisItems;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** The small, tumbling item-model shard used by the legacy boulder impact. */
public final class BoulderShardParticle extends Particle {
    private static final ParticleRenderType RENDER_TYPE = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder buffer, TextureManager textureManager) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true);
            RenderSystem.setShader(GameRenderer::getRendertypeCutoutShader);
            RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
        }

        @Override
        public String toString() {
            return "IBLIS_BOULDER_SHARD";
        }
    };

    private final ItemStack boulder = new ItemStack(IblisItems.BOULDER.get());

    private BoulderShardParticle(ClientLevel level, double x, double y, double z,
                                 double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        lifetime = 15;
        gravity = 10.0F;
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTick) {
        Vec3 cameraPosition = camera.getPosition();
        float renderX = (float) (Mth.lerp(partialTick, xo, x) - cameraPosition.x);
        float renderY = (float) (Mth.lerp(partialTick, yo, y) - cameraPosition.y);
        float renderZ = (float) (Mth.lerp(partialTick, zo, z) - cameraPosition.z);

        PoseStack poseStack = new PoseStack();
        poseStack.translate(renderX, renderY, renderZ);
        poseStack.scale(0.1F, 0.1F, 0.1F);
        Vec3 cameraMotion = camera.getEntity().getDeltaMovement();
        double axisLength = cameraMotion.length();
        if (axisLength > 1.0E-7) {
            poseStack.mulPose(Axis.of(new org.joml.Vector3f(
                    (float) (cameraMotion.x / axisLength),
                    (float) (cameraMotion.y / axisLength),
                    (float) (cameraMotion.z / axisLength))).rotationDegrees(random.nextInt(360)));
        }

        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getItemRenderer().getModel(boulder, level, null, 0);
        model = model.applyTransform(ItemDisplayContext.GROUND, poseStack, false);
        minecraft.getItemRenderer().renderModelLists(model, boulder,
                getLightColor(partialTick), OverlayTexture.NO_OVERLAY, poseStack, consumer);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return RENDER_TYPE;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new BoulderShardParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
        }
    }
}
