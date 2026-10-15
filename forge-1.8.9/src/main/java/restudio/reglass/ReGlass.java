package restudio.reglass;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import restudio.reglass.proxy.CommonProxy;

@Mod(modid = ReGlass.MOD_ID, name = "ReGlass", version = "2.1.0",
        acceptedMinecraftVersions = "[1.8.9]", acceptableRemoteVersions = "*",
        guiFactory = "restudio.reglass.client.gui.ReGlassGuiFactory")
public final class ReGlass {
    public static final String MOD_ID = "reglass";

    @Mod.Instance(MOD_ID)
    public static ReGlass INSTANCE;

    @SidedProxy(clientSide = "restudio.reglass.proxy.ClientProxy", serverSide = "restudio.reglass.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) { proxy.preInit(event); }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) { proxy.init(event); }
}
