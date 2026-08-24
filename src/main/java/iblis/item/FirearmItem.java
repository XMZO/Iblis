package iblis.item;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import iblis.IblisMod;
import iblis.config.IblisConfig;
import iblis.config.Legacy112Feature;
import iblis.damage.IblisDamageTypes;
import iblis.network.IblisNetwork;
import iblis.player.PlayerDataAccess;
import iblis.player.PlayerCharacteristic;
import iblis.player.PlayerSkill;
import iblis.registry.IblisAttributes;
import iblis.registry.IblisSounds;
import iblis.util.FirearmDamageRules;
import iblis.util.IblisMath;
import iblis_headshots.config.HeadshotsConfig;
import iblis_headshots.util.HeadshotFeedback;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.entity.PartEntity;

public abstract class FirearmItem extends Item implements CustomLeftClickItem {
    public static final String DURABILITY = "durability";
    public static final String AMMO = "ammo";
    public static final String COCKED_STATE = "cockedState";
    public static final String DAMAGE = "damage";
    public static final String AMMO_TYPE = "ammo_type";
    private static final String FIRE_COOLDOWN_START = "fireCooldownStart";
    private static final String FIRE_COOLDOWN_END = "fireCooldownEnd";
    private static final String DRY_FIRE_SOUND_END = "dryFireSoundEnd";
    private static final int DRY_FIRE_SOUND_INTERVAL_TICKS = 10;
    private static final double FIRE_RATE_MAX_BONUS = 0.25;
    private static final double FIRE_RATE_LEVEL_SOFT_CAP = 20.0;
    private static final double AIM_ATTACK_SPEED_MAX_BONUS = 0.10;
    private static final double AIM_INTELLIGENCE_MAX_BONUS = 0.06;
    private static final double AIM_LUCK_MAX_BONUS = 0.04;
    private static final float DOOR_PENETRATION_DAMAGE_MULTIPLIER = 0.55F;
    private static final int SHOTGUN_SHIELD_COOLDOWN_TICKS = 40;
    private static final TagKey<EntityType<?>> SHOTGUN_BREAKABLE_BOATS = TagKey.create(
            Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(
                    IblisMod.MOD_ID, "shotgun_breakable_boats"));
    private static final TagKey<EntityType<?>> FIREARM_BREAKABLE_TARGETS = TagKey.create(
            Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(
                    IblisMod.MOD_ID, "firearm_breakable_targets"));
    private static final TagKey<EntityType<?>> SHOTGUN_ENDERMAN_TARGETS = TagKey.create(
            Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(
                    IblisMod.MOD_ID, "shotgun_enderman_targets"));
    private static final TagKey<Item> SHOTGUN_SHIELD_COOLDOWN_IMMUNE = TagKey.create(
            Registries.ITEM, ResourceLocation.fromNamespaceAndPath(
                    IblisMod.MOD_ID, "shotgun_shield_cooldown_immune"));
    private static final java.util.UUID PROJECTILE_DAMAGE_MODIFIER =
            java.util.UUID.fromString("75717fc3-7f6f-0857-4cdf-000009f5f2d7");
    private static final Supplier<Multimap<Attribute, AttributeModifier>> MAIN_HAND_MODIFIERS =
            Suppliers.memoize(() -> ImmutableMultimap.of(
                    IblisAttributes.PROJECTILE_DAMAGE.get(),
                    new AttributeModifier(PROJECTILE_DAMAGE_MODIFIER, "Weapon modifier", 12.0,
                            AttributeModifier.Operation.ADDITION),
                    Attributes.ATTACK_SPEED,
                    new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -2.4,
                            AttributeModifier.Operation.ADDITION)));

    protected FirearmItem(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(iblis.client.FirearmClientItemExtensions.ready());
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72_000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void onLeftClick(ServerLevel level, ServerPlayer player, InteractionHand hand) {
        shootLoaded(level, player, hand);
    }

    public void shootLoaded(ServerLevel level, ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.hasTag()) {
            return;
        }
        CompoundTag tag = stack.getTag();
        if (baseFireCooldownTicks() > 0
                && tag.getLong(FIRE_COOLDOWN_END) > level.getGameTime()) {
            return;
        }
        ListTag ammunition = tag.getList(AMMO, Tag.TAG_COMPOUND);
        int cocked = tag.getInt(COCKED_STATE);
        PlayerDataAccess.get(player).setReloadTick(0);
        if (cocked > ammunition.size()) {
            if (allowDryFireSound(level, tag)) {
                playDropStringSound(player);
            }
            tag.putInt(COCKED_STATE, --cocked);
            return;
        }
        if (ammunition.isEmpty()) {
            if (allowDryFireSound(level, tag)) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        IblisSounds.SHOTGUN_HAMMER_CLICK.get(), SoundSource.PLAYERS,
                        1.0F, level.random.nextFloat() * 0.2F + 0.8F);
            }
            return;
        }

        Vec3 aim = player.getLookAngle();
        double accuracy = shootingAccuracy(player);
        double luck = player.getAttributeValue(Attributes.LUCK);
        boolean critical = level.random.nextDouble() < (accuracy + luck - 4.0) / 100.0;
        aim = aim.add((level.random.nextFloat() - 0.5F) / accuracy,
                (level.random.nextFloat() - 0.5F) / accuracy,
                (level.random.nextFloat() - 0.5F) / accuracy);
        CompoundTag cartridge = ammunition.getCompound(ammunition.size() - 1);
        fire(level, aim, player, critical, accuracy,
                cartridge.getFloat(DAMAGE), cartridge.getInt(AMMO_TYPE));
        startFireCooldown(level, player, tag);

        if (!player.getAbilities().instabuild) {
            ammunition.remove(ammunition.size() - 1);
            tag.put(AMMO, ammunition);
            if (cocked > 0) {
                tag.putInt(COCKED_STATE, --cocked);
            }
            stack.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(hand));
        }
        if (player.isUsingItem()) {
            player.stopUsingItem();
            player.startUsingItem(hand);
        }
        player.resetAttackStrengthTicker();
        IblisNetwork.resetClientUse(player);
    }

    protected abstract void fire(ServerLevel level, Vec3 aim, ServerPlayer player,
                                 boolean critical, double accuracy, float ammoDamage, int ammoType);

    /** Mechanical cycle time before character upgrades, in game ticks. */
    protected abstract int baseFireCooldownTicks();

    private int fireCooldownTicks(Player player) {
        int baseTicks = baseFireCooldownTicks();
        if (baseTicks <= 0) {
            return 0;
        }
        double levels = PlayerCharacteristic.ATTACK_SPEED.getInvestedLevels(player);
        double rateBonus = diminishingBonus(
                levels, FIRE_RATE_MAX_BONUS, FIRE_RATE_LEVEL_SOFT_CAP);
        return Math.max(1, (int) Math.ceil(baseTicks / (1.0 + rateBonus)));
    }

    /** Client request cadence; the server still performs the authoritative check. */
    public int fireAttemptIntervalTicks(ItemStack stack) {
        if (!stack.hasTag()) {
            return DRY_FIRE_SOUND_INTERVAL_TICKS;
        }
        CompoundTag tag = stack.getTag();
        ListTag ammunition = tag.getList(AMMO, Tag.TAG_COMPOUND);
        int cocked = tag.getInt(COCKED_STATE);
        // A live-shot request is retried briefly until the authoritative NBT
        // arrives. This avoids losing a whole cycle to one tick of clock skew.
        if (ammunition.isEmpty() || cocked > ammunition.size()) {
            return DRY_FIRE_SOUND_INTERVAL_TICKS;
        }
        return baseFireCooldownTicks() <= 0 ? 1 : 2;
    }

    private static boolean allowDryFireSound(Level level, CompoundTag tag) {
        long now = level.getGameTime();
        if (tag.getLong(DRY_FIRE_SOUND_END) > now) {
            return false;
        }
        tag.putLong(DRY_FIRE_SOUND_END, now + DRY_FIRE_SOUND_INTERVAL_TICKS);
        return true;
    }

    private void startFireCooldown(Level level, Player player, CompoundTag tag) {
        int ticks = fireCooldownTicks(player);
        if (ticks <= 0) {
            tag.remove(FIRE_COOLDOWN_START);
            tag.remove(FIRE_COOLDOWN_END);
            return;
        }
        long start = level.getGameTime();
        tag.putLong(FIRE_COOLDOWN_START, start);
        tag.putLong(FIRE_COOLDOWN_END, start + ticks);
    }

    /** Remaining per-stack cooldown, rendered with the vanilla hotbar style. */
    public static float fireCooldownPercent(
            ItemStack stack, Level level, float partialTick) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return 0.0F;
        }
        if (stack.getItem() instanceof FirearmItem firearm
                && firearm.baseFireCooldownTicks() <= 0) {
            return 0.0F;
        }
        CompoundTag tag = stack.getTag();
        if (!tag.contains(FIRE_COOLDOWN_START, Tag.TAG_LONG)
                || !tag.contains(FIRE_COOLDOWN_END, Tag.TAG_LONG)) {
            return 0.0F;
        }
        long start = tag.getLong(FIRE_COOLDOWN_START);
        long end = tag.getLong(FIRE_COOLDOWN_END);
        if (end <= start) {
            return 0.0F;
        }
        double remaining = end - (level.getGameTime() + partialTick);
        return (float) Math.max(0.0, Math.min(1.0, remaining / (end - start)));
    }

    protected LivingEntity damageEntitiesOnPath(ServerLevel level, DamageSource source,
                                                Entity shooter, Vec3 start, Vec3 end,
                                                float projectileDamage, float splashCone,
                                                double[] doorDistances, int doorCount,
                                                boolean legacyLuckyShot) {
        LivingEntity lastVictim = null;
        Set<LivingEntity> damagedTargets =
                Collections.newSetFromMap(new IdentityHashMap<>());
        List<Entity> entities = level.getEntities(shooter,
                new AABB(start, end).inflate(0.5),
                entity -> EntitySelector.NO_SPECTATORS.test(entity)
                        && entity.isAlive() && entity.isPickable());
        for (Entity entity : entities) {
            LivingEntity target = livingTarget(entity);
            if (target != null && damagedTargets.contains(target)) {
                continue;
            }
            double hitDistance = hitDistance(entity, start, end);
            if (target != null && entity == target
                    && iblis_headshots.util.HeadshotGeometry.intersectsHead(target, start, end)
                    && iblis_headshots.util.HeadshotRules.acceptsHeadshot(
                    target, shooter)) {
                DamageSource targetSource = targetDamageSource(level, source, shooter, target);
                float headshotBaseDamage = applyDoorPenetration(
                        FirearmDamageRules.scaleBaseDamage(target,
                                FirearmDamageRules.normalizeLegacyLuckyShot(
                                        target, projectileDamage, legacyLuckyShot)),
                        hitDistance, doorDistances, doorCount);
                float headshotDamage = headshotBaseDamage
                        * FirearmDamageRules.headshotMultiplier(
                        target, iblis_headshots.util.HeadshotRules.damageMultiplier(
                                target, shooter));
                if (target instanceof net.minecraft.world.entity.monster.Slime slime
                        && target.getHealth() < headshotDamage && slime.getSize() > 1) {
                    slime.setSize(1, false);
                }
                if (hurtTarget(entity, target, targetSource, headshotDamage)) {
                    damagedTargets.add(target);
                    lastVictim = target;
                    HeadshotFeedback.apply(level, target, shooter);
                }
                continue;
            }

            float pathMultiplier = damageMultiplierOnPath(entity, start, end, splashCone);
            if (pathMultiplier <= 0.0F) {
                continue;
            }
            float penetratedDamage = applyDoorPenetration(
                    projectileDamage * pathMultiplier, hitDistance,
                    doorDistances, doorCount);

            if (target == null && IblisConfig.useLegacy112(
                    Legacy112Feature.SHOTGUN_ENTITY_INTERACTIONS)) {
                continue;
            }

            if (entity.getType().is(FIREARM_BREAKABLE_TARGETS)) {
                entity.hurt(source, Math.max(1.0F, penetratedDamage));
                continue;
            }

            if (entity instanceof Boat || entity.getType().is(SHOTGUN_BREAKABLE_BOATS)) {
                entity.hurt(source, penetratedDamage);
                continue;
            }

            if (target == null) {
                continue;
            }
            penetratedDamage = FirearmDamageRules.scaleBaseDamage(target,
                    FirearmDamageRules.normalizeLegacyLuckyShot(
                            target, penetratedDamage, legacyLuckyShot))
                    * HeadshotsConfig.bodyshotDamageMultiplier;
            DamageSource targetSource = targetDamageSource(level, source, shooter, target);
            if (hurtTarget(entity, target, targetSource, penetratedDamage)) {
                damagedTargets.add(target);
                lastVictim = target;
            }
        }
        return lastVictim;
    }

    private static float damageMultiplierOnPath(
            Entity entity, Vec3 start, Vec3 end, float splashCone) {
        if (splashCone == 0.0F) {
            return entity.getBoundingBox().clip(start, end).isPresent() ? 1.0F : 0.0F;
        }
        return IblisMath.calculateOverlapMultiplier(
                entity.getBoundingBox(), start, end, splashCone);
    }

    private static double hitDistance(Entity entity, Vec3 start, Vec3 end) {
        Optional<Vec3> intersection = entity.getBoundingBox().clip(start, end);
        if (intersection.isPresent()) {
            return start.distanceTo(intersection.get());
        }
        Vec3 path = end.subtract(start);
        double pathLengthSquared = path.lengthSqr();
        if (pathLengthSquared <= 1.0E-12) {
            return 0.0;
        }
        double projection = entity.getBoundingBox().getCenter().subtract(start).dot(path)
                / pathLengthSquared;
        return Math.sqrt(pathLengthSquared) * Math.max(0.0, Math.min(1.0, projection));
    }

    private static float applyDoorPenetration(
            float damage, double hitDistance, double[] doorDistances, int doorCount) {
        for (int i = 0; i < doorCount; ++i) {
            if (doorDistances[i] + 1.0E-4 < hitDistance) {
                damage *= DOOR_PENETRATION_DAMAGE_MULTIPLIER;
            }
        }
        return damage;
    }

    private static DamageSource targetDamageSource(
            ServerLevel level, DamageSource source, Entity shooter, LivingEntity target) {
        if (IblisConfig.shotgunHitsEndermen
                && target.getType().is(SHOTGUN_ENDERMAN_TARGETS)
                && shooter instanceof Player player) {
            return IblisDamageTypes.shotgunAgainstEnderman(level, player);
        }
        return source;
    }

    private static boolean hurtTarget(
            Entity hitEntity, LivingEntity target, DamageSource source, float damage) {
        Item shieldItem = blockingShieldItem(target, source);
        float healthBefore = target.getHealth();
        boolean hurt = hitEntity.hurt(source, damage);
        if (shieldItem != null && target instanceof Player player) {
            player.getCooldowns().addCooldown(shieldItem, SHOTGUN_SHIELD_COOLDOWN_TICKS);
            player.stopUsingItem();
            player.level().broadcastEntityEvent(player, (byte) 30);
        }
        if (!IblisConfig.shotgunHitsEndermen
                && target.getType().is(SHOTGUN_ENDERMAN_TARGETS)
                && target.getHealth() >= healthBefore) {
            return false;
        }
        return hurt;
    }

    private static Item blockingShieldItem(LivingEntity target, DamageSource source) {
        if (!IblisConfig.shotgunDisablesShields || !(target instanceof Player player)) {
            return null;
        }
        ItemStack blockingStack = player.getUseItem();
        if (blockingStack.isEmpty()
                || blockingStack.is(SHOTGUN_SHIELD_COOLDOWN_IMMUNE)
                || !blockingStack.canPerformAction(ToolActions.SHIELD_BLOCK)
                || !player.isDamageSourceBlocked(source)) {
            return null;
        }
        return blockingStack.getItem();
    }

    private static LivingEntity livingTarget(Entity entity) {
        if (entity instanceof LivingEntity living) {
            return living;
        }
        if (entity instanceof PartEntity<?> part
                && part.getParent() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(DURABILITY)
                ? stack.getTag().getInt(DURABILITY)
                : super.getMaxDamage(stack);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(
            EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getAttributeModifiers(slot, stack);
        }
        return MAIN_HAND_MODIFIERS.get();
    }

    public abstract ItemStack toReloading(ItemStack stack);

    public abstract void playReloadingSound(Player player);

    protected abstract void playDropStringSound(Player player);

    protected static ItemStack transfer(Item target, ItemStack source) {
        ItemStack result = new ItemStack(target);
        result.setDamageValue(source.getDamageValue());
        if (source.hasTag()) {
            result.setTag(source.getTag().copy());
        }
        return result;
    }

    public static double shootingAccuracy(Player player) {
        int useTicks = player.isUsingItem()
                ? player.getUseItem().getUseDuration() - player.getUseItemRemainingTicks()
                : 0;
        double focusedAccuracy = useTicks * 0.1 * aimingSpeedMultiplier(player);
        return (PlayerSkill.SHARPSHOOTING.getFullValue(player) + 1.0 + focusedAccuracy)
                * (1.0 + player.getAttackStrengthScale(0.0F))
                * (player.isCrouching() ? 2.0 : 1.0)
                * (player.isSprinting() ? 0.5 : 1.0);
    }

    /**
     * Aiming keeps its original speed at base stats. Handling, focus and luck
     * use separate saturating terms, with a combined hard limit of +20%.
     */
    public static double aimingSpeedMultiplier(Player player) {
        if (IblisConfig.useLegacy112(Legacy112Feature.FIREARM_AIMING_SPEED)) {
            return 1.0;
        }
        double attackLevels = PlayerCharacteristic.ATTACK_SPEED.getInvestedLevels(player);
        double intelligence = Math.max(
                PlayerCharacteristic.INTELLIGENCE.getCurrentValue(player), 0.0);
        double luck = Math.max(player.getAttributeValue(Attributes.LUCK), 0.0);
        double bonus = diminishingBonus(attackLevels, AIM_ATTACK_SPEED_MAX_BONUS, 20.0)
                + diminishingBonus(intelligence, AIM_INTELLIGENCE_MAX_BONUS, 10.0)
                + diminishingBonus(luck, AIM_LUCK_MAX_BONUS, 5.0);
        return 1.0 + Math.min(bonus, 0.20);
    }

    private static double diminishingBonus(double value, double maximum, double softCap) {
        return value <= 0.0 ? 0.0 : maximum * value / (value + softCap);
    }
}
