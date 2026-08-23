package iblis.client.gui;

import iblis.item.FirearmItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.IItemDecorator;

/** Draws vanilla-style cooldown shading from each firearm stack's own timer. */
public final class FirearmCooldownDecorator implements IItemDecorator {
    public static final FirearmCooldownDecorator INSTANCE = new FirearmCooldownDecorator();

    private FirearmCooldownDecorator() {
    }

    @Override
    public boolean render(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return false;
        }
        float cooldown = FirearmItem.fireCooldownPercent(
                stack, minecraft.level, minecraft.getFrameTime());
        if (cooldown <= 0.0F) {
            return false;
        }

        int top = y + Mth.floor(16.0F * (1.0F - cooldown));
        int bottom = top + Mth.ceil(16.0F * cooldown);
        graphics.fill(RenderType.guiOverlay(), x, top, x + 16, bottom, Integer.MAX_VALUE);
        return false;
    }
}
