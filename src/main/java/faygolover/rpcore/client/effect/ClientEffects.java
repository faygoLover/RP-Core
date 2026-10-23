package faygolover.rpcore.client.effect;

import faygolover.rpcore.effect.EffectSync;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

/** Клиент: эффекты RP Core на сущностях вокруг (их присылает сервер — {@link EffectSync}), слой глаз, помехи. */
public final class ClientEffects {
    private ClientEffects() {}

    private static final Int2IntMap MASKS = new Int2IntOpenHashMap();

    public static void init(IEventBus modBus) {
        CoreTechniques.init();
        modBus.addListener(ClientEffects::onLayers);
        MinecraftForge.EVENT_BUS.addListener(Interference::onRenderLevel);
        MinecraftForge.EVENT_BUS.addListener(Interference::onClientTick);
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> clear());
        // Смена измерения: старые id сущностей больше не значат ничего; сервер пришлёт заново, когда начнёт показывать.
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.level.LevelEvent.Unload e) -> {
            if (e.getLevel().isClientSide()) clear();
        });
    }

    public static synchronized void set(int entityId, int mask) {
        if (mask == 0) MASKS.remove(entityId);
        else MASKS.put(entityId, mask);
    }

    static synchronized int mask(Entity e) {
        return MASKS.getOrDefault(e.getId(), 0);
    }

    static synchronized int[][] entries() {
        int[][] out = new int[MASKS.size()][];
        int i = 0;
        for (Int2IntMap.Entry en : MASKS.int2IntEntrySet()) out[i++] = new int[]{en.getIntKey(), en.getIntValue()};
        return out;
    }

    static synchronized void clear() {
        MASKS.clear();
    }

    private static void onLayers(EntityRenderersEvent.AddLayers e) {
        for (String skin : e.getSkins()) {
            if (e.getSkin(skin) instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer r) r.addLayer(new RedEyesLayer(r));
        }
    }
}
