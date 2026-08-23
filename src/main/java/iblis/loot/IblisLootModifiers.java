package iblis.loot;

import com.mojang.serialization.Codec;
import iblis.IblisMod;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class IblisLootModifiers {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    IblisMod.MOD_ID);
    public static final RegistryObject<Codec<RandomGuideLootModifier>> RANDOM_GUIDES =
            SERIALIZERS.register("random_guides", () -> RandomGuideLootModifier.CODEC);

    private IblisLootModifiers() {
    }

    public static void register(IEventBus modBus) {
        SERIALIZERS.register(modBus);
    }
}
