package faygolover.rpcore.client.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Элемент общего HUD. Мод регистрирует его в {@link HudRegistry}; положение, масштаб и видимость хранит
 * RP Core, двигать — в одном редакторе для всех модов ({@code /rpcore hud}).
 * <p>
 * Значения по умолчанию — первая раскладка: мод может передать сюда положение из своего старого конфига,
 * и раскладка игрока переедет сама.
 */
public interface HudElement {
    ResourceLocation id();

    /** Подпись в редакторе. */
    Component title();

    /** Размер до масштаба, пиксели GUI (рамка в редакторе и расчёт якоря). */
    int width();

    int height();

    Anchor defaultAnchor();

    int defaultX();

    int defaultY();

    default double defaultScale() {
        return 1.0;
    }

    default boolean defaultVisible() {
        return true;
    }

    /**
     * Нарисовать в локальных координатах: (0,0) — левый верхний угол, масштаб уже применён.
     * {@code preview} — в редакторе (рисовать образец, даже если показывать нечего).
     */
    void render(GuiGraphics g, float partialTick, boolean preview);
}
