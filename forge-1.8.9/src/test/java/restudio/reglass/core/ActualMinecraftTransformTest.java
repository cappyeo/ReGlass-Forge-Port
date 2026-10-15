package restudio.reglass.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckClassAdapter;

/**
 * Runs the transformer against the real ForgeGradle Minecraft class bytes.
 * The synthetic transformer tests cannot catch a changed descriptor or an
 * overloaded method being selected accidentally.
 */
public final class ActualMinecraftTransformTest {
    private static final String HOOK_OWNER = "restudio/reglass/client/ClientHooks";

    @Test
    public void realGuiButtonHasButtonHookAndIsIdempotent() throws Exception {
        assertTarget("net.minecraft.client.gui.GuiButton", "drawButton", "drawButton",
                "(Lnet/minecraft/client/Minecraft;II)V");
    }

    @Test
    public void realGuiScreenHasDarkeningHookAndIsIdempotent() throws Exception {
        assertTarget("net.minecraft.client.gui.GuiScreen", "drawDefaultBackground",
                "shouldCancelScreenDarkening", "()V");
    }

    @Test
    public void realGuiContainerHasBackgroundHookAndIsIdempotent() throws Exception {
        byte[] before = load("net/minecraft/client/gui/inventory/GuiContainer.class");
        byte[] once = transform("net.minecraft.client.gui.inventory.GuiContainer", before);
        assertHook(once, "drawContainerPanel", "GuiContainer background hook missing");
        assertVerified(once);
        assertArrayEquals("container transformation is not idempotent", once,
                transform("net.minecraft.client.gui.inventory.GuiContainer", once));
    }

    @Test
    public void realGuiIngameHasHotbarHooksAndIsIdempotent() throws Exception {
        byte[] before = load("net/minecraft/client/gui/GuiIngame.class");
        byte[] once = transform("net.minecraft.client.gui.GuiIngame", before);
        ClassNode node = read(once);
        int beginHooks = 0;
        int textureHooks = 0;
        for (MethodNode method : node.methods) for (AbstractInsnNode instruction = method.instructions.getFirst();
                instruction != null; instruction = instruction.getNext()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (!HOOK_OWNER.equals(call.owner)) continue;
            if ("beginHotbarRender".equals(call.name)) beginHooks++;
            if ("drawHotbarTexture".equals(call.name)) textureHooks++;
        }
        assertTrue("GuiIngame hotbar begin hook missing", beginHooks == 1);
        assertTrue("GuiIngame should redirect both hotbar background draws", textureHooks == 2);
        assertVerified(once);
        assertArrayEquals("GuiIngame transformation is not idempotent", once,
                transform("net.minecraft.client.gui.GuiIngame", once));
    }

    private static void assertTarget(String className, String methodName, String hookName,
            String expectedDescriptor) throws Exception {
        byte[] before = load(className.replace('.', '/') + ".class");
        byte[] once = transform(className, before);
        ClassNode node = read(once);
        boolean targetSeen = false;
        boolean hookSeen = false;
        for (MethodNode method : node.methods) {
            if (methodName.equals(method.name) && expectedDescriptor.equals(method.desc)) targetSeen = true;
            for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;
                    instruction = instruction.getNext()) {
                if (instruction instanceof MethodInsnNode
                        && HOOK_OWNER.equals(((MethodInsnNode) instruction).owner)
                        && hookName.equals(((MethodInsnNode) instruction).name)) hookSeen = true;
            }
        }
        assertTrue("real target method missing: " + className + "." + methodName + expectedDescriptor, targetSeen);
        assertTrue("expected hook missing from real " + className, hookSeen);
        assertVerified(once);
        assertArrayEquals(className + " transformation is not idempotent", once,
                transform(className, once));
    }

    private static void assertHook(byte[] bytes, String name, String message) {
        boolean found = false;
        for (MethodNode method : read(bytes).methods) for (AbstractInsnNode instruction = method.instructions.getFirst();
                instruction != null; instruction = instruction.getNext()) {
            if (instruction instanceof MethodInsnNode && HOOK_OWNER.equals(((MethodInsnNode) instruction).owner)
                    && name.equals(((MethodInsnNode) instruction).name)) found = true;
        }
        assertTrue(message, found);
    }

    private static byte[] transform(String className, byte[] bytes) {
        byte[] output = new ReGlassClassTransformer().transform(className, className, bytes);
        assertNotNull("transformer returned null for " + className, output);
        assertFalse("transformer unexpectedly erased " + className, output.length == 0);
        return output;
    }

    private static byte[] load(String resource) throws IOException {
        InputStream stream = ActualMinecraftTransformTest.class.getClassLoader().getResourceAsStream(resource);
        assertNotNull("Minecraft class resource missing from test runtime: " + resource, stream);
        try (InputStream in = stream) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) != -1) bytes.write(buffer, 0, count);
            return bytes.toByteArray();
        }
    }

    private static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static void assertVerified(byte[] bytes) {
        StringWriter output = new StringWriter();
        CheckClassAdapter.verify(new ClassReader(bytes), false, new PrintWriter(output));
        assertFalse("transformed Minecraft class failed ASM verification: " + output,
                output.toString().contains("AnalyzerException"));
    }
}
