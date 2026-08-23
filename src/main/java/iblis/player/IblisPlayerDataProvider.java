package iblis.player;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class IblisPlayerDataProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<IblisPlayerData> CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() { });

    private final IblisPlayerData data = new IblisPlayerData();
    private LazyOptional<IblisPlayerData> optional = LazyOptional.of(() -> data);

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(
            @NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability != CAPABILITY) {
            return LazyOptional.empty();
        }
        // Player death invalidates the old entity before PlayerEvent.Clone.
        // Entity.reviveCaps() re-enables its dispatcher, so recreate this
        // provider's invalidated handle while retaining the same backing data.
        if (!optional.isPresent()) {
            optional = LazyOptional.of(() -> data);
        }
        return optional.cast();
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.save();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        data.load(tag);
    }

    public void invalidate() {
        optional.invalidate();
    }
}
