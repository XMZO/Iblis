package iblis.item;

import iblis.IblisMod;
import iblis.damage.IblisDamageTypes;
import iblis.config.IblisConfig;
import iblis.event.IblisGameplayEvents;
import iblis.event.ShotgunVibrationEvents;
import iblis.registry.IblisItems;
import iblis.registry.IblisSounds;
import iblis.util.BloodColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;

public final class ShotgunItem extends FirearmItem {
    private static final int MAX_BLOCK_PENETRATIONS = 32;
    private static final TagKey<Block> SHOTGUN_BREAKABLE = BlockTags.create(
            ResourceLocation.fromNamespaceAndPath(IblisMod.MOD_ID, "shotgun_breakable"));
    private static final TagKey<Block> SHOTGUN_PENETRABLE = BlockTags.create(
            ResourceLocation.fromNamespaceAndPath(IblisMod.MOD_ID, "shotgun_penetrable"));
    private static final TagKey<Block> SHOTGUN_BREAKABLE_VEGETATION = BlockTags.create(
            ResourceLocation.fromNamespaceAndPath(
                    IblisMod.MOD_ID, "shotgun_breakable_vegetation"));
    private static final TagKey<Block> SHOTGUN_BREAKABLE_WOODEN_BARRIERS = BlockTags.create(
            ResourceLocation.fromNamespaceAndPath(
                    IblisMod.MOD_ID, "shotgun_breakable_wooden_barriers"));
    private static final TagKey<Block> SHOTGUN_BREAKABLE_FRAGILE_BLOCKS = BlockTags.create(
            ResourceLocation.fromNamespaceAndPath(
                    IblisMod.MOD_ID, "shotgun_breakable_fragile_blocks"));
    private static final TagKey<Block> SHOTGUN_PENETRABLE_DOORS = BlockTags.create(
            ResourceLocation.fromNamespaceAndPath(
                    IblisMod.MOD_ID, "shotgun_penetrable_doors"));

    public ShotgunItem(Properties properties) {
        super(properties);
    }

    @Override
    protected void fire(ServerLevel level, Vec3 aim, ServerPlayer player, boolean critical,
                        double accuracy, float ammoDamage, int ammoType) {
        IblisGameplayEvents.markGunshot(player);
        Vec3 start = player.getEyePosition();
        Vec3 traceEnd = start.add(aim.scale(256.0));
        try (ShotgunVibrationEvents.ShotScope vibration =
                     ShotgunVibrationEvents.begin(level, player)) {
            ShotTrace trace = traceBlocks(level, player, start, traceEnd);
            BlockHitResult blockHit = trace.blockHit;
            boolean hitBlock = blockHit.getType() == HitResult.Type.BLOCK;
            Vec3 end = hitBlock ? blockHit.getLocation() : traceEnd;
            if (hitBlock) {
                vibration.landAt(end);
            }

            boolean addDecal = false;
            if (hitBlock) {
                BlockPos pos = blockHit.getBlockPos();
                BlockState state = level.getBlockState(pos);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                        end.x, end.y, end.z, 8, 0.15, 0.15, 0.15, 0.2);
                addDecal = true;
            }

            float damage = (float) player.getAttributeValue(iblis.registry.IblisAttributes.PROJECTILE_DAMAGE.get())
                    * ammoDamage;
            if (critical) {
                damage *= (float) LUCKY_SHOT_DAMAGE_MULTIPLIER;
            }
            float splashCone = ammoType == 0 ? 0.0F : 0.02F;
            LivingEntity lastHit = damageEntitiesOnPath(level,
                    IblisDamageTypes.shotgun(level, player), player, start, end, damage, splashCone,
                    trace.doorDistances, trace.doorCount);
            if (addDecal) {
                Vec3 bloodPosition = null;
                net.minecraft.core.Direction bloodFace = null;
                int bloodColour = -1;
                if (lastHit != null) {
                    java.util.Optional<Vec3> victimIntersection = lastHit.getBoundingBox().clip(start, end);
                    if (victimIntersection.isPresent()) {
                        Vec3 from = victimIntersection.get();
                        Vec3 to = from.add(aim.scale(4.0)).add(0.0, -2.0, 0.0);
                        BlockHitResult bloodHit = level.clip(new ClipContext(from, to,
                                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                        if (bloodHit.getType() == HitResult.Type.BLOCK) {
                            bloodColour = BloodColors.forEntity(lastHit);
                            if (bloodColour >= 0) {
                                bloodPosition = bloodHit.getLocation();
                                bloodFace = bloodHit.getDirection();
                            }
                        }
                    }
                }
                iblis.network.IblisNetwork.sendShotImpact(level, end, blockHit.getDirection(),
                        ammoType, splashCone, start.distanceTo(end),
                        bloodPosition, bloodFace, bloodColour);
            }
        }
        Vec3 hand = player.isUsingItem() ? Vec3.ZERO
                : vectorForRotation(player.getXRot() - 15.0F, player.getYRot() + 9.0F);
        level.sendParticles(ParticleTypes.SMOKE,
                player.getX() + hand.x, player.getY() + player.getEyeHeight() + hand.y,
                player.getZ() + hand.z, 1, 0.0, 0.1, 0.0, 0.0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                IblisSounds.SHOOT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    protected int baseFireCooldownTicks() {
        return IblisConfig.shotgunFireCooldownTicks;
    }

    private static ShotTrace traceBlocks(
            ServerLevel level, ServerPlayer player, Vec3 start, Vec3 end) {
        ShotTrace trace = new ShotTrace();
        ClipContext collider = new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        ClipContext outline = new ClipContext(start, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        trace.blockHit = BlockGetter.traverseBlocks(start, end, level,
                (world, pos) -> {
                    BlockState state = world.getBlockState(pos);
                    boolean potentialPenetration = isPotentialPenetration(state);
                    VoxelShape shape = (potentialPenetration ? outline : collider)
                            .getBlockShape(state, world, pos);
                    BlockHitResult hit = world.clipWithInteractionOverride(
                            start, end, pos, shape, state);
                    if (hit == null) {
                        return null;
                    }
                    if (!isShotgunPenetrable(level, pos, state)
                            || ++trace.penetratedBlocks > MAX_BLOCK_PENETRATIONS) {
                        return hit;
                    }

                    if (state.is(SHOTGUN_PENETRABLE_DOORS)) {
                        trace.addDoor(state, pos, start.distanceTo(hit.getLocation()));
                    }
                    if (isShotgunBreakable(level, pos, state)) {
                        boolean vegetation = state.is(SHOTGUN_BREAKABLE_VEGETATION);
                        if (vegetation ? !trace.vegetationBreakAttempted
                                : !trace.barrierBreakAttempted) {
                            if (vegetation) {
                                trace.vegetationBreakAttempted = true;
                            } else {
                                trace.barrierBreakAttempted = true;
                            }
                            breakShotgunBlock(level, pos.immutable(), state, player);
                        }
                    }
                    return null;
                },
                world -> BlockHitResult.miss(end, Direction.getNearest(
                        end.x - start.x, end.y - start.y, end.z - start.z),
                        BlockPos.containing(end)));
        return trace;
    }

    private static boolean isPotentialPenetration(BlockState state) {
        return state.is(SHOTGUN_BREAKABLE)
                || state.is(SHOTGUN_PENETRABLE)
                || state.is(SHOTGUN_PENETRABLE_DOORS)
                || state.getSoundType() == SoundType.GLASS;
    }

    private static boolean isShotgunPenetrable(
            ServerLevel level, BlockPos pos, BlockState state) {
        boolean explicitlyPenetrable = state.is(SHOTGUN_PENETRABLE);
        if (state.hasBlockEntity() || state.is(BlockTags.PORTALS)
                || !explicitlyPenetrable && (state.is(BlockTags.DRAGON_IMMUNE)
                || state.is(BlockTags.WITHER_IMMUNE))) {
            return false;
        }
        return explicitlyPenetrable
                || state.is(SHOTGUN_PENETRABLE_DOORS)
                || isShotgunBreakable(level, pos, state);
    }

    private static boolean breakShotgunBlock(
            ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(level, pos, state, player);
        if (!MinecraftForge.EVENT_BUS.post(breakEvent)
                && level.getBlockState(pos) == state) {
            level.levelEvent(player, 2001, pos, Block.getId(state));
            return level.destroyBlock(pos, true, player);
        }
        return false;
    }

    private static boolean isShotgunBreakable(
            ServerLevel level, BlockPos pos, BlockState state) {
        float destroySpeed = state.getDestroySpeed(level, pos);
        if (destroySpeed < 0.0F || state.hasBlockEntity()
                || state.is(BlockTags.PORTALS) || state.is(BlockTags.DRAGON_IMMUNE)
                || state.is(BlockTags.WITHER_IMMUNE)) {
            return false;
        }
        if (state.is(SHOTGUN_BREAKABLE_WOODEN_BARRIERS)
                || state.is(SHOTGUN_BREAKABLE_FRAGILE_BLOCKS)) {
            return true;
        }
        return destroySpeed < 0.6F
                && (state.is(SHOTGUN_BREAKABLE)
                || state.getSoundType() == SoundType.GLASS);
    }

    private static final class ShotTrace {
        private final double[] doorDistances = new double[MAX_BLOCK_PENETRATIONS];
        private final long[] doorKeys = new long[MAX_BLOCK_PENETRATIONS];
        private BlockHitResult blockHit;
        private int penetratedBlocks;
        private int doorCount;
        private boolean vegetationBreakAttempted;
        private boolean barrierBreakAttempted;

        private void addDoor(BlockState state, BlockPos pos, double distance) {
            BlockPos keyPos = state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                    && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)
                    == DoubleBlockHalf.UPPER ? pos.below() : pos;
            long key = keyPos.asLong();
            for (int i = 0; i < doorCount; ++i) {
                if (doorKeys[i] == key) {
                    return;
                }
            }
            doorKeys[doorCount] = key;
            doorDistances[doorCount++] = distance;
        }
    }

    private static Vec3 vectorForRotation(float pitch, float yaw) {
        float cosYaw = net.minecraft.util.Mth.cos(-yaw * net.minecraft.util.Mth.DEG_TO_RAD
                - net.minecraft.util.Mth.PI);
        float sinYaw = net.minecraft.util.Mth.sin(-yaw * net.minecraft.util.Mth.DEG_TO_RAD
                - net.minecraft.util.Mth.PI);
        float horizontal = -net.minecraft.util.Mth.cos(-pitch * net.minecraft.util.Mth.DEG_TO_RAD);
        float vertical = net.minecraft.util.Mth.sin(-pitch * net.minecraft.util.Mth.DEG_TO_RAD);
        return new Vec3(sinYaw * horizontal, vertical, cosYaw * horizontal);
    }

    @Override
    public ItemStack toReloading(ItemStack stack) {
        return transfer(IblisItems.SHOTGUN_RELOADING.get(), ensureTag(stack));
    }

    private static ItemStack ensureTag(ItemStack stack) {
        stack.getOrCreateTag();
        return stack;
    }

    @Override
    public void playReloadingSound(Player player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                IblisSounds.SHOTGUN_CHARGING.get(), SoundSource.PLAYERS,
                1.0F, player.getRandom().nextFloat() * 0.2F + 0.8F);
    }

    @Override
    protected void playDropStringSound(Player player) {
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forge", "ingots/steel")))
                || super.isValidRepairItem(stack, repairCandidate);
    }
}
