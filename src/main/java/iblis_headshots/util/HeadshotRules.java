package iblis_headshots.util;

import iblis_headshots.config.HeadshotsConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class HeadshotRules {
    private HeadshotRules() {
    }

    /** Called only after geometry has confirmed that the player's head was hit. */
    public static boolean acceptsPlayerHeadshot(LivingEntity victim, Entity attacker) {
        if (!(victim instanceof Player)) {
            return true;
        }
        if (HeadshotsConfig.playersHaveNoHeads) {
            return false;
        }
        if (!HeadshotsConfig.playerHeadshotChanceAffectsPvp && attacker instanceof Player) {
            return true;
        }
        float chance = HeadshotsConfig.playerHeadshotChance;
        return chance >= 1.0F || chance > 0.0F && victim.getRandom().nextFloat() < chance;
    }
}
