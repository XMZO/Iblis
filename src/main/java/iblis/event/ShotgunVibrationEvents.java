package iblis.event;

import iblis.IblisMod;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.VanillaGameEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Lets the muzzle vibration resolve before the shot's impact vibrations. */
@Mod.EventBusSubscriber(modid = IblisMod.MOD_ID)
public final class ShotgunVibrationEvents {
    private static final ThreadLocal<ShotContext> ACTIVE_SHOT = new ThreadLocal<>();
    private static final Map<ServerLevel, ArrayDeque<PendingVibration>> PENDING_VIBRATIONS =
            new IdentityHashMap<>();

    private ShotgunVibrationEvents() {
    }

    public static ShotScope begin(ServerLevel level, ServerPlayer shooter) {
        Vec3 origin = shooter.position();
        level.gameEvent(shooter, GameEvent.PROJECTILE_SHOOT, origin);
        ShotContext context = new ShotContext(level, ACTIVE_SHOT.get());
        ACTIVE_SHOT.set(context);
        return new ShotScope(context);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void vanillaGameEvent(VanillaGameEvent event) {
        ShotContext shot = ACTIVE_SHOT.get();
        if (shot == null || event.getLevel() != shot.level) {
            return;
        }
        GameEvent type = event.getVanillaEvent();
        if (type == GameEvent.BLOCK_DESTROY
                || type == GameEvent.ENTITY_DAMAGE
                || type == GameEvent.ENTITY_DIE) {
            shot.record(type, event.getEventPosition(), event.getContext());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.level instanceof ServerLevel level)) {
            return;
        }
        ArrayDeque<PendingVibration> vibrations = PENDING_VIBRATIONS.get(level);
        if (vibrations == null) {
            return;
        }
        long gameTime = level.getGameTime();
        while (!vibrations.isEmpty() && vibrations.peekFirst().gameTime <= gameTime) {
            PendingVibration vibration = vibrations.removeFirst();
            level.gameEvent(vibration.type, vibration.position, vibration.context);
        }
        if (vibrations.isEmpty()) {
            PENDING_VIBRATIONS.remove(level);
        }
    }

    @SubscribeEvent
    public static void levelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            PENDING_VIBRATIONS.remove(level);
        }
    }

    public static final class ShotScope implements AutoCloseable {
        private final ShotContext context;
        private boolean closed;

        private ShotScope(ShotContext context) {
            this.context = context;
        }

        public void landAt(Vec3 position) {
            BlockPos blockPos = BlockPos.containing(position);
            context.record(GameEvent.PROJECTILE_LAND, position,
                    GameEvent.Context.of(context.level.getBlockState(blockPos)));
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (context.previous == null) {
                ACTIVE_SHOT.remove();
            } else {
                ACTIVE_SHOT.set(context.previous);
            }
            if (!context.vibrations.isEmpty()) {
                ArrayDeque<PendingVibration> pending = PENDING_VIBRATIONS.computeIfAbsent(
                        context.level, ignored -> new ArrayDeque<>());
                long gameTime = context.level.getGameTime() + 1L;
                for (RoutedVibration vibration : context.vibrations) {
                    pending.addLast(new PendingVibration(gameTime, vibration.type,
                            vibration.position, vibration.context));
                }
            }
        }
    }

    private static final class ShotContext {
        private final ServerLevel level;
        private final ShotContext previous;
        private final List<RoutedVibration> vibrations = new ArrayList<>(2);

        private ShotContext(ServerLevel level, ShotContext previous) {
            this.level = level;
            this.previous = previous;
        }

        private void record(GameEvent type, Vec3 position, GameEvent.Context context) {
            for (RoutedVibration existing : vibrations) {
                if (existing.type == type
                        && existing.position.distanceToSqr(position) < 0.25) {
                    return;
                }
            }
            vibrations.add(new RoutedVibration(type, position, context));
        }
    }

    private record RoutedVibration(
            GameEvent type, Vec3 position, GameEvent.Context context) {
    }

    private record PendingVibration(
            long gameTime, GameEvent type, Vec3 position, GameEvent.Context context) {
    }
}
