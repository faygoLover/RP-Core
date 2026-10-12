package faygolover.rpcore.client;

import faygolover.rpcore.client.hud.HudEditorScreen;
import faygolover.rpcore.client.hud.HudRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

/** Клиент RP Core: общий слой HUD, команда {@code /rphud} — редактор HUD всех модов семейства, фото игроков. */
public final class CoreClient {
    private CoreClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(CoreClient::onOverlays);
        MinecraftForge.EVENT_BUS.addListener(CoreClient::onClientCommands);
        MinecraftForge.EVENT_BUS.addListener(faygolover.rpcore.client.photo.SkinCapture::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(CoreClient::onLogout);
    }

    private static void onLogout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut e) {
        faygolover.rpcore.client.photo.ClientPhotos.clear();
    }

    private static void onOverlays(RegisterGuiOverlaysEvent e) {
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
