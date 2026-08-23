package iblis_headshots.network;

import iblis_headshots.IblisHeadshotsMod;
import iblis_headshots.network.packet.HeadshotParticlePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class HeadshotsNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(IblisHeadshotsMod.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private HeadshotsNetwork() {
    }

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> CHANNEL
                .messageBuilder(HeadshotParticlePacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(HeadshotParticlePacket::encode)
                .decoder(HeadshotParticlePacket::decode)
                .consumerMainThread(HeadshotParticlePacket::handle)
                .add());
    }

    public static void spawnParticle(ServerLevel level, Vec3 position, Vec3 speed, int lifetime) {
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        position.x, position.y, position.z, 64.0, level.dimension())),
                new HeadshotParticlePacket(position, speed, lifetime));
    }
}
