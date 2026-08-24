package iblis.compat.tacz;

import iblis.IblisMod;
import iblis.player.PlayerSkill;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.LogicalSide;

/** Awards one bounded sharpshooting action for each server-confirmed TACZ hit. */
final class TaczExperienceCompat {
    private static final String HURT_EVENT =
            "com.tacz.guns.api.event.common.EntityHurtByGunEvent$Post";
    private static final String KILL_EVENT =
            "com.tacz.guns.api.event.common.EntityKillByGunEvent";
    private static volatile boolean failed;

    private TaczExperienceCompat() {
    }

    static void register() {
        Class<? extends Event> hurtType = TaczEventAccess.eventType(HURT_EVENT);
        Class<? extends Event> killType = TaczEventAccess.eventType(KILL_EVENT);
        HitAccess hurt = new HitAccess(
                TaczEventAccess.method(hurtType, "getLogicalSide"),
                TaczEventAccess.method(hurtType, "getHurtEntity"),
                TaczEventAccess.method(hurtType, "getAttacker"),
                TaczEventAccess.method(hurtType, "getBaseAmount"));
        HitAccess kill = new HitAccess(
                TaczEventAccess.method(killType, "getLogicalSide"),
                TaczEventAccess.method(killType, "getKilledEntity"),
                TaczEventAccess.method(killType, "getAttacker"),
                TaczEventAccess.method(killType, "getBaseDamage"));
        TaczEventAccess.listen(hurtType, event -> award(event, hurt));
        TaczEventAccess.listen(killType, event -> award(event, kill));
    }

    private static void award(Object event, HitAccess access) {
        if (failed) {
            return;
        }
        try {
            if (TaczEventAccess.call(access.side(), event) != LogicalSide.SERVER
                    || !(TaczEventAccess.call(access.target(), event)
                    instanceof LivingEntity target)
                    || !(TaczEventAccess.call(access.attacker(), event)
                    instanceof ServerPlayer player)) {
                return;
            }
            float damage = ((Number) TaczEventAccess.call(access.damage(), event)).floatValue();
            float training = Math.min(target.getMaxHealth(), Math.max(damage, 0.0F));
            if (training > 0.0F && Float.isFinite(training)) {
                PlayerSkill.SHARPSHOOTING.raise(player, training);
            }
        } catch (RuntimeException | LinkageError error) {
            if (!failed) {
                failed = true;
                IblisMod.LOGGER.error(
                        "Disabled failed TACZ sharpshooting experience compatibility", error);
            }
        }
    }

    private record HitAccess(Method side, Method target, Method attacker, Method damage) {
    }
}
