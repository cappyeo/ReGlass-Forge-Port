package restudio.reglass.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Tiny, idempotent ASM hooks for the 1.8.9 GUI methods. Names are accepted in
 * both ForgeGradle development mappings and the production SRG/obfuscated jar.
 */
public final class ReGlassClassTransformer implements IClassTransformer {
    private static final String HOOKS = "restudio/reglass/client/ClientHooks";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        String className = transformedName == null ? name : transformedName;
        if (basicClass == null || className == null) return basicClass;
        if (is(className, "net.minecraft.client.gui.GuiButton"))
            return transformButton(name, basicClass);
        if (is(className, "net.minecraft.client.gui.GuiScreen"))
            return transformDarkening(name, basicClass);
        if (is(className, "net.minecraft.client.gui.inventory.GuiContainer"))
            return transformContainer(name, basicClass);
        if (is(className, "net.minecraft.client.gui.GuiIngame"))
            return transformHotbar(name, basicClass);
        return basicClass;
    }

    private static boolean is(String actual, String deobf) {
        return deobf.equals(actual) || deobf.replace('.', '/').equals(actual)
                || ("net.minecraft.client.gui.GuiButton".equals(deobf) && "avs".equals(actual))
                || ("net.minecraft.client.gui.GuiScreen".equals(deobf) && "axu".equals(actual))
                || ("net.minecraft.client.gui.inventory.GuiContainer".equals(deobf) && "ayl".equals(actual))
                || ("net.minecraft.client.gui.GuiIngame".equals(deobf) && "avo".equals(actual));
    }

    private static byte[] transformButton(String runtimeName, byte[] bytes) {
        ClassNode node = read(bytes);
        String owner = node.name;
        for (MethodNode method : node.methods) {
            if (!isVoidButtonMethod(method)) continue;
            if (already(method, "drawButton")) return bytes;
            InsnList hook = new InsnList();
            LabelNode vanilla = new LabelNode();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
            hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
            hook.add(new VarInsnNode(Opcodes.ILOAD, 2));
            hook.add(new VarInsnNode(Opcodes.ILOAD, 3));
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "drawButton",
                    "(" + Type.getObjectType(owner).getDescriptor() + method.desc.substring(1, method.desc.indexOf(')')) + ")Z", false));
            hook.add(new JumpInsnNode(Opcodes.IFEQ, vanilla));
            hook.add(new InsnNode(Opcodes.RETURN));
            hook.add(vanilla);
            hook.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
            method.instructions.insert(hook);
            return write(node);
        }
        return bytes;
    }

    private static byte[] transformDarkening(String runtimeName, byte[] bytes) {
        ClassNode node = read(bytes);
        for (MethodNode method : node.methods) {
            if (!isNoArgVoid(method, "drawDefaultBackground", "func_146276_q_")) continue;
            if (already(method, "cancelScreenDarkening")) return bytes;
            InsnList hook = new InsnList();
            LabelNode vanilla = new LabelNode();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "shouldCancelScreenDarkening",
                    "(" + Type.getObjectType(node.name).getDescriptor() + ")Z", false));
            hook.add(new JumpInsnNode(Opcodes.IFEQ, vanilla));
            hook.add(new InsnNode(Opcodes.RETURN));
            hook.add(vanilla);
            hook.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
            method.instructions.insert(hook);
            return write(node);
        }
        return bytes;
    }

    private static byte[] transformContainer(String runtimeName, byte[] bytes) {
        ClassNode node = read(bytes);
        String owner = node.name;
        for (MethodNode method : node.methods) {
            if (!isScreenDraw(method)) continue;
            if (already(method, "drawContainerPanel")) return bytes;
            for (org.objectweb.asm.tree.AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) insn;
                if (!("(F)V".equals(call.desc) || "(FII)V".equals(call.desc)) || !("drawGuiContainerBackgroundLayer".equals(call.name)
                        || "func_146976_a".equals(call.name) || "a".equals(call.name))) continue;
                InsnList hook = new InsnList();
                hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
                hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "drawContainerPanel",
                        "(" + Type.getObjectType(owner).getDescriptor() + ")V", false));
                method.instructions.insert(insn, hook);
                return write(node);
            }
        }
        return bytes;
    }

    private static byte[] transformHotbar(String runtimeName, byte[] bytes) {
        ClassNode node = read(bytes);
        String owner = node.name;
        for (MethodNode method : node.methods) {
            if (!isHotbarMethod(method)) continue;
            if (already(method, "beginHotbarRender")) return bytes;
            InsnList begin = new InsnList();
            begin.add(new VarInsnNode(Opcodes.ALOAD, 0));
            begin.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "beginHotbarRender",
                    "(" + Type.getObjectType(owner).getDescriptor() + ")V", false));
            method.instructions.insert(begin);
            int replaced = 0;
            for (org.objectweb.asm.tree.AbstractInsnNode insn = method.instructions.getFirst(); insn != null && replaced < 2; insn = insn.getNext()) {
                if (!(insn instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) insn;
                if (!"(IIIIII)V".equals(call.desc) || !("drawTexturedModalRect".equals(call.name)
                        || "func_73729_b".equals(call.name) || "b".equals(call.name))) continue;
                // Mutate in place: InsnList.set detaches the old node and loses getNext().
                call.setOpcode(Opcodes.INVOKESTATIC);
                call.owner = HOOKS;
                call.name = "drawHotbarTexture";
                call.desc = "(" + Type.getObjectType(owner).getDescriptor() + "IIIIII)V";
                call.itf = false;
                replaced++;
            }
            return replaced == 2 ? write(node) : bytes;
        }
        return bytes;
    }

    private static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }
    private static byte[] write(ClassNode node) {
        // Preserve the game's frames. Recomputing them loads game classes while
        // LaunchClassLoader is still transforming them, before deobfuscation.
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }
    private static boolean isVoidButtonMethod(MethodNode m) {
        return m.desc.startsWith("(L") && m.desc.endsWith(";II)V")
                && ("drawButton".equals(m.name) || "func_146112_a".equals(m.name) || "a".equals(m.name));
    }
    private static boolean isNoArgVoid(MethodNode m, String first, String second) {
        return "()V".equals(m.desc) && (first.equals(m.name) || second.equals(m.name)
                || "c".equals(m.name));
    }
    private static boolean isScreenDraw(MethodNode m) {
        return "(IIF)V".equals(m.desc) && ("drawScreen".equals(m.name) || "func_73863_a".equals(m.name) || "a".equals(m.name));
    }
    private static boolean isHotbarMethod(MethodNode m) {
        return m.desc.startsWith("(L") && m.desc.endsWith(";F)V")
                && ("renderTooltip".equals(m.name) || "func_180479_a".equals(m.name) || "a".equals(m.name));
    }
    private static boolean already(MethodNode m, String token) {
        for (org.objectweb.asm.tree.AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext())
            if (insn instanceof MethodInsnNode && (token.equals(((MethodInsnNode) insn).name)
                    || ("cancelScreenDarkening".equals(token) && "shouldCancelScreenDarkening".equals(((MethodInsnNode) insn).name)))) return true;
        return false;
    }
}
