package restudio.reglass.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import restudio.reglass.client.api.ReGlassApi;
import restudio.reglass.client.api.ReGlassConfig;
import restudio.reglass.client.api.WidgetStyle;
import restudio.reglass.client.render.LiquidGlassRenderer;

/** Static, side-safe hooks used by the optional Forge coremod transformer. */
public final class ClientHooks {
    private static final Logger LOGGER = LogManager.getLogger("ReGlass/Hooks");
    private static final ThreadLocal<HotbarState> HOTBAR = new ThreadLocal<HotbarState>();
    private ClientHooks() { }
    public static boolean drawButton(GuiButton button, Minecraft minecraft, int mouseX, int mouseY) {
        return ReGlassClient.drawButton(button, minecraft, mouseX, mouseY);
    }

    public static boolean shouldCancelScreenDarkening(GuiScreen screen) {
        return !(screen instanceof ReGlassOptOut) && !ReGlassConfig.INSTANCE.features.isClassExcluded(screen.getClass())
                && !(screen instanceof GuiMainMenu)
                && Minecraft.getMinecraft().theWorld != null
                && ReGlassConfig.INSTANCE.features.enableRedesign
                && LiquidGlassRenderer.get().isAvailable()
                && ReGlassConfig.INSTANCE.features.cancelScreenDarkening;
    }

    /** Drawn after a container background and before slots/items, so item rendering stays vanilla. */
    public static void drawContainerPanel(GuiContainer container) {
        if (container instanceof ReGlassOptOut || ReGlassConfig.INSTANCE.features.isClassExcluded(container.getClass())
                || !ReGlassConfig.INSTANCE.features.enableRedesign || !ReGlassConfig.INSTANCE.features.containers
                || !LiquidGlassRenderer.get().isAvailable()) return;
        try {
            int left = field(container, "guiLeft", "field_147003_i");
            int top = field(container, "guiTop", "field_147009_r");
            int width = field(container, "xSize", "field_146999_f");
            int height = field(container, "ySize", "field_147000_g");
            // The vanilla background layer also paints furnace progress, brewing flames,
            // enchantment bars and other live gauges. Keep that content readable by
            // drawing only a thin glass frame around the panel after the layer.
            WidgetStyle frame = WidgetStyle.create().tint(0x16161E, .12f).blurRadius(0)
                    .shadow(4f, .10f, 0, 1).shadowColor(0, .45f);
            ReGlassApi.create().dimensions(left - 2, top - 2, width + 4, 2).cornerRadius(2).style(frame).render();
            ReGlassApi.create().dimensions(left - 2, top + height, width + 4, 2).cornerRadius(2).style(frame).render();
            ReGlassApi.create().dimensions(left - 2, top, 2, height).cornerRadius(2).style(frame).render();
            ReGlassApi.create().dimensions(left + width, top, 2, height).cornerRadius(2).style(frame).render();
        } catch (Exception error) { LOGGER.warn("Could not locate GuiContainer bounds; keeping vanilla container", error); }
    }

    /** Starts the vanilla GuiIngame hotbar method without changing its item draw order. */
    public static void beginHotbarRender(GuiIngame gui) {
        HOTBAR.set(new HotbarState(gui));
    }

    /** Replacement for only the two hotbar background texture calls. */
    public static void drawHotbarTexture(GuiIngame ignored, int x, int y, int u, int v, int width, int height) {
        HotbarState state = HOTBAR.get();
        if (state == null) return;
        int index = state.backgroundCalls++;
        if (ReGlassConfig.INSTANCE.features.enableRedesign && ReGlassConfig.INSTANCE.features.hotbar
                && LiquidGlassRenderer.get().isAvailable()) {
            if (index == 0) drawHotbarGlass();
            else if (index == 1) state.drawVanilla(x, y, u, v, width, height);
            return;
        }
        state.drawVanilla(x, y, u, v, width, height);
    }

    private static void drawHotbarGlass() {
        Minecraft mc = Minecraft.getMinecraft();
        int scaledWidth = new net.minecraft.client.gui.ScaledResolution(mc).getScaledWidth();
        int scaledHeight = new net.minecraft.client.gui.ScaledResolution(mc).getScaledHeight();
        ReGlassApi.create().dimensions((scaledWidth - 182) / 2, scaledHeight - 22, 182, 22).cornerRadius(5)
                .style(WidgetStyle.create().tint(0x101018, .15f).blurRadius(0)
                        .shadow(4f, .12f, 0, 1).shadowColor(0, .45f)).render();
    }

    private static final class HotbarState {
        private final GuiIngame gui;
        private int backgroundCalls;
        HotbarState(GuiIngame gui) { this.gui = gui; }
        void drawVanilla(int x, int y, int u, int v, int width, int height) {
            gui.drawTexturedModalRect(x, y, u, v, width, height);
        }
    }

    private static int field(Object object, String mcp, String srg) throws IllegalAccessException {
        return ReflectionHelper.findField(GuiContainer.class, mcp, srg).getInt(object);
    }
}
