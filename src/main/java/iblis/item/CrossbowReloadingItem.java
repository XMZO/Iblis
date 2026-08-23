package iblis.item;

import iblis.registry.IblisItems;
import iblis.registry.IblisSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public final class CrossbowReloadingItem extends ReloadingFirearmItem {
    public CrossbowReloadingItem(Properties properties,
                                 java.util.function.Supplier<? extends FirearmItem> firearm) {
        super(properties, firearm);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.getList(FirearmItem.AMMO, Tag.TAG_COMPOUND).size() >= 2) {
            player.resetAttackStrengthTicker();
            return InteractionResultHolder.success(FirearmItem.transfer(firearm.get(), stack));
        }
        if (findAmmo(player).isEmpty()) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!(user instanceof Player player)) {
            return stack;
        }
        if (level.isClientSide) {
            return stack;
        }
        CompoundTag tag = stack.getOrCreateTag();
        int cocked = tag.getInt(FirearmItem.COCKED_STATE);
        int ammunition = tag.getList(FirearmItem.AMMO, Tag.TAG_COMPOUND).size();
        if (ammunition >= 2) {
            return FirearmItem.transfer(firearm.get(), stack);
        }
        if (ammunition >= cocked) {
            tag.putInt(FirearmItem.COCKED_STATE, cocked + 1);
        } else {
            ItemStack ammunitionStack = findAmmo(player);
            if (!ammunitionStack.isEmpty()) {
                reloadAmmo(ammunitionStack, player, tag);
                if (tag.getList(FirearmItem.AMMO, Tag.TAG_COMPOUND).size() >= 2) {
                    player.resetAttackStrengthTicker();
                    return FirearmItem.transfer(firearm.get(), stack);
                }
            }
        }
        // Modern Minecraft stops item use after this callback. Returning a new
        // stack makes the completed stage sync before held use starts the next one.
        return stack.copy();
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getInt(FirearmItem.COCKED_STATE) == 0 ? 15 : 45;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingTicks) {
        if (remainingTicks != 4) {
            return;
        }
        int cocked = stack.hasTag() ? stack.getTag().getInt(FirearmItem.COCKED_STATE) : 0;
        net.minecraft.sounds.SoundEvent sound = cocked == 0
                ? IblisSounds.CROSSBOW_COCK.get()
                : IblisSounds.CROSSBOW_PUTTING_BOLT.get();
        level.playSound(null, user.getX(), user.getY(), user.getZ(), sound,
                SoundSource.PLAYERS, cocked == 0 ? 0.8F : 0.4F,
                level.random.nextFloat() * 0.2F + 0.8F);
    }

    @Override
    protected boolean isAmmo(ItemStack stack) {
        return stack.is(IblisItems.CROSSBOW_BOLT.get());
    }
}
