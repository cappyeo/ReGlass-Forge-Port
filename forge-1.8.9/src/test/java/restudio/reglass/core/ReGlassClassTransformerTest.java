package restudio.reglass.core;

import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckClassAdapter;
import java.io.PrintWriter;
import java.io.StringWriter;

/** Regression coverage for both ForgeGradle development and production descriptors. */
public class ReGlassClassTransformerTest {
    @Test public void transformsDevelopmentGuiButton() {
        assertButton("net/minecraft/client/gui/GuiButton", "drawButton",
                "(Lnet/minecraft/client/Minecraft;II)V", "net.minecraft.client.gui.GuiButton");
    }

    @Test public void transformsProductionGuiButton() {
        assertButton("avs", "a", "(Lave;II)V", "net.minecraft.client.gui.GuiButton");
    }

    @Test public void transformsProductionDarkeningMethod() {
        assertHook("axu", "c", "()V", "net.minecraft.client.gui.GuiScreen", "shouldCancelScreenDarkening");
    }

    @Test public void transformsHotbarWithoutRemovingItemOrder() {
        ClassNode input = new ClassNode();
        input.version = Opcodes.V1_8;
        input.access = Opcodes.ACC_PUBLIC;
        input.name = "avo";
        input.superName = "java/lang/Object";
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "a",
                "(Lnet/minecraft/client/gui/ScaledResolution;F)V", null, null);
        for (int i = 0; i < 2; i++) {
            method.instructions.add(new org.objectweb.asm.tree.VarInsnNode(Opcodes.ALOAD, 0));
            for (int arg = 0; arg < 6; arg++) method.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.ICONST_0));
            method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "avo", "b", "(IIIIII)V", false));
        }
        method.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
        input.methods.add(method);
        ClassWriter writer = new ClassWriter(0);
        input.accept(writer);
        byte[] output = new ReGlassClassTransformer().transform("avo", "net.minecraft.client.gui.GuiIngame", writer.toByteArray());
        ClassNode result = new ClassNode();
        new ClassReader(output).accept(result, 0);
        verify(output);
        boolean hook = false;
        int vanillaCalls = 0;
        for (MethodNode candidate : result.methods) for (org.objectweb.asm.tree.AbstractInsnNode insn = candidate.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode && "drawHotbarTexture".equals(((MethodInsnNode) insn).name)) hook = true;
            if (insn instanceof MethodInsnNode && "b".equals(((MethodInsnNode) insn).name)) vanillaCalls++;
        }
        Assert.assertTrue("hotbar hook missing", hook);
        Assert.assertEquals("only background calls should be redirected", 0, vanillaCalls);
    }

    private static void assertButton(String owner, String methodName, String descriptor, String transformedName) {
        ClassNode input = new ClassNode();
        input.version = Opcodes.V1_8;
        input.access = Opcodes.ACC_PUBLIC;
        input.name = owner;
        input.superName = "java/lang/Object";
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, methodName, descriptor, null, null);
        method.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
        input.methods.add(method);
        ClassWriter writer = new ClassWriter(0);
        input.accept(writer);

        byte[] before = writer.toByteArray();
        byte[] output = new ReGlassClassTransformer().transform(owner, transformedName, before);
        Assert.assertTrue("the target method must be transformed", output.length != before.length);
        ClassNode result = new ClassNode();
        new ClassReader(output).accept(result, 0);
        verify(output);
        boolean found = false;
        for (MethodNode candidate : result.methods) {
            for (org.objectweb.asm.tree.AbstractInsnNode insn = candidate.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof MethodInsnNode && "drawButton".equals(((MethodInsnNode) insn).name)) {
                    found = true;
                    Assert.assertEquals("restudio/reglass/client/ClientHooks", ((MethodInsnNode) insn).owner);
                    Assert.assertTrue("hook descriptor must preserve runtime owner", ((MethodInsnNode) insn).desc.contains(owner));
                }
            }
        }
        Assert.assertTrue("ClientHooks drawButton call missing for " + owner, found);
    }

    private static void assertHook(String owner, String methodName, String descriptor, String transformedName, String hookName) {
        ClassNode input = new ClassNode();
        input.version = Opcodes.V1_8;
        input.access = Opcodes.ACC_PUBLIC;
        input.name = owner;
        input.superName = "java/lang/Object";
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, methodName, descriptor, null, null);
        method.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
        input.methods.add(method);
        ClassWriter writer = new ClassWriter(0);
        input.accept(writer);
        byte[] output = new ReGlassClassTransformer().transform(owner, transformedName, writer.toByteArray());
        ClassNode result = new ClassNode();
        new ClassReader(output).accept(result, 0);
        verify(output);
        boolean found = false;
        for (MethodNode candidate : result.methods) for (org.objectweb.asm.tree.AbstractInsnNode insn = candidate.instructions.getFirst(); insn != null; insn = insn.getNext())
            if (insn instanceof MethodInsnNode && hookName.equals(((MethodInsnNode) insn).name)) found = true;
        Assert.assertTrue("hook missing for " + owner + "." + methodName, found);
    }

    private static void verify(byte[] bytes) {
        StringWriter text = new StringWriter();
        CheckClassAdapter.verify(new ClassReader(bytes), false, new PrintWriter(text));
        Assert.assertFalse("transformed class failed ASM verification: " + text, text.toString().contains("AnalyzerException"));
    }
}
