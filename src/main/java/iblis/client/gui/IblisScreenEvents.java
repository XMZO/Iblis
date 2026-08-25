package iblis.client.gui;

import iblis.IblisMod;
import iblis.config.IblisConfig;
import iblis.crafting.IblisCraftingEvents;
import iblis.network.IblisNetwork;
import iblis.player.PlayerCharacteristic;
import iblis.player.PlayerSkill;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IblisMod.MOD_ID, value = Dist.CLIENT)
public final class IblisScreenEvents {
    public static final ResourceLocation ICONS = new ResourceLocation(
            IblisMod.MOD_ID, "textures/gui/icons.png");
    private static ImageButton trainButton;
    private static CraftingScreen trainScreen;
    private static InventoryScreen inventoryScreen;
    private static ImageButton characteristicsButton;
    private static ImageButton skillsButton;
    private static long trainCooldown;

    private IblisScreenEvents() {
    }

    @SubscribeEvent
    public static void screenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (screen instanceof InventoryScreen inventory) {
            inventoryScreen = inventory;
            characteristicsButton = null;
            skillsButton = null;
            if (IblisConfig.showCharacteristicsInventoryButton
                    && hasEnabledCharacteristics()) {
                characteristicsButton = iconButton(inventory.getGuiLeft() + 125,
                        inventory.getGuiTop() + 61, 0,
                        Component.translatable("iblis.screen.characteristics"),
                        ignored -> openCharacteristicsScreen(inventory));
                event.addListener(characteristicsButton);
            }
            if (IblisConfig.showSkillsInventoryButton && hasEnabledSkills()) {
                skillsButton = iconButton(inventory.getGuiLeft() + 146,
                        inventory.getGuiTop() + 61, 20,
                        Component.translatable("iblis.screen.skills"),
                        ignored -> openSkillsScreen(inventory));
                event.addListener(skillsButton);
            }
        }
        if (screen instanceof CraftingScreen crafting) {
            trainScreen = crafting;
            trainButton = iconButton(crafting.getGuiLeft() + 122,
                    crafting.getGuiTop() + 61, 60,
                    Component.translatable("iblis.trainCraftTooltip"), ignored -> {
                        IblisNetwork.sendTrainToCraft();
                        trainCooldown = Util.getMillis() + 250L;
                    });
            trainButton.visible = false;
            event.addListener(trainButton);
        }
    }

    @SubscribeEvent
    public static void screenRendered(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof InventoryScreen inventory
                && inventory == inventoryScreen) {
            if (characteristicsButton != null) {
                characteristicsButton.setPosition(
                        inventory.getGuiLeft() + 125, inventory.getGuiTop() + 61);
            }
            if (skillsButton != null) {
                skillsButton.setPosition(
                        inventory.getGuiLeft() + 146, inventory.getGuiTop() + 61);
            }
        }
        if (!(event.getScreen() instanceof CraftingScreen crafting)
                || crafting != trainScreen || trainButton == null) {
            return;
        }
        trainButton.setPosition(crafting.getGuiLeft() + 122, crafting.getGuiTop() + 61);
        ItemStack result = crafting.getMenu().getSlot(
                crafting.getMenu().getResultSlotIndex()).getItem();
        IblisCraftingEvents.CraftingProfile profile =
                IblisCraftingEvents.profileFor(result);
        Player player = Minecraft.getInstance().player;
        boolean visible = profile != null && profile.skill().enabled && player != null;
        trainButton.visible = visible;
        trainButton.active = visible && Util.getMillis() >= trainCooldown;
        if (!visible) {
            return;
        }

        String skillName = Component.translatable(
                profile.skill().getAttribute().getDescriptionId()).getString();
        String current = String.format(Locale.ROOT, "%s: %.1f", skillName,
                profile.skill().getRawFullValue(player));
        String detail = profile.minimum() > 0.0
                ? Component.translatable("iblis.requiredSkill", profile.minimum()).getString()
                : Component.translatable("iblis.skillExp", formatExperience(
                        player, profile.skill(), profile.experience())).getString();
        int x = crafting.getGuiLeft() + 88;
        int y = crafting.getGuiTop() + 6;
        event.getGuiGraphics().drawString(Minecraft.getInstance().font,
                current, x, y, 0xFF404040, false);
        event.getGuiGraphics().drawString(Minecraft.getInstance().font,
                detail, x, y + 13, 0xFF404040, false);
    }

    private static String formatExperience(Player player, PlayerSkill skill, double value) {
        double real = skill.getProgressForAction(player, value);
        return real > 0.1
                ? String.format(Locale.ROOT, "%.1f", real)
                : "1/" + Math.max(1L, Math.round(1.0 / Math.max(real, 0.000001)));
    }

    public static boolean openCharacteristicsScreen(Screen parent) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !hasEnabledCharacteristics()) {
            return false;
        }
        minecraft.setScreen(new CharacteristicsScreen(parent));
        return true;
    }

    public static boolean openSkillsScreen(Screen parent) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !hasEnabledSkills()) {
            return false;
        }
        minecraft.setScreen(new SkillsScreen(parent));
        return true;
    }

    private static boolean hasEnabledCharacteristics() {
        return Arrays.stream(PlayerCharacteristic.values()).anyMatch(value -> value.enabled);
    }

    private static boolean hasEnabledSkills() {
        return Arrays.stream(PlayerSkill.values()).anyMatch(value -> value.enabled);
    }

    private static ImageButton iconButton(int x, int y, int textureX,
                                          Component tooltip,
                                          net.minecraft.client.gui.components.Button.OnPress press) {
        ImageButton button = new ImageButton(x, y, 20, 18,
                textureX, 220, 18, ICONS, 256, 256, press, tooltip);
        button.setTooltip(Tooltip.create(tooltip));
        return button;
    }
}
