package iblis_headshots;

import com.mojang.logging.LogUtils;
import iblis_headshots.advancement.HeadshotTrigger;
import iblis_headshots.config.HeadshotsConfig;
import iblis_headshots.config.HeadshotsConfigPaths;
import iblis_headshots.config.HelmetProtectionOverrides;
import iblis_headshots.network.HeadshotsNetwork;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(IblisHeadshotsMod.MOD_ID)
public final class IblisHeadshotsMod {
    public static final String MOD_ID = "iblis_headshots";
    public static final Logger LOGGER = LogUtils.getLogger();

    public IblisHeadshotsMod(FMLJavaModLoadingContext context) {
        HeadshotsConfigPaths.prepare();
        HeadshotTrigger.register();
        IEventBus modBus = context.getModEventBus();
        modBus.addListener(HeadshotsConfig::onConfigChanged);
        modBus.addListener(HeadshotsNetwork::onCommonSetup);
        modBus.addListener(HelmetProtectionOverrides::onCommonSetup);
        context.registerConfig(
                ModConfig.Type.COMMON, HeadshotsConfig.SPEC, HeadshotsConfigPaths.COMMON);
    }
}
