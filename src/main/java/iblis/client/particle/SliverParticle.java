package iblis.client.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import iblis.IblisMod;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;

public final class SliverParticle extends Particle {
    public static final ResourceLocation[] MODELS = {
            model("sliver_1"), model("sliver_2"), model("sliver_3")
    };

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
            return "IBLIS_SLIVER";
        }
    };

    private final ResourceLocation modelLocation;
    private final float rotationYaw;
    private final float rotationPitch;

    private SliverParticle(ClientLevel level, double x, double y, double z,
                           double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        lifetime = 15;
        gravity = 10.0F;
        modelLocation = MODELS[random.nextInt(MODELS.length)];
        float horizontalSpeed = Mth.sqrt((float) (xd * xd + zd * zd));
        rotationYaw = (float) (Mth.atan2(xd, zd) * Mth.RAD_TO_DEG);
        rotationPitch = (float) (Mth.atan2(yd, horizontalSpeed) * Mth.RAD_TO_DEG);
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
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationYaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotationPitch));

        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getModelManager().getModel(modelLocation);
        minecraft.getItemRenderer().renderModelLists(model, ItemStack.EMPTY,
                getLightColor(partialTick), OverlayTexture.NO_OVERLAY, poseStack, consumer);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return RENDER_TYPE;
    }

    private static ResourceLocation model(String name) {
        return ResourceLocation.fromNamespaceAndPath(IblisMod.MOD_ID, "item/" + name);
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new SliverParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
        }
    }
}
