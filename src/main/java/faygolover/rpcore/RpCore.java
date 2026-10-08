package faygolover.rpcore;

import com.mojang.logging.LogUtils;
import faygolover.rpcore.api.RpCoreAPI;
import faygolover.rpcore.command.CoreCommand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * RP Core — общая библиотека семейства RP-модов (RP Perks, RP Stamina, RP Medicine). Каждый мод семейства
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
        MinecraftForge.EVENT_BUS.addListener(CoreCommand::register);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> faygolover.rpcore.client.CoreClient.init(modBus));
    }

    private void commonSetup(FMLCommonSetupEvent e) {
        e.enqueueWork(() -> RpCoreAPI.registerModule(MODID, "общая библиотека: навыки и черты, HUD"));
    }
}
