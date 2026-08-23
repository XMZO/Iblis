package iblis_headshots.event;

import com.google.common.collect.Multimap;
import iblis_headshots.IblisHeadshotsMod;
import iblis_headshots.advancement.HeadshotTrigger;
import iblis_headshots.config.HeadshotsConfig;
import iblis_headshots.network.HeadshotsNetwork;
import iblis_headshots.util.HeadgearProtection;
import iblis_headshots.util.HeadshotGeometry;
import iblis_headshots.util.HeadshotRules;
import iblis.damage.IblisDamageTypes;
import iblis.util.FirearmDamageRules;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IblisHeadshotsMod.MOD_ID)
public final class HeadshotEvents {
    private static final Set<LivingEntity> HANDLED_IN_HURT =
            Collections.newSetFromMap(new WeakHashMap<>());

    private HeadshotEvents() {
    }

    @SubscribeEvent
    public static void livingHurt(LivingHurtEvent event) {
        if (event.isCanceled()) {
            return;
        }
        HANDLED_IN_HURT.add(event.getEntity());
        event.setAmount(recalculateDamage(event.getAmount(), event.getEntity(), event.getSource()));
    }

    @SubscribeEvent
    public static void livingDamage(LivingDamageEvent event) {
        if (HANDLED_IN_HURT.remove(event.getEntity())) {
            return;
        }
        event.setAmount(recalculateDamage(event.getAmount(), event.getEntity(), event.getSource()));
    }

    private static float recalculateDamage(float damage, LivingEntity victim, DamageSource source) {
        if (!(victim.level() instanceof ServerLevel level) || damage < 0.1F) {
            return damage;
        }

        // Hitscan firearms calculate their exact path and headshot once at the source.
        if (IblisDamageTypes.isShotgun(source)) {
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

        if (!HeadshotGeometry.intersectsHead(victim, start, end)
                || !HeadshotRules.acceptsPlayerHeadshot(victim, source.getEntity())) {
            return damage * HeadshotsConfig.bodyshotDamageMultiplier;
        }

        HeadshotsNetwork.spawnParticle(level,
                victim.position().add(0.0, victim.getEyeHeight(), 0.0),
                new Vec3(0.0, 0.2, 0.0), 15);

        float multiplier = IblisDamageTypes.isCrossbow(source)
                ? FirearmDamageRules.headshotMultiplier(
                victim, HeadshotsConfig.damageMultiplier)
                : HeadshotsConfig.damageMultiplier;
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

        Entity attacker = source.getEntity();
        if (attacker instanceof ServerPlayer player && !(victim instanceof ServerPlayer)) {
            HeadshotTrigger.INSTANCE.trigger(player, victim);
        } else if (victim instanceof ServerPlayer player) {
            HeadshotTrigger.INSTANCE.trigger(player, victim);
        }

        if (victim instanceof Slime slime && victim.getHealth() < damage && slime.getSize() > 1) {
            slime.setSize(1, false);
        }
        return damage;
    }

    private static void temporarilyRemoveBodyArmor(LivingEntity entity) {
        EquipmentSlot[] bodySlots = {
                EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        for (EquipmentSlot slot : bodySlots) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            Multimap<Attribute, AttributeModifier> modifiers = stack.getAttributeModifiers(slot);
            modifiers.forEach((attribute, modifier) -> {
                AttributeInstance instance = entity.getAttribute(attribute);
                if (instance != null) {
                    instance.removeModifier(modifier.getId());
                }
            });
        }
        entity.getServer().execute(() -> {
            for (EquipmentSlot slot : bodySlots) {
                ItemStack current = entity.getItemBySlot(slot);
                current.getAttributeModifiers(slot).forEach((attribute, modifier) -> {
                    AttributeInstance instance = entity.getAttribute(attribute);
                    if (instance != null && instance.getModifier(modifier.getId()) == null) {
                        instance.addTransientModifier(modifier);
                    }
                });
            }
        });
    }
}
