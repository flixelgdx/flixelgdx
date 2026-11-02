// The built-in sprite shader: the texel times the vertex tint. Premultiplication is left to the blend mode.

@fragment
fn fs_main(in: FlixelVertexOutput) -> @location(0) vec4f {
  return textureSample(flixelTexture, flixelSampler, in.uv) * in.color;
}
