package iblis_headshots.util;

import iblis_headshots.config.HeadshotsConfig;
import iblis_headshots.config.HeadshotEntityBlacklist;
import iblis_headshots.config.HeadshotEntityWhitelist;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class HeadshotRules {
    private HeadshotRules() {
    }

    /** Called only after geometry has confirmed that the entity's head was hit. */
    public static boolean acceptsHeadshot(LivingEntity victim, Entity attacker) {
        if (!allowsHeadshots(victim)) {
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

    public static boolean allowsHeadshots(Entity entity) {
        return entity != null && allowsHeadshots(entity.getType());
    }

    public static boolean allowsHeadshots(EntityType<?> type) {
        return HeadshotsConfig.headshotEntityWhitelistEnabled
                ? HeadshotEntityWhitelist.contains(type)
                : !HeadshotsConfig.headshotEntityBlacklistEnabled
                || !HeadshotEntityBlacklist.contains(type);
    }

    public static float damageMultiplier(LivingEntity victim, Entity attacker) {
        if (victim instanceof Player && attacker instanceof LivingEntity
                && !(attacker instanceof Player)) {
            return HeadshotsConfig.mobToPlayerDamageMultiplier;
        }
        return HeadshotsConfig.damageMultiplier;
    }
}
