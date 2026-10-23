package faygolover.rpcore.client;

import com.mojang.blaze3d.platform.InputConstants;
import faygolover.rpcore.client.CoreClientConfig.HandMode;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Pose;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Пустая рука от первого лица. Ванилла рисует правую руку всегда, Hold My Items ещё и анимирует её. Режимы
 * ({@link HandMode}): всегда; только с предметом; с предметом и во время действия — удар, карабканье, плавание,
 * ползком, использование. Рука отменяется событием {@link RenderHandEvent} — раньше, чем её начинает рисовать
 * Hold My Items, поэтому с его анимациями не спорит. Клавиша «Пустая рука» перебирает режимы.
 */
public final class EmptyHand {
    private EmptyHand() {}

    public static final KeyMapping KEY = new KeyMapping("key.rpcore.empty_hand", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F7, "key.categories.rpcore");

    static void onKeys(RegisterKeyMappingsEvent e) {
        e.register(KEY);
    }

    static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        while (KEY.consumeClick()) {
            HandMode next = HandMode.values()[(mode().ordinal() + 1) % HandMode.values().length];
            CoreClientConfig.HAND_MODE.set(next);
            CoreClientConfig.HAND_MODE.save();
            LocalPlayer p = Minecraft.getInstance().player;
            if (p != null) p.displayClientMessage(Component.translatable("rpcore.hand." + next.name().toLowerCase()), true);
        }
    }

    static void onRenderHand(RenderHandEvent e) {
        if (!e.getItemStack().isEmpty()) return;
        HandMode m = mode();
        if (m == HandMode.ALWAYS) return;
        LocalPlayer p = Minecraft.getInstance().player;
        if (m == HandMode.ITEM_AND_ACTION && p != null && acting(p, e.getPartialTick())) return;
        e.setCanceled(true);
    }

    /** Действие, при котором рука нужна: удар или взмах, карабканье, плавание, ползком, использование. */
    private static boolean acting(LocalPlayer p, float partial) {
        return p.swinging || p.getAttackAnim(partial) > 0 || p.onClimbable() || p.isSwimming() || p.isVisuallySwimming()
                || p.getPose() == Pose.SWIMMING || p.isUsingItem();
    }

    private static HandMode mode() {
        try {
            return CoreClientConfig.HAND_MODE.get();
        } catch (IllegalStateException notLoaded) {
            return HandMode.ITEM_AND_ACTION;
        }
    }
}
