package faygolover.rpcore.client.gm;

import faygolover.rpcore.gm.GmMarkerSet;
import faygolover.rpcore.gm.GmRegistry;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Клиент «ГМских штук» RP Core: отрисовка держателей, подсветка ведущему, окна настроек пластины и держателей. */
public final class GmClient {
    private GmClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(GmClient::onRenderers);
        modBus.addListener(GmClient::onSetup);
        MinecraftForge.EVENT_BUS.addListener(GmOverlay::onHighlight);
        MinecraftForge.EVENT_BUS.addListener(GmOverlay::onRenderLevel);
        MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.LOWEST, GmOverlay::onRenderLabels);
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> GmMarkerSet.clear());
    }

    private static void onRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(GmRegistry.HOLDER_BE.get(), HolderRenderer::new);
    }

    private static void onSetup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            GmSchema.register(GmRegistry.HIDDEN_PLATE.get(), new GmSchema("plate")
                    .text("name", 32)
                    .decimal("yaw", -180, 180).decimal("width", 0.5, 32).decimal("length", 0.5, 32)
                    .choice("trigger", "players", "non_gm", "living")
                    .choice("action", "none", "teleport", "anchor", "command")
                    .when("action", "teleport").coord("tx").coord("ty").coord("tz")
                    .decimal("tyaw", -180, 180).decimal("tpitch", -90, 90).text("tdim", 128)
                    .when("action", "anchor").text("anchor", 32)
                    .when("action", "teleport", "anchor").choice("view", "target", "keep").bool("keepOffset")
                    .when("action", "command").text("command", 256).integer("cmdLevel", 0, 4)
                    .always().integer("cooldown", 0, 72000)
                    .choice("redstone", "hold", "off", "pulse")
                    .when("redstone", "hold", "pulse", "").integer("power", 1, 15)
                    .when("redstone", "pulse").integer("pulse", 1, 1200)
                    .always().bool("sound"));
            GmSchema.register(GmRegistry.WALL_HOLDER.get(), new GmSchema("holder")
                    .integer("count", 1, 64)
                    .choice("render", "flat", "block")
                    .decimal("scale", 0.1, 4).decimal("rot", -180, 180).decimal("spin", -720, 720)
                    .bool("collide").choice("takeBy", "all", "gm").integer("respawn", 0, 604800));
            GmSchema.register(GmRegistry.VACUUM_HOLDER.get(), new GmSchema("holder")
                    .integer("count", 1, 64)
                    .choice("render", "item", "flat", "block")
                    .decimal("scale", 0.1, 4).decimal("rot", -180, 180).decimal("tilt", -90, 90).decimal("spin", -720, 720)
                    .bool("bob").bool("collide").choice("takeBy", "all", "gm").integer("respawn", 0, 604800));
        });
    }
}
