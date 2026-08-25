package iblis_headshots.event;

import com.google.common.collect.Multimap;
import iblis_headshots.IblisHeadshotsMod;
import iblis_headshots.compat.NativeHeadshotSources;
import iblis_headshots.config.HeadshotsConfig;
import iblis_headshots.util.HeadgearProtection;
import iblis_headshots.util.HeadshotFeedback;
import iblis_headshots.util.HeadshotGeometry;
import iblis_headshots.util.HeadshotRules;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IblisHeadshotsMod.MOD_ID)
public final class HeadshotEvents {
    private static final EquipmentSlot[] BODY_ARMOR_SLOTS = {
            EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final Map<LivingEntity, List<RemovedModifier>> PENDING_ARMOR =
            new IdentityHashMap<>();

    private HeadshotEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void livingHurt(LivingHurtEvent event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide) {
            return;
        }
        event.setAmount(recalculateDamage(event.getAmount(), event.getEntity(), event.getSource()));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void livingDamage(LivingDamageEvent event) {
        if (!event.getEntity().level().isClientSide) {
            restoreBodyArmor(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING_ARMOR.isEmpty()) {
            return;
        }
        PENDING_ARMOR.values().forEach(HeadshotEvents::restoreModifiers);
        PENDING_ARMOR.clear();
    }

    private static float recalculateDamage(float damage, LivingEntity victim, DamageSource source) {
        if (!(victim.level() instanceof ServerLevel level) || damage < 0.1F) {
            return damage;
        }

        if (NativeHeadshotSources.hasNativeHeadshotDamage(source)) {
            return damage;
        }

        Entity directEntity = source.getDirectEntity();
        if (directEntity == null) {
            return damage;
        }

        Vec3 movement = directEntity.getDeltaMovement();
        Vec3 start = directEntity.position().subtract(movement);
        Vec3 end = directEntity.position().add(movement);
        if (directEntity instanceof Player player) {
            double distanceSquared = player.distanceToSqr(victim);
            if (distanceSquared < HeadshotsConfig.nonProjectileMinDistanceSquared) {
                return damage;
            }
            start = player.getEyePosition();
            end = start.add(player.getLookAngle().scale(distanceSquared));
        }

        Entity attacker = source.getEntity();
        if (!HeadshotGeometry.intersectsHead(victim, start, end)
                || !HeadshotRules.acceptsHeadshot(victim, attacker)) {
            return damage * HeadshotsConfig.bodyshotDamageMultiplier;
        }

        float multiplier = HeadshotRules.damageMultiplier(victim, attacker);
        ItemStack headgear = victim.getItemBySlot(EquipmentSlot.HEAD);
        temporarilyRemoveBodyArmor(victim);
        if (!headgear.isEmpty()) {
            float protection = HeadgearProtection.damageMultiplier(headgear);
            multiplier = 1.0F + Math.max(multiplier - 1.0F, 0.0F) * protection;
            int durabilityDamage = (int) ((victim.getRandom().nextFloat() * 0.5F + 1.0F)
                    * damage * HeadshotsConfig.headgearDamageMultiplier);
            headgear.hurtAndBreak(durabilityDamage, victim,
                    entity -> entity.broadcastBreakEvent(EquipmentSlot.HEAD));
        }
        damage *= multiplier;
        HeadshotFeedback.apply(level, victim, attacker);

        if (victim instanceof Slime slime && victim.getHealth() < damage && slime.getSize() > 1) {
            slime.setSize(1, false);
        }
        return damage;
    }

    private static void temporarilyRemoveBodyArmor(LivingEntity entity) {
        restoreBodyArmor(entity);
        List<RemovedModifier> removed = new ArrayList<>();
        for (EquipmentSlot slot : BODY_ARMOR_SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            Multimap<Attribute, AttributeModifier> modifiers = stack.getAttributeModifiers(slot);
            modifiers.forEach((attribute, modifier) -> {
                AttributeInstance instance = entity.getAttribute(attribute);
                AttributeModifier applied = instance == null
                        ? null : instance.getModifier(modifier.getId());
                if (applied != null) {
                    instance.removeModifier(modifier.getId());
                    removed.add(new RemovedModifier(instance, applied));
                }
            });
        }
        if (!removed.isEmpty()) {
            PENDING_ARMOR.put(entity, removed);
        }
    }

    private static void restoreBodyArmor(LivingEntity entity) {
        List<RemovedModifier> removed = PENDING_ARMOR.remove(entity);
        if (removed != null) {
            restoreModifiers(removed);
        }
    }

    private static void restoreModifiers(List<RemovedModifier> removed) {
        for (RemovedModifier entry : removed) {
            AttributeInstance instance = entry.instance;
            AttributeModifier modifier = entry.modifier;
            if (instance.getModifier(modifier.getId()) == null) {
                instance.addTransientModifier(modifier);
            }
        }
    }

    private record RemovedModifier(AttributeInstance instance, AttributeModifier modifier) {
    }
}
