package restudio.reglass.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.zip.ZipFile;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckClassAdapter;
import static org.junit.Assert.*;

/** Tests the exact obfuscated bytes that FML sees BEFORE its deobfuscation pass. */
public class ProductionMinecraftTransformTest {
    @Test public void hooksTheInstalledGamesActualClassAndMethodNames() throws Exception {
        File client = new File(System.getProperty("reglass.obfuscatedJar"));
        assertTrue("ForgeGradle must download the production Minecraft jar", client.isFile());
        String[][] targets = {
            {"avs", "net.minecraft.client.gui.GuiButton", "drawButton", "1"},
            {"axu", "net.minecraft.client.gui.GuiScreen", "shouldCancelScreenDarkening", "1"},
            {"ayl", "net.minecraft.client.gui.inventory.GuiContainer", "drawContainerPanel", "1"},
            {"avo", "net.minecraft.client.gui.GuiIngame", "drawHotbarTexture", "2"}
        };
        try (ZipFile jar = new ZipFile(client)) {
            for (String[] target : targets) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                assertNotNull("actual production class missing: " + target[0], jar.getEntry(target[0] + ".class"));
                try (InputStream in = jar.getInputStream(jar.getEntry(target[0] + ".class"))) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = in.read(buffer)) != -1) bytes.write(buffer, 0, count);
                }
                ReGlassClassTransformer transformer = new ReGlassClassTransformer();
                byte[] once = transformer.transform(target[0], target[1], bytes.toByteArray());
                ClassNode node = new ClassNode();
                new ClassReader(once).accept(node, 0);
                int hooks = 0;
                for (MethodNode method : node.methods) {
                    for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;
                            instruction = instruction.getNext()) {
                        if (instruction instanceof MethodInsnNode) {
                            MethodInsnNode call = (MethodInsnNode) instruction;
                            if ("restudio/reglass/client/ClientHooks".equals(call.owner) && target[2].equals(call.name)) hooks++;
                        }
                    }
                }
                assertEquals("production hook missing: " + target[1], Integer.parseInt(target[3]), hooks);
                // Structural verification without loading obfuscated game classes into the test JVM.
                new ClassReader(once).accept(new CheckClassAdapter(new ClassWriter(0), false), 0);
                assertArrayEquals("production hook is not idempotent", once,
                        transformer.transform(target[0], target[1], once));
            }
        }
    }
}
