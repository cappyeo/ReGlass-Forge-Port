# ReGlass for Forge 1.8.9

This target adapts ReGlass to Minecraft 1.8.9, Forge 11.15.1.2318 and Java 8.
The modern Fabric sources in the repository remain separate.

## Install

1. Install Minecraft Forge **1.8.9 – 11.15.1.2318** and run the game using Java 8.
2. Copy `build/libs/reglass-forge-1.8.9-2.1.0.jar` into the instance's `mods` directory.
3. Open **Mods → ReGlass → Config**, or assign ReGlass shortcuts in **Options → Controls**.

The config, playground and redesign toggle shortcuts are unbound by default.
Settings are stored in `config/reglass.json`. The playground supports dragging
glass panels and adding panels with the right mouse button.

The port applies glass to standard vanilla buttons and the hotbar background,
and adds a glass frame around containers. Container artwork, progress gauges,
slot contents and the selected hotbar slot remain readable. Sliders and custom
icon buttons keep their vanilla drawing. This target does not replace container
artwork or provide upstream container-text panels.

The Forge screen exposes redesign controls that have a complete 1.8.9 render
path. Sliders and container text panels keep vanilla behavior until a dedicated
track/text hook is available; they are intentionally not presented as toggles.

## Legacy API

Minecraft 1.8.9 has no `DrawContext`. Call the immediate rendering API from a GUI
draw method on the client render thread. The Forge integration resets the
backdrop cache each frame; a custom drawing phase can explicitly invalidate it
after drawing its background and before drawing glass.

```java
import restudio.reglass.client.api.ReGlassApi;
import restudio.reglass.client.api.WidgetStyle;
import restudio.reglass.client.render.LiquidGlassRenderer;

// After the screen background, before foreground text/items:
LiquidGlassRenderer.get().invalidateBackground();
LiquidGlassRenderer.get().captureBackground();

ReGlassApi.create()
    .dimensions(10, 10, 150, 40)
    .cornerRadius(12)
    .style(WidgetStyle.create()
        .tint(0xFFAA00, 0.15f)
        .blurRadius(8)
        .shadow(12, 0.2f, 0, 3)
        .shadowColor(0x000000, 1))
    .text("Liquid glass")
    .render();

// Existing GuiButton dimensions and label:
ReGlassApi.create().fromWidget(button).hover(hovered ? 1 : 0).render();
```

## Rendering and compatibility

The port uses LWJGL 2, GLSL 1.20 and framebuffer textures instead of Fabric,
modern GPU pipelines, uniform buffers or JOML. The backdrop is captured once per
frame and blurred in two half-resolution passes; text and item icons are drawn
after the glass. Resize and resource reload recreate GPU resources.

OpenGL 2.1 and framebuffer support are required for the glass effect. On shader
failure or disabled framebuffers, ordinary vanilla controls remain available;
explicit API panels use a translucent fallback. Shader errors go into the game
log. Resource reload retries shader compilation.

This is a legacy adaptation rather than binary compatibility with the Fabric
API: labels are strings, widgets are `GuiButton`, and rendering is immediate.
Draw order determines stacking; upstream batched smooth-union merging and layer
sorting are not available. Style `smoothing`, `layer`, and `fadeKey` retain their
metadata for callers; this renderer does not apply upstream batch merging,
sorting or automatic fades. Screens introduced after 1.8.9 (for example the recipe
book, loom and stonecutter) do not exist in this target. Other mods with custom
widgets or container renderers retain their own drawing unless explicitly
supported. Compatibility with OptiFine or shader packs needs testing in the
actual modpack.

Build commands and the exact toolchain are in [BUILDING.md](BUILDING.md).
