package iblis.entity;

import iblis.player.PlayerSkill;
import iblis.registry.IblisAttributes;
import iblis.registry.IblisEntities;
import iblis.registry.IblisItems;
import iblis.registry.IblisParticles;
import iblis.registry.IblisSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class BoulderEntity extends ThrowableItemProjectile {
    private float damage;

    public BoulderEntity(EntityType<? extends BoulderEntity> type, Level level) {
        super(type, level);
    }

    public BoulderEntity(Level level, Player owner, double x, double y, double z) {
        super(IblisEntities.BOULDER.get(), x, y, z, level);
        setOwner(owner);
        double baseDamage = owner.getAttributeValue(IblisAttributes.PROJECTILE_DAMAGE.get());
        damage = (float) (baseDamage * (PlayerSkill.THROWING.getFullValue(owner) * 0.1 + 0.2));
    }

    @Override
    protected Item getDefaultItem() {
        return IblisItems.BOULDER.get();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (result instanceof EntityHitResult entityHit) {
            Entity owner = getOwner();
            entityHit.getEntity().hurt(damageSources().thrown(this, owner), damage);
        }
        level().playSound(null, result.getLocation().x, result.getLocation().y, result.getLocation().z,
                IblisSounds.BOULDER_IMPACT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        if (!level().isClientSide) {
            level().broadcastEntityEvent(this, (byte) 3);
            discard();
        }
    }

    @Override
    public void handleEntityEvent(byte event) {
        if (event != 3) {
            super.handleEntityEvent(event);
            return;
        }
        for (int index = 0; index < 8; index++) {
            double x = xo - getDeltaMovement().x;
            double y = yo - getDeltaMovement().y;
            double z = zo - getDeltaMovement().z;
            double velocityX = random.nextDouble() - 0.5 - getDeltaMovement().x * 0.1;
            double velocityY = random.nextDouble() - 0.75 - getDeltaMovement().y * 0.1;
            double velocityZ = random.nextDouble() - 0.5 - getDeltaMovement().z * 0.1;
            level().addParticle(IblisParticles.BOULDER_SHARD.get(),
                    x, y, z, velocityX, velocityY, velocityZ);
            level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK,
                            Blocks.STONE.defaultBlockState()),
                    x, y, z, velocityX, velocityY, velocityZ);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("damage", damage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("damage");
    }
}
