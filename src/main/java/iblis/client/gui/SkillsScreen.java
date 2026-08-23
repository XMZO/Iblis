package iblis.client.gui;

import iblis.player.PlayerSkill;
import iblis.registry.IblisAttributes;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;

public final class SkillsScreen extends Screen {
    private static final int PANEL_WIDTH = 122;
    private static final int ROW_HEIGHT = 16;
    private static final List<PlayerSkill> MARTIAL = List.of(
            PlayerSkill.BOXING, PlayerSkill.SWORDSMANSHIP, PlayerSkill.PARRY,
            PlayerSkill.ARCHERY, PlayerSkill.THROWING, PlayerSkill.SHARPSHOOTING);
    private static final List<PlayerSkill> CRAFT = List.of(
            PlayerSkill.ARMORSMITH, PlayerSkill.WEAPONSMITH, PlayerSkill.MECHANICS,
            PlayerSkill.MEDICAL_AID, PlayerSkill.DIGGING, PlayerSkill.CHEMISTRY);
    private static final List<PlayerSkill> ACROBATICS = List.of(
            PlayerSkill.RUNNING, PlayerSkill.JUMPING, PlayerSkill.FALLING,
            PlayerSkill.EQUILIBRIUM);

    private final Screen parent;

    public SkillsScreen(Screen parent) {
        super(Component.translatable("iblis.screen.skills"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.back"),
                        ignored -> onClose())
                .bounds(width / 2 - 45, height - 24, 90, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            int wisdomX = width / 2 - PANEL_WIDTH / 2;
            drawAttribute(graphics, player, IblisAttributes.WISDOM.get(), wisdomX, 24);

            int available = Math.max(width - 24, PANEL_WIDTH * 3);
            int gap = Math.max(8, (available - PANEL_WIDTH * 3) / 2);
            int startX = (width - (PANEL_WIDTH * 3 + gap * 2)) / 2;
            int categoryY = 52;
            drawBranch(graphics, player, IblisAttributes.MARTIAL_ARTS.get(),
                    MARTIAL, startX, categoryY, wisdomX + PANEL_WIDTH / 2);
            drawBranch(graphics, player, IblisAttributes.CRAFTMANSHIP.get(),
                    CRAFT, startX + PANEL_WIDTH + gap, categoryY,
                    wisdomX + PANEL_WIDTH / 2);
            drawBranch(graphics, player, IblisAttributes.ACROBATICS.get(),
                    ACROBATICS, startX + (PANEL_WIDTH + gap) * 2, categoryY,
                    wisdomX + PANEL_WIDTH / 2);
        }
        graphics.drawCenteredString(font, title, width / 2, 7, 0xFFFFAA33);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawBranch(GuiGraphics graphics, Player player, Attribute category,
                            List<PlayerSkill> skills, int x, int y, int rootX) {
        int center = x + PANEL_WIDTH / 2;
        graphics.hLine(Math.min(rootX, center), Math.max(rootX, center), y - 8, 0xFFB36900);
        graphics.vLine(center, y - 8, y - 1, 0xFFB36900);
        drawAttribute(graphics, player, category, x, y);
        int childY = y + 22;
        for (PlayerSkill skill : skills) {
            if (!skill.enabled) {
                continue;
            }
            graphics.vLine(center, childY - 6, childY - 1, 0xFFB36900);
            drawSkill(graphics, player, skill, x, childY);
            childY += ROW_HEIGHT;
        }
    }

    private void drawAttribute(GuiGraphics graphics, Player player, Attribute attribute,
                               int x, int y) {
        double value = player.getAttributeValue(attribute);
        drawPanel(graphics, x, y, Component.translatable(attribute.getDescriptionId()).getString(),
                value, 0xEE7E3300);
    }

    private void drawSkill(GuiGraphics graphics, Player player, PlayerSkill skill,
                           int x, int y) {
        drawPanel(graphics, x, y,
                Component.translatable(skill.getAttribute().getDescriptionId()).getString(),
                player.getAttributeValue(skill.getAttribute()), 0xCC3F2600);
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, String name,
                           double value, int background) {
        graphics.fill(x, y, x + PANEL_WIDTH, y + 13, background);
        graphics.renderOutline(x, y, PANEL_WIDTH, 13, 0xFFFF9A25);
        String text = String.format(Locale.ROOT, "%s: %.1f", name, value);
        if (font.width(text) > PANEL_WIDTH - 6) {
            text = font.plainSubstrByWidth(text, PANEL_WIDTH - 8);
        }
        graphics.drawCenteredString(font, text, x + PANEL_WIDTH / 2, y + 2, 0xFFFFD19A);
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
