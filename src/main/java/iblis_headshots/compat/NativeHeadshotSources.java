package iblis_headshots.compat;

import iblis_headshots.IblisHeadshotsMod;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.world.damagesource.DamageSource;

/** Headshots-owned extension point for damage already processed by another mod. */
public final class NativeHeadshotSources {
    private static final List<SafeSource> SOURCES = new CopyOnWriteArrayList<>();

    private NativeHeadshotSources() {
    }

    public static void register(String id, Predicate<DamageSource> predicate) {
        SOURCES.add(new SafeSource(id, predicate));
    }

    public static boolean hasNativeHeadshotDamage(DamageSource source) {
        for (SafeSource candidate : SOURCES) {
            if (candidate.test(source)) {
                return true;
            }
        }
        return false;
    }

    private static final class SafeSource {
        private final String id;
        private final Predicate<DamageSource> predicate;
        private volatile boolean failed;

        private SafeSource(String id, Predicate<DamageSource> predicate) {
            this.id = Objects.requireNonNull(id, "id");
            this.predicate = Objects.requireNonNull(predicate, "predicate");
        }

        private boolean test(DamageSource source) {
            if (failed) {
                return false;
            }
            try {
                return predicate.test(source);
            } catch (RuntimeException | LinkageError error) {
                failed = true;
                IblisHeadshotsMod.LOGGER.error(
                        "Disabled failed native headshot source {}", id, error);
                return false;
            }
        }
    }
}
