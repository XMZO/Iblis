package iblis_headshots.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import iblis_headshots.IblisHeadshotsMod;
import iblis_headshots.util.HeadshotGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IblisHeadshotsMod.MOD_ID, value = Dist.CLIENT)
public final class HeadshotDebugRenderer {
    private HeadshotDebugRenderer() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null
                || !minecraft.getEntityRenderDispatcher().shouldRenderHitBoxes()) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poses = event.getPoseStack();
        poses.pushPose();
        poses.translate(-camera.x, -camera.y, -camera.z);
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        AABB search = minecraft.player.getBoundingBox().inflate(6.0);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, search,
                candidate -> candidate != minecraft.player)) {
            AABB head = HeadshotGeometry.getHeadBox(living);
            if (head.getSize() > 0.0) {
                net.minecraft.client.renderer.LevelRenderer.renderLineBox(
                        poses, lines, head.inflate(0.002), 1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
        buffers.endBatch(RenderType.lines());
        poses.popPose();
    }
}
