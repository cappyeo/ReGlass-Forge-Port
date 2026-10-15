package restudio.reglass.core;

import java.util.Map;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

/** Registers the narrow bytecode hooks while leaving all normal Forge mod loading intact. */
@IFMLLoadingPlugin.Name("ReGlassCore")
@IFMLLoadingPlugin.MCVersion("1.8.9")
public final class ReGlassLoadingPlugin implements IFMLLoadingPlugin {
    @Override public String[] getASMTransformerClass() { return new String[] { ReGlassClassTransformer.class.getName() }; }
    @Override public String getModContainerClass() { return null; }
    @Override public String getSetupClass() { return null; }
    @Override public void injectData(Map<String, Object> data) { }
    @Override public String getAccessTransformerClass() { return null; }
}
