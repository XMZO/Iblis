package iblis.client.gui;

import iblis.IblisMod;
import iblis.client.IblisClientEvents;
import iblis.config.IblisConfig;
import iblis.item.FirearmItem;
import iblis.item.ShotgunReloadingItem;
import iblis.player.ExtendedFoodData;
import iblis.player.PlayerCharacteristic;
import iblis.registry.IblisItems;
import java.util.Random;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IblisMod.MOD_ID, value = Dist.CLIENT)
public final class IblisHud {
    private static final ResourceLocation VANILLA_ICONS =
            ResourceLocation.withDefaultNamespace("textures/gui/icons.png");
    private static final Random RANDOM = new Random();
    private static UUID healthPlayer;
    private static long healthUpdateCounter;
    private static int playerHealth;
    private static long lastSystemTime;
    private static int lastPlayerHealth;

    private IblisHud() {
    }

    @SubscribeEvent
    public static void overlay(RenderGuiOverlayEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui || player.isSpectator()) {
            return;
        }
        ResourceLocation overlay = event.getOverlay().id();
        GuiGraphics graphics = event.getGuiGraphics();
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();

        if (overlay.equals(VanillaGuiOverlay.CROSSHAIR.id())
                && player.getMainHandItem().getItem() instanceof FirearmItem) {
            renderAimFrame(graphics, player, width, height);
            event.setCanceled(true);
            return;
        }
        if (overlay.equals(VanillaGuiOverlay.PLAYER_HEALTH.id())
                && !ModList.get().isLoaded("rpghud")) {
            renderAmmoAndSprinting(graphics, player, width, height);
            if (IblisConfig.renderHpBar) {
                renderHealth(graphics, player, width, height);
                event.setCanceled(true);
            }
            return;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void foodOverlay(RenderGuiOverlayEvent.Pre event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.FOOD_LEVEL.id())
                || ModList.get().isLoaded("appleskin")) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui || player.isSpectator()
                || !(player.getFoodData() instanceof ExtendedFoodData)) {
            return;
        }
        int maximum = Math.max(0, Mth.floor(
                PlayerCharacteristic.GLUTTONY.getCurrentValue(player)));
        if (maximum == ExtendedFoodData.DEFAULT_MAX_FOOD_LEVEL
                || !forgeGui().shouldDrawSurvivalElements()) {
            return;
        }
        renderFood(event.getGuiGraphics(), player,
                event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight(), maximum);
        event.setCanceled(true);
    }

    private static void renderAimFrame(GuiGraphics graphics, Player player,
                                       int width, int height) {
        int centerX = width / 2 + 1;
        int centerY = height / 2 + 1;
        double divider = Math.max(FirearmItem.shootingAccuracy(player), 0.01);
        int size = Math.min((int) (2.0 * height / divider), height - 4);
        int half = size / 2;
        int third = size / 3;
        int colour = 0x44FF9600;
        graphics.fill(centerX - half, centerY - half, centerX - third, centerY - half + 1, colour);
        graphics.fill(centerX + third, centerY - half, centerX + half, centerY - half + 1, colour);
        graphics.fill(centerX - half, centerY + half, centerX - third, centerY + half + 1, colour);
        graphics.fill(centerX + third, centerY + half, centerX + half, centerY + half + 1, colour);
        graphics.fill(centerX - half, centerY - half, centerX - half + 1, centerY - third, colour);
        graphics.fill(centerX - half, centerY + third, centerX - half + 1, centerY + half, colour);
        graphics.fill(centerX + half, centerY - half, centerX + half + 1, centerY - third, colour);
        graphics.fill(centerX + half, centerY + third, centerX + half + 1, centerY + half, colour);
    }

    private static void renderAmmoAndSprinting(GuiGraphics graphics, Player player,
                                                int width, int height) {
        ItemStack held = player.getMainHandItem();
        if (held.hasTag() && (held.is(IblisItems.SHOTGUN.get())
                || held.is(IblisItems.SHOTGUN_RELOADING.get()))) {
            ListTag ammo = held.getTag().getList(FirearmItem.AMMO, Tag.TAG_COMPOUND);
            int right = width / 2 + 91;
            int top = height - forgeGui().leftHeight;
            for (int i = 0; i < ShotgunReloadingItem.MAX_AMMO; i++) {
                int type = i < ammo.size()
                        ? ammo.getCompound(i).getInt(FirearmItem.AMMO_TYPE) : -1;
                graphics.blit(IblisScreenEvents.ICONS, right - 7 - i * 7, top - 27,
                        7 * (type + 1), 0, 7, 16);
            }
        }
        for (int i = 0; i < IblisClientEvents.sprintCounter() / 4; i++) {
            graphics.blit(IblisScreenEvents.ICONS, width - 9,
                    height - 9 * i, 247, 0, 9, 9);
        }
        for (int i = 0; i < IblisClientEvents.sprintButtonCounter() / 4; i++) {
            graphics.blit(IblisScreenEvents.ICONS, width - 18,
                    height - 9 * i, 238, 0, 9, 9);
        }
    }

    private static void renderHealth(GuiGraphics graphics, Player player,
                                     int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        ForgeGui gui = forgeGui();
        int top = height - gui.leftHeight;
        int left = width / 2 - 91;
        int health = Mth.ceil(player.getHealth());
        int updateCounter = minecraft.gui.getGuiTicks();

        if (!player.getUUID().equals(healthPlayer)) {
            healthPlayer = player.getUUID();
            playerHealth = health;
            lastPlayerHealth = health;
            lastSystemTime = Util.getMillis();
            healthUpdateCounter = 0L;
        }

        boolean highlight = healthUpdateCounter > updateCounter
                && (healthUpdateCounter - updateCounter) / 3L % 2L == 1L;
        if (health < playerHealth && player.invulnerableTime > 0) {
            lastSystemTime = Util.getMillis();
            healthUpdateCounter = updateCounter + 20L;
        } else if (health > playerHealth && player.invulnerableTime > 0) {
            lastSystemTime = Util.getMillis();
            healthUpdateCounter = updateCounter + 10L;
        }
        if (Util.getMillis() - lastSystemTime > 1000L) {
            playerHealth = health;
            lastPlayerHealth = health;
            lastSystemTime = Util.getMillis();
        }
        playerHealth = health;

        int maxHealth = Mth.ceil(player.getMaxHealth());
        int absorption = Mth.ceil(player.getAbsorptionAmount());
        int healthRows = Mth.ceil((maxHealth + absorption) / 20.0F);
        gui.leftHeight += healthRows + 9;
        RANDOM.setSeed(updateCounter * 312871L);

        int regeneration = player.hasEffect(MobEffects.REGENERATION)
                ? updateCounter % 25 : -1;
        int textureTop = player.level().getLevelData().isHardcore() ? 45 : 0;
        int background = highlight ? 25 : 16;
        int margin = 16;
        if (player.hasEffect(MobEffects.POISON)) {
            margin += 36;
        } else if (player.hasEffect(MobEffects.WITHER)) {
            margin += 72;
        }

        int absorptionRemaining = absorption;
        int firstIcon = Math.max(0, (Mth.floor((health - 1.0F) / 2.0F) / 10) * 10);
        int lastIcon = Math.min(firstIcon + 10,
                Mth.ceil((maxHealth + absorption) / 2.0F));
        if (firstIcon > 0) {
            String hint = "+" + firstIcon;
            graphics.drawString(minecraft.font, hint,
                    left + 88 - minecraft.font.width(hint), top + 1,
                    0xFFBB0000, true);
            top -= 3;
        }

        for (int i = firstIcon; i < lastIcon; i++) {
            int x = left + (i % 10) * 8;
            int y = top + (health <= 4 ? RANDOM.nextInt(2) : 0);
            if (i == regeneration) {
                y -= 2;
            }
            int backgroundWidth = i * 2 + 2 > maxHealth + absorption ? 5 : 9;
            graphics.blit(VANILLA_ICONS, x, y, background, textureTop,
                    backgroundWidth, 9);

            if (highlight) {
                if (i * 2 + 1 < lastPlayerHealth) {
                    graphics.blit(VANILLA_ICONS, x, y, margin + 54,
                            textureTop, 9, 9);
                } else if (i * 2 + 1 == lastPlayerHealth) {
                    graphics.blit(VANILLA_ICONS, x, y, margin + 63,
                            textureTop, 9, 9);
                }
            }

            if (i * 2 + 1 < health) {
                graphics.blit(VANILLA_ICONS, x, y, margin + 36,
                        textureTop, 9, 9);
            } else if (i * 2 + 1 == health) {
                if (absorptionRemaining > 0) {
                    graphics.blit(VANILLA_ICONS, x, y, margin + 36,
                            textureTop, 9, 9);
                    graphics.blit(VANILLA_ICONS, x, y, margin + 153,
                            textureTop, 9, 9);
                    absorptionRemaining--;
                } else {
                    graphics.blit(VANILLA_ICONS, x, y, margin + 45,
                            textureTop, 9, 9);
                }
            } else if (absorptionRemaining > 0) {
                int texture = absorptionRemaining == 1 ? margin + 153 : margin + 144;
                graphics.blit(VANILLA_ICONS, x, y, texture, textureTop, 9, 9);
                absorptionRemaining -= Math.min(absorptionRemaining, 2);
            }
        }
    }

    private static void renderFood(GuiGraphics graphics, Player player,
                                   int width, int height, int maximum) {
        ForgeGui gui = forgeGui();
        int level = player.getFoodData().getFoodLevel();
        int left = width / 2 + 91;
        int top = height - gui.rightHeight;
        int firstIcon = Math.max(0, ((level - 1) / 20) * 10);
        int lastIcon = Math.min(firstIcon + 10, Mth.ceil(maximum / 2.0F));
        if (firstIcon > 0) {
            String hint = "+" + firstIcon;
            graphics.drawString(Minecraft.getInstance().font, hint,
                    left + 8 - Minecraft.getInstance().font.width(hint), top + 1,
                    0xFFBB9900, true);
            top -= 3;
        }
        RANDOM.setSeed(Minecraft.getInstance().gui.getGuiTicks() * 312871L);
        boolean hunger = player.hasEffect(MobEffects.HUNGER);
        for (int i = firstIcon; i < lastIcon; i++) {
            int value = i * 2 + 1;
            int x = left - (i % 10) * 8 - 9;
            int y = top;
            if (player.getFoodData().getSaturationLevel() <= 0.0F
                    && Minecraft.getInstance().gui.getGuiTicks() % (level * 3 + 1) == 0) {
                y += RANDOM.nextInt(3) - 1;
            }
            int background = hunger ? 133 : 16;
            int full = hunger ? 88 : 52;
            graphics.blit(VANILLA_ICONS, x, y, background, 27, 9, 9);
            if (value < level) {
                graphics.blit(VANILLA_ICONS, x, y, full, 27, 9, 9);
            } else if (value == level) {
                graphics.blit(VANILLA_ICONS, x, y, full + 9, 27, 9, 9);
            }
        }
        gui.rightHeight += 10;
    }

    private static ForgeGui forgeGui() {
        return (ForgeGui) Minecraft.getInstance().gui;
    }
}
