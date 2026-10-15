package restudio.reglass.client.render;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/** Owns a GLSL 1.20 program; compilation failures are surfaced with driver logs. */
final class ShaderProgram {
    private final int program;
    private final Map<String, Integer> uniforms = new HashMap<String, Integer>();

    ShaderProgram(String fragment) throws IOException {
        int vertex = 0, pixel = 0, linked = 0;
        try {
            vertex = compile(GL20.GL_VERTEX_SHADER, "glass.vsh");
            pixel = compile(GL20.GL_FRAGMENT_SHADER, fragment);
            linked = GL20.glCreateProgram();
            GL20.glAttachShader(linked, vertex);
            GL20.glAttachShader(linked, pixel);
            GL20.glLinkProgram(linked);
            if (GL20.glGetProgrami(linked, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                throw new IOException("ReGlass shader link failed: " + GL20.glGetProgramInfoLog(linked, 16384));
            }
            program = linked;
            linked = 0;
        } finally {
            if (vertex != 0) GL20.glDeleteShader(vertex);
            if (pixel != 0) GL20.glDeleteShader(pixel);
            if (linked != 0) GL20.glDeleteProgram(linked);
        }
    }

    private static int compile(int type, String name) throws IOException {
        String source;
        ResourceLocation location = new ResourceLocation("reglass", "shaders/" + name);
        try (InputStream in = Minecraft.getMinecraft().getResourceManager().getResource(location).getInputStream()) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = in.read(buffer)) != -1) bytes.write(buffer, 0, count);
            source = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        }
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader, 16384);
            GL20.glDeleteShader(shader);
            throw new IOException("ReGlass " + name + " compilation failed: " + log);
        }
        return shader;
    }

    void bind() { GL20.glUseProgram(program); }
    void delete() { GL20.glDeleteProgram(program); }
    private int location(String name) {
        Integer value = uniforms.get(name);
        if (value == null) {
            value = GL20.glGetUniformLocation(program, name);
            uniforms.put(name, value);
        }
        return value;
    }
    void integer(String name, int value) { GL20.glUniform1i(location(name), value); }
    void scalar(String name, float value) { GL20.glUniform1f(location(name), value); }
    void vec2(String name, float x, float y) { GL20.glUniform2f(location(name), x, y); }
    void vec4(String name, float x, float y, float z, float w) {
        GL20.glUniform4f(location(name), x, y, z, w);
    }
    void color(String name, int rgb, float alpha) {
        vec4(name, ((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, alpha);
    }
}
