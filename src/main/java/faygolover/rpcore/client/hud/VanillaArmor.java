package faygolover.rpcore.client.hud;

import faygolover.rpcore.RpCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

/**
 * Ванильные единицы брони (02.11, Алекс): у нас броня по зонам, поэтому полоска скрыта по умолчанию. Ванильный слой
 * не рисуется никогда; вместо него — элемент общего HUD, который можно включить (ПКМ) и подвинуть в {@code /rphud}.
 *
 * @since 0.12.0
 */
public final class VanillaArmor implements HudElement {
    private static final ResourceLocation ID = new ResourceLocation(RpCore.MODID, "vanilla_armor");
    private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");

    /** Ванильный слой брони — убрать; рисует элемент {@link VanillaArmor}, если игрок его включил. */
    public static void onOverlay(RenderGuiOverlayEvent.Pre e) {
        if (e.getOverlay().id().equals(VanillaGuiOverlay.ARMOR_LEVEL.id())) e.setCanceled(true);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Component title() {
        return Component.translatable("rpcore.hud.element.vanilla_armor");
    }

    @Override
    public int width() {
        return 81;
    }

    @Override
    public int height() {
        return 9;
    }

    /** Как у ванили: над сердцами слева от середины. */
    @Override
    public Anchor defaultAnchor() {
        return Anchor.BOTTOM_CENTER;
    }

    @Override
    public int defaultX() {
        return -51;
    }

    @Override
    public int defaultY() {
        return -40;
    }

    @Override
    public boolean defaultVisible() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, float partialTick, boolean preview) {
        Minecraft mc = Minecraft.getInstance();
        int armor = preview ? 14 : mc.player != null ? mc.player.getArmorValue() : 0;
        if (armor <= 0 || !preview && (mc.gameMode == null || !mc.gameMode.canHurtPlayer())) return;
        for (int k = 0; k < 10; k++) {
            int u = k * 2 + 1 < armor ? 34 : k * 2 + 1 == armor ? 25 : 16;
            g.blit(ICONS, k * 8, 0, u, 9, 9, 9);
        }
    }
}
