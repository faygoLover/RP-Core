package faygolover.rpcore;

import com.mojang.logging.LogUtils;
import faygolover.rpcore.api.RpCoreAPI;
import faygolover.rpcore.command.CoreCommand;
import faygolover.rpcore.command.TimeCommand;
import faygolover.rpcore.network.CoreNetwork;
import faygolover.rpcore.photo.PhotoServer;
import faygolover.rpcore.time.RpTime;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * RP Core — общая библиотека семейства RP-модов (RP Perks, RP Stamina, RP Medicine, RP Culinary, RP PDA). Каждый мод семейства
 * требует только её; друг с другом моды связываются через {@link RpCoreAPI}, поэтому работают и по
 * отдельности, и вместе.
 */
@Mod(RpCore.MODID)
public class RpCore {
    public static final String MODID = "rpcore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RpCore() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);
        faygolover.rpcore.gm.GmRegistry.register(modBus);
        faygolover.rpcore.effect.RpEffects.register(modBus);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, CoreConfig.SPEC);
        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener(CoreCommand::register);
        bus.addListener(TimeCommand::register);
        bus.addListener(faygolover.rpcore.command.PointCommand::register);
        bus.addListener(RpTime::onServerStarting);
        // Общая база напитков, еды и курева (вода, кофеин, алкоголь, никотин).
        bus.addListener((net.minecraftforge.event.AddReloadListenerEvent e) -> e.addListener(faygolover.rpcore.api.Consumables.LOADER));
        // Фото игроков (медкарта, КПК).
        bus.addListener(PhotoServer::onLogin);
        bus.addListener(PhotoServer::onLogout);
        bus.addListener(PhotoServer::onServerStopped);
        bus.addListener(faygolover.rpcore.photo.PictureServer::onLogout);
        bus.addListener(faygolover.rpcore.photo.PictureServer::onServerStopped);
        // Анкета тела.
        bus.addListener(faygolover.rpcore.profile.ProfileServer::onLogin);
        bus.addListener(faygolover.rpcore.profile.ProfileServer::onRespawn);
        bus.addListener(faygolover.rpcore.profile.ProfileServer::onDimension);
        // Замок семейства: доступ ведущего с выделенного сервера.
        bus.addListener(faygolover.rpcore.access.AccessServer::onLogin);
        bus.addListener(faygolover.rpcore.access.AccessServer::onLogout);
        bus.addListener(faygolover.rpcore.access.AccessServer::onServerTick);
        bus.addListener(faygolover.rpcore.access.AccessServer::onServerStopped);
        // Эффекты «Красные глаза» и «Помехи»: рассылка видящим, без частиц.
        bus.addListener(faygolover.rpcore.effect.EffectSync::onAdded);
        bus.addListener(faygolover.rpcore.effect.EffectSync::onRemove);
        bus.addListener(faygolover.rpcore.effect.EffectSync::onExpired);
        bus.addListener(faygolover.rpcore.effect.EffectSync::onJoin);
        bus.addListener(faygolover.rpcore.effect.EffectSync::onStartTracking);
        bus.addListener(faygolover.rpcore.effect.EffectSync::onServerTick);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> faygolover.rpcore.client.CoreClient.init(modBus));
    }

    private void commonSetup(FMLCommonSetupEvent e) {
        CoreNetwork.register();
        e.enqueueWork(() -> {
            faygolover.rpcore.effect.RpEffects.registerBrewing();
            RpCoreAPI.registerModule(MODID, "общая библиотека: навыки и черты, анкета тела, HUD, время RP, фото");
            RpCoreAPI.registerCommand(MODID, "/rpcore", "rpcore.help.rpcore", 0, false);
            RpCoreAPI.registerCommand(MODID, "/rpcore commands [мод]", "rpcore.help.commands", 0, false);
            RpCoreAPI.registerCommand(MODID, "/rpcore skill <игрок> <навык> [уровень|reset]", "rpcore.help.skill", 2, false);
            RpCoreAPI.registerCommand(MODID, "/rpcore profile <игрок> [sex|age|height <значение>|reset]", "rpcore.help.profile", 2, false);
            RpCoreAPI.registerCommand(MODID, "/rpcore point set|remove|tp <имя>, /rpcore point list", "rpcore.help.point", 2, false);
            RpCoreAPI.registerCommand(MODID, "/rpcore taste <игрок> [<любимое> <нелюбимое>|reset]", "rpcore.help.taste", 2, false);
            RpCoreAPI.registerCommand(MODID, "/rphud", "rpcore.help.rphud", 0, false);
            RpCoreAPI.registerCommand(MODID, "/rptime", "rpcore.help.rptime", 0, false);
            RpCoreAPI.registerCommand(MODID, "/rptime start|stop", "rpcore.help.rptime_start", 2, false);
            RpCoreAPI.registerCommand(MODID, "/rptime sleep <время> <игроки> [all|wounds|food] [online|all|offline]", "rpcore.help.rptime_sleep", 2, false);
            RpCoreAPI.registerCommand(MODID, "/rptime skip <время> <игроки> [all|wounds|food] [online|all|offline]", "rpcore.help.rptime_skip", 2, false);
        });
    }
}
