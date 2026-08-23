package iblis.item;

import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public abstract class ReloadingFirearmItem extends Item implements CustomLeftClickItem {
    protected final java.util.function.Supplier<? extends FirearmItem> firearm;

    protected ReloadingFirearmItem(Properties properties,
                                   java.util.function.Supplier<? extends FirearmItem> firearm) {
        super(properties);
        this.firearm = firearm;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(iblis.client.FirearmClientItemExtensions.reloading());
    }

    protected ItemStack findAmmo(Player player) {
        if (isAmmo(player.getOffhandItem())) {
            return player.getOffhandItem();
        }
        if (isAmmo(player.getMainHandItem())) {
            return player.getMainHandItem();
        }
        Inventory inventory = player.getInventory();
        for (ItemStack stack : inventory.items) {
            if (isAmmo(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    protected void reloadAmmo(ItemStack ammunition, Player player, CompoundTag firearmTag) {
        ListTag ammunitionList = firearmTag.getList(FirearmItem.AMMO, Tag.TAG_COMPOUND);
        ammunitionList.add(toCartridge(ammunition));
        firearmTag.put(FirearmItem.AMMO, ammunitionList);
        if (!player.getAbilities().instabuild) {
            ammunition.shrink(1);
        }
    }

    private static CompoundTag toCartridge(ItemStack ammunition) {
        CompoundTag cartridge = new CompoundTag();
        if (ammunition.getItem() instanceof AmmoItem ammo) {
            cartridge.putFloat(FirearmItem.DAMAGE, ammo.getAmmoDamage(ammunition));
            cartridge.putInt(FirearmItem.AMMO_TYPE, ammo.getAmmoType(ammunition));
        } else {
            cartridge.putFloat(FirearmItem.DAMAGE, 1.0F);
            cartridge.putInt(FirearmItem.AMMO_TYPE, 0);
        }
        return cartridge;
    }

    protected abstract boolean isAmmo(ItemStack stack);

    @Override
    public void onLeftClick(ServerLevel level, ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.setItemInHand(hand, FirearmItem.transfer(firearm.get(), stack));
        player.resetAttackStrengthTicker();
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return firearm.get().isValidRepairItem(stack, repairCandidate);
    }
}
