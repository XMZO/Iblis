package iblis.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import iblis.IblisMod;
import iblis.player.PlayerAttributeEffects;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = IblisMod.MOD_ID)
public final class IblisCommands {
    private static final SuggestionProvider<CommandSourceStack> ATTRIBUTES = (context, builder) ->
            SharedSuggestionProvider.suggest(Stream.concat(
                    ForgeRegistries.ATTRIBUTES.getKeys().stream()
                            .map(ResourceLocation::toString),
                    ForgeRegistries.ATTRIBUTES.getKeys().stream()
                            .map(IblisCommands::legacyAttributeName)
                            .filter(Objects::nonNull)).distinct(), builder);

    private IblisCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("getattribute")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("attribute", StringArgumentType.word())
                        .suggests(ATTRIBUTES)
                        .executes(context -> getAttribute(context.getSource(),
                                StringArgumentType.getString(context, "attribute")))));
        event.getDispatcher().register(Commands.literal("setattribute")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("attribute", StringArgumentType.word())
                        .suggests(ATTRIBUTES)
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                .executes(context -> setAttribute(context.getSource(),
                                        StringArgumentType.getString(context, "attribute"),
                                        DoubleArgumentType.getDouble(context, "value"))))));
        event.getDispatcher().register(Commands.literal("iblis")
                .requires(source -> source.hasPermission(2))
                .executes(context -> showNbt(context.getSource(), "held"))
                .then(Commands.literal("playernbt")
                        .executes(context -> showNbt(context.getSource(), "player")))
                .then(Commands.literal("playernbtraw")
                        .executes(context -> showNbt(context.getSource(), "raw"))));
    }

    private static int getAttribute(CommandSourceStack source, String name)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        AttributeInstance instance = findAttribute(player, name);
        source.sendSuccess(() -> Component.literal("Attribute base value: "
                + instance.getBaseValue()), false);
        source.sendSuccess(() -> Component.literal("Attribute current value: "
                + instance.getValue()), false);
        return 1;
    }

    private static int setAttribute(CommandSourceStack source, String name, double value)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        AttributeInstance instance = findAttribute(player, name);
        instance.setBaseValue(value);
        PlayerAttributeEffects.refreshMeleeDamageBonus(player);
        PlayerAttributeEffects.refreshWeaponSkill(player);
        source.sendSuccess(() -> Component.literal("Attribute base value set to " + value), true);
        return 1;
    }

    private static AttributeInstance findAttribute(ServerPlayer player, String name)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ResourceLocation id = ResourceLocation.tryParse(name);
        Attribute attribute = id == null ? null : ForgeRegistries.ATTRIBUTES.getValue(id);
        if (attribute == null) {
            id = modernAttributeId(name);
            attribute = id == null ? null : ForgeRegistries.ATTRIBUTES.getValue(id);
        }
        if (attribute == null) {
            attribute = ForgeRegistries.ATTRIBUTES.getValues().stream()
                    .filter(value -> value.getDescriptionId().equals(name))
                    .findFirst().orElse(null);
        }
        AttributeInstance instance = attribute == null ? null : player.getAttribute(attribute);
        if (instance == null) {
            throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(
                    Component.literal("Unknown player attribute: " + name)).create();
        }
        return instance;
    }

    private static ResourceLocation modernAttributeId(String name) {
        if (name.startsWith("iblis.")) {
            return new ResourceLocation(IblisMod.MOD_ID,
                    name.substring("iblis.".length()).toLowerCase(Locale.ROOT));
        }
        if (name.startsWith("generic.")) {
            return new ResourceLocation("minecraft", "generic."
                    + camelToSnake(name.substring("generic.".length())));
        }
        return null;
    }

    private static String legacyAttributeName(ResourceLocation id) {
        if (id.getNamespace().equals(IblisMod.MOD_ID)) {
            return "iblis." + id.getPath();
        }
        if (id.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)
                && id.getPath().startsWith("generic.")) {
            String path = id.getPath().substring("generic.".length());
            StringBuilder result = new StringBuilder("generic.");
            boolean upper = false;
            for (int index = 0; index < path.length(); index++) {
                char character = path.charAt(index);
                if (character == '_') {
                    upper = true;
                } else if (upper) {
                    result.append(Character.toUpperCase(character));
                    upper = false;
                } else {
                    result.append(character);
                }
            }
            return result.toString();
        }
        return null;
    }

    private static String camelToSnake(String value) {
        StringBuilder result = new StringBuilder(value.length() + 4);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isUpperCase(character)) {
                result.append('_').append(Character.toLowerCase(character));
            } else {
                result.append(Character.toLowerCase(character));
            }
        }
        return result.toString();
    }

    private static int showNbt(CommandSourceStack source, String mode)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CompoundTag tag;
        if (mode.equals("player") || mode.equals("raw")) {
            tag = player.saveWithoutId(new CompoundTag());
        } else {
            ItemStack held = player.getMainHandItem();
            tag = held.hasTag() ? held.getTag().copy() : null;
            if (tag == null) {
                Entity nearby = player.level().getEntities(player,
                                player.getBoundingBox().inflate(4.0)).stream()
                        .findFirst().orElse(null);
                tag = nearby == null ? new CompoundTag() : nearby.saveWithoutId(new CompoundTag());
            }
        }
        if (mode.equals("raw")) {
            String raw = tag.toString();
            source.sendSuccess(() -> Component.literal(raw), false);
            return 1;
        }
        showStructuredNbt(source, tag);
        return tag.size();
    }

    private static void showStructuredNbt(CommandSourceStack source, CompoundTag tag) {
        for (String key : tag.getAllKeys()) {
            Tag value = tag.get(key);
            if (value instanceof CompoundTag compound) {
                sendLine(source, "NBTtag '" + key + "':");
                showCompoundEntries(source, compound);
            } else if (value instanceof ListTag list) {
                sendLine(source, "NBTtagList '" + key + "':");
                if (!list.isEmpty() && list.getElementType() == Tag.TAG_COMPOUND) {
                    for (int index = 0; index < list.size(); index++) {
                        showCompoundEntries(source, list.getCompound(index));
                    }
                }
            } else {
                String display = tag.getString(key);
                if (display.isEmpty()) {
                    display = Float.toString(tag.getFloat(key));
                }
                sendLine(source, " " + key + "=" + display);
            }
        }
    }

    private static void showCompoundEntries(CommandSourceStack source, CompoundTag compound) {
        if (compound.isEmpty()) {
            return;
        }
        sendLine(source, " -NBT compound tag subkeys:");
        for (String key : compound.getAllKeys()) {
            String display = compound.getString(key);
            if (display.isEmpty()) {
                display = Integer.toString(compound.getInt(key));
            }
            sendLine(source, "    " + key + "=" + display);
        }
    }

    private static void sendLine(CommandSourceStack source, String line) {
        source.sendSuccess(() -> Component.literal(line), false);
    }
}
