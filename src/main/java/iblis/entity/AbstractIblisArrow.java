package iblis.entity;

import iblis.registry.IblisSounds;
import iblis.registry.IblisParticles;
import iblis_headshots.util.HeadshotGeometry;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

abstract class AbstractIblisArrow extends AbstractArrow {
    private boolean keepRotation;
    private float fixedYaw;
    private float fixedPitch;
    public boolean onHardSurface;

    protected AbstractIblisArrow(EntityType<? extends AbstractIblisArrow> type, Level level) {
        super(type, level);
    }

    protected AbstractIblisArrow(EntityType<? extends AbstractIblisArrow> type, Level level,
                                 Player owner, double x, double y, double z) {
        super(type, x, y, z, level);
        setOwner(owner);
        pickup = Pickup.ALLOWED;
    }

    @Override
    public void tick() {
        super.tick();
        if (keepRotation) {
            yRotO = fixedYaw;
            xRotO = fixedPitch;
            setYRot(fixedYaw);
            setXRot(fixedPitch);
        }
    }

    @Override
    protected EntityHitResult findHitEntity(Vec3 start, Vec3 end) {
        Entity nearest = null;
        Vec3 nearestHit = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Entity candidate : level().getEntities(this,
                new AABB(start, end).inflate(0.5), this::canHitEntity)) {
            Optional<Vec3> hit = candidate instanceof net.minecraft.world.entity.LivingEntity living
                    ? HeadshotGeometry.getHeadBox(living).clip(start, end)
                    : Optional.empty();
            if (hit.isEmpty()) {
                hit = candidate.getBoundingBox().clip(start, end);
            }
            if (hit.isPresent()) {
                double distance = start.distanceToSqr(hit.get());
                if (distance < nearestDistance) {
                    nearest = candidate;
                    nearestHit = hit.get();
                    nearestDistance = distance;
                }
            }
        }
        return nearest == null ? null : new EntityHitResult(nearest, nearestHit);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity owner = getOwner();
        float damage = modifyImpactDamage(result.getEntity(), (float) getBaseDamage());
        result.getEntity().hurt(createDamageSource(owner), damage);
        level().playSound(null, result.getLocation().x, result.getLocation().y, result.getLocation().z,
                IblisSounds.KNIFE_IMPACT.get(), SoundSource.PLAYERS,
                1.0F, random.nextFloat() * 0.4F + 0.6F);
        if (!level().isClientSide) {
            discard();
        }
    }

    protected float modifyImpactDamage(Entity target, float damage) {
        return damage;
    }

    protected DamageSource createDamageSource(Entity owner) {
        return damageSources().thrown(this, owner);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        BlockState state = level().getBlockState(result.getBlockPos());
        boolean hard = isHardSurface(state);
        if (shouldRicochet(result, hard)) {
            Vec3 movement = getDeltaMovement();
            double velocitySquared = movement.lengthSqr();
            movement = reflect(movement, result.getDirection());
            setDeltaMovement(movement.x * 0.4, movement.y * 0.4 - 0.1, movement.z * 0.4);
            keepRotation = true;
            fixedPitch = getXRot();
            fixedYaw = getYRot();
            level().playSound(null, result.getLocation().x, result.getLocation().y, result.getLocation().z,
                    IblisSounds.KNIFE_IMPACT_STONE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            if (!level().isClientSide) {
                level().broadcastEntityEvent(this, (byte) 3);
                afterHardRicochet(result, movement, velocitySquared);
            }
            return;
        }
        super.onHitBlock(result);
        onHardSurface = hard;
        if (hard) {
            playSound(IblisSounds.KNIFE_FALL.get(), 0.5F,
                    1.2F / (random.nextFloat() * 0.2F + 0.9F));
        } else {
            playSound(IblisSounds.KNIFE_IMPACT.get(), 0.5F,
                    1.2F / (random.nextFloat() * 0.2F + 0.9F));
        }
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        // The legacy entities supplied only their knife/bolt impact sounds.
        // AbstractArrow otherwise adds a second vanilla arrow-hit sound here.
        return SoundEvents.EMPTY;
    }

    protected boolean shouldRicochet(BlockHitResult result, boolean hard) {
        return hard;
    }

    protected void afterHardRicochet(BlockHitResult result, Vec3 reflected, double velocitySquared) {
    }

    @Override
    public void handleEntityEvent(byte event) {
        if (event != 3) {
            super.handleEntityEvent(event);
            return;
        }
        Vec3 movement = getDeltaMovement();
        for (int index = 0; index < 8; index++) {
            level().addParticle(IblisParticles.SPARK.get(),
                    xo - movement.x, yo - movement.y, zo - movement.z,
                    random.nextDouble() - 0.5 - movement.x * 0.1,
                    random.nextDouble() - 0.75,
                    random.nextDouble() - 0.5 - movement.z * 0.1);
        }
    }

    private static Vec3 reflect(Vec3 movement, Direction direction) {
        return switch (direction.getAxis()) {
            case X -> new Vec3(-movement.x, movement.y, movement.z);
            case Y -> new Vec3(movement.x, -movement.y, movement.z);
            case Z -> new Vec3(movement.x, movement.y, -movement.z);
        };
    }

    private static boolean isHardSurface(BlockState state) {
        SoundType sound = state.getSoundType();
        return sound == SoundType.GLASS || sound == SoundType.ANVIL
                || sound == SoundType.METAL || sound == SoundType.STONE
                || state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(Blocks.CLAY);
    }
}
