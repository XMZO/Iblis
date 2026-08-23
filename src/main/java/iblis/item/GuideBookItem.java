package iblis.item;

import iblis.network.IblisNetwork;
import iblis.player.IblisPlayerData;
import iblis.player.PlayerDataAccess;
import iblis.player.PlayerSkill;
import iblis.registry.IblisSounds;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class GuideBookItem extends Item {
    public static final int DIARY = 0;
    public static final int CLOSED_GUIDE = 1;
    public static final int OPENED_GUIDE = 2;

    public static final String BOOK_TYPE = "CustomModelData";
    public static final String SKILLS = "skills";
    public static final String EXPLORED_BOOKS = "exploredBooks";
    public static final String BOOK_ID = "id";
    public static final String CREATION_TIME = "timeOfCreation";
    public static final String AUTHOR = "author";
    private static final float BOOK_KNOWLEDGE_FACTOR = 0.7F;
    private static final String[] GUIDE_LEVELS = {
            "beginners", "novice", "experienced", "professional", "experts", "ultimate"
    };

    public GuideBookItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createGuide(Item item, PlayerSkill skill, double value) {
        ItemStack stack = new ItemStack(item);
        setBookType(stack, CLOSED_GUIDE);
        ListTag skills = new ListTag();
        CompoundTag skillTag = new CompoundTag();
        skillTag.putString("name", skill.name());
        skillTag.putDouble("value", value);
        skills.add(skillTag);
        stack.getOrCreateTag().put(SKILLS, skills);
        return stack;
    }

    public static ItemStack createDiary(Item item, String author) {
        ItemStack stack = new ItemStack(item);
        setBookType(stack, DIARY);
        stack.getOrCreateTag().putString(AUTHOR, author);
        return stack;
    }

    public static void fillDiary(ItemStack stack, ServerPlayer player) {
        CompoundTag book = stack.getOrCreateTag();
        setBookType(stack, DIARY);
        int id = player.getGameProfile().getName().hashCode();
        long creationTime = player.level().getGameTime();
        book.putInt(BOOK_ID, id);
        book.putString(AUTHOR, player.getGameProfile().getName());
        book.putLong(CREATION_TIME, creationTime);

        ListTag skills = new ListTag();
        for (PlayerSkill skill : PlayerSkill.values()) {
            CompoundTag skillTag = new CompoundTag();
            skillTag.putString("name", skill.name());
            skillTag.putDouble("value", skill.getCurrentValue(player));
            skills.add(skillTag);
        }
        book.put(SKILLS, skills);

        IblisPlayerData data = PlayerDataAccess.get(player);
        ListTag knownBooks = deduplicateBooks(data.exploredBooks());
        ListTag inheritedBooks = new ListTag();
        for (int index = 0; index < knownBooks.size(); index++) {
            CompoundTag known = knownBooks.getCompound(index);
            if (known.getInt(BOOK_ID) != id) {
                inheritedBooks.add(known.copy());
            }
        }
        book.put(EXPLORED_BOOKS, inheritedBooks);

        CompoundTag ownBook = findBook(knownBooks, id);
        if (ownBook == null) {
            ownBook = new CompoundTag();
            ownBook.putInt(BOOK_ID, id);
            knownBooks.add(ownBook);
        }
        ownBook.putLong(CREATION_TIME, creationTime);
        data.setExploredBooks(knownBooks);
        IblisNetwork.sendPlayerData(player);
    }

    public static int getBookType(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt(BOOK_TYPE) : DIARY;
    }

    public static void setBookType(ItemStack stack, int type) {
        stack.getOrCreateTag().putInt(BOOK_TYPE, type);
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return getBookType(stack) == DIARY
                ? "item.iblis.adventurer_diary"
                : "item.iblis.guide";
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && stack.hasTag() && stack.getTag().getInt(BOOK_ID) == 0) {
            stack.getTag().putInt(BOOK_ID, level.random.nextInt() & 15);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.hasTag()) {
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        CompoundTag book = stack.getTag();
        IblisPlayerData playerData = PlayerDataAccess.get(player);
        ListTag explored = deduplicateBooks(playerData.exploredBooks());
        int bookId = book.getInt(BOOK_ID);
        long version = book.getLong(CREATION_TIME);
        CompoundTag previous = findBook(explored, bookId);
        boolean alreadyRead = previous != null;
        boolean newerVersion = alreadyRead && version > previous.getLong(CREATION_TIME);

        if (!alreadyRead || newerVersion) {
            if (!learnSkills(player, book, newerVersion)) {
                return InteractionResultHolder.fail(stack);
            }

            if (previous == null) {
                previous = new CompoundTag();
                previous.putInt(BOOK_ID, bookId);
                explored.add(previous);
            }
            previous.putLong(CREATION_TIME, version);
            mergeExploredBooks(explored, book.getList(EXPLORED_BOOKS, Tag.TAG_COMPOUND));
            playerData.setExploredBooks(explored);
            if (player instanceof ServerPlayer serverPlayer) {
                IblisNetwork.sendPlayerData(serverPlayer);
            }
            player.displayClientMessage(Component.translatable("iblis.youLearnedSomethingNew"), false);
        } else {
            player.displayClientMessage(Component.translatable("iblis.youAlreadyReadThatBook"), false);
        }

        int type = getBookType(stack);
        if (type == OPENED_GUIDE) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    IblisSounds.BOOK_CLOSING.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            setBookType(stack, CLOSED_GUIDE);
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    IblisSounds.BOOK_READING.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            if (type == CLOSED_GUIDE) {
                setBookType(stack, OPENED_GUIDE);
            }
        }
        return InteractionResultHolder.consume(stack);
    }

    private static boolean learnSkills(Player player, CompoundTag book, boolean newerVersion) {
        ListTag skills = book.getList(SKILLS, Tag.TAG_COMPOUND);
        for (int index = 0; index < skills.size(); index++) {
            CompoundTag skillTag = skills.getCompound(index);
            PlayerSkill skill;
            try {
                skill = PlayerSkill.valueOf(skillTag.getString("name"));
            } catch (IllegalArgumentException exception) {
                return false;
            }

            double oldValue = skill.getCurrentValue(player);
            double learnedValue = skillTag.getDouble("value") * BOOK_KNOWLEDGE_FACTOR;
            skill.raiseTo(player, newerVersion && learnedValue > oldValue
                    ? learnedValue
                    : oldValue + learnedValue);
        }
        return true;
    }

    private static ListTag deduplicateBooks(ListTag input) {
        Map<Integer, Long> newestVersions = new HashMap<>();
        for (int index = 0; index < input.size(); index++) {
            CompoundTag book = input.getCompound(index);
            newestVersions.merge(book.getInt(BOOK_ID), book.getLong(CREATION_TIME), Math::max);
        }

        ListTag result = new ListTag();
        newestVersions.forEach((id, version) -> {
            CompoundTag book = new CompoundTag();
            book.putInt(BOOK_ID, id);
            book.putLong(CREATION_TIME, version);
            result.add(book);
        });
        return result;
    }

    private static void mergeExploredBooks(ListTag destination, ListTag source) {
        for (int index = 0; index < source.size(); index++) {
            CompoundTag candidate = source.getCompound(index);
            if (findBook(destination, candidate.getInt(BOOK_ID)) == null) {
                destination.add(candidate.copy());
            }
        }
    }

    @Nullable
    public static CompoundTag findBook(ListTag books, int id) {
        for (int index = 0; index < books.size(); index++) {
            CompoundTag book = books.getCompound(index);
            if (book.getInt(BOOK_ID) == id) {
                return book;
            }
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        if (level == null || !stack.hasTag()) {
            return;
        }
        CompoundTag book = stack.getTag();
        int bookId = book.getInt(BOOK_ID);
        if (getBookType(stack) == DIARY) {
            tooltip.add(Component.translatable("iblis.diary", book.getString(AUTHOR)));
        } else {
            ListTag skills = book.getList(SKILLS, Tag.TAG_COMPOUND);
            if (!skills.isEmpty()) {
                CompoundTag skill = skills.getCompound(0);
                int levelIndex = Math.min((int) (skill.getDouble("value") * 3.0), GUIDE_LEVELS.length - 1);
                Component skillName = Component.translatable("iblis." + skill.getString("name"));
                Component skillLevel = Component.translatable("iblis.guideLevel." + GUIDE_LEVELS[levelIndex]);
                tooltip.add(bookId == 0
                        ? Component.translatable("iblis.guideTitle", skillLevel, skillName)
                        : Component.translatable("iblis.guideTitleAndVolume", skillLevel, skillName, bookId));
            }
        }

        long creationTime = book.getLong(CREATION_TIME);
        if (creationTime > 0L) {
            tooltip.add(Component.translatable("iblis.thisBookWasWritten",
                    describeAge(Math.max(0L, (level.getGameTime() - creationTime) / 20L))));
        }
    }

    private static Component describeAge(long seconds) {
        if (seconds < 60L) {
            return seconds == 1L
                    ? Component.translatable("iblis.second")
                    : Component.translatable("iblis.seconds", seconds);
        }
        long minutes = seconds / 60L;
        if (minutes < 60L) {
            return minutes == 1L
                    ? Component.translatable("iblis.minute")
                    : Component.translatable("iblis.minutes", minutes);
        }
        long hours = minutes / 60L;
        if (hours < 24L) {
            return hours == 1L
                    ? Component.translatable("iblis.hour")
                    : Component.translatable("iblis.hours", hours);
        }
        long days = hours / 24L;
        return days == 1L
                ? Component.translatable("iblis.day")
                : Component.translatable("iblis.days", days);
    }
}
