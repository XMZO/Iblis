package iblis.client.gui;

import iblis.network.IblisNetwork;
import iblis.player.PlayerCharacteristic;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class CharacteristicsScreen extends Screen {
    private static final int ROW_HEIGHT = 21;
    private final Screen parent;
    private final Map<PlayerCharacteristic, Button> raiseButtons =
            new EnumMap<>(PlayerCharacteristic.class);
    private int left;
    private int top;

    public CharacteristicsScreen(Screen parent) {
        super(Component.translatable("iblis.screen.characteristics"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        raiseButtons.clear();
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        int rows = (int) java.util.Arrays.stream(PlayerCharacteristic.values())
                .filter(value -> value.enabled).count();
        left = width / 2 - 155;
        top = Math.max(28, (height - rows * ROW_HEIGHT) / 2);
        int row = 0;
        for (PlayerCharacteristic characteristic : PlayerCharacteristic.values()) {
            if (!characteristic.enabled) {
                continue;
            }
            Button button = Button.builder(Component.literal("+"), ignored -> {
                        IblisNetwork.sendCharacteristicRaise(characteristic.ordinal());
                        ignored.active = false;
                    })
                    .bounds(left, top + row * ROW_HEIGHT, 20, 18)
                    .build();
            button.active = characteristic.canRaise(player);
            raiseButtons.put(characteristic, addRenderableWidget(button));
            row++;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.back"),
                        ignored -> onClose())
                .bounds(width / 2 - 45, Math.min(height - 24, top + rows * ROW_HEIGHT + 8),
                        90, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            int row = 0;
            for (PlayerCharacteristic characteristic : PlayerCharacteristic.values()) {
                if (!characteristic.enabled) {
                    continue;
                }
                Button button = raiseButtons.get(characteristic);
                if (button != null) {
                    button.active = characteristic.canRaise(player);
                }
                String name = Component.translatable(
                        characteristic.getAttribute().getDescriptionId()).getString();
                double displayedValue = characteristic == PlayerCharacteristic.MELEE_DAMAGE_BONUS
                        ? characteristic.getEffectiveBonus(player)
                        : characteristic.getCurrentValue(player);
                String value = String.format(Locale.ROOT, "%s  Lv.%d  %.1f  [%d XP]",
                        name, characteristic.getCurrentLevel(player),
                        displayedValue,
                        characteristic.getRequiredExperienceLevels(player));
                graphics.drawString(font, value, left + 27,
                        top + row * ROW_HEIGHT + 5,
                        characteristic.canRaise(player) ? 0xFFFFAA33 : 0xFFC0C0C0, true);
                row++;
            }
        }
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFAA33);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
