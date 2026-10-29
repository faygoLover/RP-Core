package faygolover.rpcore.client;

import faygolover.rpcore.client.hud.HudEditorScreen;
import faygolover.rpcore.client.hud.HudRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

/** Клиент RP Core: общий слой HUD, команда {@code /rphud} — редактор HUD всех модов семейства, фото игроков, «ГМские штуки», пустая рука. */
public final class CoreClient {
    private CoreClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(CoreClient::onOverlays);
        MinecraftForge.EVENT_BUS.addListener(CoreClient::onClientCommands);
        MinecraftForge.EVENT_BUS.addListener(faygolover.rpcore.client.photo.SkinCapture::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(CoreClient::onLogout);
        faygolover.rpcore.client.access.ClientAccess.init(modBus);
        faygolover.rpcore.client.gm.GmClient.init(modBus);
        faygolover.rpcore.client.effect.ClientEffects.init(modBus);
        modBus.addListener(EmptyHand::onKeys);
        modBus.addListener(ShowLogins::onKeys);
        MinecraftForge.EVENT_BUS.addListener(ShowLogins::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(ShowLogins::onNameTag);
        MinecraftForge.EVENT_BUS.addListener(ShowLogins::onChat);
        MinecraftForge.EVENT_BUS.addListener(EmptyHand::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(EmptyHand::onRenderHand);
    }

    private static void onLogout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut e) {
        faygolover.rpcore.client.photo.ClientPhotos.clear();
        faygolover.rpcore.client.photo.ClientPictures.clear();
        faygolover.rpcore.profile.ProfileServer.clearClient();
        faygolover.rpcore.client.access.ClientAccess.onLogout();
        PostureOverlay.clear();
        faygolover.rpcore.pose.PostureView.clear();
    }

    private static void onOverlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("posture_sleep", PostureOverlay::render);
        e.registerAboveAll("hud", HudRegistry::render);
    }

    private static void onClientCommands(RegisterClientCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("rphud").executes(c -> {
            openHudEditor();
            return 1;
        }));
    }

    /** Открыть редактор HUD (после закрытия чата, иначе экран чата его сразу закроет). */
    public static void openHudEditor() {
        Minecraft mc = Minecraft.getInstance();
        mc.tell(() -> mc.setScreen(new HudEditorScreen()));
    }
}
