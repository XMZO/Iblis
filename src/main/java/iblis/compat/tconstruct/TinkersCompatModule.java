package iblis.compat.tconstruct;

import iblis.IblisMod;
import iblis.compat.CompatModule;
import java.util.List;
import java.util.function.Consumer;
import net.minecraftforge.eventbus.api.IEventBus;

/** Optional Tinkers' Construct 3 integration; each installer is independently removable. */
public final class TinkersCompatModule implements CompatModule {
    private static final List<Feature> FEATURES = List.of(
            new Feature("crafting", bus -> {
                TinkersCraftsmanshipModifier.register(bus);
                TinkersCraftingCompat.register();
            }),
            new Feature("ranged", bus -> TinkersRangedCompat.register())
    );

    @Override
    public void register(IEventBus modBus) {
        FEATURES.forEach(feature -> feature.install(modBus));
    }

    private record Feature(String name, Consumer<IEventBus> installer) {
        private void install(IEventBus modBus) {
            try {
                installer.accept(modBus);
            } catch (RuntimeException | LinkageError error) {
                IblisMod.LOGGER.error("Disabled Tinkers' Construct {} compatibility",
                        name, error);
            }
        }
    }
}
