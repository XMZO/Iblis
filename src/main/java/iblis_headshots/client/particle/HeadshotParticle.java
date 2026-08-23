package iblis_headshots.client.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class HeadshotParticle extends Particle {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "iblis_headshots", "textures/particle/particles.png");

    private final float size;
    private final int particleType;

    public HeadshotParticle(ClientLevel level, Vec3 position, Vec3 speed,
                            float size, int particleType, int lifetime) {
        super(level, position.x, position.y, position.z);
        this.size = size;
        this.particleType = particleType;
        this.lifetime = lifetime;
        this.xd = speed.x * (random.nextDouble() * 1.5 - 0.5);
        this.yd = speed.y * (random.nextDouble() * 1.5 - 0.5);
        this.zd = speed.z * (random.nextDouble() * 1.5 - 0.5);
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float partialTick) {
        Vec3 cameraPosition = camera.getPosition();
        float renderX = (float) (Mth.lerp(partialTick, xo, x) - cameraPosition.x);
        float renderY = (float) (Mth.lerp(partialTick, yo, y) - cameraPosition.y);
        float renderZ = (float) (Mth.lerp(partialTick, zo, z) - cameraPosition.z);
        Quaternionf rotation = new Quaternionf(camera.rotation());
        float scale = 2.0F * size;

        Vector3f[] corners = {
                new Vector3f(-1.0F, -1.0F, 0.0F),
                new Vector3f(-1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, -1.0F, 0.0F)
        };
        for (Vector3f corner : corners) {
            corner.rotate(rotation).mul(scale).add(renderX, renderY, renderZ);
        }

        float minU = (particleType - 1) * 16.0F / 256.0F;
        float maxU = minU + 16.0F / 256.0F;
        float minV = 177.0F / 256.0F;
        float maxV = minV + 16.0F / 256.0F;

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        vertex(buffer, corners[0], maxU, maxV);
        vertex(buffer, corners[1], maxU, minV);
        vertex(buffer, corners[2], minU, minV);
        vertex(buffer, corners[3], minU, maxV);
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
    }

    private void vertex(VertexConsumer buffer, Vector3f position, float u, float v) {
        buffer.vertex(position.x(), position.y(), position.z())
                .uv(u, v)
                .color((int) (rCol * 255.0F), (int) (gCol * 255.0F),
                        (int) (bCol * 255.0F), 255)
                .endVertex();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }
}
