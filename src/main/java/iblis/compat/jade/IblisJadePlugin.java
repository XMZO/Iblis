package iblis.compat.jade;

import iblis.IblisMod;
import iblis.block.IronCoalBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.ui.IProgressStyle;

@WailaPlugin("jade")
public final class IblisJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(
                SteelMixtureProvider.INSTANCE, IronCoalBlock.class);
    }

    private enum SteelMixtureProvider implements IBlockComponentProvider {
        INSTANCE;

        private static final ResourceLocation UID = new ResourceLocation(
                IblisMod.MOD_ID, "steel_mixture_progress");

        @Override
        public void appendTooltip(
                ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            BlockState state = accessor.getBlockState();
            if (!state.hasProperty(IronCoalBlock.AGE)) {
                return;
            }

            int age = state.getValue(IronCoalBlock.AGE);
            float progress = age / (float) IronCoalBlock.MAX_AGE;
            int percent = Math.round(progress * 100.0F);
            boolean ignited = IronCoalBlock.isIgnited(
                    accessor.getLevel(), accessor.getPosition());

            String translation = ignited ? "iblis.jade.steel_mixture.firing"
                    : age == 0 ? "iblis.jade.steel_mixture.waiting"
                    : "iblis.jade.steel_mixture.paused";
            Component text = age == 0 && !ignited
                    ? Component.translatable(translation)
                    : Component.translatable(translation, percent);
            IElementHelper elements = tooltip.getElementHelper();
            IProgressStyle style = elements.progressStyle()
                    .color(
                            ignited ? 0xFFFF8A00 : 0xFF777777,
                            ignited ? 0xFFFFB347 : 0xFF999999)
                    .textColor(0xFFFFFFFF);
            tooltip.add(elements.progress(progress, text, style, BoxStyle.DEFAULT, false)
                    .tag(UID));
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}
