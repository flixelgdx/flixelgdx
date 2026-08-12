$input v_texcoord0, v_color0

// The sprite fragment shader.
//
// Samples the bound texture and multiplies it by the interpolated vertex tint. Because the batch
// packs the tint (including alpha) into each vertex, tinting and fading sprites needs no extra
// uniforms; the color simply modulates the sampled texel.

#include <bgfx_shader.sh>

SAMPLER2D(s_texture, 0);

void main()
{
	gl_FragColor = texture2D(s_texture, v_texcoord0) * v_color0;
}
