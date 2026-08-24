package iblis.compat;

import iblis.IblisMod;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Supplier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;

/** Loads optional integrations without linking their classes when the target mod is absent. */
public final class CompatBootstrap {
    private static final List<OptionalModule> MODULES = List.of(
            new OptionalModule("tconstruct", () -> load(
                    "iblis.compat.tconstruct.TinkersCompatModule")),
            new OptionalModule("tacz", () -> load(
                    "iblis.compat.tacz.TaczCompatModule"))
    );

    private CompatBootstrap() {
    }

    public static void register(IEventBus modBus) {
        MODULES.stream()
                .filter(module -> ModList.get().isLoaded(module.modId()))
                .forEach(module -> module.install(modBus));
    }

    private static CompatModule load(String className) {
        try {
            return (CompatModule) Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException
                 | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Could not create compatibility module " + className,
                    exception);
        }
    }

    private record OptionalModule(String modId, Supplier<CompatModule> factory) {
        private void install(IEventBus modBus) {
            try {
                factory.get().register(modBus);
                IblisMod.LOGGER.info("Enabled {} compatibility", modId);
            } catch (RuntimeException | LinkageError error) {
                IblisMod.LOGGER.error("Disabled broken {} compatibility; Iblis will continue without it",
                        modId, error);
            }
        }
    }
}
