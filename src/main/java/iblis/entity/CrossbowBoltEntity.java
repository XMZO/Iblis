package iblis.entity;

import iblis.config.BlockInteractionConfig;
import iblis.config.IblisConfig;
import iblis.config.Legacy112Feature;
import iblis.damage.IblisDamageTypes;
import iblis.registry.IblisEntities;
import iblis.registry.IblisItems;
import iblis.registry.IblisParticles;
import iblis.util.FirearmDamageRules;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;

public final class CrossbowBoltEntity extends AbstractIblisArrow {
    private static final String LEGACY_LUCKY_SHOT = "iblisLegacyLuckyShot";
    private static final int MAX_RESISTANT_PENETRATIONS = 8;
    private static final int MAX_GLASS_BREAKS = 1;
    private static final float MAX_BREAKABLE_HARDNESS = 0.5F;
    private final LongOpenHashSet resistantPenetrations = new LongOpenHashSet();
    private BlockHitResult redirectedBlockHit;
    private long passThroughBlock = Long.MIN_VALUE;
    private int glassBreaks;

    public CrossbowBoltEntity(EntityType<? extends CrossbowBoltEntity> type, Level level) {
        super(type, level);
    }

    public CrossbowBoltEntity(Level level, Player owner, double x, double y, double z) {
        super(IblisEntities.CROSSBOW_BOLT.get(), level, owner, x, y, z);
    }

    @Override
    protected DamageSource createDamageSource(Entity owner) {
        return level() instanceof ServerLevel serverLevel
                ? IblisDamageTypes.crossbow(serverLevel, this, owner)
                : super.createDamageSource(owner);
    }

    @Override
    protected float modifyImpactDamage(Entity target, float damage) {
        return FirearmDamageRules.scaleBaseDamage(target,
                FirearmDamageRules.normalizeLegacyLuckyShot(target, damage,
                        getPersistentData().getBoolean(LEGACY_LUCKY_SHOT)));
    }

    public void setLegacyLuckyShot(boolean value) {
        if (value) {
            getPersistentData().putBoolean(LEGACY_LUCKY_SHOT, true);
        } else {
            getPersistentData().remove(LEGACY_LUCKY_SHOT);
        }
    }

    @Override
    protected EntityHitResult findHitEntity(Vec3 start, Vec3 vanillaEnd) {
        redirectedBlockHit = null;
        passThroughBlock = Long.MIN_VALUE;

        if (IblisConfig.useLegacy112(Legacy112Feature.CROSSBOW_BLOCK_INTERACTIONS)) {
            return super.findHitEntity(start, vanillaEnd);
        }

        Vec3 fullEnd = start.add(getDeltaMovement());
        BlockHitResult firstBlock = level().clip(new ClipContext(start, fullEnd,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (firstBlock.getType() != HitResult.Type.BLOCK
                || !BlockInteractionConfig.crossbowPenetrable(
                level().getBlockState(firstBlock.getBlockPos()))) {
            return super.findHitEntity(start, vanillaEnd);
        }

        EntityHitResult entityHit = super.findHitEntity(start, fullEnd);
        if (entityHit != null && entityHit.getEntity() instanceof Player target
                && getOwner() instanceof Player owner && !owner.canHarmPlayer(target)) {
            entityHit = null;
        }
        Vec3 traceEnd = entityHit == null ? fullEnd : entityHit.getLocation();
        BlockHitResult blockingHit = traceBlocks(start, traceEnd);
        if (blockingHit.getType() == HitResult.Type.BLOCK) {
            redirectedBlockHit = blockingHit;
            return null;
        }
        passThroughBlock = firstBlock.getBlockPos().asLong();
        return entityHit;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        BlockHitResult redirected = redirectedBlockHit;
        redirectedBlockHit = null;
        if (redirected != null) {
            super.onHitBlock(redirected);
            return;
        }
        if (result.getBlockPos().asLong() == passThroughBlock) {
            passThroughBlock = Long.MIN_VALUE;
            return;
        }
        super.onHitBlock(result);
    }

    private BlockHitResult traceBlocks(Vec3 start, Vec3 end) {
        ClipContext collider = new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this);
        ClipContext outline = new ClipContext(start, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, this);
        return BlockGetter.traverseBlocks(start, end, level(),
                (world, pos) -> {
                    BlockState state = world.getBlockState(pos);
                    boolean potentialPenetration = BlockInteractionConfig.crossbowPenetrable(state);
                    VoxelShape shape = (potentialPenetration ? outline : collider)
                            .getBlockShape(state, world, pos);
                    BlockHitResult hit = world.clipWithInteractionOverride(
                            start, end, pos, shape, state);
                    if (hit == null) {
                        return null;
                    }
                    return potentialPenetration && penetrateBlock(pos.immutable(), state)
                            ? null : hit;
                },
                world -> BlockHitResult.miss(end, Direction.getNearest(
                        end.x - start.x, end.y - start.y, end.z - start.z),
                        BlockPos.containing(end)));
    }

    private boolean penetrateBlock(BlockPos pos, BlockState state) {
        if (state.hasBlockEntity() || state.is(BlockTags.PORTALS)) {
            return false;
        }

        long key = pos.asLong();
        boolean resistant = !state.getCollisionShape(level(), pos).isEmpty();
        boolean newResistance = resistant && !resistantPenetrations.contains(key);
        if (newResistance
                && resistantPenetrations.size() >= MAX_RESISTANT_PENETRATIONS) {
            return false;
        }

        if (BlockInteractionConfig.crossbowBreakable(state)) {
            float hardness = state.getDestroySpeed(level(), pos);
            if (glassBreaks >= MAX_GLASS_BREAKS
                    || hardness < 0.0F || hardness > MAX_BREAKABLE_HARDNESS
                    || !breakGlass(pos, state)) {
                return false;
            }
            ++glassBreaks;
        }
        if (newResistance) {
            resistantPenetrations.add(key);
        }
        return true;
    }

    private boolean breakGlass(BlockPos pos, BlockState state) {
        if (!(level() instanceof ServerLevel level)) {
            return true;
        }
        if (!(getOwner() instanceof ServerPlayer player)) {
            return false;
        }
        BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(
                level, pos, state, player);
        if (MinecraftForge.EVENT_BUS.post(breakEvent)
                || level.getBlockState(pos) != state) {
            return false;
        }
        level.levelEvent(player, 2001, pos, Block.getId(state));
        return level.destroyBlock(pos, true, player);
    }

    @Override
    protected void afterHardRicochet(BlockHitResult result, Vec3 reflected, double velocitySquared) {
        if (velocitySquared <= 3.0) {
            return;
        }
        if (level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            double xSpeed = reflected.x;
            double ySpeed = reflected.y;
            double zSpeed = reflected.z;
            int count = random.nextInt(8) + 2;
            for (int index = 0; index < count; index++) {
                xSpeed += random.nextFloat() * 0.2F - 0.1F;
                ySpeed += random.nextFloat() * 0.2F - 0.1F;
                zSpeed += random.nextFloat() * 0.2F - 0.1F;
                serverLevel.sendParticles(IblisParticles.SLIVER.get(),
                        result.getLocation().x, result.getLocation().y,
                        result.getLocation().z, 0,
                        xSpeed, ySpeed, zSpeed, 1.0);
            }
        }
        discard();
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(IblisItems.CROSSBOW_BOLT.get());
    }
}
