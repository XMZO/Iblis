package iblis.client.network;

import iblis.IblisMod;
import iblis.config.IblisConfig;
import iblis.network.packet.ShotImpactPacket;
import iblis.client.particle.DecalManager;
import iblis.player.PlayerDataAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;

public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    public static void handlePlayerData(CompoundTag tag) {
        if (Minecraft.getInstance().player != null) {
            PlayerDataAccess.get(Minecraft.getInstance().player).load(tag);
        }
    }

    public static void handleGameplayConfig(CompoundTag tag) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean remoteServer = minecraft.getSingleplayerServer() == null;
        if (!IblisConfig.applyServerSnapshot(tag, remoteServer)) {
            IblisMod.LOGGER.warn("Rejected an invalid Iblis gameplay config snapshot");
        }
    }

    public static void resetUseAndAttackCooldown() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (minecraft.player.isUsingItem()) {
            InteractionHand hand = minecraft.player.getUsedItemHand();
            minecraft.player.stopUsingItem();
            minecraft.player.startUsingItem(hand);
        }
        minecraft.player.resetAttackStrengthTicker();
    }

    public static void handleShotImpact(ShotImpactPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        DecalManager.handleShotImpact(packet);
    }
}
