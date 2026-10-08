package faygolover.rpcore.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Все элементы HUD семейства RP. Рисуются одним слоем {@code rpcore:hud} поверх остальных; двигаются в
 * {@link HudEditorScreen}. Регистрировать — на клиенте, в FMLClientSetupEvent.
 */
public final class HudRegistry {
    private HudRegistry() {}

    private static final List<HudElement> ELEMENTS = new CopyOnWriteArrayList<>();

    public static void register(HudElement e) {
        for (HudElement old : ELEMENTS) if (old.id().equals(e.id())) return;
        ELEMENTS.add(e);
    }

    public static List<HudElement> elements() {
        return new ArrayList<>(ELEMENTS);
    }

    /** Слой HUD (сигнатура IGuiOverlay). */
    public static void render(ForgeGui gui, GuiGraphics g, float partialTick, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen instanceof HudEditorScreen) return;
        for (HudElement e : ELEMENTS) {
            HudLayout.Entry c = HudLayout.get(e);
            if (!c.visible) continue;
            int[] r = HudLayout.rect(e, sw, sh);
            g.pose().pushPose();
            // Выше чата по глубине: иначе фон сообщений закрывает элементы.
            g.pose().translate(r[0], r[1], 300);
            g.pose().scale((float) c.scale, (float) c.scale, 1);
            e.render(g, partialTick, false);
            g.pose().popPose();
        }
    }
}
