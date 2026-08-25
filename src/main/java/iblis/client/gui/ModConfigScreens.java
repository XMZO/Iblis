package iblis.client.gui;

import iblis.IblisMod;
import iblis.config.EditableConfigCategory;
import iblis.config.EditableConfigValue;
import iblis.config.IblisConfig;
import iblis_headshots.config.HeadshotsConfig;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Forge Mod List config screens replacing the legacy 1.12 GuiConfig factories. */
public final class ModConfigScreens {
    private ModConfigScreens() {
    }

    public static Screen iblis(Screen parent) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean remoteServer = minecraft.getConnection() != null
                && minecraft.getSingleplayerServer() == null;
        return new CategoryScreen(
                parent,
                Component.translatable("iblis.config.title"),
                remoteServer
                        ? IblisConfig.clientEditableCategories()
                        : IblisConfig.editableCategories(),
                IblisConfig::save);
    }

    public static Screen headshots(Screen parent) {
        EditableConfigCategory category = HeadshotsConfig.editableCategory();
        return new ValuesScreen(
                parent,
                Component.translatable("iblis_headshots.config.title"),
                category,
                HeadshotsConfig::save);
    }

    private static final class CategoryScreen extends Screen {
        private final Screen parent;
        private final List<EditableConfigCategory> categories;
        private final Runnable saveAction;

        private CategoryScreen(Screen parent, Component title,
                               List<EditableConfigCategory> categories,
                               Runnable saveAction) {
            super(title);
            this.parent = parent;
            this.categories = categories;
            this.saveAction = saveAction;
        }

        @Override
        protected void init() {
            int buttonWidth = Math.min(190, Math.max(100, (width - 36) / 2));
            int left = width / 2 - buttonWidth - 3;
            int top = Math.max(34, (height - 44 - ((categories.size() + 1) / 2) * 24) / 2);
            for (int index = 0; index < categories.size(); index++) {
                EditableConfigCategory category = categories.get(index);
                int x = left + (index & 1) * (buttonWidth + 6);
                int y = top + (index / 2) * 24;
                addRenderableWidget(Button.builder(
                                Component.translatable(category.titleKey()),
                                button -> minecraft.setScreen(new ValuesScreen(
                                        this,
                                        Component.translatable(category.titleKey()),
                                        category,
                                        saveAction)))
                        .bounds(x, y, buttonWidth, 20)
                        .build());
            }
            addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                            button -> onClose())
                    .bounds(width / 2 - 100, height - 28, 200, 20)
                    .build());
        }

        @Override
        public void onClose() {
            minecraft.setScreen(parent);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics);
            super.render(graphics, mouseX, mouseY, partialTick);
            graphics.drawCenteredString(font, title, width / 2, 14, 0xFFFFFF);
        }
    }

    private static final class ValuesScreen extends Screen {
        private static final int ROW_HEIGHT = 24;
        private final Screen parent;
        private final EditableConfigCategory category;
        private final Runnable saveAction;
        private final Map<EditableConfigValue, Object> pendingValues = new LinkedHashMap<>();
        private final List<VisibleRow> visibleRows = new ArrayList<>();
        private int page;
        private int pageSize;
        private int pageCount;

        private ValuesScreen(Screen parent, Component title,
                             EditableConfigCategory category, Runnable saveAction) {
            super(title);
            this.parent = parent;
            this.category = category;
            this.saveAction = saveAction;
            for (EditableConfigValue value : category.values()) {
                Object current = value.value().get();
                pendingValues.put(value, value.isBoolean() ? current : formatNumber((Number) current));
            }
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
                    EditBox input = new EditBox(
                            font, controlX, y, controlWidth, 20,
                            optionLabel(category, value));
                    input.setMaxLength(32);
                    input.setFilter(ValuesScreen::isPotentialNumber);
                    input.setValue((String) pendingValues.get(value));
                    input.setResponder(text -> pendingValues.put(value, text));
                    addRenderableWidget(input);
                }
            }

            int bottom = height - 28;
            int resetWidth = Math.min(100, Math.max(70, width / 5));
            int doneWidth = Math.min(100, Math.max(70, width / 5));
            Button previous = addRenderableWidget(Button.builder(
                            Component.literal("<"), button -> changePage(-1))
                    .bounds(8, bottom, 34, 20).build());
            previous.active = page > 0;
            Button next = addRenderableWidget(Button.builder(
                            Component.literal(">"), button -> changePage(1))
                    .bounds(46, bottom, 34, 20).build());
            next.active = page + 1 < pageCount;
            addRenderableWidget(Button.builder(Component.translatable("iblis.config.reset"),
                            button -> resetDefaults())
                    .bounds(width - resetWidth - doneWidth - 14, bottom, resetWidth, 20)
                    .build());
            addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                            button -> closeAndSave())
                    .bounds(width - doneWidth - 8, bottom, doneWidth, 20)
                    .build());
        }

        private void changePage(int direction) {
            page = Mth.clamp(page + direction, 0, pageCount - 1);
            rebuildWidgets();
        }

        private void resetDefaults() {
            for (EditableConfigValue value : category.values()) {
                Object defaultValue = value.value().getDefault();
                pendingValues.put(value,
                        value.isBoolean() ? defaultValue : formatNumber((Number) defaultValue));
            }
            rebuildWidgets();
        }

        private void closeAndSave() {
            try {
                applyPendingValues();
                saveAction.run();
            } catch (RuntimeException exception) {
                IblisMod.LOGGER.error(
                        "Failed to save config category {}; keeping the client running",
                        category.titleKey(), exception);
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
                    if (!Double.isFinite(parsed)) {
                        continue;
                    }
                    double clamped = Mth.clamp(parsed, value.minimum(), value.maximum());
                    if (value.isInteger()) {
                        value.set((int) Math.round(clamped));
                    } else {
                        value.set(clamped);
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
                    Component.translatable("iblis.config.page", page + 1, pageCount),
                    121, height - 22, 0xA0A0A0);

            for (VisibleRow row : visibleRows) {
                Component label = optionLabel(category, row.value());
                graphics.drawString(font, label,
                        row.controlX() - 8 - font.width(label), row.y() + 6, 0xFFFFFF);
                if (mouseY >= row.y() && mouseY < row.y() + 20) {
                    String comment = category.spec().getLevelComment(row.value().value().getPath());
                    if (comment != null && !comment.isBlank()) {
                        setTooltipForNextRenderPass(optionTooltip(category, row.value(), comment));
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
            if (number == Math.rint(number)) {
                return String.format(Locale.ROOT, "%.1f", number);
            }
            return BigDecimal.valueOf(number).stripTrailingZeros().toPlainString();
        }
    }

    private static String prettyName(String key) {
        String[] words = key.split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return result.toString();
    }

    private static Component optionLabel(
            EditableConfigCategory category, EditableConfigValue value) {
        String key = translationRoot(category) + ".option." + value.key();
        return I18n.exists(key)
                ? Component.translatable(key)
                : Component.literal(prettyName(value.key()));
    }

    private static Component optionTooltip(
            EditableConfigCategory category, EditableConfigValue value, String fallback) {
        String root = translationRoot(category) + ".tooltip.";
        String path = String.join(".", value.value().getPath());
        String exactKey = root + path;
        if (I18n.exists(exactKey)) {
            return Component.translatable(exactKey);
        }

        int separator = path.indexOf('.');
        String categoryKey = root + (separator < 0 ? path : path.substring(0, separator));
        return I18n.exists(categoryKey)
                ? Component.translatable(categoryKey)
                : Component.literal(fallback);
    }

    private static String translationRoot(EditableConfigCategory category) {
        int separator = category.titleKey().indexOf('.');
        String namespace = separator < 0
                ? category.titleKey()
                : category.titleKey().substring(0, separator);
        return namespace + ".config";
    }

    private record VisibleRow(EditableConfigValue value, int y, int controlX) {
    }
}
