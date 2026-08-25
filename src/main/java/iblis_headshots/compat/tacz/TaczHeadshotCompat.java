package iblis_headshots.compat.tacz;

import iblis_headshots.IblisHeadshotsMod;
import iblis_headshots.compat.NativeHeadshotSources;
import iblis_headshots.config.HeadshotsConfig;
import iblis_headshots.util.HeadshotFeedback;
import iblis_headshots.util.HeadshotRules;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.LogicalSide;

/** Lets TACZ own its multiplier while Headshots keeps rules and feedback. */
public final class TaczHeadshotCompat {
    private static final String PRE_EVENT =
            "com.tacz.guns.api.event.common.EntityHurtByGunEvent$Pre";
    private static final String POST_EVENT =
            "com.tacz.guns.api.event.common.EntityHurtByGunEvent$Post";
    private static final String KILL_EVENT =
            "com.tacz.guns.api.event.common.EntityKillByGunEvent";
    private static final String BULLET_TYPE = "com.tacz.guns.entity.EntityKineticBullet";
    private static volatile boolean failed;

    private TaczHeadshotCompat() {
    }

    public static void register() {
        try {
            registerEvents();
        } catch (RuntimeException | LinkageError error) {
            failed = true;
            throw error;
        }
    }

    private static void registerEvents() {
        Class<? extends Event> preType = TaczEventAccess.eventType(PRE_EVENT);
        Class<? extends Event> postType = TaczEventAccess.eventType(POST_EVENT);
        Class<? extends Event> killType = TaczEventAccess.eventType(KILL_EVENT);

        PreAccess pre = new PreAccess(
                TaczEventAccess.method(preType, "getLogicalSide"),
                TaczEventAccess.method(preType, "getHurtEntity"),
                TaczEventAccess.method(preType, "getAttacker"),
                TaczEventAccess.method(preType, "isHeadShot"),
                TaczEventAccess.method(preType, "setHeadshot", boolean.class));
        OutcomeAccess post = new OutcomeAccess(
                TaczEventAccess.method(postType, "getLogicalSide"),
                TaczEventAccess.method(postType, "getHurtEntity"),
                TaczEventAccess.method(postType, "getAttacker"),
                TaczEventAccess.method(postType, "isHeadShot"));
        OutcomeAccess kill = new OutcomeAccess(
                TaczEventAccess.method(killType, "getLogicalSide"),
                TaczEventAccess.method(killType, "getKilledEntity"),
                TaczEventAccess.method(killType, "getAttacker"),
                TaczEventAccess.method(killType, "isHeadShot"));

        TaczEventAccess.listen(preType, EventPriority.LOWEST,
                event -> filterHeadshot(event, pre));
        TaczEventAccess.listen(postType, event -> record(event, post));
        TaczEventAccess.listen(killType, event -> record(event, kill));
        NativeHeadshotSources.register("tacz:headshots",
                source -> HeadshotsConfig.preventTaczDoubleHeadshots && !failed
                        && isTaczBullet(source));
    }

    private static boolean isTaczBullet(DamageSource source) {
        if ("tacz.bullet".equals(source.getMsgId())) {
            return true;
        }
        Entity direct = source.getDirectEntity();
        for (Class<?> type = direct == null ? null : direct.getClass();
             type != null; type = type.getSuperclass()) {
            if (BULLET_TYPE.equals(type.getName())) {
                return true;
            }
        }
        return false;
    }

    private static void filterHeadshot(Object event, PreAccess access) {
        if (failed || !HeadshotsConfig.preventTaczDoubleHeadshots) {
            return;
        }
        try {
            if (TaczEventAccess.call(access.side(), event) != LogicalSide.SERVER
                    || !Boolean.TRUE.equals(TaczEventAccess.call(access.headshot(), event))
                    || !(TaczEventAccess.call(access.target(), event)
                    instanceof LivingEntity victim)) {
                return;
            }
            Entity attacker = TaczEventAccess.call(access.attacker(), event)
                    instanceof Entity entity ? entity : null;
            if (!HeadshotRules.acceptsHeadshot(victim, attacker)) {
                TaczEventAccess.call(access.setHeadshot(), event, false);
            }
        } catch (RuntimeException | LinkageError error) {
            fail(error);
        }
    }

    private static void record(Object event, OutcomeAccess access) {
        if (failed || !HeadshotsConfig.preventTaczDoubleHeadshots) {
            return;
        }
        try {
            if (TaczEventAccess.call(access.side(), event) != LogicalSide.SERVER
                    || !Boolean.TRUE.equals(TaczEventAccess.call(access.headshot(), event))
                    || !(TaczEventAccess.call(access.target(), event)
                    instanceof LivingEntity victim)
                    || !(victim.level() instanceof ServerLevel level)) {
                return;
            }
            Entity attacker = TaczEventAccess.call(access.attacker(), event)
                    instanceof Entity entity ? entity : null;
            HeadshotFeedback.apply(level, victim, attacker);
        } catch (RuntimeException | LinkageError error) {
            fail(error);
        }
    }

    private static void fail(Throwable error) {
        if (!failed) {
            failed = true;
            IblisHeadshotsMod.LOGGER.error(
                    "Disabled failed TACZ Headshots compatibility", error);
        }
    }

    private record PreAccess(Method side, Method target, Method attacker,
                             Method headshot, Method setHeadshot) {
    }

    private record OutcomeAccess(Method side, Method target, Method attacker,
                                 Method headshot) {
    }
}
