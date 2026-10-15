package restudio.reglass.client.render;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.lwjgl.LWJGLException;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;

/** Opt-in OpenGL 2.1 smoke test for the actual Forge shader resources. */
public final class ShaderSmokeTest {
    private static final int WIDTH = 320;
    private static final int HEIGHT = 180;
    private Pbuffer pbuffer;
    private boolean displayCreated;
    private int sourceTexture, solidTexture, blurTexture, glassTexture, blurFbo, glassFbo;
    private int blurProgram, glassProgram;

    @Before
    public void createContext() throws Exception {
        Assume.assumeTrue("GL smoke tests disabled; use -Dreglass.glTests=true",
                Boolean.getBoolean("reglass.glTests"));
        try {
            pbuffer = new Pbuffer(WIDTH, HEIGHT, new PixelFormat(), null);
            pbuffer.makeCurrent();
        } catch (LWJGLException pbufferFailure) {
            Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT));
            Display.setLocation(-32000, -32000);
            Display.create(new PixelFormat());
            displayCreated = true;
        }
        assertTrue("OpenGL shader support unavailable", GL11.glGetString(GL11.GL_VERSION) != null);
        blurProgram = link("glass.vsh", "blur.fsh");
        glassProgram = link("glass.vsh", "glass.fsh");
        sourceTexture = checkerboardTexture();
        solidTexture = solidTexture();
        blurTexture = emptyTexture();
        glassTexture = emptyTexture();
        blurFbo = framebuffer(blurTexture);
        glassFbo = framebuffer(glassTexture);
        assertNoError("setup");
    }

    @After
    public void destroyContext() {
        if (pbuffer == null && !displayCreated) return;
        if (glassProgram != 0) GL20.glDeleteProgram(glassProgram);
        if (blurProgram != 0) GL20.glDeleteProgram(blurProgram);
        deleteFramebuffer(blurFbo);
        deleteFramebuffer(glassFbo);
        if (sourceTexture != 0) GL11.glDeleteTextures(sourceTexture);
        if (solidTexture != 0) GL11.glDeleteTextures(solidTexture);
        if (blurTexture != 0) GL11.glDeleteTextures(blurTexture);
        if (glassTexture != 0) GL11.glDeleteTextures(glassTexture);
        if (pbuffer != null) pbuffer.destroy();
        if (displayCreated && Display.isCreated()) Display.destroy();
    }

    @Test
    public void compilesAndRendersZeroBlurGlass() throws Exception {
        renderBlur(0.0f);
        renderGlass(0.0f);
        ByteBuffer pixels = BufferUtils.createByteBuffer(WIDTH * HEIGHT * 4);
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, glassFbo);
        GL11.glReadPixels(0, 0, WIDTH, HEIGHT, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        assertNoError("readback");
        int nonEmpty = 0;
        for (int i = 0; i < WIDTH * HEIGHT; i++) {
            int r = pixels.get(i * 4) & 255;
            int g = pixels.get(i * 4 + 1) & 255;
            int b = pixels.get(i * 4 + 2) & 255;
            int a = pixels.get(i * 4 + 3) & 255;
            if (r + g + b > 8 && a > 0) nonEmpty++;
        }
        int center = ((HEIGHT / 2) * WIDTH + WIDTH / 2) * 4;
        assertTrue("glass FBO readback is empty", nonEmpty > WIDTH * HEIGHT / 20);
        assertTrue("expanded-quad corner was repainted", (pixels.get(3) & 255) < 8);
        assertTrue("glass center has no alpha coverage", (pixels.get(center + 3) & 255) > 32);
    }

    @Test
    public void blurPreservesConstantColorAtRadiusZeroAndTwelve() {
        renderBlur(0.0f, solidTexture);
        int radiusZero = readBlurCenter();
        renderBlur(12.0f, solidTexture);
        int radiusTwelve = readBlurCenter();
        assertTrue("radius zero changed constant red channel", Math.abs(((radiusZero >> 16) & 255) - 100) <= 3);
        assertTrue("radius zero changed constant green channel", Math.abs(((radiusZero >> 8) & 255) - 150) <= 3);
        assertTrue("radius zero changed constant blue channel", Math.abs((radiusZero & 255) - 200) <= 3);
        assertTrue("radius twelve changed constant red channel", Math.abs(((radiusTwelve >> 16) & 255) - 100) <= 3);
        assertTrue("radius twelve changed constant green channel", Math.abs(((radiusTwelve >> 8) & 255) - 150) <= 3);
        assertTrue("radius twelve changed constant blue channel", Math.abs((radiusTwelve & 255) - 200) <= 3);
    }

    private void renderBlur(float radius) {
        renderBlur(radius, sourceTexture);
    }

    private void renderBlur(float radius, int inputTexture) {
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, blurFbo);
        GL11.glViewport(0, 0, WIDTH, HEIGHT);
        setupMatrices();
        GL20.glUseProgram(blurProgram);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, inputTexture);
        uniform1i(blurProgram, "Scene", 0);
        uniform2f(blurProgram, "TexelStep", 1.0f / WIDTH, 0.0f);
        uniform1f(blurProgram, "BlurRadius", radius);
        drawQuad(0, 0, WIDTH, HEIGHT);
        assertNoError("zero-radius blur");
    }

    private int readBlurCenter() {
        ByteBuffer pixel = BufferUtils.createByteBuffer(4);
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, blurFbo);
        GL11.glReadPixels(WIDTH / 2, HEIGHT / 2, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
        assertNoError("blur readback");
        return ((pixel.get(0) & 255) << 16) | ((pixel.get(1) & 255) << 8) | (pixel.get(2) & 255);
    }

    private void renderGlass(float blurMix) {
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, glassFbo);
        GL11.glViewport(0, 0, WIDTH, HEIGHT);
        GL11.glClearColor(0, 0, 0, 0);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        setupMatrices();
        GL20.glUseProgram(glassProgram);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, sourceTexture);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, blurTexture);
        uniform1i(glassProgram, "Scene", 0);
        uniform1i(glassProgram, "Blur", 1);
        uniform2f(glassProgram, "ScreenSize", WIDTH, HEIGHT);
        uniform2f(glassProgram, "GuiSize", WIDTH, HEIGHT);
        uniform4f(glassProgram, "Rect", 32, 24, 256, 132);
        uniform1f(glassProgram, "Radius", 20);
        uniform1f(glassProgram, "Time", 1);
        uniform1f(glassProgram, "Hover", 0);
        uniform1f(glassProgram, "Focus", 1);
        uniform1f(glassProgram, "BlurMix", blurMix);
        uniform4f(glassProgram, "Tint", 0.55f, 0.75f, 1.0f, 0.25f);
        uniform4f(glassProgram, "ShadowColor", 0.02f, 0.03f, 0.06f, 0.65f);
        uniform4f(glassProgram, "Shadow", 10, 0.8f, 2, 3);
        uniform4f(glassProgram, "Refraction", 13, 1.4f, 7, 40);
        uniform2f(glassProgram, "Fresnel", 20, 20);
        uniform4f(glassProgram, "Glare", 30, 20, 50, 80);
        uniform2f(glassProgram, "GlareLight", 90, -0.785f);
        uniform1f(glassProgram, "HoverScale", 0);
        uniform1f(glassProgram, "FocusScale", 0);
        uniform1f(glassProgram, "FocusBorderWidth", 2);
        uniform1f(glassProgram, "FocusBorderIntensity", 0.5f);
        uniform1f(glassProgram, "FocusBorderSpeed", 2);
        uniform1f(glassProgram, "PixelGrid", 0);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        drawQuad(15, 10, 305, 170);
        GL11.glDisable(GL11.GL_BLEND);
        assertNoError("glass render");
    }

    private static int link(String vertexName, String fragmentName) throws IOException {
        int vertex = compile(GL20.GL_VERTEX_SHADER, vertexName);
        int fragment = compile(GL20.GL_FRAGMENT_SHADER, fragmentName);
        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertex);
        GL20.glAttachShader(program, fragment);
        GL20.glLinkProgram(program);
        GL20.glDeleteShader(vertex);
        GL20.glDeleteShader(fragment);
        assertTrue("shader link failed: " + GL20.glGetProgramInfoLog(program, 4096),
                GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) != GL11.GL_FALSE);
        return program;
    }

    private static int compile(int type, String name) throws IOException {
        InputStream stream = ShaderSmokeTest.class.getClassLoader().getResourceAsStream("assets/reglass/shaders/" + name);
        if (stream == null) throw new IOException("missing shader resource " + name);
        String source;
        try (InputStream in = stream) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = in.read(buffer)) != -1) bytes.write(buffer, 0, count);
            source = new String(bytes.toByteArray(), "UTF-8");
        }
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        assertTrue("shader compilation failed for " + name + ": " + GL20.glGetShaderInfoLog(shader, 4096),
                GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) != GL11.GL_FALSE);
        return shader;
    }

    private static int checkerboardTexture() {
        ByteBuffer data = BufferUtils.createByteBuffer(WIDTH * HEIGHT * 4);
        for (int y = 0; y < HEIGHT; y++) for (int x = 0; x < WIDTH; x++) {
            boolean light = (((x / 20) + (y / 20)) & 1) == 0;
            data.put((byte) (light ? 185 : 45)).put((byte) (light ? 205 : 70));
            data.put((byte) (light ? 230 : 105)).put((byte) 255);
        }
        data.flip();
        return uploadTexture(data);
    }

    private static int emptyTexture() { return uploadTexture(null); }

    private static int solidTexture() {
        ByteBuffer data = BufferUtils.createByteBuffer(WIDTH * HEIGHT * 4);
        for (int i = 0; i < WIDTH * HEIGHT; i++) {
            data.put((byte) 100).put((byte) 150).put((byte) 200).put((byte) 255);
        }
        data.flip();
        return uploadTexture(data);
    }

    private static int uploadTexture(ByteBuffer data) {
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, WIDTH, HEIGHT, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, data);
        return texture;
    }

    private static int framebuffer(int texture) {
        int fbo = EXTFramebufferObject.glGenFramebuffersEXT();
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, fbo);
        EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT, GL11.GL_TEXTURE_2D, texture, 0);
        assertTrue("incomplete FBO", EXTFramebufferObject.glCheckFramebufferStatusEXT(
                EXTFramebufferObject.GL_FRAMEBUFFER_EXT) == EXTFramebufferObject.GL_FRAMEBUFFER_COMPLETE_EXT);
        return fbo;
    }

    private static void deleteFramebuffer(int fbo) {
        if (fbo != 0) EXTFramebufferObject.glDeleteFramebuffersEXT(fbo);
    }

    private static void setupMatrices() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, WIDTH, HEIGHT, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
    }

    private static void drawQuad(float left, float top, float right, float bottom) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 1); GL11.glVertex2f(left, top);
        GL11.glTexCoord2f(0, 0); GL11.glVertex2f(left, bottom);
        GL11.glTexCoord2f(1, 0); GL11.glVertex2f(right, bottom);
        GL11.glTexCoord2f(1, 1); GL11.glVertex2f(right, top);
        GL11.glEnd();
    }

    private static void uniform1i(int program, String name, int value) {
        GL20.glUniform1i(GL20.glGetUniformLocation(program, name), value);
    }
    private static void uniform1f(int program, String name, float value) {
        GL20.glUniform1f(GL20.glGetUniformLocation(program, name), value);
    }
    private static void uniform2f(int program, String name, float x, float y) {
        GL20.glUniform2f(GL20.glGetUniformLocation(program, name), x, y);
    }
    private static void uniform4f(int program, String name, float x, float y, float z, float w) {
        GL20.glUniform4f(GL20.glGetUniformLocation(program, name), x, y, z, w);
    }

    private static void assertNoError(String phase) {
        int error = GL11.glGetError();
        assertTrue("OpenGL error during " + phase + ": " + error, error == GL11.GL_NO_ERROR);
    }
}
