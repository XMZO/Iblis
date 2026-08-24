package iblis.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import iblis.IblisMod;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IblisMod.MOD_ID)
public final class BlockInteractionConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String SYNTAX_HELP =
            "Use namespace:block or #namespace:tag. Restart or run /reload after editing.";

    private static final Definition SHOTGUN_DESTRUCTION = new Definition(
            "shotgun_block_destruction.json",
            "Blocks the shotgun may destroy. Destructible blocks are also penetrable. "
                    + SYNTAX_HELP,
            List.of(
                    "#forge:glass",
                    "#forge:glass_panes",
                    "#minecraft:ice",
                    "#iblis:shotgun_breakable_vegetation",
                    "#iblis:shotgun_breakable_wooden_barriers",
                    "#iblis:shotgun_breakable_fragile_blocks"));
    private static final Definition SHOTGUN_PENETRATION = new Definition(
            "shotgun_block_penetration.json",
            "Blocks the shotgun may penetrate. " + SYNTAX_HELP,
            List.of(
                    "#iblis:shotgun_breakable_vegetation",
                    "#iblis:shotgun_penetrable_doors",
                    "#minecraft:wooden_fences",
                    "#forge:fence_gates",
                    "minecraft:iron_bars",
                    "#iblis:shotgun_penetrable_chains",
                    "minecraft:cobweb",
                    "minecraft:snow",
                    "minecraft:powder_snow"));
    private static final Definition CROSSBOW_DESTRUCTION = new Definition(
            "crossbow_block_destruction.json",
            "Blocks the crossbow may destroy. Destructible blocks are also penetrable. "
                    + SYNTAX_HELP,
            List.of("#forge:glass", "#forge:glass_panes"));
    private static final Definition CROSSBOW_PENETRATION = new Definition(
            "crossbow_block_penetration.json",
            "Blocks the crossbow may penetrate. " + SYNTAX_HELP,
            List.of(
                    "#minecraft:leaves",
                    "#minecraft:flowers",
                    "#minecraft:saplings",
                    "#minecraft:crops",
                    "#iblis:shotgun_breakable_vines",
                    "#iblis:shotgun_breakable_aquatic_plants",
                    "#iblis:shotgun_breakable_small_plants",
                    "#minecraft:wooden_fences",
                    "#forge:fence_gates/wooden",
                    "minecraft:iron_bars",
                    "minecraft:snow",
                    "minecraft:powder_snow"));
    private static final List<Definition> DEFINITIONS = List.of(
            SHOTGUN_DESTRUCTION, SHOTGUN_PENETRATION,
            CROSSBOW_DESTRUCTION, CROSSBOW_PENETRATION);

    private static volatile Lists values = Lists.EMPTY;
    private static boolean loaded;

    private BlockInteractionConfig() {
    }

    public static void prepareFiles() {
        for (Definition definition : DEFINITIONS) {
            Path path = IblisConfigPaths.resolve(definition.fileName());
            if (Files.exists(path)) {
                continue;
            }
            try {
                Files.createDirectories(path.getParent());
                Files.writeString(path, defaultJson(definition), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW);
            } catch (FileAlreadyExistsException ignored) {
                // Another startup task created the same file first.
            } catch (IOException exception) {
                IblisMod.LOGGER.warn("Could not create Iblis block list {}", path, exception);
            }
        }
    }

    @SubscribeEvent
    public static void tagsUpdated(TagsUpdatedEvent event) {
        if (!event.shouldUpdateStaticData()) {
            return;
        }
        event.getRegistryAccess().registry(Registries.BLOCK)
                .ifPresent(BlockInteractionConfig::reload);
    }

    public static boolean shotgunBreakable(BlockState state) {
        return values.shotgunDestruction().contains(state.getBlock());
    }

    public static boolean shotgunPenetrable(BlockState state) {
        return values.shotgunPenetration().contains(state.getBlock());
    }

    public static boolean crossbowBreakable(BlockState state) {
        return values.crossbowDestruction().contains(state.getBlock());
    }

    public static boolean crossbowPenetrable(BlockState state) {
        Lists current = values;
        Block block = state.getBlock();
        return current.crossbowPenetration().contains(block)
                || current.crossbowDestruction().contains(block);
    }

    private static synchronized void reload(Registry<Block> registry) {
        Lists previous = values;
        Lists updated = new Lists(
                load(SHOTGUN_DESTRUCTION, registry, previous.shotgunDestruction()),
                load(SHOTGUN_PENETRATION, registry, previous.shotgunPenetration()),
                load(CROSSBOW_DESTRUCTION, registry, previous.crossbowDestruction()),
                load(CROSSBOW_PENETRATION, registry, previous.crossbowPenetration()));
        values = updated;
        loaded = true;
        IblisMod.LOGGER.info(
                "Loaded Iblis block lists: shotgun {}/{} and crossbow {}/{} (destroy/penetrate)",
                updated.shotgunDestruction().size(), updated.shotgunPenetration().size(),
                updated.crossbowDestruction().size(), updated.crossbowPenetration().size());
    }

    private static Set<Block> load(
            Definition definition, Registry<Block> registry, Set<Block> previous) {
        Path path = IblisConfigPaths.resolve(definition.fileName());
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                throw new JsonParseException("root must be a JSON object");
            }
            JsonElement valuesElement = root.getAsJsonObject().get("values");
            if (valuesElement == null || !valuesElement.isJsonArray()) {
                throw new JsonParseException("'values' must be a JSON array");
            }

            List<String> entries = new ArrayList<>();
            JsonArray array = valuesElement.getAsJsonArray();
            for (int index = 0; index < array.size(); ++index) {
                JsonElement element = array.get(index);
                if (!element.isJsonPrimitive()
                        || !element.getAsJsonPrimitive().isString()) {
                    IblisMod.LOGGER.warn("Skipping non-string entry {} in {}", index, path);
                    continue;
                }
                entries.add(element.getAsString());
            }
            return resolve(entries, registry, path.toString());
        } catch (IOException | RuntimeException exception) {
            String fallback = loaded ? "keeping its previous valid values"
                    : "using its built-in defaults";
            IblisMod.LOGGER.error("Could not load {}: {}; {}",
                    path, exception.getMessage(), fallback);
            return loaded ? previous
                    : resolve(definition.defaults(), registry, "built-in " + definition.fileName());
        }
    }

    private static Set<Block> resolve(
            Iterable<String> entries, Registry<Block> registry, String source) {
        Set<Block> blocks = new HashSet<>();
        for (String configuredEntry : entries) {
            String entry = configuredEntry.trim();
            boolean tagEntry = entry.startsWith("#");
            String idText = tagEntry ? entry.substring(1) : entry;
            ResourceLocation id = ResourceLocation.tryParse(idText);
            if (id == null) {
                IblisMod.LOGGER.warn("Skipping invalid block entry '{}' in {}",
                        configuredEntry, source);
                continue;
            }

            if (tagEntry) {
                TagKey<Block> tag = TagKey.create(Registries.BLOCK, id);
                registry.getTag(tag).ifPresentOrElse(
                        values -> values.forEach(holder -> blocks.add(holder.value())),
                        () -> IblisMod.LOGGER.warn(
                                "Skipping unknown block tag '#{}' in {}", id, source));
            } else {
                registry.getOptional(id).ifPresentOrElse(blocks::add,
                        () -> IblisMod.LOGGER.warn(
                                "Skipping unknown block '{}' in {}", id, source));
            }
        }
        return Set.copyOf(blocks);
    }

    private static String defaultJson(Definition definition) {
        JsonObject root = new JsonObject();
        root.addProperty("_comment", definition.description());
        JsonArray entries = new JsonArray();
        definition.defaults().forEach(entries::add);
        root.add("values", entries);
        return GSON.toJson(root) + System.lineSeparator();
    }

    private record Definition(String fileName, String description, List<String> defaults) {
    }

    private record Lists(
            Set<Block> shotgunDestruction,
            Set<Block> shotgunPenetration,
            Set<Block> crossbowDestruction,
            Set<Block> crossbowPenetration) {
        private static final Lists EMPTY = new Lists(Set.of(), Set.of(), Set.of(), Set.of());
    }
}
