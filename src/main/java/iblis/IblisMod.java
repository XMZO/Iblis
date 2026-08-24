package iblis;

import com.mojang.logging.LogUtils;
import iblis.config.BlockInteractionConfig;
import iblis.config.IblisConfig;
import iblis.config.IblisConfigPaths;
import iblis.compat.CompatBootstrap;
import iblis.registry.IblisAttributes;
import iblis.registry.IblisBlocks;
import iblis.registry.IblisCreativeTabs;
import iblis.registry.IblisEffects;
import iblis.registry.IblisEntities;
import iblis.registry.IblisItems;
import iblis.registry.IblisParticles;
import iblis.registry.IblisSounds;
import iblis.network.IblisNetwork;
import iblis.loot.IblisLootModifiers;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

@Mod(IblisMod.MOD_ID)
public final class IblisMod {
    public static final String MOD_ID = "iblis";
    public static final Logger LOGGER = LogUtils.getLogger();

    public IblisMod(FMLJavaModLoadingContext context) {
        IblisConfigPaths.prepare();
        BlockInteractionConfig.prepareFiles();
        IEventBus modBus = context.getModEventBus();
        IblisAttributes.register(modBus);
        IblisBlocks.register(modBus);
        IblisItems.register(modBus);
        IblisEffects.register(modBus);
        IblisEntities.register(modBus);
        IblisParticles.register(modBus);
        IblisSounds.register(modBus);
        IblisCreativeTabs.register(modBus);
        IblisLootModifiers.register(modBus);
        CompatBootstrap.register(modBus);
        modBus.addListener(IblisAttributes::addPlayerAttributes);
        modBus.addListener(IblisEntities::createAttributes);
        modBus.addListener(IblisConfig::onConfigChanged);
        modBus.addListener(IblisNetwork::onCommonSetup);
        modBus.addListener(IblisMod::onCommonSetup);

        context.registerConfig(ModConfig.Type.COMMON, IblisConfig.SPEC, IblisConfigPaths.COMMON);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ((RangedAttribute) Attributes.ARMOR).maxValue = Double.MAX_VALUE;
            ((RangedAttribute) Attributes.ARMOR_TOUGHNESS).maxValue = Double.MAX_VALUE;
            Items.SHIELD.maxDamage = 200;
        });
    }

}
