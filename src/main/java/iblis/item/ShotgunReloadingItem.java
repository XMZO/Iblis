package iblis.item;

import iblis.player.IblisPlayerData;
import iblis.player.PlayerDataAccess;
import iblis.registry.IblisSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ShotgunReloadingItem extends ReloadingFirearmItem {
    public static final int MAX_AMMO = 6;
    private static final int RELOAD_TICKS_PER_SHELL = 18;
    private static final int RELOAD_FINISH_TICKS = 8;

    public ShotgunReloadingItem(Properties properties,
                                java.util.function.Supplier<? extends FirearmItem> firearm) {
        super(properties, firearm);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack loaded = FirearmItem.transfer(firearm.get(), player.getItemInHand(hand));
        player.resetAttackStrengthTicker();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                IblisSounds.SHOTGUN_HAMMER_COCK.get(), SoundSource.PLAYERS,
                1.0F, level.random.nextFloat() * 0.2F + 0.8F);
        return InteractionResultHolder.success(loaded);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(entity instanceof Player player) || stack != player.getMainHandItem() || !selected) {
            return;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return;
        }
        int loaded = tag.getList(FirearmItem.AMMO, Tag.TAG_COMPOUND).size();
        if (loaded < MAX_AMMO) {
            player.resetAttackStrengthTicker();
            if (!level.isClientSide) {
                ItemStack ammunition = findAmmo(player);
                if (!ammunition.isEmpty()) {
                    IblisPlayerData data = PlayerDataAccess.get(player);
                    int reloadTick = data.reloadTick();
                    if (++reloadTick >= RELOAD_TICKS_PER_SHELL) {
                        reloadAmmo(ammunition, player, tag);
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                IblisSounds.SHOTGUN_AMMO_LOADING.get(), SoundSource.PLAYERS,
                                1.0F, 1.0F);
                        reloadTick = 0;
                    }
                    data.setReloadTick(reloadTick);
                }
            }
        } else if (!level.isClientSide) {
            IblisPlayerData data = PlayerDataAccess.get(player);
            int reloadTick = data.reloadTick() + 1;
            if (reloadTick >= RELOAD_FINISH_TICKS) {
                data.setReloadTick(0);
                player.setItemInHand(InteractionHand.MAIN_HAND,
                        FirearmItem.transfer(firearm.get(), stack));
                player.resetAttackStrengthTicker();
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        IblisSounds.SHOTGUN_HAMMER_COCK.get(), SoundSource.PLAYERS,
                        1.0F, level.random.nextFloat() * 0.2F + 0.8F);
            } else {
                data.setReloadTick(reloadTick);
            }
        }
    }

    @Override
    protected boolean isAmmo(ItemStack stack) {
        return stack.getItem() instanceof AmmoItem;
    }

    @Override
    public void onLeftClick(net.minecraft.server.level.ServerLevel level,
                            net.minecraft.server.level.ServerPlayer player, InteractionHand hand) {
        super.onLeftClick(level, player, hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                IblisSounds.SHOTGUN_HAMMER_COCK.get(), SoundSource.PLAYERS,
                1.0F, level.random.nextFloat() * 0.2F + 0.8F);
    }
}
