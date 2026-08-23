package iblis.network.packet;

import iblis.client.network.ClientPacketHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ResetUsePacket() {
    public static void encode(ResetUsePacket packet, FriendlyByteBuf buffer) {
    }

    public static ResetUsePacket decode(FriendlyByteBuf buffer) {
        return new ResetUsePacket();
    }

    public static void handle(ResetUsePacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.resetUseAndAttackCooldown());
        context.setPacketHandled(true);
    }
}
