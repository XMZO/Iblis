package iblis.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import iblis.IblisMod;
import iblis.item.CrossbowReloadingItem;
import iblis.item.FirearmItem;
import iblis.network.packet.PlayerAnimationPacket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;

@Mod.EventBusSubscriber(modid = IblisMod.MOD_ID, value = Dist.CLIENT)
public final class ClientActionAnimations {
    private static final int KICK_LENGTH = 15;
    private static final Map<Integer, KickState> KICKS = new HashMap<>();
    private static final Set<Integer> PUSHED_POSES = new HashSet<>();

    private ClientActionAnimations() {
    }

    public static void handle(PlayerAnimationPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Entity entity = minecraft.level.getEntity(packet.playerId());
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        if (packet.type() == PlayerAnimationPacket.Type.SHIELD_PUNCH) {
            InteractionHand hand = living.isUsingItem()
                    ? living.getUsedItemHand() : InteractionHand.MAIN_HAND;
            living.swing(hand, true);
        } else {
            KICKS.put(packet.playerId(), new KickState(KICK_LENGTH,
                    Math.max(0.1F, packet.power())));
        }
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || Minecraft.getInstance().isPaused()) {
            return;
        }
        Iterator<Map.Entry<Integer, KickState>> iterator = KICKS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, KickState> entry = iterator.next();
            if (--entry.getValue().ticks <= 0) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderPlayerPre(RenderPlayerEvent.Pre event) {
        if (event.isCanceled()) {
            return;
        }
        KickState kick = KICKS.get(event.getEntity().getId());
        if (kick == null) {
            return;
        }
        float limbSwingAmount = Math.min(1.5F,
                event.getEntity().walkAnimation.speed(event.getPartialTick()) + kick.power);
        if (limbSwingAmount <= 0.0F) {
            return;
        }
        float yaw = event.getEntity().getYRot() * net.minecraft.util.Mth.DEG_TO_RAD;
        float axisYaw = (event.getEntity().getYRot() + 90.0F)
                * net.minecraft.util.Mth.DEG_TO_RAD;
        PoseStack poses = event.getPoseStack();
        poses.pushPose();
        poses.translate(-net.minecraft.util.Mth.sin(yaw) * limbSwingAmount,
                0.0,
                net.minecraft.util.Mth.cos(yaw) * limbSwingAmount);
        poses.mulPose(new Quaternionf().rotationAxis(
                limbSwingAmount * 30.0F * net.minecraft.util.Mth.DEG_TO_RAD,
                -net.minecraft.util.Mth.sin(axisYaw), 0.0F,
                net.minecraft.util.Mth.cos(axisYaw)));
        PUSHED_POSES.add(event.getEntity().getId());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void renderPlayerPost(RenderPlayerEvent.Post event) {
        if (PUSHED_POSES.remove(event.getEntity().getId())) {
            event.getPoseStack().popPose();
        }
    }

    @SubscribeEvent
    public static void renderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (event.getHand() == InteractionHand.MAIN_HAND
                && minecraft.player.isUsingItem()
                && minecraft.player.getUseItem().getItem() instanceof CrossbowReloadingItem) {
            var stack = minecraft.player.getUseItem();
            int cocked = stack.hasTag()
                    ? stack.getTag().getInt(FirearmItem.COCKED_STATE) : 0;
            // The legacy renderer read the compound-list ammo tag as an integer,
            // which always produced zero. Keep the resulting hand pose behavior.
            if (cocked == 0) {
                event.getPoseStack().mulPose(
                        Axis.XP.rotationDegrees(event.getInterpolatedPitch() - 90.0F));
            }
        }
        if (event.getHand() != InteractionHand.OFF_HAND) {
            return;
        }
        KickState kick = KICKS.get(minecraft.player.getId());
        if (kick == null) {
            return;
        }
        float progress = wave(kick, event.getPartialTick());
        renderKickLegs(event, minecraft.player, 1.7F * progress);
    }

    private static void renderKickLegs(RenderHandEvent event,
                                       AbstractClientPlayer player, float progress) {
        EntityRenderer<? super AbstractClientPlayer> renderer =
                Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if (!(renderer instanceof PlayerRenderer playerRenderer)) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = playerRenderer.getModel();
        boolean leftLegVisible = model.leftLeg.visible;
        boolean rightLegVisible = model.rightLeg.visible;
        boolean leftPantsVisible = model.leftPants.visible;
        boolean rightPantsVisible = model.rightPants.visible;

        PoseStack poses = event.getPoseStack();
        poses.pushPose();
        poses.mulPose(Axis.XP.rotationDegrees(event.getInterpolatedPitch() - 180.0F));
        poses.translate(0.0F, -0.52F, -0.4F);
        model.prepareMobModel(player, 1.5F, progress, event.getPartialTick());
        model.setupAnim(player, 1.5F, progress,
                player.tickCount + event.getPartialTick(), 0.0F, 0.0F);
        model.leftLeg.visible = true;
        model.rightLeg.visible = true;
        model.leftPants.visible = player.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG);
        model.rightPants.visible = player.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG);
        VertexConsumer buffer = event.getMultiBufferSource().getBuffer(
                model.renderType(player.getSkinTextureLocation()));
        model.leftLeg.render(poses, buffer, event.getPackedLight(), OverlayTexture.NO_OVERLAY);
        model.leftPants.render(poses, buffer, event.getPackedLight(), OverlayTexture.NO_OVERLAY);
        model.rightLeg.render(poses, buffer, event.getPackedLight(), OverlayTexture.NO_OVERLAY);
        model.rightPants.render(poses, buffer, event.getPackedLight(), OverlayTexture.NO_OVERLAY);
        poses.popPose();

        model.leftLeg.visible = leftLegVisible;
        model.rightLeg.visible = rightLegVisible;
        model.leftPants.visible = leftPantsVisible;
        model.rightPants.visible = rightPantsVisible;
    }

    private static float wave(KickState state, float partialTick) {
        float elapsed = KICK_LENGTH - state.ticks + partialTick;
        return net.minecraft.util.Mth.sin(elapsed / KICK_LENGTH
                * net.minecraft.util.Mth.PI);
    }

    private static final class KickState {
        private int ticks;
        private final float power;

        private KickState(int ticks, float power) {
            this.ticks = ticks;
            this.power = power;
        }
    }
}
