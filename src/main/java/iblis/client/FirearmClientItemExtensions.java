package iblis.client;

import iblis.item.CrossbowReloadingItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

@OnlyIn(Dist.CLIENT)
public final class FirearmClientItemExtensions {
    private static final IClientItemExtensions READY = new FirearmPose(false);
    private static final IClientItemExtensions RELOADING = new FirearmPose(true);

    private FirearmClientItemExtensions() {
    }

    public static IClientItemExtensions ready() {
        return READY;
    }

    public static IClientItemExtensions reloading() {
        return RELOADING;
    }

    private record FirearmPose(boolean reloading) implements IClientItemExtensions {
        @Override
        public HumanoidModel.ArmPose getArmPose(
                LivingEntity entity, InteractionHand hand, ItemStack stack) {
            if (hand != InteractionHand.MAIN_HAND
                    || entity.isUsingItem() && entity.getUsedItemHand() != hand) {
                return null;
            }
            if (reloading && stack.getItem() instanceof CrossbowReloadingItem
                    && entity.isUsingItem() && entity.getUseItem() == stack) {
                return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
            }
            return HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }
    }
}
