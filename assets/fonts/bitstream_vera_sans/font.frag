#version 120
#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_texture;

varying vec4 v_color;
varying vec2 v_texCoord;
uniform float outlineDistance = 0.2; // Between 0 and 0.5, 0 = thick outline, 0.5 = no outline
uniform vec4 outlineColor = vec4(255, 255, 255, 1);
uniform float scale = 1.0f;

void main() {
    float smoothing = 0.25f / (5.5f * scale);
    float distance = texture2D(u_texture, v_texCoord).a;
    float outlineFactor = smoothstep(0.5 - smoothing, 0.5 + smoothing, distance);
    vec4 color = mix(outlineColor, v_color, outlineFactor);
    float alpha = smoothstep(outlineDistance - smoothing, outlineDistance + smoothing, distance);
    gl_FragColor = vec4(color.rgb, color.a * alpha);
}