package faygolover.rpcore.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** Сон сидя или лёжа ({@code /sleep}): экран плавно темнеет, внизу подсказка, как проснуться. */
public final class PostureOverlay {
    private PostureOverlay() {}

    /** Сколько миллисекунд темнеет и светлеет экран. */
    private static final long FADE_MS = 1500;
    private static boolean asleep;
    private static long changedMs;

    public static void setAsleep(boolean value) {
        if (asleep == value) return;
        asleep = value;
        changedMs = System.currentTimeMillis();
    }

    public static void clear() {
        asleep = false;
        changedMs = 0;
    }

    static void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        float t = Math.min(1f, (System.currentTimeMillis() - changedMs) / (float) FADE_MS);
        float a = asleep ? t : 1f - t;
        if (a <= 0.001f) return;
        int alpha = (int) (a * 0.88f * 255f);
        g.fill(0, 0, width, height, alpha << 24);
        if (asleep && t >= 1f) {
            Minecraft mc = Minecraft.getInstance();
            Component hint = Component.translatable("rpcore.posture.wake_hint");
            g.drawCenteredString(mc.font, hint, width / 2, height - 40, 0x9A9A9A);
        }
    }
}
