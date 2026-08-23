package iblis.registry;

import iblis.IblisMod;
import iblis.block.IronCoalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class IblisBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, IblisMod.MOD_ID);

    private static final BlockBehaviour.Properties FIRED_MIX_PROPERTIES = BlockBehaviour.Properties.of()
            .mapColor(MapColor.CLAY)
            .strength(3.0F, 5.0F)
            .sound(SoundType.STONE);

    public static final RegistryObject<Block> IRON_COAL =
            BLOCKS.register("iron_coal", () -> new IronCoalBlock(FIRED_MIX_PROPERTIES));
    public static final RegistryObject<Block> IRONORE_COAL =
            BLOCKS.register("ironore_coal", () -> new IronCoalBlock(FIRED_MIX_PROPERTIES));
    public static final RegistryObject<Block> SLAG =
            BLOCKS.register("slag", () -> new Block(FIRED_MIX_PROPERTIES));

    private IblisBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
