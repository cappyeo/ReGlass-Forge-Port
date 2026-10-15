package restudio.reglass.client.api;

import org.junit.Before;
import org.junit.Test;
import restudio.reglass.client.runtime.ReGlassAnim;
import static org.junit.Assert.*;

public class WidgetStyleTest {
    @Before public void initializeAnimationDefaults() {
        ReGlassAnim.INSTANCE.update(ReGlassConfig.INSTANCE, 100);
    }

    @Test public void changingOneOpticalValuePreservesOtherDefaults() {
        WidgetStyle style = WidgetStyle.create().refractionThickness(5).glareAngleRad(1);
        assertEquals(5, style.getRefThickness(), 0);
        assertEquals(ReGlassConfig.INSTANCE.defaultRefFactor, style.getRefFactor(), 0.001f);
        assertEquals(ReGlassConfig.INSTANCE.defaultRefFresnelFactor, style.getRefFresnelFactor(), 0.001f);
        assertEquals(1, style.getGlareAngleRad(), 0);
        assertEquals(ReGlassConfig.INSTANCE.defaultGlareFactor, style.getGlareFactor(), 0.001f);
    }

    @Test public void shadowGeometryDoesNotEraseDefaultColorOpacity() {
        WidgetStyle style = WidgetStyle.create().shadow(10, .2f, 0, 2);
        assertEquals(ReGlassConfig.INSTANCE.defaultShadowColorAlpha, style.getShadowColorAlpha(), 0);
        assertEquals(10, style.getShadowExpand(), 0);
        WidgetStyle colored = WidgetStyle.create().shadowColor(0xFF0000, .5f);
        assertEquals(ReGlassConfig.INSTANCE.defaultShadowFactor, colored.getShadowFactor(), .001f);
    }

    @Test public void zeroBlurIsSupportedAndKernelInputIsBounded() {
        assertEquals(0, WidgetStyle.create().blurRadius(0).getBlurRadius());
        assertEquals(0, WidgetStyle.create().blurRadius(-20).getBlurRadius());
        assertEquals(64, WidgetStyle.create().blurRadius(Integer.MAX_VALUE).getBlurRadius());
    }
}
