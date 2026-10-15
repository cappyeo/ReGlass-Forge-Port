package restudio.reglass.client.api;

import restudio.reglass.client.runtime.ReGlassAnim;

public class WidgetStyle {

    private boolean hasTint;
    private int tintColor;
    private float tintAlpha;

    private boolean hasSmoothing;
    private float smoothingFactor;

    private boolean hasBlurRadius;
    private int blurRadius;

    private boolean hasLayer;
    private int layer;

    private boolean hasFadeKey;
    private long fadeKey;

    private boolean hasShadow;
    private boolean hasShadowColor;
    private float shadowExpand;
    private float shadowFactor;
    private float shadowOffsetX;
    private float shadowOffsetY;
    private int shadowColor;
    private float shadowColorAlpha;


    private boolean hasRefThickness;
    private float refThickness;
    private boolean hasRefFactor;
    private float refFactor;
    private boolean hasRefDispersion;
    private float refDispersion;
    private boolean hasRefFresnelRange;
    private float refFresnelRange;
    private boolean hasRefFresnelHardness;
    private float refFresnelHardness;
    private boolean hasRefFresnelFactor;
    private float refFresnelFactor;


    private boolean hasGlareRange;
    private float glareRange;
    private boolean hasGlareHardness;
    private float glareHardness;
    private boolean hasGlareConvergence;
    private float glareConvergence;
    private boolean hasGlareOppositeFactor;
    private float glareOppositeFactor;
    private boolean hasGlareFactor;
    private float glareFactor;
    private boolean hasGlareAngleRad;
    private float glareAngleRad;

    public static WidgetStyle create() { return new WidgetStyle(); }

    public WidgetStyle tint(int color, float alpha) { this.hasTint = true; this.tintColor = color; this.tintAlpha = alpha; return this; }
    /** Metadata for upstream batching; legacy panels do not perform smooth unions. */
    public WidgetStyle smoothing(float factor) { this.hasSmoothing = true; this.smoothingFactor = factor; return this; }
    /** Metadata only; immediate rendering uses call order for stacking. */
    public WidgetStyle layer(int v) { this.hasLayer = true; this.layer = v; return this; }
    /** Stable identity for callers; automatic upstream batch fading is not applied. */
    public WidgetStyle fadeKey(long key) { this.hasFadeKey = true; this.fadeKey = key; return this; }
    public WidgetStyle blurRadius(int radius) { this.hasBlurRadius = true; this.blurRadius = Math.max(0, Math.min(64, radius)); return this; }
    public WidgetStyle shadow(float expand, float factor, float offsetX, float offsetY) { this.hasShadow = true; this.shadowExpand = expand; this.shadowFactor = factor; this.shadowOffsetX = offsetX; this.shadowOffsetY = offsetY; return this; }
    public WidgetStyle shadowColor(int color, float alpha) { this.hasShadowColor = true; this.shadowColor = color; this.shadowColorAlpha = alpha; return this; }
    public WidgetStyle refractionThickness(float v) { this.hasRefThickness = true; this.refThickness = v; return this; }
    public WidgetStyle refractionFactor(float v) { this.hasRefFactor = true; this.refFactor = v; return this; }
    public WidgetStyle refractionDispersion(float v) { this.hasRefDispersion = true; this.refDispersion = v; return this; }
    public WidgetStyle fresnelRange(float v) { this.hasRefFresnelRange = true; this.refFresnelRange = v; return this; }
    public WidgetStyle fresnelHardness(float v) { this.hasRefFresnelHardness = true; this.refFresnelHardness = v; return this; }
    public WidgetStyle fresnelFactor(float v) { this.hasRefFresnelFactor = true; this.refFresnelFactor = v; return this; }
    public WidgetStyle glareRange(float v) { this.hasGlareRange = true; this.glareRange = v; return this; }
    public WidgetStyle glareHardness(float v) { this.hasGlareHardness = true; this.glareHardness = v; return this; }
    public WidgetStyle glareConvergence(float v) { this.hasGlareConvergence = true; this.glareConvergence = v; return this; }
    public WidgetStyle glareOppositeFactor(float v) { this.hasGlareOppositeFactor = true; this.glareOppositeFactor = v; return this; }
    public WidgetStyle glareFactor(float v) { this.hasGlareFactor = true; this.glareFactor = v; return this; }
    public WidgetStyle glareAngleRad(float v) { this.hasGlareAngleRad = true; this.glareAngleRad = v; return this; }

    public int getTintColor() { return hasTint ? tintColor : ReGlassConfig.INSTANCE.defaultTintColor; }
    public float getTintAlpha() { return hasTint ? tintAlpha : ReGlassAnim.INSTANCE.tintAlpha(); }

    public float getSmoothing() { return hasSmoothing ? smoothingFactor : ReGlassAnim.INSTANCE.smoothing(); }
    public int getLayer() { return hasLayer ? layer : ReGlassConfig.INSTANCE.defaultLayer; }
    public boolean hasFadeKey() { return hasFadeKey; }
    public long getFadeKey() { return fadeKey; }
    public int getBlurRadius() { return hasBlurRadius ? blurRadius : ReGlassAnim.INSTANCE.blurRadiusInt(); }

    public float getShadowExpand() { return hasShadow ? shadowExpand : ReGlassAnim.INSTANCE.shadowExpand(); }
    public float getShadowFactor() { return hasShadow ? shadowFactor : ReGlassAnim.INSTANCE.shadowFactor(); }
    public float getShadowOffsetX() { return hasShadow ? shadowOffsetX : ReGlassAnim.INSTANCE.shadowOffsetX(); }
    public float getShadowOffsetY() { return hasShadow ? shadowOffsetY : ReGlassAnim.INSTANCE.shadowOffsetY(); }
    public int getShadowColor() { return hasShadowColor ? shadowColor : ReGlassConfig.INSTANCE.defaultShadowColor; }
    public float getShadowColorAlpha() { return hasShadowColor ? shadowColorAlpha : ReGlassConfig.INSTANCE.defaultShadowColorAlpha; }

    public float getRefThickness() { return hasRefThickness ? refThickness : ReGlassAnim.INSTANCE.refThickness(); }
    public float getRefFactor() { return hasRefFactor ? refFactor : ReGlassAnim.INSTANCE.refFactor(); }
    public float getRefDispersion() { return hasRefDispersion ? refDispersion : ReGlassAnim.INSTANCE.refDispersion(); }
    public float getRefFresnelRange() { return hasRefFresnelRange ? refFresnelRange : ReGlassAnim.INSTANCE.refFresnelRange(); }
    public float getRefFresnelHardness() { return hasRefFresnelHardness ? refFresnelHardness : ReGlassAnim.INSTANCE.refFresnelHardness(); }
    public float getRefFresnelFactor() { return hasRefFresnelFactor ? refFresnelFactor : ReGlassAnim.INSTANCE.refFresnelFactor(); }

    public float getGlareRange() { return hasGlareRange ? glareRange : ReGlassAnim.INSTANCE.glareRange(); }
    public float getGlareHardness() { return hasGlareHardness ? glareHardness : ReGlassAnim.INSTANCE.glareHardness(); }
    public float getGlareConvergence() { return hasGlareConvergence ? glareConvergence : ReGlassAnim.INSTANCE.glareConvergence(); }
    public float getGlareOppositeFactor() { return hasGlareOppositeFactor ? glareOppositeFactor : ReGlassAnim.INSTANCE.glareOppositeFactor(); }
    public float getGlareFactor() { return hasGlareFactor ? glareFactor : ReGlassAnim.INSTANCE.glareFactor(); }
    public float getGlareAngleRad() { return hasGlareAngleRad ? glareAngleRad : ReGlassAnim.INSTANCE.glareAngleRad(); }
}
