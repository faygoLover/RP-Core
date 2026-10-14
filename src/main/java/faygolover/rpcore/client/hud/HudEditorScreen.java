package faygolover.rpcore.client.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * Один редактор HUD для всех модов семейства (раньше у RP Stamina и RP Medicine были свои копии):
 * перетаскивание мышью, колесо — масштаб, ПКМ — скрыть или показать, кнопка — якорь выбранного элемента.
 */
public class HudEditorScreen extends Screen {
    @Nullable
    private HudElement selected;
    @Nullable
    private HudElement dragging;
    private int grabX;
    private int grabY;
    private Button anchorButton;

    public HudEditorScreen() {
        super(Component.translatable("rpcore.hud.editor.title"));
    }

    @Override
    protected void init() {
        // Кнопки маленькие, в правом верхнем углу — не закрывают элементы.
        int y = 4;
        int right = width - 4;
        anchorButton = addRenderableWidget(Button.builder(anchorLabel(), b -> {
            if (selected == null) return;
            HudLayout.Entry c = HudLayout.get(selected);
            c.anchor = c.anchor.next();
            c.x = 0;
            c.y = 0;
            b.setMessage(anchorLabel());
            HudLayout.save();
        }).bounds(right - 236, y, 120, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("rpcore.hud.editor.reset"), b -> {
            for (HudElement e : HudRegistry.elements()) HudLayout.reset(e);
            HudLayout.save();
        }).bounds(right - 112, y, 54, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("rpcore.hud.editor.done"), b -> onClose())
                .bounds(right - 54, y, 54, 14).build());
    }

    private Component anchorLabel() {
        if (selected == null) return Component.translatable("rpcore.hud.editor.select");
        return Component.translatable("rpcore.hud.editor.anchor", HudLayout.get(selected).anchor.name());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        // Под строкой кнопок (на узком окне кнопки закрывали заголовок).
        g.drawCenteredString(font, title, width / 2, 24, 0xFFFFFF);
        g.drawCenteredString(font, Component.translatable("rpcore.hud.editor.hint"), width / 2, 36, 0xA0A0A0);
        if (HudRegistry.elements().isEmpty())
            g.drawCenteredString(font, Component.translatable("rpcore.hud.editor.empty"), width / 2, height / 2, 0xFFFFFF);
        for (HudElement e : HudRegistry.elements()) {
            HudLayout.Entry c = HudLayout.get(e);
            int[] r = HudLayout.rect(e, width, height);
            g.pose().pushPose();
            g.pose().translate(r[0], r[1], 0);
            g.pose().scale((float) c.scale, (float) c.scale, 1);
            e.render(g, pt, true);
            g.pose().popPose();
            int color = !c.visible ? 0xFF803030 : e == selected ? 0xFFFFFFFF : 0xFF707070;
            g.renderOutline(r[0] - 2, r[1] - 2, r[2] + 4, r[3] + 4, color);
            g.drawString(font, e.title(), r[0], r[1] - 11, color, true);
        }
        super.render(g, mx, my, pt);
    }

    @Nullable
    private HudElement at(double mx, double my) {
        for (HudElement e : HudRegistry.elements()) {
            int[] r = HudLayout.rect(e, width, height);
            if (mx >= r[0] - 3 && mx <= r[0] + r[2] + 3 && my >= r[1] - 3 && my <= r[1] + r[3] + 3) return e;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        HudElement e = at(mx, my);
        if (e == null) return false;
        selected = e;
        anchorButton.setMessage(anchorLabel());
        if (button == 1) {
            HudLayout.Entry c = HudLayout.get(e);
            c.visible = !c.visible;
            HudLayout.save();
            return true;
        }
        if (button == 0) {
            int[] r = HudLayout.rect(e, width, height);
            dragging = e;
            grabX = (int) mx - r[0];
            grabY = (int) my - r[1];
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) {
            setPosition(dragging, (int) mx - grabX, (int) my - grabY);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging != null) {
            dragging = null;
            HudLayout.save();
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        HudElement e = at(mx, my);
        if (e == null) e = selected;
        if (e == null) return false;
        HudLayout.Entry c = HudLayout.get(e);
        // Шаг 0,05; с Shift — 0,01 (подогнать точно, например, под ширину хотбара).
        double step = hasShiftDown() ? 0.01 : 0.05;
        c.scale = Mth.clamp(Math.round((c.scale + (delta > 0 ? step : -step)) * 100) / 100.0, 0.25, 4.0);
        HudLayout.save();
        return true;
    }

    /** Абсолютный левый верхний угол → смещение от якоря. */
    private void setPosition(HudElement e, int x, int y) {
        HudLayout.Entry c = HudLayout.get(e);
        int[] r = HudLayout.rect(e, width, height);
        int baseX = Math.round(width * c.anchor.fx - r[2] * c.anchor.fx);
        int baseY = Math.round(height * c.anchor.fy - r[3] * c.anchor.fy);
        c.x = Mth.clamp(x - baseX, -4000, 4000);
        c.y = Mth.clamp(y - baseY, -4000, 4000);
    }

    @Override
    public void onClose() {
        HudLayout.save();
        super.onClose();
    }
}
