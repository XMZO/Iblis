package iblis_headshots;

import com.mojang.logging.LogUtils;
import iblis.config.IblisConfigPaths;
import iblis_headshots.compat.HeadshotsCompatBootstrap;
import iblis_headshots.config.HeadshotsConfig;
import iblis_headshots.advancement.HeadshotTrigger;
import iblis_headshots.config.HeadshotEntityBlacklist;
import iblis_headshots.config.HeadshotEntityWhitelist;
import iblis_headshots.config.HelmetProtectionOverrides;
import iblis_headshots.network.HeadshotsNetwork;
import iblis_headshots.stats.HeadshotScoreboardCriteria;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(IblisHeadshotsMod.MOD_ID)
public final class IblisHeadshotsMod {
    public static final String MOD_ID = "iblis_headshots";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** Keeps the companion loadable on Forge 47.0.x-47.3.x, which may reflect a no-arg constructor. */
    public IblisHeadshotsMod() {
        this(FMLJavaModLoadingContext.get());
    }

    public IblisHeadshotsMod(FMLJavaModLoadingContext context) {
        IblisConfigPaths.prepare();
        HeadshotEntityBlacklist.prepareFile();
        HeadshotEntityWhitelist.prepareFile();
        HeadshotTrigger.register();
        HeadshotScoreboardCriteria.bootstrap();
        IEventBus modBus = context.getModEventBus();
        modBus.addListener(HeadshotsCompatBootstrap::onCommonSetup);
        modBus.addListener(HeadshotsConfig::onConfigChanged);
        modBus.addListener(HeadshotsNetwork::onCommonSetup);
        modBus.addListener(HelmetProtectionOverrides::onCommonSetup);
        modBus.addListener(HeadshotEntityBlacklist::onCommonSetup);
        modBus.addListener(HeadshotEntityWhitelist::onCommonSetup);
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.COMMON, HeadshotsConfig.SPEC, IblisConfigPaths.HEADSHOTS_COMMON);
    }
}
