// Varying and attribute declarations shared by the sprite vertex and fragment shaders.
//
// This mirrors the vertex layout the desktop backend registers in FlixelBgfxGraphics:
// position (2 floats), texcoord0 (2 floats), and color0 (4 unsigned bytes, normalized).

vec2 v_texcoord0 : TEXCOORD0 = vec2(0.0, 0.0);
vec4 v_color0    : COLOR0    = vec4(1.0, 1.0, 1.0, 1.0);

vec2 a_position  : POSITION;
vec2 a_texcoord0 : TEXCOORD0;
vec4 a_color0    : COLOR0;
