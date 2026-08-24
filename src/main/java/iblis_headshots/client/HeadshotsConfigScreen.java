package iblis_headshots.client;

import iblis_headshots.IblisHeadshotsMod;
import iblis_headshots.config.EditableConfigCategory;
import iblis_headshots.config.EditableConfigValue;
import iblis_headshots.config.HeadshotsConfig;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Small standalone Forge Mod List config screen for Iblis Headshots. */
public final class HeadshotsConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private final Screen parent;
    private final EditableConfigCategory category;
    private final Map<EditableConfigValue, Object> pendingValues = new LinkedHashMap<>();
    private final List<VisibleRow> visibleRows = new ArrayList<>();
    private int page;
    private int pageSize;
    private int pageCount;

    private HeadshotsConfigScreen(Screen parent) {
        super(Component.translatable("iblis_headshots.config.title"));
        this.parent = parent;
        this.category = HeadshotsConfig.editableCategory();
        for (EditableConfigValue value : category.values()) {
            Object current = value.value().get();
            pendingValues.put(value, value.isBoolean() ? current : formatNumber((Number) current));
        }
    }

    public static Screen create(Screen parent) {
        return new HeadshotsConfigScreen(parent);
    }

    @Override
    protected void init() {
        visibleRows.clear();
        pageSize = Math.max(1, (height - 84) / ROW_HEIGHT);
        pageCount = Math.max(1, (category.values().size() + pageSize - 1) / pageSize);
        page = Mth.clamp(page, 0, pageCount - 1);

        int controlWidth = Math.min(150, Math.max(90, width / 3));
        int controlX = width / 2 + 8;
        int start = page * pageSize;
        int end = Math.min(category.values().size(), start + pageSize);
        for (int index = start; index < end; index++) {
            EditableConfigValue value = category.values().get(index);
            int y = 34 + (index - start) * ROW_HEIGHT;
            visibleRows.add(new VisibleRow(value, y, controlX));
            if (value.isBoolean()) {
                boolean current = (Boolean) pendingValues.get(value);
                addRenderableWidget(Button.builder(booleanText(current), button -> {
                            boolean changed = !(Boolean) pendingValues.get(value);
                            pendingValues.put(value, changed);
                            button.setMessage(booleanText(changed));
                        })
                        .bounds(controlX, y, controlWidth, 20)
                        .build());
            } else {
                EditBox input = new EditBox(font, controlX, y, controlWidth, 20,
                        optionLabel(value));
                input.setMaxLength(32);
                input.setFilter(HeadshotsConfigScreen::isPotentialNumber);
                input.setValue((String) pendingValues.get(value));
                input.setResponder(text -> pendingValues.put(value, text));
                addRenderableWidget(input);
            }
        }

        int bottom = height - 28;
        Button previous = addRenderableWidget(Button.builder(
                        Component.literal("<"), button -> changePage(-1))
                .bounds(8, bottom, 34, 20).build());
        previous.active = page > 0;
        Button next = addRenderableWidget(Button.builder(
                        Component.literal(">"), button -> changePage(1))
                .bounds(46, bottom, 34, 20).build());
        next.active = page + 1 < pageCount;
        addRenderableWidget(Button.builder(
                        Component.translatable("iblis_headshots.config.reset"),
                        button -> resetDefaults())
                .bounds(width - 214, bottom, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                        button -> closeAndSave())
                .bounds(width - 108, bottom, 100, 20).build());
    }

    private void changePage(int direction) {
        page = Mth.clamp(page + direction, 0, pageCount - 1);
        rebuildWidgets();
    }

    private void resetDefaults() {
        for (EditableConfigValue value : category.values()) {
            Object defaultValue = value.value().getDefault();
            pendingValues.put(value, value.isBoolean()
                    ? defaultValue : formatNumber((Number) defaultValue));
        }
        rebuildWidgets();
    }

    private void closeAndSave() {
        try {
            applyPendingValues();
            HeadshotsConfig.save();
        } catch (RuntimeException exception) {
            IblisHeadshotsMod.LOGGER.error(
                    "Failed to save Iblis Headshots config; keeping the client running",
                    exception);
        } finally {
            minecraft.setScreen(parent);
        }
    }

    private void applyPendingValues() {
        for (EditableConfigValue value : category.values()) {
            Object pending = pendingValues.get(value);
            if (value.isBoolean()) {
                value.set(pending);
                continue;
            }
            try {
                double parsed = Double.parseDouble((String) pending);
                if (Double.isFinite(parsed)) {
                    double clamped = Mth.clamp(parsed, value.minimum(), value.maximum());
                    value.set(value.isInteger() ? (int) Math.round(clamped) : clamped);
                }
            } catch (NumberFormatException ignored) {
                // Keep the last valid value when an unfinished edit is closed.
            }
        }
    }

    @Override
    public void onClose() {
        closeAndSave();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        graphics.drawCenteredString(font,
                Component.translatable("iblis_headshots.config.page", page + 1, pageCount),
                121, height - 22, 0xA0A0A0);

        for (VisibleRow row : visibleRows) {
            Component label = optionLabel(row.value());
            graphics.drawString(font, label,
                    row.controlX() - 8 - font.width(label), row.y() + 6, 0xFFFFFF);
            if (mouseY >= row.y() && mouseY < row.y() + 20) {
                String comment = category.spec().getLevelComment(row.value().value().getPath());
                if (comment != null && !comment.isBlank()) {
                    setTooltipForNextRenderPass(optionTooltip(row.value(), comment));
                }
            }
        }
    }

    private static Component booleanText(boolean value) {
        return Component.translatable(value ? "options.on" : "options.off");
    }

    private static boolean isPotentialNumber(String value) {
        return value.isEmpty() || value.equals("-")
                || value.matches("-?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d*)?");
    }

    private static String formatNumber(Number value) {
        if (value instanceof Integer || value instanceof Long) {
            return value.toString();
        }
        double number = value.doubleValue();
        return number == Math.rint(number)
                ? String.format(Locale.ROOT, "%.1f", number) : Double.toString(number);
    }

    private static Component optionLabel(EditableConfigValue value) {
        String key = "iblis_headshots.config.option." + value.key();
        return I18n.exists(key) ? Component.translatable(key) : Component.literal(prettyName(value.key()));
    }

    private static Component optionTooltip(EditableConfigValue value, String fallback) {
        String path = String.join(".", value.value().getPath());
        String key = "iblis_headshots.config.tooltip." + path;
        return I18n.exists(key) ? Component.translatable(key) : Component.literal(fallback);
    }

    private static String prettyName(String key) {
        StringBuilder result = new StringBuilder();
        for (String word : key.split("_")) {
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private record VisibleRow(EditableConfigValue value, int y, int controlX) {
    }
}
