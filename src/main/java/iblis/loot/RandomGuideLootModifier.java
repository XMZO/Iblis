package iblis.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import iblis.IblisMod;
import iblis.item.GuideBookItem;
import iblis.player.PlayerSkill;
import iblis.registry.IblisItems;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

public final class RandomGuideLootModifier extends LootModifier {
    public static final Codec<RandomGuideLootModifier> CODEC = RecordCodecBuilder.create(instance ->
            codecStart(instance).apply(instance, RandomGuideLootModifier::new));
    private static final String[] TARGET_PATH_PARTS = {
            "pyramid", "city", "jungle_temple", "simple_dungeon", "library", "mansion"
    };
    private static final ResourceLocation LIBRARY_LOOT = new ResourceLocation(
            IblisMod.MOD_ID, "library_loot");
    private static final ResourceLocation DUNGEON_LOOT = new ResourceLocation(
            IblisMod.MOD_ID, "dungeon_loot");
    private static final int MAX_LABYRINTH_LOOT_LEVEL = 15;

    public RandomGuideLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(
            ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        ResourceLocation table = context.getQueriedLootTableId();
        if (table.getNamespace().equals(IblisMod.MOD_ID)) {
            return generatedLoot;
        }
        if (table.getNamespace().equals("labyrinth")) {
            applyLabyrinthLoot(generatedLoot, context, table.getPath());
            return generatedLoot;
        }
        if (!matchesPath(table.getPath())) {
            return generatedLoot;
        }
        RandomSource random = context.getRandom();
        int rolls = Math.max(0, 1 + Mth.floor(context.getLuck()));
        for (int roll = 0; roll < rolls; roll++) {
            if (random.nextBoolean()) {
                PlayerSkill[] skills = PlayerSkill.values();
                PlayerSkill skill = skills[random.nextInt(skills.length)];
                double value = random.nextFloat() * random.nextFloat() + 0.1F;
                generatedLoot.add(GuideBookItem.createGuide(IblisItems.GUIDE.get(), skill, value));
            }
        }
        return generatedLoot;
    }

    /**
     * Replays the legacy Labyrinth integration by evaluating the named pools
     * from Iblis' reference loot tables in the original chest context.
     */
    private static void applyLabyrinthLoot(ObjectArrayList<ItemStack> generatedLoot,
                                           LootContext context, String path) {
        int lootLevel = Math.min(extractFirstInteger(path), MAX_LABYRINTH_LOOT_LEVEL);
        if (path.contains("library_loot_tables")) {
            LootTable library = context.getResolver().getLootTable(LIBRARY_LOOT);
            for (PlayerSkill skill : PlayerSkill.values()) {
                LootPool pool = library.getPool(skill.name() + "_level_" + lootLevel);
                if (pool != null) {
                    pool.addRandomItems(generatedLoot::add, context);
                    continue;
                }
                while (--lootLevel > 0) {
                    pool = library.getPool(skill.name() + "_level_" + lootLevel);
                    if (pool != null) {
                        pool.addRandomItems(generatedLoot::add, context);
                        return;
                    }
                }
            }
        } else if (path.contains("dungeon_loot_tables")) {
            LootPool pool = context.getResolver().getLootTable(DUNGEON_LOOT)
                    .getPool("level_" + lootLevel);
            if (pool != null) {
                pool.addRandomItems(generatedLoot::add, context);
            }
        }
    }

    private static int extractFirstInteger(String value) {
        StringBuilder number = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current >= '0' && current <= '9') {
                if (number.isEmpty() && index > 0 && value.charAt(index - 1) == '-') {
                    number.append('-');
                }
                number.append(current);
            } else if (!number.isEmpty()) {
                break;
            }
        }
        if (number.isEmpty() || number.toString().equals("-")) {
            return 0;
        }
        try {
            return Integer.parseInt(number.toString());
        } catch (NumberFormatException exception) {
            return number.charAt(0) == '-' ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        }
    }

    private static boolean matchesPath(String path) {
        for (String target : TARGET_PATH_PARTS) {
            if (path.contains(target)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return IblisLootModifiers.RANDOM_GUIDES.get();
    }
}
