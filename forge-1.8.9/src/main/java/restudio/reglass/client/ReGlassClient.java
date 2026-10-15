package restudio.reglass.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptionButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import restudio.reglass.client.api.ReGlassConfig;
import restudio.reglass.client.config.ReGlassSettingsIO;
import restudio.reglass.client.render.LiquidGlassRenderer;
import restudio.reglass.client.screen.ReGlassConfigScreen;
import restudio.reglass.client.screen.PlaygroundScreen;

/** Forge 1.8.9 client entrypoint. Every method is reached only from the client proxy. */
public final class ReGlassClient {
    public static final KeyBinding CONFIG_KEY = new KeyBinding("key.reglass.config", Keyboard.KEY_NONE, "key.categories.reglass");
    public static final KeyBinding PLAYGROUND_KEY = new KeyBinding("key.reglass.playground", Keyboard.KEY_NONE, "key.categories.reglass");
    public static final KeyBinding TOGGLE_REDESIGN_KEY = new KeyBinding("key.reglass.toggle_redesign", Keyboard.KEY_NONE, "key.categories.reglass");
    private static boolean keysReady;
    private static boolean redesignWasDown;

    public static void initKeys() { keysReady = true; }

    @SubscribeEvent
    public void keyInput(InputEvent.KeyInputEvent event) {
        handleKeys();
    }

    @SubscribeEvent
    public void screenKeyInput(GuiScreenEvent.KeyboardInputEvent.Post event) {
        handleKeys();
    }

    private static void handleKeys() {
        if (!keysReady) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (CONFIG_KEY.isPressed() && mc.currentScreen == null) mc.displayGuiScreen(new ReGlassConfigScreen(null));
        if (PLAYGROUND_KEY.isPressed() && mc.currentScreen == null) mc.displayGuiScreen(new PlaygroundScreen());
        int toggleCode = TOGGLE_REDESIGN_KEY.getKeyCode();
        // GuiScreen consumes key state before Minecraft updates KeyBinding state.
        boolean down = toggleCode > 0 ? Keyboard.isKeyDown(toggleCode)
                : toggleCode < 0 && toggleCode + 100 >= 0 && org.lwjgl.input.Mouse.isButtonDown(toggleCode + 100);
        if (down && !redesignWasDown) {
            ReGlassConfig.INSTANCE.features.enableRedesign = !ReGlassConfig.INSTANCE.features.enableRedesign;
            ReGlassSettingsIO.saveFromMemory();
        }
        redesignWasDown = down;
    }

    @SubscribeEvent
    public void renderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) LiquidGlassRenderer.get().beginFrame();
    }

    @SubscribeEvent
    public void guiOpened(GuiOpenEvent event) {
        // The Forge hook deliberately does not replace vanilla GUI/button objects.
        LiquidGlassRenderer.get().invalidateBackground();
    }

    @SubscribeEvent
    public void backgroundDrawn(GuiScreenEvent.BackgroundDrawnEvent event) {
        // Screen background and vanilla darkening are complete at this point.
        if (ReGlassConfig.INSTANCE.features.enableRedesign && !(event.gui instanceof ReGlassOptOut)
                && !ReGlassConfig.INSTANCE.features.isClassExcluded(event.gui.getClass())) {
            LiquidGlassRenderer.get().captureBackground();
        }
    }

    /** Called by the tiny GuiButton.drawButton ASM hook when present. */
    public static boolean drawButton(GuiButton button, Minecraft minecraft, int mouseX, int mouseY) {
        if (!ReGlassConfig.INSTANCE.features.enableRedesign || !ReGlassConfig.INSTANCE.features.buttons
                || !button.visible || !LiquidGlassRenderer.get().isAvailable()) return false;
        GuiScreen screen = minecraft.currentScreen;
        if (screen instanceof ReGlassOptOut || (screen != null
                && ReGlassConfig.INSTANCE.features.isClassExcluded(screen.getClass()))) return false;
        // Mod buttons and sliders retain their own rendering/drag semantics.
        if (button instanceof ReGlassOptOut || ReGlassConfig.INSTANCE.features.isClassExcluded(button.getClass())
                || !button.enabled || !(button.getClass() == GuiButton.class || button.getClass() == GuiOptionButton.class)) return false;
        boolean hovered = mouseX >= button.xPosition && mouseY >= button.yPosition
                && mouseX < button.xPosition + button.width && mouseY < button.yPosition + button.height;
        setHovered(button, hovered);
        float hover = hovered ? 1.0f : 0.0f;
        ReGlassApiBridge.drawButton(button, hover, 0.0f);
        return true;
    }

    private static java.lang.reflect.Field cachedHoverField;
    private static boolean hoverFieldUnavailable;
    private static void setHovered(GuiButton button, boolean value) {
        try {
            if (hoverFieldUnavailable) return;
            if (cachedHoverField == null) {
                cachedHoverField = net.minecraftforge.fml.relauncher.ReflectionHelper.findField(
                        GuiButton.class, "hovered", "field_146123_n");
            }
            cachedHoverField.setBoolean(button, value);
        } catch (Exception ignored) { hoverFieldUnavailable = true; }
    }

    private static final class ReGlassApiBridge {
        private static void drawButton(GuiButton button, float hover, float focus) {
            restudio.reglass.client.api.ReGlassApi.create().fromWidget(button)
                    .hover(hover).focus(focus).text(button.displayString)
                    .textColor(button.packedFGColour != 0 ? button.packedFGColour : hover > 0 ? 0xFFFFA0 : 0xE0E0E0)
                    .render();
        }
    }
}
