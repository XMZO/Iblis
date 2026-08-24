package iblis_headshots.util;

import iblis_headshots.config.HeadshotsConfig;
import iblis_headshots.config.HeadshotEntityBlacklist;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class HeadshotRules {
    private HeadshotRules() {
    }

    /** Called only after geometry has confirmed that the entity's head was hit. */
    public static boolean acceptsHeadshot(LivingEntity victim, Entity attacker) {
        if (HeadshotEntityBlacklist.contains(victim)) {
            return false;
        }
        if (!(victim instanceof Player)) {
            return true;
        }
        if (!HeadshotsConfig.playerHeadshotChanceAffectsPvp && attacker instanceof Player) {
            return true;
        }
        float chance = HeadshotsConfig.playerHeadshotChance;
        return chance >= 1.0F || chance > 0.0F && victim.getRandom().nextFloat() < chance;
    }

    public static float damageMultiplier(LivingEntity victim, Entity attacker) {
        if (victim instanceof Player && attacker instanceof LivingEntity
                && !(attacker instanceof Player)) {
            return HeadshotsConfig.mobToPlayerDamageMultiplier;
        }
        return HeadshotsConfig.damageMultiplier;
    }
}
