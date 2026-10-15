#version 120

uniform sampler2D Scene;
uniform sampler2D Blur;

// ScreenSize is the framebuffer size in pixels. GuiSize and Rect use the
// scaled GUI coordinate system; Rect is x, y, width, height with y at top.
uniform vec2 ScreenSize;
uniform vec2 GuiSize;
uniform vec4 Rect;
uniform float Radius;
uniform float Time;
uniform float Hover;
uniform float Focus;
uniform float BlurMix;
uniform vec4 Tint;
uniform vec4 ShadowColor;
uniform vec4 Shadow;       // expand, factor, offsetX, offsetY (GUI units)
uniform vec4 Refraction;   // thickness, factor, dispersion, fresnel range
uniform vec2 Fresnel;      // hardness, factor
uniform vec4 Glare;        // range, hardness, convergence, opposite factor
uniform vec2 GlareLight;   // factor, angle in radians
uniform float HoverScale;
uniform float FocusScale;
uniform float FocusBorderWidth;
uniform float FocusBorderIntensity;
uniform float FocusBorderSpeed;
// PixelGrid.x is the GUI pixel size. A value <= 0 disables quantization.
uniform float PixelGrid;

varying vec2 texCoord;

float safeDiv(float n, float d) {
    return n / max(abs(d), 0.0001);
}

float roundedBox(vec2 p, vec2 halfSize, float radius) {
    vec2 q = abs(p) - halfSize + radius;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}

vec2 roundedNormal(vec2 p, vec2 halfSize, float radius) {
    vec2 ap = abs(p);
    vec2 q = ap - halfSize + radius;
    vec2 n;
    if (max(q.x, q.y) <= 0.0) {
        n = (q.x > q.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
    } else {
        n = normalize(max(q, 0.0));
    }
    n *= vec2(p.x < 0.0 ? -1.0 : 1.0, p.y < 0.0 ? -1.0 : 1.0);
    return normalize(n);
}

vec2 sceneUv(vec2 pixel) {
    return pixel / max(ScreenSize, vec2(1.0));
}

vec3 sampleRefracted(vec2 uv, vec2 normal, float amount, float dispersion) {
    vec2 aspect = vec2(safeDiv(ScreenSize.y, ScreenSize.x), 1.0);
    vec2 delta = normal * amount * aspect;
    float red = texture2D(Blur, uv + delta * (1.0 + dispersion)).r;
    float green = texture2D(Blur, uv + delta).g;
    float blue = texture2D(Blur, uv + delta * (1.0 - dispersion)).b;
    return vec3(red, green, blue);
}

void main() {
    vec2 screen = max(ScreenSize, vec2(1.0));
    vec2 guiSize = max(GuiSize, vec2(1.0));
    vec2 gui = vec2(gl_FragCoord.x / screen.x * guiSize.x,
                    (1.0 - gl_FragCoord.y / screen.y) * guiSize.y);
    vec2 center = Rect.xy + Rect.zw * 0.5;
    vec2 halfSize = max(Rect.zw * 0.5, vec2(0.001));
    float scale = clamp(Hover, 0.0, 1.0) * HoverScale
                + clamp(Focus, 0.0, 1.0) * FocusScale;
    halfSize += vec2(scale);
    float radius = clamp(Radius, 0.0, min(halfSize.x, halfSize.y));
    vec2 local = gui - center;
    float sdf = roundedBox(local, halfSize, radius);
    vec2 normal = roundedNormal(local, halfSize, radius);

    vec2 uv = sceneUv(gl_FragCoord.xy);
    vec2 samplePixel = gl_FragCoord.xy;
    float gridPixels = PixelGrid * screen.y / guiSize.y;
    if (gridPixels > 0.0) {
        samplePixel = (floor(samplePixel / gridPixels) + 0.5) * gridPixels;
    }
    uv = sceneUv(samplePixel);

    vec4 base = texture2D(Scene, uv);
    float aa = max(1.0, guiSize.y / screen.y * 1.5);

    // The shadow is evaluated outside the panel as well, so callers can draw
    // a quad expanded by Shadow.x and retain a soft edge.
    vec2 shadowLocal = local - Shadow.zw;
    float shadowDistance = roundedBox(shadowLocal, halfSize + vec2(max(Shadow.x, 0.0)), radius + max(Shadow.x, 0.0));
    float shadowAlpha = exp(-max(shadowDistance, 0.0) / max(Shadow.x, 0.001));
    shadowAlpha *= clamp(Shadow.y, 0.0, 1.0) * ShadowColor.a;
    vec3 color = mix(base.rgb, ShadowColor.rgb, clamp(shadowAlpha, 0.0, 1.0));

    float inside = 1.0 - smoothstep(-aa, aa, sdf);
    if (inside > 0.0) {
        float edge = 1.0 - smoothstep(0.0, max(Refraction.w, 0.001), -sdf);
        float thickness = max(Refraction.x, 0.0);
        float ior = max(Refraction.y, 0.001);
        float refraction = thickness * (1.0 - edge) * (1.0 + 0.25 * (ior - 1.0));
        vec3 refracted = sampleRefracted(uv, normal, refraction / max(screen.y, 1.0), Refraction.z * 0.025);
        // Blur is bound to Scene when the style requests zero blur. Keep the
        // refracted sample active in that case so zero blur still has optics.
        vec3 backdrop = mix(refracted, base.rgb, clamp(1.0 - BlurMix, 0.0, 1.0) * 0.15);
        vec3 glass = mix(backdrop, Tint.rgb, clamp(Tint.a, 0.0, 1.0));

        float fresnelHardness = clamp(Fresnel.x * 0.01, 0.001, 1.0);
        float fresnelFactor = clamp(Fresnel.y * 0.01, 0.0, 1.0);
        float fresnelEdge = pow(clamp(edge, 0.0, 1.0), fresnelHardness);
        glass = mix(glass, mix(vec3(1.0), Tint.rgb, 0.5), fresnelEdge * fresnelFactor);

        float glareHardness = clamp(Glare.y * 0.01, 0.001, 1.0);
        float glareOpposite = clamp(Glare.w * 0.01, 0.0, 1.0);
        float glareFactor = clamp(GlareLight.x * 0.01, 0.0, 1.0);
        float glareEdge = 1.0 - smoothstep(0.0, max(Glare.x, 0.001), -sdf);
        glareEdge = pow(clamp(glareEdge, 0.0, 1.0), glareHardness);
        float lightAngle = atan(normal.y, normal.x);
        float angular = 0.5 + 0.5 * cos(lightAngle - GlareLight.y);
        angular = pow(clamp(angular, 0.0, 1.0), max(Glare.z, 0.001));
        float opposite = 0.5 + 0.5 * cos(lightAngle - GlareLight.y + 3.14159265);
        float glare = glareEdge * glareFactor * (angular + opposite * glareOpposite * 0.5);
        glass = mix(glass, vec3(1.0), clamp(glare, 0.0, 1.0));

        float pulse = 0.5 + 0.5 * sin(Time * FocusBorderSpeed);
        float borderWidth = max(FocusBorderWidth, 0.001);
        float border = 1.0 - smoothstep(0.0, borderWidth, abs(sdf));
        float focusGlow = border * clamp(Focus, 0.0, 1.0) * clamp(FocusBorderIntensity, 0.0, 1.0) * (0.75 + 0.25 * pulse);
        float hoverGlow = clamp(Hover, 0.0, 1.0) * 0.06 * (0.5 + 0.5 * sin(Time));
        glass = mix(glass, vec3(1.0), clamp(focusGlow + hoverGlow, 0.0, 1.0));
        color = mix(color, glass, inside);
    }

    // The caller draws an expanded quad. Outside the panel, emit only the
    // shadow coverage so the existing framebuffer remains visible beneath it.
    float shadowCoverage = clamp(shadowAlpha, 0.0, 1.0) * (1.0 - inside);
    float outputAlpha = clamp(inside + shadowCoverage, 0.0, 1.0);
    if (outputAlpha < 0.001) discard;
    vec3 composited = color * inside + ShadowColor.rgb * shadowCoverage;
    gl_FragColor = vec4(composited / max(outputAlpha, 0.001), outputAlpha);
}
