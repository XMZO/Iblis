package iblis_headshots.config;

import iblis_headshots.IblisHeadshotsMod;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Entity types allowed to receive headshot damage when the whitelist is enabled. */
public final class HeadshotEntityWhitelist {
    private static final String DEFAULT_CONTENT = String.join(System.lineSeparator(),
            "# Enable headshot_entity_whitelist_enabled in the common config to use this file.",
            "# Only listed Forge entity IDs can receive headshot damage when enabled.",
            "# Add one ID per line, for example minecraft:zombie or modid:entity_name.",
            "# Blank lines, comments, invalid IDs, and unknown IDs are skipped.",
            "# Restart the game or server after editing.",
            "# minecraft:zombie",
            "");
    private static volatile Set<EntityType<?>> values = Set.of();

    private HeadshotEntityWhitelist() {
    }

    public static void prepareFile() {
        Path path = HeadshotsConfigPaths.entityWhitelist();
        if (Files.exists(path)) {
            return;
        }
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, DEFAULT_CONTENT, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW);
        } catch (FileAlreadyExistsException ignored) {
            // Another startup task created the file first.
        } catch (IOException exception) {
            IblisHeadshotsMod.LOGGER.warn(
                    "Could not create headshot entity whitelist {}", path, exception);
        }
    }

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(HeadshotEntityWhitelist::load);
    }

    public static boolean contains(Entity entity) {
        return entity != null && contains(entity.getType());
    }

    public static boolean contains(EntityType<?> type) {
        return type != null && values.contains(type);
    }

    private static void load() {
        Path path = HeadshotsConfigPaths.entityWhitelist();
        Set<EntityType<?>> updated = new HashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                int comment = line.indexOf('#');
                String entry = (comment >= 0 ? line.substring(0, comment) : line).trim();
                if (entry.isEmpty()) {
                    continue;
                }
                ResourceLocation id = ResourceLocation.tryParse(entry);
                if (id == null) {
                    IblisHeadshotsMod.LOGGER.warn(
                            "Skipping invalid entity ID '{}' at {}:{}", entry, path, lineNumber);
                    continue;
                }
                EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
                if (type == null) {
                    IblisHeadshotsMod.LOGGER.warn(
                            "Skipping unknown entity '{}' at {}:{}", id, path, lineNumber);
                    continue;
                }
                updated.add(type);
            }
        } catch (IOException | RuntimeException exception) {
            IblisHeadshotsMod.LOGGER.error(
                    "Could not load headshot entity whitelist {}; keeping previous values",
                    path, exception);
            return;
        }
        values = Set.copyOf(updated);
        IblisHeadshotsMod.LOGGER.info(
                "Loaded {} headshot-whitelist entity types from {}", values.size(), path);
    }
}
