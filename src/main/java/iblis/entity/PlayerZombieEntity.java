package iblis.entity;

import iblis.config.IblisConfig;
import iblis.player.PlayerCharacteristic;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class PlayerZombieEntity extends Zombie {
    private final NonNullList<ItemStack> inheritedInventory =
            NonNullList.withSize(36, ItemStack.EMPTY);

    public PlayerZombieEntity(EntityType<? extends PlayerZombieEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static PlayerZombieEntity inheritFrom(Player player) {
        PlayerZombieEntity zombie = new PlayerZombieEntity(
                iblis.registry.IblisEntities.PLAYER_ZOMBIE.get(), player.level());
        zombie.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        zombie.xpReward = player.totalExperience;
        if (!IblisConfig.noDeathPenalty) {
            for (PlayerCharacteristic characteristic : PlayerCharacteristic.values()) {
                zombie.xpReward += investedExperience(characteristic.getCurrentLevel(player));
            }
        }

        Inventory inventory = player.getInventory();
        zombie.setItemSlot(EquipmentSlot.MAINHAND, inventory.getSelected().copy());
        zombie.setItemSlot(EquipmentSlot.OFFHAND, inventory.offhand.get(0).copy());
        zombie.setItemSlot(EquipmentSlot.FEET, inventory.armor.get(0).copy());
        zombie.setItemSlot(EquipmentSlot.CHEST, inventory.armor.get(1).copy());
        zombie.setItemSlot(EquipmentSlot.LEGS, inventory.armor.get(2).copy());
        zombie.setItemSlot(EquipmentSlot.HEAD, inventory.armor.get(3).copy());
        inventory.items.set(inventory.selected, ItemStack.EMPTY);
        for (int slot = 0; slot < zombie.inheritedInventory.size(); slot++) {
            zombie.inheritedInventory.set(slot, inventory.items.get(slot).copy());
        }
        inventory.clearContent();
        return zombie;
    }

    private static int investedExperience(int characteristicLevel) {
        int total = 0;
        for (int outer = characteristicLevel - 1; outer >= 0; outer--) {
            for (int level = outer - 1; level >= 0; level--) {
                total += experienceNeeded(level);
            }
        }
        return total;
    }

    private static int experienceNeeded(int level) {
        if (level >= 30) {
            return 112 + (level - 30) * 9;
        }
        if (level >= 15) {
            return 37 + (level - 15) * 5;
        }
        return 7 + level * 2;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ListTag inventory = new ListTag();
        for (int slot = 0; slot < inheritedInventory.size(); slot++) {
            ItemStack stack = inheritedInventory.get(slot);
            if (!stack.isEmpty()) {
                CompoundTag stackTag = new CompoundTag();
                stackTag.putByte("Slot", (byte) slot);
                stack.save(stackTag);
                inventory.add(stackTag);
            }
        }
        tag.put("inventoryInherited", inventory);
        tag.putInt("experienceValue", xpReward);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ListTag inventory = tag.getList("inventoryInherited", Tag.TAG_COMPOUND);
        for (int index = 0; index < inventory.size(); index++) {
            CompoundTag stackTag = inventory.getCompound(index);
            int slot = stackTag.getByte("Slot") & 255;
            if (slot < inheritedInventory.size()) {
                inheritedInventory.set(slot, ItemStack.of(stackTag));
            }
        }
        xpReward = tag.getInt("experienceValue");
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        for (ItemStack stack : inheritedInventory) {
            if (!stack.isEmpty()) {
                spawnAtLocation(stack.copy());
            }
        }
        inheritedInventory.clear();
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }
}
