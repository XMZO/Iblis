package iblis.network.packet;

import iblis.client.network.ClientPacketHandler;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record SyncPlayerDataPacket(CompoundTag data) {
    public static void encode(SyncPlayerDataPacket packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.data);
    }

    public static SyncPlayerDataPacket decode(FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return new SyncPlayerDataPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(SyncPlayerDataPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT, () -> () -> ClientPacketHandler.handlePlayerData(packet.data));
        context.setPacketHandled(true);
    }
}
