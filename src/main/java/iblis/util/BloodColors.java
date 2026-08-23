package iblis.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;

public final class BloodColors {
    private BloodColors() {
    }

    public static int forEntity(LivingEntity victim) {
        if (victim instanceof Ghast || victim instanceof Blaze || victim instanceof MagmaCube) {
            return 0xFFA200;
        }
        if (victim instanceof Slime) {
            return 0x037200;
        }
        if (victim instanceof Silverfish || victim instanceof Spider) {
            return 0xDEDDB7;
        }
        if (victim instanceof EnderMan || victim instanceof Endermite || victim instanceof Shulker) {
            return 0xFF00F6;
        }
        if (victim instanceof Squid) {
            return 0x00E4FF;
        }
        if (victim instanceof Zombie) {
            return 0x380B0B;
        }
        if (victim instanceof Player || victim instanceof Vex || victim instanceof EnderDragon
                || victim instanceof AbstractVillager || victim instanceof AbstractIllager
                || victim instanceof Wolf || victim instanceof Sheep || victim instanceof Rabbit
                || victim instanceof Cat || victim instanceof Ocelot || victim instanceof Parrot
                || victim instanceof AbstractHorse || victim instanceof Llama
                || victim instanceof Cow || victim instanceof Chicken || victim instanceof Bat
                || victim instanceof Guardian || victim instanceof PolarBear || victim instanceof Pig
                || victim instanceof Witch) {
            return 0xC82100;
        }
        return -1;
    }
}
