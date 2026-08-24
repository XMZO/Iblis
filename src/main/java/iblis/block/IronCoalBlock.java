package iblis.block;

import iblis.registry.IblisBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

@SuppressWarnings("deprecation")
public final class IronCoalBlock extends Block {
    public static final int MAX_AGE = 15;
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, MAX_AGE);
    private static final int BASE_TICK_DELAY = 30;

    public IronCoalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
                                BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbor, neighborPos, movedByPiston);
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, BASE_TICK_DELAY + level.random.nextInt(10));
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!isIgnited(level, pos)) {
            return;
        }

        int age = state.getValue(AGE);
        if (age < MAX_AGE) {
            int nextAge = Math.min(MAX_AGE, age + random.nextInt(3) / 2);
            level.setBlock(pos, state.setValue(AGE, nextAge), 11);
            level.scheduleTick(pos, this, BASE_TICK_DELAY + random.nextInt(10));
            igniteNeighbors(level, pos, random);
        } else {
            level.setBlock(pos, IblisBlocks.SLAG.get().defaultBlockState(), 11);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (isIgnited(level, pos)) {
            Blocks.FIRE.animateTick(Blocks.FIRE.defaultBlockState(), level, pos, random);
        }
    }

    public static boolean isIgnited(LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).is(BlockTags.FIRE)) {
                return true;
            }
        }
        return false;
    }

    private static void igniteNeighbors(ServerLevel level, BlockPos pos, RandomSource random) {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighbor = level.getBlockState(neighborPos);
            int flammability = neighbor.getFlammability(level, neighborPos, direction.getOpposite());
            if (neighbor.isAir() || random.nextInt(300) < flammability) {
                level.setBlock(neighborPos, Blocks.FIRE.defaultBlockState(), 11);
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }
}
