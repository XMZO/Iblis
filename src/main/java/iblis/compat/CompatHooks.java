package iblis.compat;

import iblis.IblisMod;
import iblis.player.PlayerSkill;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Small, dependency-free extension points used by optional compatibility modules.
 * Registrations happen once during startup; gameplay reads use snapshot lists.
 */
public final class CompatHooks {
    private static final List<CraftingHook> CRAFTING = new CopyOnWriteArrayList<>();
    private static final List<SafeFunction<ItemStack, UseItemProfile>> USE_ITEMS =
            new CopyOnWriteArrayList<>();
    private static final List<SafeFunction<Entity, ProjectileProfile>> PROJECTILES =
            new CopyOnWriteArrayList<>();
    private static final List<SafeBiPredicate<ItemStack, Player>> AIM_FRAMES =
            new CopyOnWriteArrayList<>();
    private static final List<SafePredicate<DamageSource>> NATIVE_HEADSHOT_SOURCES =
            new CopyOnWriteArrayList<>();

    private CompatHooks() {
    }

    public static void registerCrafting(String id, Predicate<ItemStack> matches,
                                        Consumer<PlayerEvent.ItemCraftedEvent> handler) {
        CRAFTING.add(new CraftingHook(id, matches, handler));
    }

    public static void registerUseItem(String id,
                                       Function<ItemStack, UseItemProfile> classifier) {
        USE_ITEMS.add(new SafeFunction<>(id, classifier));
    }

    public static void registerProjectile(String id,
                                          Function<Entity, ProjectileProfile> classifier) {
        PROJECTILES.add(new SafeFunction<>(id, classifier));
    }

    public static void registerAimFrame(String id,
                                        BiPredicate<ItemStack, Player> predicate) {
        AIM_FRAMES.add(new SafeBiPredicate<>(id, predicate));
    }

    /** Marks damage already processed by an optional mod's native headshot system. */
    public static void registerNativeHeadshotSource(String id,
                                                    Predicate<DamageSource> predicate) {
        NATIVE_HEADSHOT_SOURCES.add(new SafePredicate<>(id, predicate));
    }

    /** Returns true when an optional module owns this crafting result. */
    public static boolean handleItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        for (CraftingHook hook : CRAFTING) {
            if (hook.handle(event)) {
                return true;
            }
        }
        return false;
    }

    /** Used to keep generic anvil quality code away from externally managed tools. */
    public static boolean ownsCraftingResult(ItemStack stack) {
        for (CraftingHook hook : CRAFTING) {
            if (hook.matches(stack)) {
                return true;
            }
        }
        return false;
    }

    public static UseItemProfile useItemProfile(ItemStack stack) {
        return first(USE_ITEMS, stack);
    }

    public static ProjectileProfile projectileProfile(Entity projectile) {
        return first(PROJECTILES, projectile);
    }

    public static boolean shouldRenderAimFrame(ItemStack stack, Player player) {
        for (SafeBiPredicate<ItemStack, Player> predicate : AIM_FRAMES) {
            if (predicate.test(stack, player)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasNativeHeadshotDamage(DamageSource source) {
        for (SafePredicate<DamageSource> predicate : NATIVE_HEADSHOT_SOURCES) {
            if (predicate.test(source)) {
                return true;
            }
        }
        return false;
    }

    private static <T, R> R first(List<SafeFunction<T, R>> classifiers, T value) {
        for (SafeFunction<T, R> classifier : classifiers) {
            R result = classifier.apply(value);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private static void logFailure(String id, RuntimeException error) {
        IblisMod.LOGGER.error("Disabled failed compatibility hook {}", id, error);
    }

    private static void logFailure(String id, LinkageError error) {
        IblisMod.LOGGER.error("Disabled incompatible compatibility hook {}", id, error);
    }

    public record UseItemProfile(PlayerSkill skill, int protectedFinalTicks) {
        public UseItemProfile {
            Objects.requireNonNull(skill, "skill");
            if (protectedFinalTicks < 0) {
                throw new IllegalArgumentException("protectedFinalTicks must not be negative");
            }
        }
    }

    public record ProjectileProfile(PlayerSkill skill, boolean scaleSpawnDamage) {
        public ProjectileProfile {
            Objects.requireNonNull(skill, "skill");
        }
    }

    private static final class CraftingHook {
        private final String id;
        private final Predicate<ItemStack> matches;
        private final Consumer<PlayerEvent.ItemCraftedEvent> handler;
        private volatile boolean failed;

        private CraftingHook(String id, Predicate<ItemStack> matches,
                             Consumer<PlayerEvent.ItemCraftedEvent> handler) {
            this.id = Objects.requireNonNull(id, "id");
            this.matches = Objects.requireNonNull(matches, "matches");
            this.handler = Objects.requireNonNull(handler, "handler");
        }

        private boolean matches(ItemStack stack) {
            try {
                return matches.test(stack);
            } catch (RuntimeException error) {
                fail(error);
            } catch (LinkageError error) {
                fail(error);
            }
            return false;
        }

        private boolean handle(PlayerEvent.ItemCraftedEvent event) {
            if (!matches(event.getCrafting())) {
                return false;
            }
            if (!failed) {
                try {
                    handler.accept(event);
                } catch (RuntimeException error) {
                    fail(error);
                } catch (LinkageError error) {
                    fail(error);
                }
            }
            // Once matched, never fall through to generic NBT rewriting on foreign tools.
            return true;
        }

        private void fail(RuntimeException error) {
            if (!failed) {
                failed = true;
                logFailure(id, error);
            }
        }

        private void fail(LinkageError error) {
            if (!failed) {
                failed = true;
                logFailure(id, error);
            }
        }
    }

    private static final class SafeFunction<T, R> {
        private final String id;
        private final Function<T, R> function;
        private volatile boolean failed;

        private SafeFunction(String id, Function<T, R> function) {
            this.id = Objects.requireNonNull(id, "id");
            this.function = Objects.requireNonNull(function, "function");
        }

        private R apply(T value) {
            if (failed) {
                return null;
            }
            try {
                return function.apply(value);
            } catch (RuntimeException error) {
                failed = true;
                logFailure(id, error);
            } catch (LinkageError error) {
                failed = true;
                logFailure(id, error);
            }
            return null;
        }
    }

    private static final class SafeBiPredicate<T, U> {
        private final String id;
        private final BiPredicate<T, U> predicate;
        private volatile boolean failed;

        private SafeBiPredicate(String id, BiPredicate<T, U> predicate) {
            this.id = Objects.requireNonNull(id, "id");
            this.predicate = Objects.requireNonNull(predicate, "predicate");
        }

        private boolean test(T first, U second) {
            if (failed) {
                return false;
            }
            try {
                return predicate.test(first, second);
            } catch (RuntimeException error) {
                failed = true;
                logFailure(id, error);
            } catch (LinkageError error) {
                failed = true;
                logFailure(id, error);
            }
            return false;
        }
    }

    private static final class SafePredicate<T> {
        private final String id;
        private final Predicate<T> predicate;
        private volatile boolean failed;

        private SafePredicate(String id, Predicate<T> predicate) {
            this.id = Objects.requireNonNull(id, "id");
            this.predicate = Objects.requireNonNull(predicate, "predicate");
        }

        private boolean test(T value) {
            if (failed) {
                return false;
            }
            try {
                return predicate.test(value);
            } catch (RuntimeException error) {
                failed = true;
                logFailure(id, error);
            } catch (LinkageError error) {
                failed = true;
                logFailure(id, error);
            }
            return false;
        }
    }
}
