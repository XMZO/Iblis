package iblis_headshots.client;

import iblis_headshots.client.particle.HeadshotParticle;
import iblis_headshots.config.HeadshotsConfig;
import iblis_headshots.network.packet.HeadshotParticlePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class HeadshotsClient {
    private HeadshotsClient() {
    }

    public static void spawnParticle(HeadshotParticlePacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        if (HeadshotsConfig.particleType != 0 && minecraft.getCameraEntity() != null) {
            double distance = Math.sqrt(
                    minecraft.getCameraEntity().distanceToSqr(packet.position()));
            float size = (float) (distance / 1000.0 + 0.01) * HeadshotsConfig.particleSize;
            minecraft.particleEngine.add(new HeadshotParticle(
                    minecraft.level, packet.position(), packet.speed(), size,
                    HeadshotsConfig.particleType, packet.lifetime()));
        }
        if (minecraft.player != null) {
            minecraft.level.playLocalSound(
                    minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS,
                    0.25F, 1.0F, false);
        }
    }
}
