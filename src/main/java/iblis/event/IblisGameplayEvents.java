package iblis.event;

import com.google.common.collect.Multimap;
import iblis.IblisMod;
import iblis.compat.CompatHooks;
import iblis.config.IblisConfig;
import iblis.damage.IblisDamageTypes;
import iblis.player.ExtendedFoodData;
import iblis.player.IblisPlayerData;
import iblis.player.PlayerAttributeEffects;
import iblis.player.PlayerDataAccess;
import iblis.player.PlayerSkill;
import iblis.registry.IblisAttributes;
import iblis.registry.IblisEffects;
import iblis.entity.PlayerZombieEntity;
import iblis.item.CrossbowReloadingItem;
import iblis.network.IblisNetwork;
import iblis.network.packet.PlayerAnimationPacket;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IblisMod.MOD_ID)
public final class IblisGameplayEvents {
    private static final int MAX_SPRINT_COUNTER = 32;
    private static final CompatHooks.UseItemProfile BOW_USE =
            new CompatHooks.UseItemProfile(PlayerSkill.ARCHERY, 0);
    private static final CompatHooks.UseItemProfile CROSSBOW_RELOAD =
            new CompatHooks.UseItemProfile(PlayerSkill.SHARPSHOOTING, 6);
    private static final CompatHooks.ProjectileProfile ARROW_PROJECTILE =
            new CompatHooks.ProjectileProfile(PlayerSkill.ARCHERY, true);
    private static final CompatHooks.ProjectileProfile CROSSBOW_ARROW_PROJECTILE =
            new CompatHooks.ProjectileProfile(PlayerSkill.SHARPSHOOTING, false);

    private IblisGameplayEvents() {
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Player player = event.player;
        if (player.foodData.getClass() == FoodData.class) {
            player.foodData = new ExtendedFoodData(player.foodData);
        }
        if (player.level().isClientSide || player.isSpectator()
                || !player.isAlive() || player.isRemoved()) {
            return;
        }

        IblisPlayerData data = PlayerDataAccess.get(player);
        if (IblisConfig.mobReactOnlyOnShooting) {
            int ticks = data.awarenessTicks();
            if (ticks > 0) {
                data.setAwarenessTicks(ticks - 1);
                notifyRandomEntityAboutPlayer((ServerLevel) player.level(), player);
            }
        } else {
            notifyRandomEntityAboutPlayer((ServerLevel) player.level(), player);
        }
        processKnock(player, data);
    }

    private static void processKnock(Player player, IblisPlayerData data) {
        int knock = data.knockState();
        if (knock == 0) {
            return;
        }
        data.setKnockState(0);
        if (knock == 1 && !player.isBlocking()) {
            return;
        }
        float power = player.getAttackStrengthScale(0.0F);
        if (power < 0.1F) {
            return;
        }
        player.resetAttackStrengthTicker();
        if (player instanceof ServerPlayer serverPlayer) {
            IblisNetwork.sendPlayerAnimation(serverPlayer,
                    knock == 1 ? PlayerAnimationPacket.Type.SHIELD_PUNCH
                            : PlayerAnimationPacket.Type.KICK,
                    power);
        }
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        double forwardX = -Mth.sin(yaw) * 1.2F;
        double forwardZ = Mth.cos(yaw) * 1.2F;
        List<Entity> targets = player.level().getEntities(player,
                player.getBoundingBox().expandTowards(forwardX, 0.0, forwardZ),
                EntitySelector.pushableBy(player));
        double skill = (knock == 1 ? PlayerSkill.PARRY : PlayerSkill.SWORDSMANSHIP)
                .getFullValue(player) + 1.5;
        float playerSize = player.getBbHeight() * player.getBbWidth();
        float training = 0.0F;
        boolean hit = false;
        for (Entity entity : targets) {
            if (!(entity instanceof LivingEntity living)
                    || player.isPassengerOfSameVehicle(entity) || entity.noPhysics || entity.isVehicle()) {
                continue;
            }
            float targetSize = Math.max(entity.getBbHeight() * entity.getBbWidth(), 0.1F);
            double dx = (entity.getX() - player.getX()) * forwardX;
            double dz = (entity.getZ() - player.getZ()) * forwardZ;
            if (dx < 0.0 || dz < 0.0) {
                continue;
            }
            float strength = (float) skill * playerSize / targetSize * 0.2F * power;
            training += targetSize;
            if (strength > 0.3F) {
                living.hurt(player.damageSources().playerAttack(player), strength);
            }
            living.knockback(strength, -forwardX, -forwardZ);
            hit = true;
        }
        if (training > 1.0F) {
            (knock == 1 ? PlayerSkill.PARRY : PlayerSkill.SWORDSMANSHIP).raise(player, training);
        }
        if (hit) {
            player.playSound(SoundEvents.SHIELD_BLOCK, 0.8F,
                    0.8F + player.getRandom().nextFloat() * 0.4F);
        }
    }

    private static void notifyRandomEntityAboutPlayer(ServerLevel level, Player player) {
        if (IblisConfig.noIncreasedMobSeekRange) {
            return;
        }
        Entity random = LoadedEntityIndex.random(level);
        if (!(random instanceof Mob mob) || !(mob instanceof Enemy)) {
            return;
        }
        Vec3 delta = player.position().subtract(mob.position());
        if (mob.getLookAngle().dot(delta) < 0.0 || !mob.getSensing().hasLineOfSight(player)) {
            return;
        }
        int distance = Mth.ceil(mob.distanceTo(player));
        MobEffectInstance awareness = new MobEffectInstance(
                IblisEffects.AWARENESS.get(), 1200, distance);
        for (Entity entity : level.getEntities(mob,
                new AABB(mob.blockPosition()).inflate(16.0, 4.0, 16.0),
                candidate -> candidate instanceof Mob)) {
            ((Mob) entity).addEffect(new MobEffectInstance(awareness));
        }
    }

    @SubscribeEvent
    public static void breakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.getOriginalSpeed() <= 0.0F || !PlayerSkill.DIGGING.enabled
                || !event.getEntity().getMainHandItem().isCorrectToolForDrops(event.getState())) {
            return;
        }
        event.setNewSpeed((float) (event.getOriginalSpeed()
                * (PlayerSkill.DIGGING.getFullValue(event.getEntity()) * 0.1 + 0.2)));
    }

    @SubscribeEvent
    public static void blockBroken(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        float hardness = event.getState().getDestroySpeed(event.getLevel(), event.getPos());
        if (hardness > 0.0F && !player.level().isClientSide) {
            PlayerSkill.DIGGING.raise(player, hardness);
        }
    }

    @SubscribeEvent
    public static void livingFall(LivingFallEvent event) {
        if (event.getDistance() > 2.0F && event.getEntity() instanceof ServerPlayer player) {
            PlayerSkill.FALLING.raise(player, 1.0);
            event.setDistance((float) (event.getDistance() - PlayerSkill.FALLING.getFullValue(player)));
        }
    }

    @SubscribeEvent
    public static void livingJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player) || !PlayerSkill.JUMPING.enabled
                || player.getFoodData().getFoodLevel() <= 6) {
            return;
        }
        float sprintState = PlayerDataAccess.get(player).sprintButtonCounter()
                / (float) MAX_SPRINT_COUNTER;
        double multiplier = PlayerSkill.JUMPING.getFullValue(player) * 0.1 * sprintState + 1.0;
        player.setDeltaMovement(player.getDeltaMovement().scale(multiplier));
        if (player instanceof ServerPlayer) {
            PlayerSkill.JUMPING.raise(player, 1.0F + sprintState);
            if (sprintState > 0.2F) {
                player.causeFoodExhaustion(0.2F * sprintState);
            }
        }
    }

    @SubscribeEvent
    public static void horseDismount(EntityMountEvent event) {
        if (!event.isDismounting() || !(event.getEntityBeingMounted() instanceof AbstractHorse horse)
                || horse.isTamed()) {
            return;
        }
        event.getEntityMounting().hurt(event.getLevel().damageSources().mobAttack(horse), 1.0F);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void livingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DamageSource source = event.getSource();
        float damage = event.getAmount();
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            damage -= value(player, IblisAttributes.EXPLOSION_DAMAGE_REDUCTION.get());
        } else if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            damage -= value(player, IblisAttributes.PROJECTILE_DAMAGE_REDUCTION.get());
        } else if (source.is(DamageTypeTags.IS_FIRE)) {
            MobEffectInstance current = player.getEffect(IblisEffects.OVERHEATING.get());
            int amplifier = current == null ? 1 : current.getAmplifier() + 1;
            player.addEffect(new MobEffectInstance(IblisEffects.OVERHEATING.get(), 200, amplifier));
            damage -= value(player, IblisAttributes.FIRE_DAMAGE_REDUCTION.get());
        } else if ("mob".equals(source.getMsgId())) {
            damage -= value(player, IblisAttributes.MELEE_DAMAGE_REDUCTION.get());
        }
        player.removeEffect(MobEffects.REGENERATION);
        event.setAmount(damage);
    }

    @SubscribeEvent
    public static void playerDied(LivingDeathEvent event) {
        if (IblisConfig.spawnPlayerZombie && event.getEntity() instanceof ServerPlayer player) {
            player.level().addFreshEntity(PlayerZombieEntity.inheritFrom(player));
        }
    }

    @SubscribeEvent
    public static void playerAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            PlayerSkill.BOXING.raise(player, target.getMaxHealth());
        } else if (held.getAttributeModifiers(EquipmentSlot.MAINHAND)
                .containsKey(Attributes.ATTACK_DAMAGE)) {
            PlayerSkill.SWORDSMANSHIP.raise(player, target.getMaxHealth());
        }
    }

    @SubscribeEvent
    public static void livingAttacked(LivingAttackEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) {
            return;
        }
        DamageSource source = event.getSource();
        Entity shooter = source.getEntity();
        if (shooter == null) {
            return;
        }
        if (target instanceof Mob mob && mob instanceof Enemy && target.level() instanceof ServerLevel level) {
            int distance = Mth.ceil(target.distanceTo(shooter));
            MobEffectInstance awareness = new MobEffectInstance(
                    IblisEffects.AWARENESS.get(), 1200, distance);
            for (Entity entity : level.getEntities(mob,
                    new AABB(mob.blockPosition()).inflate(16.0, 4.0, 16.0),
                    candidate -> mob.getClass().isInstance(candidate))) {
                if (entity instanceof Mob comrade) {
                    comrade.addEffect(new MobEffectInstance(awareness));
                }
            }
        }
        if (shooter instanceof ServerPlayer player) {
            CompatHooks.ProjectileProfile profile = projectileProfile(source.getDirectEntity());
            if (profile != null) {
                profile.skill().raise(player, target.getMaxHealth());
            } else if ("thrown".equals(source.getMsgId())) {
                PlayerSkill.THROWING.raise(player, target.getMaxHealth());
            } else if (source.is(IblisDamageTypes.SHOTGUN)) {
                PlayerSkill.SHARPSHOOTING.raise(player, target.getMaxHealth());
            }
        }
        if (target instanceof ServerPlayer player) {
            AttributeInstance resistance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (resistance != null) {
                double skill = PlayerSkill.EQUILIBRIUM.getFullValue(player) + 1.0;
                resistance.removeModifier(PlayerAttributeEffects.EQUILIBRIUM_KNOCKBACK);
                resistance.addTransientModifier(new AttributeModifier(
                        PlayerAttributeEffects.EQUILIBRIUM_KNOCKBACK,
                        "Equilibrium modifier", 1.0 - 1.0 / skill,
                        AttributeModifier.Operation.ADDITION));
            }
            if (player.isDamageSourceBlocked(source)) {
                PlayerSkill.PARRY.raise(player, 1.0);
            }
            PlayerSkill.EQUILIBRIUM.raise(player, 1.0);
        }
    }

    @SubscribeEvent
    public static void itemUseTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack stack = event.getItem();
        CompatHooks.UseItemProfile profile;
        if (stack.getItem() instanceof BowItem) {
            profile = BOW_USE;
        } else if (stack.getItem() instanceof CrossbowReloadingItem) {
            profile = CROSSBOW_RELOAD;
        } else {
            profile = CompatHooks.useItemProfile(stack);
        }
        if (profile == null || !profile.skill().enabled) {
            return;
        }
        double skill = profile.skill().getFullValue(player) + 1.0;
        int duration = event.getDuration();
        if (duration <= profile.protectedFinalTicks()) {
            return;
        }
        if (skill > 63.0) {
            int skipped = Math.min((int) (skill - 63.0),
                    duration - profile.protectedFinalTicks());
            event.setDuration(duration - skipped);
        } else if (skill >= 1.0 && duration % (int) (128.0 / skill) == 0) {
            event.setDuration(duration - 1);
        }
    }

    @SubscribeEvent
    public static void entityJoined(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        LoadedEntityIndex.add(level, event.getEntity());
        if (event.getEntity() instanceof AbstractArrow arrow) {
            CompatHooks.ProjectileProfile profile = projectileProfile(arrow);
            Entity owner = arrow.getOwner();
            if (profile != null && profile.scaleSpawnDamage()
                    && (profile.skill().enabled || PlayerSkill.MECHANICS.enabled)
                    && owner instanceof ServerPlayer player) {
                double damage = arrow.getBaseDamage();
                double qualityDamage = value(player, IblisAttributes.PROJECTILE_DAMAGE.get());
                if (qualityDamage > 0.0) {
                    damage += qualityDamage - 3.0;
                }
                if (profile.skill().enabled) {
                    damage *= profile.skill().getFullValue(player) + 0.2;
                }
                arrow.setBaseDamage(Math.max(damage, 0.25));
            }
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerAttributeEffects.refreshMeleeDamageBonus(player);
            PlayerAttributeEffects.refreshWeaponSkill(player);
        }
    }

    private static CompatHooks.ProjectileProfile projectileProfile(Entity entity) {
        if (entity instanceof AbstractArrow arrow
                && (arrow instanceof Arrow || arrow instanceof SpectralArrow)) {
            boolean crossbow = arrow.shotFromCrossbow();
            return crossbow ? CROSSBOW_ARROW_PROJECTILE : ARROW_PROJECTILE;
        }
        return entity == null ? null : CompatHooks.projectileProfile(entity);
    }

    @SubscribeEvent
    public static void entityLeft(EntityLeaveLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LoadedEntityIndex.remove(level, event.getEntity());
        }
    }

    @SubscribeEvent
    public static void levelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LoadedEntityIndex.clear(level);
        }
    }

    @SubscribeEvent
    public static void equipmentChanged(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof Player player && event.getSlot().getType() == EquipmentSlot.Type.HAND) {
            PlayerAttributeEffects.refreshWeaponSkill(player);
        }
    }

    public static void markGunshot(Player player) {
        PlayerDataAccess.get(player).setAwarenessTicks(1200);
    }

    private static double value(LivingEntity entity, Attribute attribute) {
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance == null ? 0.0 : instance.getValue();
    }
}
