package restudio.reglass.client.gui;

import java.util.Set;
import java.util.Collections;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;
import restudio.reglass.client.screen.ReGlassConfigScreen;

/** Forge Mod List entry point; the same screen is also reachable through the unbound key. */
public class ReGlassGuiFactory implements IModGuiFactory {
    @Override public void initialize(Minecraft minecraftInstance) { }
    @Override public Class<? extends GuiScreen> mainConfigGuiClass() { return ReGlassConfigScreen.class; }
    @Override public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() { return Collections.emptySet(); }
    @Override public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) { return null; }
}
