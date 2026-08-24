package iblis_headshots.config;

import iblis_headshots.IblisHeadshotsMod;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraftforge.fml.loading.FMLPaths;

public final class HeadshotsConfigPaths {
    public static final String COMMON = "iblis/iblis-headshots-common.toml";
    private static final String COMMON_FILE = "iblis-headshots-common.toml";
    private static final String HELMETS_FILE = "iblis_headshots_helmets_config.json";
    private static final String ENTITY_BLACKLIST_FILE = "headshot_entity_blacklist.txt";
    private static boolean prepared;

    private HeadshotsConfigPaths() {
    }

    public static synchronized void prepare() {
        if (prepared) {
            return;
        }
        prepared = true;

        Path configRoot = FMLPaths.CONFIGDIR.get();
        Path directory = configRoot.resolve("iblis");
        try {
            Files.createDirectories(directory);
            migrate(configRoot, directory, COMMON_FILE);
            migrate(configRoot, directory, HELMETS_FILE);
        } catch (IOException exception) {
            IblisHeadshotsMod.LOGGER.warn(
                    "Could not migrate legacy Iblis Headshots configuration files", exception);
        }
    }

    public static Path helmets() {
        return FMLPaths.CONFIGDIR.get().resolve("iblis").resolve(HELMETS_FILE);
    }

    public static Path entityBlacklist() {
        return FMLPaths.CONFIGDIR.get().resolve("iblis").resolve(ENTITY_BLACKLIST_FILE);
    }

    private static void migrate(Path root, Path directory, String fileName) throws IOException {
        Path legacy = root.resolve(fileName);
        Path current = directory.resolve(fileName);
        if (Files.isRegularFile(legacy) && Files.notExists(current)) {
            Files.move(legacy, current);
        }
    }
}
