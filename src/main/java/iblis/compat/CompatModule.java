package iblis.compat;

import net.minecraftforge.eventbus.api.IEventBus;

/** A self-contained optional-mod integration installed during mod construction. */
@FunctionalInterface
public interface CompatModule {
    void register(IEventBus modBus);
}
