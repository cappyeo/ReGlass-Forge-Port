#version 120

uniform sampler2D Scene;
uniform vec2 TexelStep;
uniform float BlurRadius;

varying vec2 texCoord;

void main() {
    // A bounded Gaussian keeps this shader valid on the old GLSL compiler and
    // avoids dynamic array indexing or uniform blocks.
    float radius = clamp(floor(BlurRadius + 0.5), 0.0, 12.0);
    float sigma = max(0.5, radius * 0.5);
    float sigma2 = 2.0 * sigma * sigma;
    vec4 sum = texture2D(Scene, texCoord);
    float total = 1.0;
    float weight;
    vec2 offset;

    for (int i = 1; i <= 12; ++i) {
        if (float(i) > radius) break;
        weight = exp(-float(i * i) / sigma2);
        offset = TexelStep * float(i);
        sum += texture2D(Scene, texCoord + offset) * weight;
        sum += texture2D(Scene, texCoord - offset) * weight;
        total += 2.0 * weight;
    }

    gl_FragColor = sum / max(total, 0.0001);
}
