package faygolover.rpcore.client.access;

import faygolover.rpcore.RpCore;
import faygolover.rpcore.access.RpAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Клиент замка семейства: ключ из {@code config/rpcore-client.toml}, фильтр вкладок креатива (вкладка без
 * предметов сама не показывается, в поиске их тоже нет) и пересборка креатива и JEI, когда доступ меняется.
 */
public final class ClientAccess {
    private ClientAccess() {}

    private static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.ConfigValue<String> KEY;

    static {
        var b = new ForgeConfigSpec.Builder();
        b.push("access");
        KEY = b.comment("Ключ ведущего RP, если выдан.").define("key", "");
        b.pop();
        SPEC = b.build();
    }

    /** Хук JEI: ставит плагин JEI, когда JEI установлен. */
    private static Runnable jeiRefresh = () -> {};

    public static void init(IEventBus modBus) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC);
        modBus.addListener(ClientAccess::onConfig);
        modBus.addListener(ClientAccess::onConfigReload);
        modBus.addListener(EventPriority.LOWEST, ClientAccess::onBuildTab);
    }

    public static void setJeiRefresh(Runnable r) {
        jeiRefresh = r;
    }

    private static void onConfig(ModConfigEvent.Loading e) {
        if (e.getConfig().getSpec() == SPEC) checkKey();
    }

    private static void onConfigReload(ModConfigEvent.Reloading e) {
        if (e.getConfig().getSpec() == SPEC) checkKey();
    }

    private static void checkKey() {
        if (!RpAccess.lockEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (RpAccess.setKey(KEY.get()) && mc != null) mc.execute(ClientAccess::refresh);
        RpCore.LOGGER.info("RP Core: доступ {}", RpAccess.isUnlocked() ? "открыт" : "закрыт");
    }

    public static void onServerGrant(boolean grant) {
        if (RpAccess.setServerGrant(grant)) refresh();
    }

    public static void onLogout() {
        if (RpAccess.setServerGrant(false)) refresh();
    }

    private static void onBuildTab(BuildCreativeModeTabContentsEvent e) {
        if (RpAccess.isUnlocked()) return;
        var it = e.getEntries().iterator();
        while (it.hasNext()) if (RpAccess.isHidden(it.next().getKey())) it.remove();
    }

    /** Доступ изменился: пересобрать вкладки креатива (если уже собраны в мире) и список JEI. */
    private static void refresh() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.getConnection() != null) {
            var params = new CreativeModeTab.ItemDisplayParameters(mc.getConnection().enabledFeatures(),
                    mc.player.canUseGameMasterBlocks() && mc.options.operatorItemsTab().get(), mc.player.level().registryAccess());
            // Как CreativeModeTabs.buildAllTabContents: сначала категории, потом поиск (он собирается из них).
            CreativeModeTabs.allTabs().stream().filter(t -> t.getType() == CreativeModeTab.Type.CATEGORY).forEach(t -> t.buildContents(params));
            CreativeModeTabs.allTabs().stream().filter(t -> t.getType() != CreativeModeTab.Type.CATEGORY).forEach(t -> t.buildContents(params));
            if (mc.screen instanceof CreativeModeInventoryScreen) mc.setScreen(null);
        }
        jeiRefresh.run();
    }
}
