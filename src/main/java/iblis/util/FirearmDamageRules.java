package iblis.util;

import iblis.IblisMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.Tags;
import net.minecraftforge.entity.PartEntity;

public final class FirearmDamageRules {
    private static final float BOSS_BASE_DAMAGE_MULTIPLIER = 0.6F;
    private static final float BOSS_HEADSHOT_MULTIPLIER_CAP = 1.5F;
    private static final TagKey<EntityType<?>> FIREARM_BOSSES = TagKey.create(
            Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(
                    IblisMod.MOD_ID, "firearm_bosses"));
    private static final ClassValue<Boolean> HAS_SERVER_BOSS_BAR = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            for (Class<?> current = type;
                 current != null && LivingEntity.class.isAssignableFrom(current);
                 current = current.getSuperclass()) {
                try {
                    for (java.lang.reflect.Field field : current.getDeclaredFields()) {
                        if (ServerBossEvent.class.isAssignableFrom(field.getType())) {
                            return true;
                        }
                    }
                } catch (LinkageError | SecurityException ignored) {
                    // A broken optional field must not prevent tag-based detection.
                }
            }
            return false;
        }
    };

    private FirearmDamageRules() {
    }

    public static boolean isBoss(Entity target) {
        while (target instanceof PartEntity<?> part) {
            target = part.getParent();
        }
        if (!(target instanceof LivingEntity) || target instanceof Player) {
            return false;
        }
        return target.getType().is(FIREARM_BOSSES)
                || target.getType().is(Tags.EntityTypes.BOSSES)
                || HAS_SERVER_BOSS_BAR.get(target.getClass());
    }

    public static float scaleBaseDamage(Entity target, float damage) {
        return isBoss(target) ? damage * BOSS_BASE_DAMAGE_MULTIPLIER : damage;
    }

    public static float headshotMultiplier(Entity target, float normalMultiplier) {
        return isBoss(target)
                ? Math.min(normalMultiplier, BOSS_HEADSHOT_MULTIPLIER_CAP)
                : normalMultiplier;
    }
}
