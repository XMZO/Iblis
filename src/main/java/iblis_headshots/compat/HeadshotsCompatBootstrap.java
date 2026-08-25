package iblis_headshots.compat;

import iblis_headshots.IblisHeadshotsMod;
import iblis_headshots.compat.tacz.TaczHeadshotCompat;
import java.util.List;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/** Loads optional Headshots integrations after entity registries are ready. */
public final class HeadshotsCompatBootstrap {
    private static final List<OptionalModule> MODULES = List.of(
            new OptionalModule("tacz", TaczHeadshotCompat::register)
    );

    private HeadshotsCompatBootstrap() {
    }

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(HeadshotsCompatBootstrap::register);
    }

    private static void register() {
        MODULES.stream()
                .filter(module -> ModList.get().isLoaded(module.modId()))
                .forEach(OptionalModule::install);
    }

    private record OptionalModule(String modId, Runnable installer) {
        private void install() {
            try {
                installer.run();
                IblisHeadshotsMod.LOGGER.info("Enabled {} Headshots compatibility", modId);
            } catch (RuntimeException | LinkageError error) {
                IblisHeadshotsMod.LOGGER.error(
                        "Disabled broken {} Headshots compatibility; gameplay will continue",
                        modId, error);
            }
        }
    }
}
