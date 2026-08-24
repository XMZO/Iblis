package iblis.compat.tconstruct;

import iblis.compat.CompatHooks;
import iblis.player.PlayerSkill;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.tools.entity.ModifiableArrow;
import slimeknights.tconstruct.tools.entity.ThrownShuriken;

/** Skill, projectile, and HUD hooks for Tinkers' Construct ranged equipment. */
public final class TinkersRangedCompat {
    private static final CompatHooks.UseItemProfile BOW_USE =
            new CompatHooks.UseItemProfile(PlayerSkill.ARCHERY, 0);
    private static final CompatHooks.UseItemProfile CROSSBOW_USE =
            new CompatHooks.UseItemProfile(PlayerSkill.SHARPSHOOTING, 0);
    private static final CompatHooks.ProjectileProfile ARROW_PROJECTILE =
            new CompatHooks.ProjectileProfile(PlayerSkill.ARCHERY, true);
    private static final CompatHooks.ProjectileProfile CROSSBOW_PROJECTILE =
            new CompatHooks.ProjectileProfile(PlayerSkill.SHARPSHOOTING, false);
    private static final CompatHooks.ProjectileProfile THROWN_PROJECTILE =
            new CompatHooks.ProjectileProfile(PlayerSkill.THROWING, false);

    private TinkersRangedCompat() {
    }

    static void register() {
        CompatHooks.registerUseItem("tconstruct:ranged_use", stack -> {
            if (stack.is(TinkerTags.Items.CROSSBOWS)) {
                return CROSSBOW_USE;
            }
            if (stack.is(TinkerTags.Items.BOWS)) {
                return BOW_USE;
            }
            return null;
        });
        CompatHooks.registerProjectile("tconstruct:projectiles", entity -> {
            if (entity instanceof ModifiableArrow arrow) {
                return arrow.shotFromCrossbow()
                        ? CROSSBOW_PROJECTILE : ARROW_PROJECTILE;
            }
            if (entity instanceof ThrownShuriken) {
                return THROWN_PROJECTILE;
            }
            return null;
        });
        CompatHooks.registerAimFrame("tconstruct:crossbow_aim",
                (stack, player) -> stack.is(TinkerTags.Items.CROSSBOWS)
                        && !player.isUsingItem());
    }
}
