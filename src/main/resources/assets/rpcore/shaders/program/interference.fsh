#version 150

// RP Core "Interference": a camcorder in a radiation zone. Grain, white specks, line jitter, torn bands,
// colour fringing, a rolling bar and scanlines; everything scales with Strength (0..1).

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform float Time;
uniform float Strength;

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(443.897, 441.423));
    p += dot(p, p.yx + 19.19);
    return fract((p.x + p.y) * p.x);
}

void main() {
    float s = clamp(Strength, 0.0, 1.0);
    float t = Time;
    vec2 uv = texCoord;

    // Line jitter: every 2-pixel row shifts a little each frame.
    float row = floor(uv.y * OutSize.y / 2.0);
    uv.x += (hash(vec2(row, floor(t * 30.0))) - 0.5) * 0.006 * s;

    // Torn bands: some of 24 horizontal bands jump sideways for a moment.
    float bandId = floor(uv.y * 24.0);
    float tick = floor(t * 11.0);
    float torn = step(1.0 - 0.4 * s * s, hash(vec2(bandId, tick)));
    uv.x += torn * (hash(vec2(bandId, tick + 7.0)) - 0.5) * 0.12 * s;

    // Colour fringing.
    float ca = (0.0015 + 0.004 * torn) * s;
    vec3 col;
    col.r = texture(DiffuseSampler, uv + vec2(ca, 0.0)).r;
    col.g = texture(DiffuseSampler, uv).g;
    col.b = texture(DiffuseSampler, uv - vec2(ca, 0.0)).b;

    // Grain.
    float n = hash(floor(uv * OutSize) + vec2(floor(t * 60.0) * 13.0, floor(t * 60.0) * 7.0));
    col = mix(col, vec3(n), 0.08 * s + 0.32 * s * s);

    // White specks.
    float speck = step(1.0 - 0.015 * s * s, hash(floor(uv * OutSize / 2.0) + vec2(floor(t * 24.0), 3.1)));
    col += speck * 0.75;

    // Rolling bar.
    float roll = fract(t * 0.21);
    float bar = 1.0 - smoothstep(0.0, 0.05, abs(uv.y - roll));
    col += bar * 0.12 * s;

    // Scanlines and desaturation.
    col *= 1.0 - 0.14 * s * (0.5 + 0.5 * sin(uv.y * OutSize.y * 3.14159));
    float l = dot(col, vec3(0.299, 0.587, 0.114));
    col = mix(col, vec3(l), 0.45 * s);

    fragColor = vec4(col, 1.0);
}
