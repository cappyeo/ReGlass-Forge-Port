package restudio.reglass.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import restudio.reglass.client.api.ReGlassConfig;

/** Small Gson adapter that keeps the public ReGlassConfig shape stable across ports. */
public final class ReGlassSettingsIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LogManager.getLogger("ReGlass/Config");
    private static File directory;

    private ReGlassSettingsIO() { }
    public static void setConfigDirectory(File value) { directory = value; }
    private static File file() { return new File(directory == null ? new File("config") : directory, "reglass.json"); }

    public static void loadIntoMemory() {
        File f = file();
        if (!f.isFile()) { saveFromMemory(); return; }
        try (Reader reader = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8)) {
            ReGlassConfig loaded = GSON.fromJson(reader, ReGlassConfig.class);
            if (loaded != null) { sanitize(loaded); copyObject(loaded, ReGlassConfig.INSTANCE); }
        } catch (Exception error) { LOGGER.warn("Could not read reglass.json; using defaults", error); }
    }

    public static void saveFromMemory() {
        File f = file();
        try {
            File parent = f.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            Path destination = f.toPath();
            Path temporary = destination.resolveSibling("reglass.json.tmp");
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(ReGlassConfig.INSTANCE, writer);
            }
            try { Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (Exception ignored) { Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING); }
        } catch (Exception error) { LOGGER.warn("Could not write reglass.json", error); }
    }

    private static void sanitize(Object object) throws IllegalAccessException {
        for (Field field : object.getClass().getFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers)) continue;
            Object value = field.get(object);
            if (value instanceof Float && !Float.isFinite((Float) value)) field.set(object, 0.0f);
            else if (value instanceof Double && !Double.isFinite((Double) value)) field.set(object, 0.0d);
            else if (value != null && !isSimple(value.getClass()) && !(value instanceof Set)) sanitize(value);
        }
    }

    private static void copyObject(Object from, Object to) throws IllegalAccessException {
        for (Field field : from.getClass().getFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers)) continue;
            if (Modifier.isFinal(modifiers)) {
                    Object source = field.get(from), target = field.get(to);
                    if (source instanceof Set && target instanceof Set) {
                        ((Set) target).clear();
                        ((Set) target).addAll((Set) source);
                    } else if (source != null && target != null && !isSimple(source.getClass())) copyObject(source, target);
                continue;
            }
            Object value = field.get(from);
            if (value != null && !isSimple(value.getClass())) {
                Object target = field.get(to);
                if (target != null) copyObject(value, target);
            } else field.set(to, value);
        }
    }
    private static boolean isSimple(Class<?> type) {
        return type.isPrimitive() || type == String.class || Number.class.isAssignableFrom(type)
                || type == Boolean.class || type.isEnum();
    }
}
