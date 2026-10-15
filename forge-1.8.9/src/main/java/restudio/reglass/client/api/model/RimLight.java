package restudio.reglass.client.api.model;

/** Java 8 counterpart of the upstream rim-light record; no JOML dependency. */
public final class RimLight {
    public static final RimLight DEFAULT = new RimLight(-1.0f, 1.0f, 0xFFFFFF, 0.1f);
    public final float directionX;
    public final float directionY;
    public final int color;
    public final float intensity;

    public RimLight(float x, float y, int color, float intensity) {
        float length = (float) Math.sqrt(x * x + y * y);
        directionX = length > 0 ? x / length : 0;
        directionY = length > 0 ? y / length : 1;
        this.color = color;
        this.intensity = intensity;
    }
}
