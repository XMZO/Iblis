package iblis.registry;

import iblis.IblisMod;
import iblis.item.GuideBookItem;
import iblis.player.PlayerSkill;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class IblisCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, IblisMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> IBLIS = TABS.register("iblis", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.iblis.tab"))
                    .icon(() -> GuideBookItem.createGuide(
                            IblisItems.GUIDE.get(), PlayerSkill.ARMORSMITH, 0.7))
                    .displayItems((parameters, output) -> {
                        output.accept(IblisItems.IRON_COAL.get());
                        output.accept(IblisItems.IRONORE_COAL.get());
                        output.accept(IblisItems.SLAG.get());
                        output.accept(GuideBookItem.createGuide(
                                IblisItems.GUIDE.get(), PlayerSkill.ARMORSMITH, 0.7));
                        output.accept(GuideBookItem.createGuide(
                                IblisItems.GUIDE.get(), PlayerSkill.CHEMISTRY, 0.7));
                        ItemStack diary = GuideBookItem.createDiary(IblisItems.GUIDE.get(), "Foghrye4");
                        diary.getOrCreateTag().putInt(GuideBookItem.BOOK_ID, 1);
                        output.accept(diary);
                        output.accept(IblisItems.INGOT_STEEL.get());
                        output.accept(IblisItems.INGOT_BRONZE.get());
                        output.accept(IblisItems.NUGGET_STEEL.get());
                        output.accept(IblisItems.TRIGGER_SPRING.get());
                        output.accept(IblisItems.RAISIN.get());
                        output.accept(IblisItems.NONSTERILE_MEDKIT.get());
                        output.accept(IblisItems.MEDKIT.get());
                        output.accept(IblisItems.SHOTGUN_BULLET.get());
                        output.accept(IblisItems.SHOTGUN_SHOT.get());
                        output.accept(IblisItems.CROSSBOW_BOLT.get());
                        output.accept(IblisItems.SHOTGUN.get());
                        output.accept(IblisItems.CROSSBOW.get());
                        output.accept(IblisItems.HEAVY_SHIELD.get());
                        output.accept(IblisItems.BOULDER.get());
                        output.accept(IblisItems.IRON_THROWING_KNIFE.get());
                        output.accept(IblisItems.STEEL_HELMET.get());
                        output.accept(IblisItems.STEEL_CHESTPLATE.get());
                        output.accept(IblisItems.STEEL_LEGGINS.get());
                        output.accept(IblisItems.STEEL_BOOTS.get());
                    })
                    .build());

    private IblisCreativeTabs() {
    }

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }
}
