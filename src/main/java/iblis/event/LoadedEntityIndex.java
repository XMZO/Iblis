package iblis.event;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

final class LoadedEntityIndex {
    private static final Map<ServerLevel, LoadedEntityIndex> LEVELS = new IdentityHashMap<>();

    private final ObjectArrayList<Entity> entities = new ObjectArrayList<>();
    private final Int2IntOpenHashMap positions = new Int2IntOpenHashMap();

    private LoadedEntityIndex() {
        positions.defaultReturnValue(-1);
    }

    static void add(ServerLevel level, Entity entity) {
        LoadedEntityIndex index = LEVELS.computeIfAbsent(level, ignored -> new LoadedEntityIndex());
        if (index.positions.containsKey(entity.getId())) {
            return;
        }
        index.positions.put(entity.getId(), index.entities.size());
        index.entities.add(entity);
    }

    static void remove(ServerLevel level, Entity entity) {
        LoadedEntityIndex index = LEVELS.get(level);
        if (index == null) {
            return;
        }
        int position = index.positions.remove(entity.getId());
        if (position < 0) {
            return;
        }
        Entity last = index.entities.remove(index.entities.size() - 1);
        if (position < index.entities.size()) {
            index.entities.set(position, last);
            index.positions.put(last.getId(), position);
        }
    }

    static Entity random(ServerLevel level) {
        LoadedEntityIndex index = LEVELS.get(level);
        if (index == null || index.entities.isEmpty()) {
            return null;
        }
        return index.entities.get(level.random.nextInt(index.entities.size()));
    }

    static void clear(ServerLevel level) {
        LEVELS.remove(level);
    }
}
