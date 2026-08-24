package iblis.config;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

public final class IblisConfigPaths {
    public static final String COMMON = "iblis/iblis-common.toml";
    public static final String HEADSHOTS_COMMON = "iblis/iblis-headshots-common.toml";
    public static final String HEADSHOT_HELMETS = "iblis_headshots_helmets_config.json";
    public static final String HEADSHOT_ENTITY_BLACKLIST = "headshot_entity_blacklist.txt";
    private static final String[] LEGACY_FILES = {
            "iblis-common.toml", "iblis-headshots-common.toml", HEADSHOT_HELMETS
    };
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean prepared;

    private IblisConfigPaths() {
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
            for (String fileName : LEGACY_FILES) {
                Path legacy = configRoot.resolve(fileName);
                Path current = directory.resolve(fileName);
                if (Files.isRegularFile(legacy) && Files.notExists(current)) {
                    Files.move(legacy, current);
                }
            }
        } catch (IOException exception) {
            LOGGER.warn("Could not migrate legacy Iblis configuration files", exception);
        }
    }

    public static Path headshotHelmets() {
        return resolve(HEADSHOT_HELMETS);
    }

    public static Path headshotEntityBlacklist() {
        return resolve(HEADSHOT_ENTITY_BLACKLIST);
    }

    public static Path resolve(String fileName) {
        return FMLPaths.CONFIGDIR.get().resolve("iblis").resolve(fileName);
    }
}
