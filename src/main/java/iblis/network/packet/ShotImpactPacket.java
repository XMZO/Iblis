package iblis.network.packet;

import iblis.client.network.ClientPacketHandler;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ShotImpactPacket(Vec3 position, Direction face, int ammunitionType,
                               float splashCone, float distance, Vec3 bloodPosition,
                               Direction bloodFace, int bloodColour) {
    public static void encode(ShotImpactPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.position.x);
        buffer.writeDouble(packet.position.y);
        buffer.writeDouble(packet.position.z);
        buffer.writeEnum(packet.face);
        buffer.writeVarInt(packet.ammunitionType);
        buffer.writeFloat(packet.splashCone);
        buffer.writeFloat(packet.distance);
        boolean hasBlood = packet.bloodPosition != null && packet.bloodFace != null
                && packet.bloodColour >= 0;
        buffer.writeBoolean(hasBlood);
        if (hasBlood) {
            buffer.writeDouble(packet.bloodPosition.x);
            buffer.writeDouble(packet.bloodPosition.y);
            buffer.writeDouble(packet.bloodPosition.z);
            buffer.writeEnum(packet.bloodFace);
            buffer.writeInt(packet.bloodColour);
        }
    }

    public static ShotImpactPacket decode(FriendlyByteBuf buffer) {
        Vec3 position = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        Direction face = buffer.readEnum(Direction.class);
        int ammunitionType = buffer.readVarInt();
        float splashCone = buffer.readFloat();
        float distance = buffer.readFloat();
        if (!buffer.readBoolean()) {
            return new ShotImpactPacket(position, face, ammunitionType, splashCone,
                    distance, null, null, -1);
        }
        Vec3 bloodPosition = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        Direction bloodFace = buffer.readEnum(Direction.class);
        return new ShotImpactPacket(position, face, ammunitionType, splashCone,
                distance, bloodPosition, bloodFace, buffer.readInt());
    }

    public static void handle(ShotImpactPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleShotImpact(packet));
        context.setPacketHandled(true);
    }
}
