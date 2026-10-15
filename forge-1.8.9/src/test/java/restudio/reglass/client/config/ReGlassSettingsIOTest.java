package restudio.reglass.client.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Assert;
import org.junit.After;
import org.junit.Test;
import restudio.reglass.client.api.ReGlassConfig;

public class ReGlassSettingsIOTest {
    private boolean originalRedesign;
    private int originalBlur;

    @org.junit.Before public void captureDefaults() {
        originalRedesign = ReGlassConfig.INSTANCE.features.enableRedesign;
        originalBlur = ReGlassConfig.INSTANCE.defaultBlurRadius;
    }

    @After public void restoreDefaults() {
        ReGlassConfig.INSTANCE.features.enableRedesign = originalRedesign;
        ReGlassConfig.INSTANCE.defaultBlurRadius = originalBlur;
    }
    @Test public void roundTripDoesNotRecurseThroughStaticSingleton() throws Exception {
        Path dir = Files.createTempDirectory("reglass-config");
        ReGlassSettingsIO.setConfigDirectory(dir.toFile());
        ReGlassConfig.INSTANCE.features.enableRedesign = false;
        ReGlassConfig.INSTANCE.defaultBlurRadius = 17;
        ReGlassSettingsIO.saveFromMemory();
        ReGlassConfig.INSTANCE.features.enableRedesign = true;
        ReGlassConfig.INSTANCE.defaultBlurRadius = 1;
        ReGlassSettingsIO.loadIntoMemory();
        Assert.assertFalse(ReGlassConfig.INSTANCE.features.enableRedesign);
        Assert.assertEquals(17, ReGlassConfig.INSTANCE.defaultBlurRadius);
    }

    @Test public void missingOptionalFieldsKeepExistingFinalFeatures() throws Exception {
        Path dir = Files.createTempDirectory("reglass-config-missing");
        ReGlassSettingsIO.setConfigDirectory(dir.toFile());
        Files.write(Paths.get(dir.toString(), "reglass.json"), "{}".getBytes(StandardCharsets.UTF_8));
        ReGlassConfig.INSTANCE.features.enableRedesign = false;
        ReGlassSettingsIO.loadIntoMemory();
        Assert.assertNotNull(ReGlassConfig.INSTANCE.features);
    }
}
