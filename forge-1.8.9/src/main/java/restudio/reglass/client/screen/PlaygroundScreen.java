package restudio.reglass.client.screen;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;
import restudio.reglass.client.api.ReGlassApi;
import restudio.reglass.client.api.WidgetStyle;

/** A minimal, useful renderer playground: right click adds a draggable glass widget. */
public class PlaygroundScreen extends GuiScreen {
    private boolean blur = true;

    @Override
    public void initGui() {
        buttonList.clear();
        buttonList.add(new LiquidGlassButton(width / 2 - 75, height / 2 - 25, 150, 50, "ReGlass widget"));
        buttonList.add(new GuiButton(1, 10, 30, 120, 20, "Panel blur: ON"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 1) {
            blur = !blur;
            button.displayString = "Panel blur: " + (blur ? "ON" : "OFF");
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, "ReGlass Playground", width / 2, 10, 0xFFFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 1) buttonList.add(new LiquidGlassButton(mouseX - 50, mouseY - 25, 100, 50, "Liquid glass"));
    }

    private class LiquidGlassButton extends GuiButton {
        private boolean dragging;
        private int dragX, dragY;
        LiquidGlassButton(int x, int y, int w, int h, String text) { super(1000, x, y, w, h, text); }

        @Override
        public boolean mousePressed(net.minecraft.client.Minecraft mc, int x, int y) {
            if (super.mousePressed(mc, x, y)) { dragging = true; dragX = x - xPosition; dragY = y - yPosition; return true; }
            return false;
        }
        @Override
        public void mouseReleased(int x, int y) { dragging = false; }
        @Override
        public void mouseDragged(net.minecraft.client.Minecraft mc, int x, int y) {
            if (dragging) { xPosition = x - dragX; yPosition = y - dragY; }
        }
        @Override
        public void drawButton(net.minecraft.client.Minecraft mc, int mouseX, int mouseY) {
            if (!visible) return;
            mouseDragged(mc, mouseX, mouseY);
            ReGlassApi.create().fromWidget(this).cornerRadius(Math.min(width, height) * .5f)
                    .style(WidgetStyle.create().tint(0xFFAA00, .4f).blurRadius(blur ? 8 : 0)
                            .shadow(25f, .2f, 0f, 3f).smoothing(.05f).shadowColor(0, 1f))
                    .hover(mouseX >= xPosition && mouseY >= yPosition
                            && mouseX < xPosition + width && mouseY < yPosition + height ? 1f : 0f)
                    .text(displayString).render();
        }
    }
}
