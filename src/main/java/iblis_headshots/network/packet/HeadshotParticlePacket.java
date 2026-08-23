package iblis_headshots.network.packet;

import iblis_headshots.client.HeadshotsClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record HeadshotParticlePacket(Vec3 position, Vec3 speed, int lifetime) {
    public static void encode(HeadshotParticlePacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.position.x);
        buffer.writeDouble(packet.position.y);
        buffer.writeDouble(packet.position.z);
        buffer.writeDouble(packet.speed.x);
        buffer.writeDouble(packet.speed.y);
        buffer.writeDouble(packet.speed.z);
        buffer.writeVarInt(packet.lifetime);
    }

    public static HeadshotParticlePacket decode(FriendlyByteBuf buffer) {
        Vec3 position = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        Vec3 speed = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        return new HeadshotParticlePacket(position, speed, buffer.readVarInt());
    }

    public static void handle(HeadshotParticlePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> HeadshotsClient.spawnParticle(packet));
        context.setPacketHandled(true);
    }
}
