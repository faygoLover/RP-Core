package faygolover.rpcore.client.gm;

import faygolover.rpcore.gm.GmMarkerSet;
import faygolover.rpcore.gm.GmRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> GmMarkerSet.clear());
    }

    private static void onRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(GmRegistry.HOLDER_BE.get(), HolderRenderer::new);
    }

    private static void onSetup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            GmSchema.register(GmRegistry.HIDDEN_PLATE.get(), new GmSchema("plate")
                    .text("name", 32)
                    .choice("trigger", "players", "non_gm", "living")
                    .choice("action", "none", "teleport", "anchor", "command")
                    .when("action", "teleport").coord("tx").coord("ty").coord("tz")
                    .decimal("tyaw", -180, 180).decimal("tpitch", -90, 90).text("tdim", 128)
                    .when("action", "anchor").text("anchor", 32)
                    .when("action", "teleport", "anchor").bool("keepView").bool("keepOffset")
                    .when("action", "command").text("command", 256).integer("cmdLevel", 0, 4)
                    .always().integer("cooldown", 0, 72000)
                    .choice("redstone", "hold", "off", "pulse")
                    .when("redstone", "hold", "pulse", "").integer("power", 1, 15)
                    .when("redstone", "pulse").integer("pulse", 1, 1200)
                    .always().bool("sound")
                    .action("rpcore.gm.plate.here", GmClient::hereAsTarget));
            GmSchema.register(GmRegistry.WALL_HOLDER.get(), new GmSchema("holder")
                    .choice("render", "flat", "block")
                    .decimal("scale", 0.1, 4).decimal("rot", -180, 180).decimal("spin", -720, 720)
                    .bool("collide").choice("takeBy", "all", "gm").integer("respawn", 0, 604800));
            GmSchema.register(GmRegistry.VACUUM_HOLDER.get(), new GmSchema("holder")
                    .choice("render", "item", "flat", "block")
                    .decimal("scale", 0.1, 4).decimal("rot", -180, 180).decimal("tilt", -90, 90).decimal("spin", -720, 720)
                    .bool("bob").bool("collide").choice("takeBy", "all", "gm").integer("respawn", 0, 604800));
        });
    }

    /** «Сюда: моё место» — цель телепорта пластины = где стоит и куда смотрит ведущий. */
    private static void hereAsTarget(net.minecraft.nbt.CompoundTag t) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        t.putString("action", "teleport");
        t.putDouble("tx", Math.round(p.getX() * 100) / 100.0);
        t.putDouble("ty", Math.round(p.getY() * 100) / 100.0);
        t.putDouble("tz", Math.round(p.getZ() * 100) / 100.0);
        t.putFloat("tyaw", Math.round(net.minecraft.util.Mth.wrapDegrees(p.getYRot()) * 10) / 10f);
        t.putFloat("tpitch", Math.round(p.getXRot() * 10) / 10f);
        t.putString("tdim", p.level().dimension().location().toString());
    }
}
