package faygolover.rpcore.client.gm;

import faygolover.rpcore.network.CoreNetwork;
import faygolover.rpcore.network.GmPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Окно настроек «ГМской штуки» по её схеме ({@link GmSchema}): поля в два столбца (подпись — значение), колесо мыши
 * прокручивает длинный список, внизу — кнопки-помощники схемы, «Готово» и «Отмена». Значения уходят на сервер,
 * он сам проверяет права и пределы.
 */
public class GmSettingsScreen extends Screen {
    private static final int ROW = 22;

    private final BlockPos pos;
    private final GmSchema schema;
    private final CompoundTag tag;
    private int scroll;
    private final List<Label> labels = new ArrayList<>();

    private record Label(Component text, int x, int y) {}

    public GmSettingsScreen(BlockPos pos, GmSchema schema, CompoundTag tag) {
        super(Component.translatable("rpcore.gm." + schema.id + ".title"));
        this.pos = pos;
        this.schema = schema;
        this.tag = tag;
    }

    public static void open(BlockPos pos, CompoundTag tag) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || tag == null) return;
        GmSchema schema = GmSchema.of(mc.level.getBlockState(pos).getBlock());
        if (schema != null) mc.setScreen(new GmSettingsScreen(pos, schema, tag));
    }

    private List<GmSchema.Field> visible() {
        List<GmSchema.Field> out = new ArrayList<>();
        for (GmSchema.Field f : schema.fields) if (f.visible().test(tag)) out.add(f);
        return out;
    }

    private int rowsFit() {
        int bottom = height - 30 - (schema.actions.isEmpty() ? 0 : 24);
        return Math.max(1, (bottom - 32) / ROW);
    }

    @Override
    protected void init() {
        labels.clear();
        List<GmSchema.Field> fields = visible();
        scroll = Math.max(0, Math.min(scroll, fields.size() - rowsFit()));
        int lx = width / 2 - 170, wx = width / 2 + 10, ww = 160;
        int y = 32;
        for (int i = scroll; i < fields.size() && i < scroll + rowsFit(); i++, y += ROW) {
            GmSchema.Field f = fields.get(i);
            labels.add(new Label(Component.translatable("rpcore.gm." + schema.id + "." + f.key()), lx, y + 6));
            switch (f.kind()) {
                case CHOICE -> addRenderableWidget(Button.builder(choiceText(f), b -> {
                    String[] v = f.values();
                    int idx = 0;
                    for (int k = 0; k < v.length; k++) if (v[k].equals(tag.getString(f.key()))) idx = k;
                    tag.putString(f.key(), v[(idx + 1) % v.length]);
                    rebuildWidgets();
                }).bounds(wx, y, ww, 20).build());
                case BOOL -> addRenderableWidget(Button.builder(tag.getBoolean(f.key()) ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF, b -> {
                    tag.putBoolean(f.key(), !tag.getBoolean(f.key()));
                    rebuildWidgets();
                }).bounds(wx, y, ww, 20).build());
                default -> {
                    EditBox box = new EditBox(font, wx + 1, y + 1, ww - 2, 18, Component.empty());
                    box.setMaxLength(Math.max(1, f.maxLen()));
                    box.setValue(textOf(f));
                    box.setResponder(s -> store(f, s));
                    addRenderableWidget(box);
                }
            }
        }
        int by = height - 28;
        if (!schema.actions.isEmpty()) {
            int n = schema.actions.size(), w = Math.min(160, (340 - (n - 1) * 4) / n), x = width / 2 - (n * w + (n - 1) * 4) / 2;
            for (GmSchema.Action a : schema.actions) {
                addRenderableWidget(Button.builder(Component.translatable(a.labelKey()), b -> {
                    a.apply().accept(tag);
                    rebuildWidgets();
                }).bounds(x, by - 24, w, 20).build());
                x += w + 4;
            }
        }
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> {
            CoreNetwork.sendToServer(new GmPackets.Apply(pos, tag));
            onClose();
        }).bounds(width / 2 - 154, by, 150, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose()).bounds(width / 2 + 4, by, 150, 20).build());
    }

    private Component choiceText(GmSchema.Field f) {
        String v = tag.getString(f.key());
        if (v.isEmpty() && f.values().length > 0) v = f.values()[0];
        String key = "rpcore.gm." + schema.id + "." + f.key() + "." + v;
        return I18n.exists(key) ? Component.translatable(key) : Component.literal(v);
    }

    private String textOf(GmSchema.Field f) {
        return switch (f.kind()) {
            case INT -> Integer.toString(tag.getInt(f.key()));
            case FLOAT -> trim(tag.getFloat(f.key()));
            case DOUBLE -> trim(tag.getDouble(f.key()));
            default -> tag.getString(f.key());
        };
    }

    private static String trim(double d) {
        String s = String.format(Locale.ROOT, "%.3f", d);
        s = s.replaceAll("0+$", "");
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }

    private void store(GmSchema.Field f, String s) {
        try {
            switch (f.kind()) {
                case INT -> tag.putInt(f.key(), (int) Math.max(f.min(), Math.min(f.max(), Integer.parseInt(s.strip()))));
                case FLOAT -> tag.putFloat(f.key(), (float) Math.max(f.min(), Math.min(f.max(), Double.parseDouble(s.strip().replace(',', '.')))));
                case DOUBLE -> tag.putDouble(f.key(), Math.max(f.min(), Math.min(f.max(), Double.parseDouble(s.strip().replace(',', '.')))));
                default -> tag.putString(f.key(), s);
            }
        } catch (NumberFormatException ignored) {
            // недописанное число — оставляем прежнее значение
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int max = Math.max(0, visible().size() - rowsFit());
        int next = Math.max(0, Math.min(max, scroll - (int) Math.signum(delta)));
        if (next != scroll) {
            scroll = next;
            rebuildWidgets();
        }
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        for (Label l : labels) g.drawString(font, l.text(), l.x(), l.y(), 0xE0E0E0);
        int total = visible().size(), fit = rowsFit();
        if (total > fit) g.drawCenteredString(font, Component.translatable("rpcore.gm.scroll", scroll + 1, Math.min(total, scroll + fit), total),
                width / 2, height - 28 - (schema.actions.isEmpty() ? 0 : 24) - 11, 0x909090);
        super.render(g, mouseX, mouseY, partial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
