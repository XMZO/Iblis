package iblis.network.packet;

import iblis.client.network.ClientPacketHandler;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server-to-client snapshot of every Iblis setting which affects gameplay. */
public record SyncGameplayConfigPacket(CompoundTag data) {
    public static void encode(SyncGameplayConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.data);
    }

    public static SyncGameplayConfigPacket decode(FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return new SyncGameplayConfigPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(
            SyncGameplayConfigPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT, () -> () -> ClientPacketHandler.handleGameplayConfig(packet.data));
        context.setPacketHandled(true);
    }
}
