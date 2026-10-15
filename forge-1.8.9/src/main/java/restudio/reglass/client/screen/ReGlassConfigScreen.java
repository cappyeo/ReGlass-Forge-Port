package restudio.reglass.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import restudio.reglass.client.api.ReGlassConfig;
import restudio.reglass.client.config.ReGlassSettingsIO;

public class ReGlassConfigScreen extends GuiScreen {
    private final GuiScreen parent;
    private final List<GuiButton> toggles = new ArrayList<GuiButton>();
    public ReGlassConfigScreen(GuiScreen parent) { this.parent = parent; }

    @Override public void initGui() {
        toggles.clear();
        addToggle(0, "Redesign", ReGlassConfig.INSTANCE.features.enableRedesign);
        addToggle(1, "Buttons", ReGlassConfig.INSTANCE.features.buttons);
        addToggle(2, "Hotbar", ReGlassConfig.INSTANCE.features.hotbar);
        addToggle(3, "Containers", ReGlassConfig.INSTANCE.features.containers);
        addToggle(4, "Cancel darkening", ReGlassConfig.INSTANCE.features.cancelScreenDarkening);
        buttonList.add(new GuiButton(100, width / 2 - 100, height - 35, 200, 20, "Done"));
    }
    private void addToggle(int id, String label, boolean value) {
        int columns = width >= 420 ? 2 : 1;
        int column = columns == 1 ? 0 : id / 4;
        int row = columns == 1 ? id : id % 4;
        int buttonWidth = columns == 1 ? 200 : 155;
        int x = columns == 1 ? width / 2 - buttonWidth / 2 : width / 2 - 160 + column * 165;
        GuiButton b = new GuiButton(id, x, 35 + row * 24, buttonWidth, 20, label + ": " + (value ? "ON" : "OFF"));
        toggles.add(b); buttonList.add(b);
    }
    @Override protected void actionPerformed(GuiButton button) {
        if (button.id == 100) { ReGlassSettingsIO.saveFromMemory(); mc.displayGuiScreen(parent); return; }
        if (button.id >= 0 && button.id < toggles.size()) {
            switch (button.id) {
                case 0: ReGlassConfig.INSTANCE.features.enableRedesign = !ReGlassConfig.INSTANCE.features.enableRedesign; break;
                case 1: ReGlassConfig.INSTANCE.features.buttons = !ReGlassConfig.INSTANCE.features.buttons; break;
                case 2: ReGlassConfig.INSTANCE.features.hotbar = !ReGlassConfig.INSTANCE.features.hotbar; break;
                case 3: ReGlassConfig.INSTANCE.features.containers = !ReGlassConfig.INSTANCE.features.containers; break;
                case 4: ReGlassConfig.INSTANCE.features.cancelScreenDarkening = !ReGlassConfig.INSTANCE.features.cancelScreenDarkening; break;
                default: break;
            }
            button.displayString = button.displayString.substring(0, button.displayString.indexOf(':') + 2)
                    + (isEnabled(button.id) ? "ON" : "OFF");
        }
    }
    private boolean isEnabled(int id) {
        switch (id) {
            case 0: return ReGlassConfig.INSTANCE.features.enableRedesign;
            case 1: return ReGlassConfig.INSTANCE.features.buttons;
            case 2: return ReGlassConfig.INSTANCE.features.hotbar;
            case 3: return ReGlassConfig.INSTANCE.features.containers;
            case 4: return ReGlassConfig.INSTANCE.features.cancelScreenDarkening;
            default: return false;
        }
    }
    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground(); drawCenteredString(fontRendererObj, "ReGlass settings", width / 2, 12, 0xFFFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    @Override public void onGuiClosed() { ReGlassSettingsIO.saveFromMemory(); }
}
