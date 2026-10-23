#version 150

// RP Core "Interference": a camera sensor near a radiation source. Particle hits light up single pixels and short
// streaks at random places every frame: different sizes and brightness, greyish, no grid. Light grain under them.
// Everything scales with Strength (0..1). Sizes are in "720p pixels", so they look the same at any resolution.

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform float Clock;
uniform float Strength;

in vec2 texCoord;
out vec4 fragColor;

float h1(vec2 p) {
    vec3 q = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    q += dot(q, q.yzx + 33.33);
    return fract((q.x + q.y) * q.z);
}

vec2 h2(vec2 p) {
    return vec2(h1(p), h1(p + vec2(57.3, 11.9)));
}

// One layer of hits: at most one hit per cell per frame; the hit can spill into neighbour cells.
float hits(vec2 u, float cell, float frame, float prob, float seed) {
    vec2 c = floor(u / cell);
    float best = 0.0;
    for (int dy = -1; dy <= 1; dy++) {
        for (int dx = -1; dx <= 1; dx++) {
            vec2 cc = c + vec2(float(dx), float(dy));
            vec2 key = cc + vec2(frame * 7.13 + seed, frame * 3.71 - seed * 1.7);
            if (h1(key) > prob) continue;
            vec2 center = (cc + h2(key + 11.0)) * cell;
            float r = 0.7 + 2.3 * pow(h1(key + 5.0), 2.5);
            float len = h1(key + 9.0) < 0.4 ? 3.0 + 10.0 * h1(key + 13.0) : 0.0;
            float ang = h1(key + 17.0) * 6.2831;
            vec2 dir = vec2(cos(ang), sin(ang));
            vec2 d = u - center;
            float t = clamp(dot(d, dir), -len * 0.5, len * 0.5);
            float dist = length(d - dir * t);
            float v = 1.0 - smoothstep(r * 0.45, r, dist);
            float bright = 0.3 + 0.7 * h1(key + 21.0);
            best = max(best, v * bright);
        }
    }
    return best;
}

void main() {
    float s = clamp(Strength, 0.0, 1.0);
    vec2 u = texCoord * OutSize * (720.0 / max(OutSize.y, 1.0));
    vec3 col = texture(DiffuseSampler, texCoord).rgb;
    float frame = floor(Clock * 24.0);

    float prob = 0.006 + 0.09 * s * s;
    float hit = hits(u, 13.0, frame, prob, 0.0);
    hit = max(hit, 0.8 * hits(u + vec2(6.1, 3.7), 29.0, frame, prob * 0.6, 3.1));

    // Grain and a little washed-out sensor.
    float g = h1(floor(u) + frame * 13.1) - 0.5;
    col += g * (0.03 + 0.07 * s) * s;
    float l = dot(col, vec3(0.299, 0.587, 0.114));
    col = mix(col, vec3(l), 0.3 * s);

    vec3 hitCol = vec3(0.74, 0.76, 0.78) * (0.85 + 0.15 * h1(floor(u * 0.5) + frame));
    col = mix(col, max(col, hitCol), hit);

    fragColor = vec4(col, 1.0);
}
