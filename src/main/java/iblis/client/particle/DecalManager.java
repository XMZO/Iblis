package iblis.client.particle;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import iblis.IblisMod;
import iblis.network.packet.ShotImpactPacket;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IblisMod.MOD_ID, value = Dist.CLIENT)
public final class DecalManager {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            IblisMod.MOD_ID, "textures/particle/particles.png");
    private static final RenderType RENDER_TYPE = DecalRenderType.create(TEXTURE);
    private static final BufferBuilder BUFFER = new BufferBuilder(RENDER_TYPE.bufferSize());
    private static final double SURFACE_OFFSET = 1.0 / 256.0;
    private static final double LAYER_OFFSET = 1.0 / 8192.0;
    private static final int MAX_DECALS = 256;
    private static final long LIFETIME = 1500L;
    private static final Deque<Decal> DECALS = new ArrayDeque<>();
    private static ClientLevel currentLevel;

    private DecalManager() {
    }

    public static void handleShotImpact(ShotImpactPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        ensureLevel(level);
        BlockPos target = BlockPos.containing(packet.position().subtract(
                packet.face().getStepX() * 0.01,
                packet.face().getStepY() * 0.01,
                packet.face().getStepZ() * 0.01));
        if (!level.getBlockState(target).isAir()) {
            int colour = decalColour(level, target);
            int sprite = packet.ammunitionType() == 0 ? 0 : 2;
            float size = packet.ammunitionType() == 0 ? 0.6F
                    : packet.distance() * packet.splashCone() * 2.0F + 0.3F;
            addAcrossFace(level, packet.position(), packet.face(), target,
                    Math.min(size, 3.0F), colour, sprite);
        }
        if (packet.bloodPosition() != null && packet.bloodFace() != null
                && packet.bloodColour() >= 0) {
            BlockPos bloodTarget = BlockPos.containing(packet.bloodPosition().subtract(
                    packet.bloodFace().getStepX() * 0.01,
                    packet.bloodFace().getStepY() * 0.01,
                    packet.bloodFace().getStepZ() * 0.01));
            if (!level.getBlockState(bloodTarget).isAir()) {
                addAcrossFace(level, packet.bloodPosition(), packet.bloodFace(), bloodTarget,
                        1.6F, packet.bloodColour(), 1);
            }
        }
    }

    private static int decalColour(ClientLevel level, BlockPos position) {
        BlockState state = level.getBlockState(position);
        if (state.is(Blocks.GRASS_BLOCK)) {
            return 0x996A45;
        }
        if (state.is(BlockTags.BEDS)) {
            return 0x999999;
        }
        return state.getMapColor(level, position).col;
    }

    private static void addAcrossFace(ClientLevel level, Vec3 center, Direction face,
                                      BlockPos target, float size, int colour, int sprite) {
        double half = size * 0.5;
        int minA;
        int maxA;
        int minB;
        int maxB;
        if (face.getAxis() == Direction.Axis.Y) {
            minA = net.minecraft.util.Mth.floor(center.x - half);
            maxA = net.minecraft.util.Mth.floor(center.x + half);
            minB = net.minecraft.util.Mth.floor(center.z - half);
            maxB = net.minecraft.util.Mth.floor(center.z + half);
            for (int x = minA; x <= maxA; x++) {
                for (int z = minB; z <= maxB; z++) {
                    add(level, center, face, new BlockPos(x, target.getY(), z), size, colour, sprite);
                }
            }
        } else if (face.getAxis() == Direction.Axis.Z) {
            minA = net.minecraft.util.Mth.floor(center.x - half);
            maxA = net.minecraft.util.Mth.floor(center.x + half);
            minB = net.minecraft.util.Mth.floor(center.y - half);
            maxB = net.minecraft.util.Mth.floor(center.y + half);
            for (int x = minA; x <= maxA; x++) {
                for (int y = minB; y <= maxB; y++) {
                    add(level, center, face, new BlockPos(x, y, target.getZ()), size, colour, sprite);
                }
            }
        } else {
            minA = net.minecraft.util.Mth.floor(center.y - half);
            maxA = net.minecraft.util.Mth.floor(center.y + half);
            minB = net.minecraft.util.Mth.floor(center.z - half);
            maxB = net.minecraft.util.Mth.floor(center.z + half);
            for (int y = minA; y <= maxA; y++) {
                for (int z = minB; z <= maxB; z++) {
                    add(level, center, face, new BlockPos(target.getX(), y, z), size, colour, sprite);
                }
            }
        }
    }

    private static void add(ClientLevel level, Vec3 center, Direction face,
                            BlockPos blockPos, float size, int colour, int sprite) {
        BlockState state = level.getBlockState(blockPos);
        if (state.isAir() || state.getCollisionShape(level, blockPos).isEmpty()) {
            return;
        }
        int layer = 0;
        for (Decal decal : DECALS) {
            if (decal.blockPos.equals(blockPos) && decal.face == face) {
                layer++;
            }
        }
        DECALS.addLast(new Decal(center, face, blockPos, blockPos.relative(face), state, size, colour,
                sprite, level.random.nextInt(4), level.getGameTime() + LIFETIME, layer));
        while (DECALS.size() > MAX_DECALS) {
            DECALS.removeFirst();
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            DECALS.clear();
            currentLevel = null;
            return;
        }
        ensureLevel(level);
        long time = level.getGameTime();
        DECALS.removeIf(decal -> decal.expiresAt <= time
                || level.getBlockState(decal.blockPos) != decal.state);
        if (DECALS.isEmpty()) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        PoseStack.Pose pose = event.getPoseStack().last();
        boolean building = false;
        for (Decal decal : DECALS) {
            if (decal.center.distanceToSqr(camera) <= 16384.0) {
                if (!building) {
                    BUFFER.begin(RENDER_TYPE.mode(), RENDER_TYPE.format());
                    building = true;
                }
                renderDecal(level, pose, BUFFER, decal, camera);
            }
        }
        if (building) {
            RENDER_TYPE.end(BUFFER, VertexSorting.DISTANCE_TO_ORIGIN);
        }
    }

    private static void renderDecal(ClientLevel level, PoseStack.Pose pose,
                                    VertexConsumer consumer, Decal decal, Vec3 camera) {
        double half = decal.size * 0.5;
        double offset = SURFACE_OFFSET + decal.layer * LAYER_OFFSET;
        double x1 = Math.max(decal.center.x - half, decal.blockPos.getX()) - camera.x;
        double x2 = Math.min(decal.center.x + half, decal.blockPos.getX() + 1.0) - camera.x;
        double y1 = Math.max(decal.center.y - half, decal.blockPos.getY()) - camera.y;
        double y2 = Math.min(decal.center.y + half, decal.blockPos.getY() + 1.0) - camera.y;
        double z1 = Math.max(decal.center.z - half, decal.blockPos.getZ()) - camera.z;
        double z2 = Math.min(decal.center.z + half, decal.blockPos.getZ() + 1.0) - camera.z;
        double x = decal.center.x - camera.x + decal.face.getStepX() * offset;
        double y = decal.center.y - camera.y + decal.face.getStepY() * offset;
        double z = decal.center.z - camera.z + decal.face.getStepZ() * offset;

        float u1 = decal.spriteX * 32.0F / 256.0F;
        float u2 = u1 + 32.0F / 256.0F;
        float v1 = (33.0F + decal.spriteY * 32.0F) / 256.0F;
        float v2 = v1 + 32.0F / 256.0F;
        int red = decal.colour >> 16 & 255;
        int green = decal.colour >> 8 & 255;
        int blue = decal.colour & 255;
        int light = LevelRenderer.getLightColor(level, decal.lightPos);
        switch (decal.face) {
            case UP, DOWN -> quad(consumer, pose,
                    x1, y, z1, x2, y, z1, x2, y, z2, x1, y, z2,
                    u1, v1, u2, v2, red, green, blue, light, decal.face);
            case NORTH, SOUTH -> quad(consumer, pose,
                    x1, y1, z, x2, y1, z, x2, y2, z, x1, y2, z,
                    u1, v2, u2, v1, red, green, blue, light, decal.face);
            case WEST, EAST -> quad(consumer, pose,
                    x, y1, z1, x, y1, z2, x, y2, z2, x, y2, z1,
                    u1, v2, u2, v1, red, green, blue, light, decal.face);
        }
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose,
                             double x1, double y1, double z1,
                             double x2, double y2, double z2,
                             double x3, double y3, double z3,
                             double x4, double y4, double z4,
                             float u1, float v1, float u2, float v2,
                             int red, int green, int blue, int light, Direction normal) {
        vertex(consumer, pose, x1, y1, z1, u1, v1, red, green, blue, light, normal);
        vertex(consumer, pose, x2, y2, z2, u2, v1, red, green, blue, light, normal);
        vertex(consumer, pose, x3, y3, z3, u2, v2, red, green, blue, light, normal);
        vertex(consumer, pose, x4, y4, z4, u1, v2, red, green, blue, light, normal);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                               double x, double y, double z, float u, float v,
                               int red, int green, int blue, int light, Direction normal) {
        consumer.vertex(pose.pose(), (float) x, (float) y, (float) z)
                .color(red, green, blue, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose.normal(), normal.getStepX(), normal.getStepY(), normal.getStepZ())
                .endVertex();
    }

    private static void ensureLevel(ClientLevel level) {
        if (level != currentLevel) {
            DECALS.clear();
            currentLevel = level;
        }
    }

    private record Decal(Vec3 center, Direction face, BlockPos blockPos, BlockPos lightPos,
                         BlockState state, float size, int colour,
                         int spriteX, int spriteY, long expiresAt, int layer) {
    }
}
