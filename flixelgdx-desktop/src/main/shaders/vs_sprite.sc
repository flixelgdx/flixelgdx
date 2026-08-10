$input a_position, a_texcoord0, a_color0
$output v_texcoord0, v_color0

// The sprite vertex shader.
//
// Every quad corner is already transformed on the CPU by the batch, so the only work left here is
// applying the combined view-projection matrix (bgfx exposes it as u_modelViewProj) to move the
// world-space corner into clip space. The texture coordinate and tint color pass straight through
// to the fragment shader.

#include <bgfx_shader.sh>

void main()
{
	gl_Position = mul(u_modelViewProj, vec4(a_position.xy, 0.0, 1.0));
	v_texcoord0 = a_texcoord0;
	v_color0 = a_color0;
}
