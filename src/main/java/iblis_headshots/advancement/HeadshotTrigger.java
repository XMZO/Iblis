package iblis_headshots.advancement;

import com.google.gson.JsonObject;
import iblis_headshots.IblisHeadshotsMod;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;

public final class HeadshotTrigger extends SimpleCriterionTrigger<HeadshotTrigger.Instance> {
    private static final ResourceLocation ID =
            new ResourceLocation(IblisHeadshotsMod.MOD_ID, "headshot");
    public static final HeadshotTrigger INSTANCE = CriteriaTriggers.register(new HeadshotTrigger());

    private HeadshotTrigger() {
    }

    public static void register() {
        // Forces class initialization before advancement JSON is parsed.
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected Instance createInstance(JsonObject json, ContextAwarePredicate player,
                                      DeserializationContext context) {
        ContextAwarePredicate target = EntityPredicate.fromJson(json, "target", context);
        return new Instance(player, target);
    }

    public void trigger(ServerPlayer player, Entity target) {
        LootContext targetContext = EntityPredicate.createContext(player, target);
        trigger(player, instance -> instance.matches(targetContext));
    }

    public static final class Instance extends AbstractCriterionTriggerInstance {
        private final ContextAwarePredicate target;

        private Instance(ContextAwarePredicate player, ContextAwarePredicate target) {
            super(ID, player);
            this.target = target;
        }

        private boolean matches(LootContext targetContext) {
            return target.matches(targetContext);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            JsonObject json = super.serializeToJson(context);
            json.add("target", target.toJson(context));
            return json;
        }
    }
}
