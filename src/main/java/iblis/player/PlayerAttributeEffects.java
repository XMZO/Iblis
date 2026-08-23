package iblis.player;

import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public final class PlayerAttributeEffects {
    public static final UUID ATTACK_DAMAGE_BY_CHARACTERISTIC =
            UUID.fromString("73b1f7c9-7f6f-0857-7ac7-000009f5f2d7");
    public static final UUID ATTACK_DAMAGE_BY_SKILL =
            UUID.fromString("73b1f7c9-7f6f-0857-0857-000009f5f2d7");
    public static final UUID EQUILIBRIUM_KNOCKBACK =
            UUID.fromString("ffffffff-ff62-bd91-0000-00001580fe92");
    public static final UUID SPRINTING_SPEED =
            UUID.fromString("0571979f-fefd-f1e7-00af-4a7ac791571c");

    private PlayerAttributeEffects() {
    }

    public static void refreshMeleeDamageBonus(Player player) {
        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage == null) {
            return;
        }

        attackDamage.removeModifier(ATTACK_DAMAGE_BY_CHARACTERISTIC);
        if (PlayerCharacteristic.MELEE_DAMAGE_BONUS.enabled) {
            attackDamage.addTransientModifier(new AttributeModifier(
                    ATTACK_DAMAGE_BY_CHARACTERISTIC,
                    "Iblis characteristic melee damage",
                    PlayerCharacteristic.MELEE_DAMAGE_BONUS.getEffectiveBonus(player),
                    AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    public static void refreshWeaponSkill(Player player) {
        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage == null) {
            return;
        }
        attackDamage.removeModifier(ATTACK_DAMAGE_BY_SKILL);

        boolean weapon = player.getMainHandItem().getAttributeModifiers(
                net.minecraft.world.entity.EquipmentSlot.MAINHAND).containsKey(Attributes.ATTACK_DAMAGE);
        double amount;
        String name;
        if (weapon && PlayerSkill.SWORDSMANSHIP.enabled) {
            amount = PlayerSkill.SWORDSMANSHIP.getFullValue(player);
            name = "Weapon skill modifier";
        } else if (player.getMainHandItem().isEmpty()) {
            amount = PlayerSkill.BOXING.getFullValue(player);
            name = "Boxing skill modifier";
        } else {
            return;
        }
        attackDamage.addTransientModifier(new AttributeModifier(
                ATTACK_DAMAGE_BY_SKILL, name, amount, AttributeModifier.Operation.ADDITION));
    }

    public static void refreshSprintingSpeed(Player player, int sprintCounter) {
        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed == null) {
            return;
        }
        movementSpeed.removeModifier(SPRINTING_SPEED);
        if (sprintCounter <= 0 || !PlayerSkill.RUNNING.enabled) {
            return;
        }
        double amount = (PlayerSkill.RUNNING.getFullValue(player) - 0.1) * 0.1;
        if (amount > 0.0) {
            amount *= sprintCounter / 32.0;
        }
        movementSpeed.addTransientModifier(new AttributeModifier(
                SPRINTING_SPEED, "Sprinting speed boost", amount,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
    }
}
