package iblis_headshots.util;

import iblis_headshots.advancement.HeadshotTrigger;
import iblis_headshots.network.HeadshotsNetwork;
import iblis_headshots.stats.HeadshotScoreboardCriteria;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Shared feedback and tracking for a confirmed headshot. */
public final class HeadshotFeedback {
    private static final Vec3 PARTICLE_SPEED = new Vec3(0.0, 0.2, 0.0);

    private HeadshotFeedback() {
    }

    public static void apply(ServerLevel level, LivingEntity victim, Entity attacker) {
        HeadshotsNetwork.spawnParticle(level,
                victim.position().add(0.0, victim.getEyeHeight(), 0.0),
                PARTICLE_SPEED, 15);
        HeadshotScoreboardCriteria.record(victim, attacker);

        if (attacker instanceof ServerPlayer player && !(victim instanceof ServerPlayer)) {
            HeadshotTrigger.INSTANCE.trigger(player, victim);
        } else if (victim instanceof ServerPlayer player) {
            HeadshotTrigger.INSTANCE.trigger(player, victim);
        }
    }
}
