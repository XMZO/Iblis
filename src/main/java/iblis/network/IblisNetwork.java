package iblis.network;

import iblis.IblisMod;
import iblis.config.IblisConfig;
import iblis.network.packet.SyncGameplayConfigPacket;
import iblis.network.packet.SyncPlayerDataPacket;
import iblis.network.packet.PlayerActionPacket;
import iblis.network.packet.ResetUsePacket;
import iblis.network.packet.ShotImpactPacket;
import iblis.network.packet.PlayerAnimationPacket;
import iblis.player.PlayerDataAccess;
import iblis.player.PlayerAttributeEffects;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.common.crafting.CraftingHelper;
import iblis.crafting.ConfigEnabledCondition;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class IblisNetwork {
    private static final String PROTOCOL = "2";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(IblisMod.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private IblisNetwork() {
    }

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CraftingHelper.register(ConfigEnabledCondition.Serializer.INSTANCE);
            CHANNEL
                .messageBuilder(SyncPlayerDataPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncPlayerDataPacket::encode)
                .decoder(SyncPlayerDataPacket::decode)
                .consumerMainThread(SyncPlayerDataPacket::handle)
                .add();
            CHANNEL
                    .messageBuilder(PlayerActionPacket.class, 1, NetworkDirection.PLAY_TO_SERVER)
                    .encoder(PlayerActionPacket::encode)
                    .decoder(PlayerActionPacket::decode)
                    .consumerMainThread(PlayerActionPacket::handle)
                    .add();
            CHANNEL
                    .messageBuilder(ResetUsePacket.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(ResetUsePacket::encode)
                    .decoder(ResetUsePacket::decode)
                    .consumerMainThread(ResetUsePacket::handle)
                    .add();
            CHANNEL
                    .messageBuilder(ShotImpactPacket.class, 3, NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(ShotImpactPacket::encode)
                    .decoder(ShotImpactPacket::decode)
                    .consumerMainThread(ShotImpactPacket::handle)
                    .add();
            CHANNEL
                    .messageBuilder(PlayerAnimationPacket.class, 4, NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(PlayerAnimationPacket::encode)
                    .decoder(PlayerAnimationPacket::decode)
                    .consumerMainThread(PlayerAnimationPacket::handle)
                    .add();
            CHANNEL
                    .messageBuilder(SyncGameplayConfigPacket.class, 5,
                            NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(SyncGameplayConfigPacket::encode)
                    .decoder(SyncGameplayConfigPacket::decode)
                    .consumerMainThread(SyncGameplayConfigPacket::handle)
                    .add();
        });
    }

    public static void sendGameplayConfig(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncGameplayConfigPacket(IblisConfig.createGameplaySnapshot()));
    }

    /** Pushes live server config changes and refreshes derived attributes. */
    public static void broadcastGameplayConfig() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                PlayerAttributeEffects.refreshMeleeDamageBonus(player);
                PlayerAttributeEffects.refreshWeaponSkill(player);
                PlayerAttributeEffects.refreshSprintingSpeed(
                        player, PlayerDataAccess.get(player).sprintCounter());
                sendGameplayConfig(player);
            }
        });
    }

    public static void sendPlayerData(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncPlayerDataPacket(PlayerDataAccess.get(player).save()));
    }

    public static void sendPlayerAction(PlayerActionPacket.Action action, InteractionHand hand) {
        CHANNEL.sendToServer(new PlayerActionPacket(action, hand, -1));
    }

    public static void sendCharacteristicRaise(int ordinal) {
        CHANNEL.sendToServer(new PlayerActionPacket(
                PlayerActionPacket.Action.RAISE_CHARACTERISTIC,
                InteractionHand.MAIN_HAND, ordinal));
    }

    public static void sendTrainToCraft() {
        CHANNEL.sendToServer(new PlayerActionPacket(
                PlayerActionPacket.Action.TRAIN_TO_CRAFT,
                InteractionHand.MAIN_HAND, -1));
    }

    public static void sendSprintButtonState(int counter) {
        CHANNEL.sendToServer(new PlayerActionPacket(
                PlayerActionPacket.Action.SPRINT_BUTTON_STATE,
                InteractionHand.MAIN_HAND, counter));
    }

    public static void sendSprintState(int counter) {
        CHANNEL.sendToServer(new PlayerActionPacket(
                PlayerActionPacket.Action.SPRINT_STATE,
                InteractionHand.MAIN_HAND, counter));
    }

    public static void resetClientUse(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ResetUsePacket());
    }

    public static void sendShotImpact(ServerLevel level, Vec3 position, Direction face,
                                      int ammunitionType, float splashCone, double distance,
                                      Vec3 bloodPosition, Direction bloodFace, int bloodColour) {
        ShotImpactPacket packet = new ShotImpactPacket(position, face, ammunitionType,
                splashCone, (float) distance, bloodPosition, bloodFace, bloodColour);
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                position.x, position.y, position.z, 64.0, level.dimension())), packet);
    }

    public static void sendPlayerAnimation(ServerPlayer player,
                                           PlayerAnimationPacket.Type type, float power) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new PlayerAnimationPacket(player.getId(), type, power));
    }
}
