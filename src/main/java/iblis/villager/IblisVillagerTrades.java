package iblis.villager;

import iblis.IblisMod;
import iblis.item.GuideBookItem;
import iblis.player.PlayerSkill;
import iblis.registry.IblisItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IblisMod.MOD_ID)
public final class IblisVillagerTrades {
    private IblisVillagerTrades() {
    }

    @SubscribeEvent
    public static void addTrades(VillagerTradesEvent event) {
        if (event.getType() == VillagerProfession.LIBRARIAN) {
            event.getTrades().get(2).add(new RandomGuideTrade());
        }
    }

    private static final class RandomGuideTrade implements VillagerTrades.ItemListing {
        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            PlayerSkill[] skills = PlayerSkill.values();
            PlayerSkill skill = skills[random.nextInt(skills.length)];
            double value = random.nextFloat() * random.nextFloat() + 0.1F;
            ItemStack guide = GuideBookItem.createGuide(IblisItems.GUIDE.get(), skill, value);
            return new MerchantOffer(new ItemStack(Items.EMERALD, 4 + random.nextInt(5)),
                    guide, 7, 10, 0.05F);
        }
    }
}
