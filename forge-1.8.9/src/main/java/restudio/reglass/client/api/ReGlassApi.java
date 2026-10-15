package restudio.reglass.client.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import restudio.reglass.client.render.LiquidGlassRenderer;

/** Immediate-mode counterpart of the upstream API for Minecraft 1.8.9. */
public final class ReGlassApi {
    public static WidgetStyle inactiveStyle = WidgetStyle.create().tint(0x000000, 0.3f);

    private ReGlassApi() {}

    public static ReGlassConfig getGlobalConfig() { return ReGlassConfig.INSTANCE; }
    public static Builder create() { return new Builder(); }

    public static final class Builder {
        private int x, y, width, height;
        private float radius = -1;
        private float hover, focus;
        private String text;
        private int textColor = 0xFFFFFF;
        private WidgetStyle style = WidgetStyle.create();

        private Builder() {}

        public Builder fromWidget(GuiButton button) {
            return dimensions(button.xPosition, button.yPosition, button.width, button.height)
                    .text(button.displayString);
        }
        public Builder position(int x, int y) { this.x = x; this.y = y; return this; }
        public Builder size(int width, int height) { this.width = width; this.height = height; return this; }
        public Builder dimensions(int x, int y, int width, int height) {
            return position(x, y).size(width, height);
        }
        public Builder cornerRadius(float radius) { this.radius = radius; return this; }
        public Builder text(String text) { this.text = text; return this; }
        public Builder textColor(int color) { this.textColor = color; return this; }
        public Builder style(WidgetStyle style) {
            if (style == null) throw new IllegalArgumentException("style must not be null");
            this.style = style;
            return this;
        }
        public Builder hover(float amount) { hover = unit(amount); return this; }
        public Builder focus(float amount) { focus = unit(amount); return this; }
        public Builder selected(float amount) { return focus(amount); }

        public void render() {
            if (width <= 0 || height <= 0) return;
            float corner = radius < 0 ? Math.min(width, height) * 0.5f : radius;
            LiquidGlassRenderer.get().render(x, y, width, height, corner, style, hover, focus);
            if (text != null && !text.isEmpty()) {
                Minecraft mc = Minecraft.getMinecraft();
                mc.fontRendererObj.drawStringWithShadow(text,
                        x + (width - mc.fontRendererObj.getStringWidth(text)) * 0.5f,
                        y + (height - mc.fontRendererObj.FONT_HEIGHT) * 0.5f, textColor);
            }
        }

        private static float unit(float value) {
            return Float.isNaN(value) ? 0 : Math.max(0, Math.min(1, value));
        }
    }
}
