package iblis_headshots.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import iblis.config.IblisConfigPaths;
import iblis_headshots.IblisHeadshotsMod;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public final class HelmetProtectionOverrides {
    private static final Object2FloatMap<ResourceLocation> VALUES = new Object2FloatOpenHashMap<>();

    private HelmetProtectionOverrides() {
    }

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(HelmetProtectionOverrides::load);
    }

    public static float getOrDefault(ResourceLocation item, float fallback) {
        return VALUES.getOrDefault(item, fallback);
    }

    private static void load() {
        VALUES.clear();
        Path path = IblisConfigPaths.headshotHelmets();
        if (!Files.isRegularFile(path)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonArray()) {
                IblisHeadshotsMod.LOGGER.warn("Ignoring non-array headshot helmet config at {}", path);
                return;
            }

            for (JsonElement element : root.getAsJsonArray()) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject entry = element.getAsJsonObject();
                if (!entry.has("item") || !entry.has("protection")) {
                    continue;
                }
                ResourceLocation item = ResourceLocation.tryParse(entry.get("item").getAsString());
                if (item != null) {
                    VALUES.put(item, 1.0F - entry.get("protection").getAsInt() / 100.0F);
                }
            }
        } catch (IOException | RuntimeException exception) {
            IblisHeadshotsMod.LOGGER.error("Failed to read headshot helmet config {}", path, exception);
        }
    }
}
