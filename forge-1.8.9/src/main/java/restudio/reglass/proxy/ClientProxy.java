package restudio.reglass.proxy;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.IReloadableResourceManager;
import restudio.reglass.client.render.LiquidGlassRenderer;
import org.lwjgl.input.Keyboard;
import restudio.reglass.client.ReGlassClient;
import restudio.reglass.client.config.ReGlassSettingsIO;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        ReGlassSettingsIO.setConfigDirectory(event.getModConfigurationDirectory());
        ReGlassSettingsIO.loadIntoMemory();
        ReGlassClient.initKeys();
        ClientRegistry.registerKeyBinding(ReGlassClient.CONFIG_KEY);
        ClientRegistry.registerKeyBinding(ReGlassClient.PLAYGROUND_KEY);
        ClientRegistry.registerKeyBinding(ReGlassClient.TOGGLE_REDESIGN_KEY);
        ReGlassClient client = new ReGlassClient();
        MinecraftForge.EVENT_BUS.register(client);
        FMLCommonHandler.instance().bus().register(client);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        if (Minecraft.getMinecraft().getResourceManager() instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(new IResourceManagerReloadListener() {
                @Override public void onResourceManagerReload(IResourceManager manager) { LiquidGlassRenderer.get().reload(); }
            });
        }
    }
}
