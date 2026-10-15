package restudio.reglass.client.render;

import java.nio.ByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.OpenGlHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;
import restudio.reglass.client.api.ReGlassConfig;
import restudio.reglass.client.api.WidgetStyle;
import restudio.reglass.client.runtime.ReGlassAnim;

/**
 * LWJGL 2 renderer. A backdrop is copied once per frame and blurred in two
 * separable half-resolution passes. Text and item icons never enter the blur.
 * All GL mutations use raw calls inside a saved state so Minecraft's cached
 * GlStateManager state and the actual driver state remain consistent.
 */
public final class LiquidGlassRenderer {
    private static final LiquidGlassRenderer INSTANCE = new LiquidGlassRenderer();
    private static final Logger LOGGER = LogManager.getLogger("ReGlass/Renderer");
    private ShaderProgram glass, blur;
    private Target scene, horizontal, vertical;
    private boolean failed, captured;
    private int blurredRadius = -1;
    private long lastFrame = System.nanoTime();
    private final long start = System.nanoTime();

    private LiquidGlassRenderer() { ReGlassAnim.INSTANCE.update(ReGlassConfig.INSTANCE, 0); }
    public static LiquidGlassRenderer get() { return INSTANCE; }

    /** Called at RenderTickEvent.START on the client thread. */
    public void beginFrame() {
        captured = false;
        blurredRadius = -1;
        long now = System.nanoTime();
        ReGlassAnim.INSTANCE.update(ReGlassConfig.INSTANCE, Math.min(0.1, (now - lastFrame) / 1.0e9));
        lastFrame = now;
    }

    public void invalidateBackground() { captured = false; blurredRadius = -1; }

    public boolean isAvailable() {
        if (failed || !GLContext.getCapabilities().OpenGL21 || !OpenGlHelper.isFramebufferEnabled()) return false;
        if (glass == null) {
            try {
                glass = new ShaderProgram("glass.fsh");
                blur = new ShaderProgram("blur.fsh");
            } catch (Exception error) {
                fail(error);
                return false;
            }
        }
        return true;
    }

    /** Snapshot the currently bound Minecraft framebuffer, before drawing glass. */
    public void captureBackground() {
        if (!isAvailable() || captured) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.displayWidth < 1 || mc.displayHeight < 1) return;
        try (State state = new State()) {
            ensureTargets(mc.displayWidth, mc.displayHeight);
            state.restoreFramebuffer();
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, scene.texture);
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, scene.width, scene.height);
            captured = true;
        } catch (Exception error) {
            fail(error);
        }
    }

    public void render(int x, int y, int width, int height, float radius,
                       WidgetStyle style, float hover, float focus) {
        if (width <= 0 || height <= 0) return;
        captureBackground();
        if (!captured || !isAvailable()) {
            fallback(x, y, width, height, style);
            return;
        }
        ScaledResolution scaled = new ScaledResolution(Minecraft.getMinecraft());
        float guiWidth = (float) scaled.getScaledWidth_double();
        float guiHeight = (float) scaled.getScaledHeight_double();
        int blurRadius = Math.max(0, Math.min(64, style.getBlurRadius()));
        try (State state = new State()) {
            if (blurRadius > 0 && blurredRadius != blurRadius) {
                // Both axes step in half-resolution pixels, even when the first input is full size.
                float pixels = Math.min(12, blurRadius * scaled.getScaleFactor() * 0.5f);
                blurPass(scene, horizontal, 1f / horizontal.width, 0, pixels, guiWidth, guiHeight);
                blurPass(horizontal, vertical, 0, 1f / horizontal.height, pixels, guiWidth, guiHeight);
                blurredRadius = blurRadius;
            }
            state.restoreFramebuffer();
            configureDraw();
            glass.bind();
            bindTexture(0, scene.texture);
            bindTexture(1, blurRadius > 0 ? vertical.texture : scene.texture);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            glass.integer("Scene", 0);
            glass.integer("Blur", 1);
            glass.vec2("ScreenSize", scene.width, scene.height);
            glass.vec2("GuiSize", guiWidth, guiHeight);
            glass.vec4("Rect", x, y, width, height);
            glass.scalar("Radius", Math.max(0, Math.min(Math.min(width, height) * 0.5f, radius)));
            glass.scalar("Time", (float) ((System.nanoTime() - start) / 1.0e9));
            glass.scalar("Hover", hover);
            glass.scalar("Focus", focus);
            glass.scalar("BlurMix", blurRadius > 0 ? 1 : 0);
            glass.color("Tint", style.getTintColor(), style.getTintAlpha());
            glass.color("ShadowColor", style.getShadowColor(), style.getShadowColorAlpha());
            glass.vec4("Shadow", style.getShadowExpand(), style.getShadowFactor(),
                    style.getShadowOffsetX(), style.getShadowOffsetY());
            glass.vec4("Refraction", style.getRefThickness(), style.getRefFactor(),
                    style.getRefDispersion(), style.getRefFresnelRange());
            glass.vec2("Fresnel", style.getRefFresnelHardness(), style.getRefFresnelFactor());
            glass.vec4("Glare", style.getGlareRange(), style.getGlareHardness(),
                    style.getGlareConvergence(), style.getGlareOppositeFactor());
            glass.vec2("GlareLight", style.getGlareFactor(), style.getGlareAngleRad());
            ReGlassConfig config = ReGlassConfig.INSTANCE;
            glass.scalar("HoverScale", ReGlassAnim.INSTANCE.hoverScalePx());
            glass.scalar("FocusScale", ReGlassAnim.INSTANCE.focusScalePx());
            glass.scalar("FocusBorderWidth", ReGlassAnim.INSTANCE.focusBorderWidthPx());
            glass.scalar("FocusBorderIntensity", ReGlassAnim.INSTANCE.focusBorderIntensity());
            glass.scalar("FocusBorderSpeed", ReGlassAnim.INSTANCE.focusBorderSpeed());
            glass.scalar("PixelGrid", config.features.pixelatedGrid ? Math.max(1, config.pixelatedGridSize) : 0);
            float margin = Math.max(2, Math.max(style.getShadowExpand(), 0) * 3
                    + Math.max(Math.abs(style.getShadowOffsetX()), Math.abs(style.getShadowOffsetY()))
                    + Math.max(config.hoverScalePx, config.focusScalePx) + 2);
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            quad(x - margin, y - margin, x + width + margin, y + height + margin);
        } catch (Exception error) {
            fail(error);
            fallback(x, y, width, height, style);
        }
    }

    /** Invoked by the resource reload listener, on Minecraft's render thread. */
    public void reload() {
        if (glass != null) glass.delete();
        if (blur != null) blur.delete();
        glass = null;
        blur = null;
        releaseTargets();
        failed = false;
        invalidateBackground();
    }

    private void fail(Exception error) {
        if (!failed) LOGGER.error("Liquid glass disabled; keeping vanilla GUI rendering. Reload resources to retry.", error);
        failed = true;
        captured = false;
    }

    private void ensureTargets(int width, int height) {
        if (scene != null && scene.width == width && scene.height == height) return;
        releaseTargets();
        scene = new Target(width, height, false);
        horizontal = new Target(Math.max(1, width / 2), Math.max(1, height / 2), true);
        vertical = new Target(horizontal.width, horizontal.height, true);
        blurredRadius = -1;
    }

    private void releaseTargets() {
        if (scene != null) scene.delete();
        if (horizontal != null) horizontal.delete();
        if (vertical != null) vertical.delete();
        scene = horizontal = vertical = null;
    }

    private void blurPass(Target input, Target output, float dx, float dy, float radius,
                          float guiWidth, float guiHeight) {
        output.bind();
        configureDraw();
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, guiWidth, guiHeight, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        blur.bind();
        bindTexture(0, input.texture);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        blur.integer("Scene", 0);
        blur.vec2("TexelStep", dx, dy);
        blur.scalar("BlurRadius", radius);
        quad(0, 0, guiWidth, guiHeight);
    }

    private static void configureDraw() {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glColorMask(true, true, true, true);
        GL11.glColor4f(1, 1, 1, 1);
    }

    private static void bindTexture(int unit, int texture) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
    }

    private static void quad(float left, float top, float right, float bottom) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 1); GL11.glVertex2f(left, top);
        GL11.glTexCoord2f(0, 0); GL11.glVertex2f(left, bottom);
        GL11.glTexCoord2f(1, 0); GL11.glVertex2f(right, bottom);
        GL11.glTexCoord2f(1, 1); GL11.glVertex2f(right, top);
        GL11.glEnd();
    }

    private static void fallback(int x, int y, int width, int height, WidgetStyle style) {
        int alpha = Math.max(70, Math.min(255, (int) (style.getTintAlpha() * 255)));
        Gui.drawRect(x, y, x + width, y + height, (alpha << 24) | (style.getTintColor() & 0xFFFFFF));
    }

    private static final class Target {
        final int width, height, texture;
        int framebuffer;

        Target(int width, int height, boolean attach) {
            this.width = width;
            this.height = height;
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            texture = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            if (attach) {
                framebuffer = OpenGlHelper.glGenFramebuffers();
                OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, framebuffer);
                OpenGlHelper.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER,
                        OpenGlHelper.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texture, 0);
                int status = OpenGlHelper.glCheckFramebufferStatus(OpenGlHelper.GL_FRAMEBUFFER);
                if (status != OpenGlHelper.GL_FRAMEBUFFER_COMPLETE) {
                    delete();
                    throw new IllegalStateException("ReGlass framebuffer incomplete: " + status);
                }
            }
        }
        void bind() {
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, framebuffer);
            GL11.glViewport(0, 0, width, height);
        }
        void delete() {
            if (framebuffer != 0) OpenGlHelper.glDeleteFramebuffers(framebuffer);
            GL11.glDeleteTextures(texture);
        }
    }

    private static final class State implements AutoCloseable {
        final int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        final int framebuffer = GL11.glGetInteger(0x8CA6); // GL_FRAMEBUFFER_BINDING (core/ARB/EXT)
        final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        final int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        final int viewportX, viewportY, viewportWidth, viewportHeight;

        State() {
            java.nio.IntBuffer viewport = org.lwjgl.BufferUtils.createIntBuffer(16);
            GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
            viewportX = viewport.get(0); viewportY = viewport.get(1);
            viewportWidth = viewport.get(2); viewportHeight = viewport.get(3);
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
        }
        void restoreFramebuffer() {
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, framebuffer);
            GL11.glViewport(viewportX, viewportY, viewportWidth, viewportHeight);
            // Undo the blur's projection without consuming the outer saved matrices.
            GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix(); GL11.glPushMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glPushMatrix();
            GL11.glPopAttrib(); GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        }
        @Override public void close() {
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, framebuffer);
            GL20.glUseProgram(program);
            GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
            GL11.glMatrixMode(matrixMode);
            GL11.glPopAttrib();
            GL13.glActiveTexture(activeTexture);
        }
    }
}
