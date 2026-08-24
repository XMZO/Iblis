package iblis.compat.tacz;

import iblis.IblisMod;
import iblis.compat.CompatModule;
import java.util.List;
import java.util.function.Consumer;
import net.minecraftforge.eventbus.api.IEventBus;

/** Optional TACZ integration; features remain independent and removable. */
public final class TaczCompatModule implements CompatModule {
    private static final List<Feature> FEATURES = List.of(
            new Feature("sharpshooting experience", ignored -> TaczExperienceCompat.register()),
            new Feature("headshots", ignored -> TaczHeadshotCompat.register())
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
                IblisMod.LOGGER.error("Disabled TACZ {} compatibility", name, error);
            }
        }
    }
}
