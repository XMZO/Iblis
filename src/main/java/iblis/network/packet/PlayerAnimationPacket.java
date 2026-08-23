package iblis.network.packet;

import iblis.client.animation.ClientActionAnimations;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record PlayerAnimationPacket(int playerId, Type type, float power) {
    public enum Type {
        SHIELD_PUNCH,
        KICK
    }

    public static void encode(PlayerAnimationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.playerId);
        buffer.writeEnum(packet.type);
        buffer.writeFloat(packet.power);
    }

    public static PlayerAnimationPacket decode(FriendlyByteBuf buffer) {
        return new PlayerAnimationPacket(buffer.readVarInt(), buffer.readEnum(Type.class),
                buffer.readFloat());
    }

    public static void handle(PlayerAnimationPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientActionAnimations.handle(packet)));
        context.setPacketHandled(true);
    }
}
