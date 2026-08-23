package iblis.network.packet;

import iblis.item.CustomLeftClickItem;
import iblis.item.FirearmItem;
import iblis.crafting.IblisCraftingEvents;
import iblis.player.PlayerCharacteristic;
import iblis.player.PlayerSkill;
import iblis.player.PlayerDataAccess;
import iblis.player.PlayerAttributeEffects;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraftforge.network.NetworkEvent;

public record PlayerActionPacket(Action action, InteractionHand hand, int argument) {
    public enum Action {
        RELOAD,
        LEFT_CLICK,
        SHIELD_PUNCH,
        KICK,
        RAISE_CHARACTERISTIC,
        TRAIN_TO_CRAFT,
        SPRINT_STATE,
        SPRINT_BUTTON_STATE
    }

    public static void encode(PlayerActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.action);
        buffer.writeEnum(packet.hand);
        buffer.writeVarInt(packet.argument);
    }

    public static PlayerActionPacket decode(FriendlyByteBuf buffer) {
        return new PlayerActionPacket(buffer.readEnum(Action.class),
                buffer.readEnum(InteractionHand.class), buffer.readVarInt());
    }

    public static void handle(PlayerActionPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            context.enqueueWork(() -> handleOnServer(packet, player));
        }
        context.setPacketHandled(true);
    }

    private static void handleOnServer(PlayerActionPacket packet, ServerPlayer player) {
        switch (packet.action) {
            case RELOAD -> {
                ItemStack stack = player.getMainHandItem();
                if (stack.getItem() instanceof FirearmItem firearm) {
                    PlayerDataAccess.get(player).setReloadTick(0);
                    player.setItemInHand(InteractionHand.MAIN_HAND, firearm.toReloading(stack));
                    firearm.playReloadingSound(player);
                }
            }
            case LEFT_CLICK -> {
                ItemStack stack = player.getItemInHand(packet.hand);
                if (stack.getItem() instanceof CustomLeftClickItem item) {
                    item.onLeftClick(player.serverLevel(), player, packet.hand);
                }
            }
            case SHIELD_PUNCH -> {
                if (player.onGround() && player.isBlocking()) {
                    PlayerDataAccess.get(player).setKnockState(1);
                }
            }
            case KICK -> {
                if (player.onGround() && !player.isUsingItem()) {
                    PlayerDataAccess.get(player).setKnockState(2);
                }
            }
            case RAISE_CHARACTERISTIC -> {
                PlayerCharacteristic[] values = PlayerCharacteristic.values();
                if (packet.argument >= 0 && packet.argument < values.length) {
                    values[packet.argument].raise(player);
                }
            }
            case TRAIN_TO_CRAFT -> trainAtWorkbench(player);
            case SPRINT_STATE -> updateSprintState(player, packet.argument);
            case SPRINT_BUTTON_STATE -> PlayerDataAccess.get(player)
                    .setSprintButtonCounter(net.minecraft.util.Mth.clamp(packet.argument, 0, 32));
        }
    }

    private static void updateSprintState(ServerPlayer player, int rawCounter) {
        int counter = net.minecraft.util.Mth.clamp(rawCounter, 0, 32);
        var data = PlayerDataAccess.get(player);
        int previous = data.sprintCounter();
        if (previous == 0 && counter > 0) {
            data.setSprintStart(player.position());
        } else if (previous == 32 && counter == 0 && data.sprintStart() != null) {
            PlayerSkill.RUNNING.raise(player,
                    player.position().distanceTo(data.sprintStart()));
        }
        if (counter == 0) {
            data.setSprintStart(null);
        }
        data.setSprintCounter(counter);
        PlayerAttributeEffects.refreshSprintingSpeed(player, counter);
    }

    private static void trainAtWorkbench(ServerPlayer player) {
        if (!(player.containerMenu instanceof CraftingMenu menu)
                || !(menu.getSlot(menu.getResultSlotIndex()) instanceof ResultSlot resultSlot)) {
            return;
        }
        ItemStack result = resultSlot.getItem();
        IblisCraftingEvents.CraftingProfile profile = IblisCraftingEvents.profileFor(result);
        if (result.isEmpty() || profile == null || !profile.skill().enabled) {
            return;
        }

        // Calling ResultSlot#onTake directly consumes one recipe worth of
        // ingredients (including container remainders), but since nothing was
        // removed from the result slot it does not award a crafted item/event.
        resultSlot.onTake(player, result.copy());
        // Keep the two legacy progression steps separate: the second step uses
        // the skill value produced by the normal recipe-training step.
        profile.skill().raise(player, profile.experience());
        profile.skill().raise(player, 2.0);
        menu.broadcastChanges();
    }
}
